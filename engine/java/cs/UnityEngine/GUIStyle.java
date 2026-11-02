package cs.UnityEngine;

/** IMGUI is only used by debug overlays; styles keep their values but nothing is drawn. */
public class GUIStyle {
    final GUIStyleState normal = new GUIStyleState();
    int fontSize, fontStyle, alignment;
    boolean wordWrap;
    Font font;
    public GUIStyle() {}
    public GUIStyle(GUIStyle o) { fontSize = o.fontSize; fontStyle = o.fontStyle; alignment = o.alignment; wordWrap = o.wordWrap; font = o.font; normal.textColor.$set(o.normal.textColor); }
    public static GUIStyle get_none() { return new GUIStyle(); }
    public static GUIStyle op_Implicit$GUIStyle(String s) { return new GUIStyle(); }
    public GUIStyleState get_normal() { return normal; }
    public int get_fontStyle() { return fontStyle; }
    public void set_fontStyle(int s) { fontStyle = s; }
    public void set_fontSize(int s) { fontSize = s; }
    public void set_alignment(int a) { alignment = a; }
    public void set_wordWrap(boolean v) { wordWrap = v; }
    public void set_font(Font f) { font = f; }
}
