package cs.rt;

/** Behaviour changes of the Android port that translated game code calls into (see re/cil/patches.py). */
public final class Port {
    private Port() {}
    /** fill the whole display: no black bars at any aspect ratio */
    public static boolean fullscreen = true;

    public static cs.UnityEngine.Rect cameraRect(cs.UnityEngine.Rect letterboxed) {
        return fullscreen ? new cs.UnityEngine.Rect(0, 0, 1, 1) : letterboxed;
    }

    /**
     * Horizontal offset that moves positions of the original 640x480 layout to the screen edges.
     * The PC game uses 100 at 16:9; wider screens get the extra half-width so spawns stay just off screen.
     */
    public static int wideOffset(float aspect) {
        float extra = (480f * aspect - 480f * 16f / 9f) / 2f;
        return extra > 0 ? 100 + Math.round(extra) : 100;
    }

    /** width (world units, 480 = screen height) a full-screen background quad needs at the current aspect */
    public static float clearWidth(float base) {
        float aspect = (float) cs.UnityEngine.Screen.get_width() / Math.max(1, cs.UnityEngine.Screen.get_height());
        return Math.max(base, 480f * aspect + 40f);
    }
}
