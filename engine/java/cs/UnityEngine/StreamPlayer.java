package cs.UnityEngine;

/** Plays a streamed AudioClip: a background thread keeps ~2 s decoded ahead in a ring buffer, the mixer pulls. */
final class StreamPlayer implements Runnable {
    private final AudioClip clip;
    volatile boolean loop, closed;
    private AudioStream src;
    private short[] ring;
    private int head, size, ch = 2;              // in samples
    private volatile boolean ended;
    private final short[] chunk = new short[8192];
    private double frac = 1;
    private float pl, pr, cl, cr;
    private int rate = 44100;

    StreamPlayer(AudioClip c, boolean loop) {
        clip = c;
        this.loop = loop;
        Thread t = new Thread(this, "audio-stream");
        t.setDaemon(true);
        t.start();
    }

    public void run() {
        try {
            src = Host.get().openAudioStream(clip.$src);
            if (src == null) { ended = true; return; }
            synchronized (this) {
                ch = Math.max(1, src.channels());
                rate = Math.max(1, src.rate());
                ring = new short[rate * ch * 2];
            }
            while (!closed) {
                int room;
                synchronized (this) { room = ring.length - size; }
                if (room < chunk.length) { Thread.sleep(15); continue; }
                int n = src.read(chunk, chunk.length);
                if (n < 0) {
                    if (loop) { src.rewind(); continue; }
                    ended = true;
                    return;
                }
                if (n == 0) { Thread.sleep(5); continue; }
                synchronized (this) {
                    for (int i = 0; i < n; i++) ring[(head + size + i) % ring.length] = chunk[i];
                    size += n;
                }
            }
        } catch (Throwable e) {
            Debug.LogWarning("audio stream " + clip.$src + ": " + e);
            ended = true;
        } finally {
            if (src != null) try { src.close(); } catch (Throwable ignored) {}
        }
    }

    void close() { closed = true; }

    /** mix into acc (stereo); false when the clip ended */
    synchronized boolean render(float[] acc, int frames, int outRate, float vol, float pan, float pitch) {
        if (ring == null) return !ended;
        double step = Math.max(0.01, pitch) * rate / (double) outRate;
        float lv = vol * (pan > 0 ? 1 - pan : 1), rv = vol * (pan < 0 ? 1 + pan : 1);
        for (int f = 0; f < frames; f++) {
            while (frac >= 1) {
                if (size < ch) return !(ended && size < ch) || f > 0;   // underrun: silence until decoded
                pl = cl; pr = cr;
                cl = ring[head] / 32768f;
                cr = ch > 1 ? ring[(head + 1) % ring.length] / 32768f : cl;
                head = (head + ch) % ring.length;
                size -= ch;
                frac -= 1;
            }
            float t = (float) frac;
            acc[f * 2] += (pl + (cl - pl) * t) * lv;
            acc[f * 2 + 1] += (pr + (cr - pr) * t) * rv;
            frac += step;
        }
        return true;
    }
}
