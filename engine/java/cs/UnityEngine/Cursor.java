package cs.UnityEngine;

/** CursorLockMode: None 0, Locked 1, Confined 2 */
public final class Cursor {
    private Cursor() {}
    static boolean visible = true;
    static int lockState;
    public static boolean get_visible() { return visible; }
    public static void set_visible(boolean v) { visible = v; }
    public static int get_lockState() { return lockState; }
    public static void set_lockState(int v) { lockState = v; }
    public static void SetCursor(Texture2D t, Vector2 hot, int mode) {}
}
