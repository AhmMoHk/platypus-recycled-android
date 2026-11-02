package cs.UnityEngine;

public class AudioListener extends AudioBehaviour {
    static volatile float volume = 1;
    static volatile boolean paused;
    public static float get_volume() { return volume; }
    public static void set_volume(float v) { volume = Math.max(0, v); }
    public static boolean get_pause() { return paused; }
    public static void set_pause(boolean p) { paused = p; }
}
