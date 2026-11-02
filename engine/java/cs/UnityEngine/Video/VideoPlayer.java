package cs.UnityEngine.Video;

/** Video playback is not available: clips report zero length so callers proceed immediately. */
public class VideoPlayer extends cs.UnityEngine.Behaviour {
    VideoClip clip;
    boolean prepared, playing, playOnAwake = true, looping;
    float speed = 1, cameraAlpha = 1;
    public void set_clip(VideoClip c) { clip = c; prepared = false; }
    public VideoClip get_clip() { return clip; }
    public void Prepare() { prepared = true; }
    public boolean get_isPrepared() { return prepared; }
    public void Play() { prepared = true; playing = false; }
    public void Pause() { playing = false; }
    public void Stop() { playing = false; }
    public boolean get_isPlaying() { return playing; }
    public boolean get_isLooping() { return looping; }
    public void set_isLooping(boolean v) { looping = v; }
    public long get_frame() { return 0; }
    public void set_frame(long f) {}
    public long get_frameCount() { return 0; }
    public double get_length() { return 0; }
    public double get_time() { return 0; }
    public void set_time(double t) {}
    public void set_playbackSpeed(float s) { speed = s; }
    public float get_playbackSpeed() { return speed; }
    public void set_targetCameraAlpha(float a) { cameraAlpha = a; }
    public void SetDirectAudioVolume(char track, float v) {}
    public void set_playOnAwake(boolean v) { playOnAwake = v; }
    public void set_url(String u) {}
    public String get_url() { return ""; }
}
