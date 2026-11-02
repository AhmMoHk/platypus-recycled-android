package cs.UnityEngine;

import java.util.ArrayList;

/** Software mixer: the host's audio thread calls mix() to fill interleaved stereo 16-bit frames. */
public final class AudioMix {
    private AudioMix() {}
    static final Object lock = new Object();
    private static final ArrayList<AudioSource> active = new ArrayList<>();
    private static float[] acc = new float[0];

    static void add(AudioSource s) { if (!active.contains(s)) active.add(s); }
    static void remove(AudioSource s) { active.remove(s); }

    /** fill `frames` stereo frames at `rate` Hz */
    public static void mix(short[] out, int frames, int rate) {
        if (acc.length < frames * 2) acc = new float[frames * 2];
        java.util.Arrays.fill(acc, 0, frames * 2, 0f);
        float master = AudioListener.volume;
        boolean gpause = AudioListener.paused;
        synchronized (lock) {
            for (int i = active.size() - 1; i >= 0; i--) {
                AudioSource s = active.get(i);
                if (gpause && !s.ignoreListenerPause) continue;
                float vol = (s.mute ? 0 : s.volume) * (s.ignoreListenerVolume ? 1 : master);
                if (s.playing && !s.paused && s.playingClip != null) {
                    double[] p = {s.pos};
                    boolean more = render(s.playingClip, p, s.pitch, s.loop, vol, s.panStereo, frames, rate);
                    s.pos = p[0];
                    if (!more) s.playing = false;
                }
                for (int k = s.oneShots.size() - 1; k >= 0; k--) {
                    Object[] o = s.oneShots.get(k);
                    if (!render((AudioClip) o[0], (double[]) o[1], s.pitch, false, vol * (Float) o[2], s.panStereo, frames, rate)) s.oneShots.remove(k);
                }
                if (!s.playing && s.oneShots.isEmpty()) active.remove(i);
            }
        }
        for (int i = 0; i < frames * 2; i++) {
            float v = acc[i];
            if (v > 1) v = 1; else if (v < -1) v = -1;
            out[i] = (short) (v * 32767);
        }
    }

    /** linear-interpolated resampling; returns false when the clip ended */
    private static boolean render(AudioClip c, double[] posRef, float pitch, boolean loop, float vol, float pan, int frames, int rate) {
        short[] pcm = c.$pcm;
        if (pcm == null || c.samples == 0) return false;
        int ch = c.channels, n = c.samples;
        double step = pitch * c.frequency / (double) rate;
        double pos = posRef[0];
        float lv = vol * (pan > 0 ? 1 - pan : 1), rv = vol * (pan < 0 ? 1 + pan : 1);
        for (int f = 0; f < frames; f++) {
            if (pos >= n || pos < 0) {
                if (!loop) { posRef[0] = pos; return false; }
                pos = pos >= n ? pos - n : pos + n;
                if (pos >= n || pos < 0) pos = 0;
            }
            int i0 = (int) pos;
            int i1 = i0 + 1 >= n ? (loop ? 0 : i0) : i0 + 1;
            float t = (float) (pos - i0);
            float l, r;
            if (ch == 1) {
                l = r = (pcm[i0] + (pcm[i1] - pcm[i0]) * t) / 32768f;
            } else {
                l = (pcm[i0 * ch] + (pcm[i1 * ch] - pcm[i0 * ch]) * t) / 32768f;
                r = (pcm[i0 * ch + 1] + (pcm[i1 * ch + 1] - pcm[i0 * ch + 1]) * t) / 32768f;
            }
            acc[f * 2] += l * lv;
            acc[f * 2 + 1] += r * rv;
            pos += step;
        }
        posRef[0] = pos;
        return true;
    }
}
