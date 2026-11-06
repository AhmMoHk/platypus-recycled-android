package cs.UnityEngine;

/** A playing voice is mixed by AudioMix on the host's audio thread. */
public class AudioSource extends AudioBehaviour {
    AudioClip clip;
    volatile float volume = 1, pitch = 1, panStereo, spatialBlend;
    volatile boolean loop, mute, playOnAwake = true, ignoreListenerPause, ignoreListenerVolume;
    int priority = 128;
    boolean bypassEffects, bypassListenerEffects, bypassReverbZones;
    /** playback state (guarded by AudioMix.lock) */
    double pos;
    boolean playing, paused;
    AudioClip playingClip;
    final java.util.ArrayList<Object[]> oneShots = new java.util.ArrayList<>();   // {clip, posArr(double[]), volume}

    public AudioClip get_clip() { return clip; }
    public void set_clip(AudioClip c) { clip = c; }
    public float get_volume() { return volume; }
    public void set_volume(float v) { volume = Mathf.Clamp01(v); }
    public float get_pitch() { return pitch; }
    public void set_pitch(float p) { pitch = Math.max(-3, Math.min(3, p)); }
    public boolean get_loop() { return loop; }
    public void set_loop(boolean v) { loop = v; }
    public boolean get_mute() { return mute; }
    public void set_mute(boolean v) { mute = v; }
    public boolean get_playOnAwake() { return playOnAwake; }
    public void set_playOnAwake(boolean v) { playOnAwake = v; }
    public int get_priority() { return priority; }
    public void set_priority(int p) { priority = p; }
    public float get_spatialBlend() { return spatialBlend; }
    public void set_spatialBlend(float v) { spatialBlend = v; }
    public float get_panStereo() { return panStereo; }
    public void set_panStereo(float v) { panStereo = Math.max(-1, Math.min(1, v)); }
    public void set_bypassEffects(boolean v) { bypassEffects = v; }
    public void set_bypassListenerEffects(boolean v) { bypassListenerEffects = v; }
    public void set_bypassReverbZones(boolean v) { bypassReverbZones = v; }
    public boolean get_ignoreListenerPause() { return ignoreListenerPause; }
    public void set_ignoreListenerPause(boolean v) { ignoreListenerPause = v; }
    public boolean get_ignoreListenerVolume() { return ignoreListenerVolume; }
    public void set_ignoreListenerVolume(boolean v) { ignoreListenerVolume = v; }
    public void set_outputAudioMixerGroup(Object g) {}
    public void set_dopplerLevel(float v) {}
    public void set_rolloffMode(int m) {}
    public void set_minDistance(float v) {}
    public void set_maxDistance(float v) {}
    public void set_spread(float v) {}
    public void set_reverbZoneMix(float v) {}

    public boolean get_isPlaying() { synchronized (AudioMix.lock) { return playing && !paused; } }
    public float get_time() {
        synchronized (AudioMix.lock) {
            AudioClip c = playingClip != null ? playingClip : clip;
            return c == null || c.frequency == 0 ? 0 : (float) (pos / c.frequency);
        }
    }
    public void set_time(float t) {
        synchronized (AudioMix.lock) {
            AudioClip c = playingClip != null ? playingClip : clip;
            if (c != null) pos = Math.max(0, t * c.frequency);
        }
    }
    public int get_timeSamples() { synchronized (AudioMix.lock) { return (int) pos; } }
    public void set_timeSamples(int s) { synchronized (AudioMix.lock) { pos = s; } }

    StreamPlayer $stream;

    public void Play() {
        if (clip == null || !get_isActiveAndEnabled()) return;
        clip.$request();
        synchronized (AudioMix.lock) {
            if ($stream != null) { $stream.close(); $stream = null; }
            if (clip.$streamed()) $stream = new StreamPlayer(clip, loop);
            playingClip = clip;
            pos = pitch < 0 ? Math.max(0, clip.samples - 1) : 0;
            playing = true;
            paused = false;
            AudioMix.add(this);
        }
    }
    public void Play(long delay) { Play(); }
    public void PlayDelayed(float d) { Play(); }
    public void PlayScheduled(double t) { Play(); }
    public void Stop() { synchronized (AudioMix.lock) { playing = false; paused = false; pos = 0; oneShots.clear(); if ($stream != null) { $stream.close(); $stream = null; } } }
    public void Pause() { synchronized (AudioMix.lock) { if (playing) paused = true; } }
    public void UnPause() { synchronized (AudioMix.lock) { paused = false; } }
    public void PlayOneShot(AudioClip c) { PlayOneShot(c, 1f); }
    public void PlayOneShot(AudioClip c, float vol) {
        if (c == null || !get_isActiveAndEnabled()) return;
        c.$request();
        synchronized (AudioMix.lock) {
            oneShots.add(new Object[]{c, new double[]{0}, vol});
            AudioMix.add(this);
        }
    }
    public static void PlayClipAtPoint(AudioClip c, Vector3 p) { PlayClipAtPoint(c, p, 1f); }
    public static void PlayClipAtPoint(AudioClip c, Vector3 p, float vol) {
        GameObject g = new GameObject("One shot audio");
        AudioSource s = (AudioSource) g.AddComponent(cs.System.Type.of(AudioSource.class));
        s.clip = c;
        s.volume = vol;
        s.Play();
        UObject.Destroy(g, c == null ? 0 : c.get_length() + .1f);
    }

    @Override public void $nativeEnable() { if (playOnAwake && clip != null && !gameObject.$asset) Play(); }
    @Override public void $nativeDisable() { Stop(); }
    @Override public void $onDestroyNative() { synchronized (AudioMix.lock) { playing = false; oneShots.clear(); AudioMix.remove(this); } }

    @Override public void $load(java.util.HashMap<String, Object> d, Loader.Ctx ctx) {
        UObject c = ctx.ref(d.get("m_audioClip"));
        clip = c instanceof AudioClip ? (AudioClip) c : null;
        playOnAwake = Loader.b(d, "m_PlayOnAwake", true);
        volume = Loader.f(d, "m_Volume", 1);
        pitch = Loader.f(d, "m_Pitch", 1);
        loop = Loader.b(d, "Loop", false);
        mute = Loader.b(d, "Mute", false);
        priority = Loader.i(d, "Priority", 128);
        panStereo = Loader.f(d, "Pan2D", 0);
    }
    @Override public void $cloneFrom(Component src, Eng.CloneCtx ctx) {
        AudioSource s = (AudioSource) src;
        clip = s.clip; playOnAwake = s.playOnAwake; volume = s.volume; pitch = s.pitch; loop = s.loop; mute = s.mute; priority = s.priority;
        panStereo = s.panStereo; spatialBlend = s.spatialBlend;
    }
}
