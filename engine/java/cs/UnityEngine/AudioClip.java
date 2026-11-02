package cs.UnityEngine;

/** AudioDataLoadState: Unloaded 0, Loading 1, Loaded 2, Failed 3 */
public class AudioClip extends UObject {
    int channels = 1, frequency = 44100, samples;
    /** interleaved 16 bit pcm */
    public short[] $pcm;
    /** compressed source (path relative to the data root) or bytes */
    public String $src;
    public byte[] $bytes;
    int loadState;
    boolean stream, preload = true;

    public AudioClip() {}
    public static AudioClip Create(String name, int lengthSamples, int channels, int frequency, boolean stream) {
        AudioClip c = new AudioClip();
        c.m_Name = name;
        c.samples = lengthSamples;
        c.channels = channels;
        c.frequency = frequency;
        c.stream = stream;
        c.$pcm = new short[lengthSamples * channels];
        c.loadState = 2;
        return c;
    }
    public static AudioClip Create(String name, int len, int ch, int freq, boolean stream, AudioClip$PCMReaderCallback cb) { return Create(name, len, ch, freq, stream); }

    public float get_length() { ensure(); return frequency == 0 ? 0 : (float) samples / frequency; }
    public int get_samples() { ensure(); return samples; }
    public int get_channels() { ensure(); return channels; }
    public int get_frequency() { ensure(); return frequency; }
    public int get_loadState() { return loadState; }
    public int get_loadType() { return stream ? 2 : 0; }
    public boolean get_preloadAudioData() { return preload; }
    public boolean get_loadInBackground() { return false; }
    public boolean get_ambisonic() { return false; }
    public boolean LoadAudioData() { ensure(); return loadState == 2; }
    public boolean UnloadAudioData() { return true; }
    public boolean GetData(float[] data, int offset) {
        ensure();
        if ($pcm == null) return false;
        for (int i = 0; i < data.length && (offset * channels + i) < $pcm.length; i++) data[i] = $pcm[offset * channels + i] / 32768f;
        return true;
    }
    public boolean SetData(float[] data, int offset) {
        ensure();
        if ($pcm == null) return false;
        for (int i = 0; i < data.length && (offset * channels + i) < $pcm.length; i++) $pcm[offset * channels + i] = (short) Math.max(-32768, Math.min(32767, Math.round(data[i] * 32767f)));
        return true;
    }

    /** decode on first use */
    public synchronized void ensure() {
        if ($pcm != null || loadState == 3) return;
        byte[] b = $bytes != null ? $bytes : ($src != null ? Host.get().readData($src) : null);
        Object[] r = b == null ? null : Host.get().decodeAudio(b);
        if (r == null) {
            loadState = 3;
            $pcm = null;
            return;
        }
        channels = (Integer) r[0];
        frequency = (Integer) r[1];
        $pcm = (short[]) r[2];
        samples = $pcm.length / Math.max(1, channels);
        loadState = 2;
        $bytes = null;
    }
    @Override public void $loadAsset(java.util.HashMap<String, Object> d, Loader.Ctx ctx) {
        channels = Loader.i(d, "m_Channels", 1);
        frequency = Loader.i(d, "m_Frequency", 44100);
        float len = Loader.f(d, "m_Length", 0);
        samples = Math.round(len * frequency);
        Object src = d.get("_ogg");
        if (src instanceof String) $src = (String) src;
    }
}
