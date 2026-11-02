package cs.UnityEngine;

/** FullScreenMode: ExclusiveFullScreen 0, FullScreenWindow 1, MaximizedWindow 2, Windowed 3 */
public final class Screen {
    private Screen() {}
    static int width = 1920, height = 1080;
    static int fullScreenMode = 1;
    static int sleepTimeout = -1;

    /** host: surface size changed */
    public static void $setSize(int w, int h) {
        if (w == width && h == height) return;
        width = w;
        height = h;
        Render.screenResized();
    }
    public static int get_width() { return width; }
    public static int get_height() { return height; }
    public static float get_dpi() { return Host.get().dpi(); }
    public static boolean get_fullScreen() { return true; }
    public static void set_fullScreen(boolean v) {}
    public static int get_fullScreenMode() { return fullScreenMode; }
    public static void set_fullScreenMode(int m) { fullScreenMode = m; }
    public static void SetResolution(int w, int h, boolean fs) {}
    public static void SetResolution(int w, int h, int mode) {}
    public static void SetResolution(int w, int h, int mode, int hz) {}
    public static Resolution get_currentResolution() { return Resolution.of(width, height); }
    public static Resolution[] get_resolutions() { return new Resolution[]{Resolution.of(width, height)}; }
    public static Rect get_safeArea() { return new Rect(0, 0, width, height); }
    public static int get_sleepTimeout() { return sleepTimeout; }
    public static void set_sleepTimeout(int v) { sleepTimeout = v; }
    public static int get_orientation() { return 3; }
    public static void set_orientation(int o) {}
    public static float get_brightness() { return 1; }
}
