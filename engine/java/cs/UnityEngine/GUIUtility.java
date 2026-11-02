package cs.UnityEngine;

public final class GUIUtility {
    private GUIUtility() {}
    public static String get_systemCopyBuffer() { return Host.get().clipboard(); }
    public static void set_systemCopyBuffer(String s) { Host.get().setClipboard(s); }
    public static int get_hotControl() { return 0; }
    public static int get_keyboardControl() { return 0; }
}
