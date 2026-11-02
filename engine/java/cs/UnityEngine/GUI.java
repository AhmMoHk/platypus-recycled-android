package cs.UnityEngine;

public final class GUI {
    private GUI() {}
    private static final GUISkin skin = new GUISkin();
    static final Color color = new Color(1, 1, 1, 1);
    public static GUISkin get_skin() { return skin; }
    public static void set_color(Color c) { color.$set(c); }
    public static Color get_color() { return color.$copy(); }
    public static void Label(Rect r, String s) {}
    public static void Label(Rect r, String s, GUIStyle st) {}
    public static boolean Button(Rect r, String s) { return false; }
    public static void Box(Rect r, String s) {}
}
