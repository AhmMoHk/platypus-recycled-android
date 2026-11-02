package cs.UnityEngine;

public final class GUILayout {
    private GUILayout() {}
    public static void BeginArea(Rect r) {}
    public static void BeginArea(Rect r, GUIStyle s) {}
    public static void EndArea() {}
    public static void BeginHorizontal(GUILayoutOption[] o) {}
    public static void EndHorizontal() {}
    public static void BeginVertical(GUILayoutOption[] o) {}
    public static void EndVertical() {}
    public static void FlexibleSpace() {}
    public static void Space(float s) {}
    public static void Label(String s, GUILayoutOption[] o) {}
    public static void Label(String s, GUIStyle st, GUILayoutOption[] o) {}
    public static String TextArea(String s, GUIStyle st, GUILayoutOption[] o) { return s; }
    public static boolean Button(String s, GUILayoutOption[] o) { return false; }
}
