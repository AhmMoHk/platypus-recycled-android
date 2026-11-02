package cs.UnityEngine;

public final class Gizmos {
    private Gizmos() {}
    static final Color color = new Color(1, 1, 1, 1);
    public static Color get_color() { return color.$copy(); }
    public static void set_color(Color c) { color.$set(c); }
    public static void DrawLine(Vector3 a, Vector3 b) {}
    public static void DrawRay(Vector3 a, Vector3 b) {}
    public static void DrawWireCube(Vector3 a, Vector3 b) {}
    public static void DrawWireSphere(Vector3 a, float r) {}
}
