package cs.UnityEngine;

import cs.System.Collections.Generic.List_1;

/** Legacy uGUI Text generator (the game uses TextMeshPro; this produces simple fixed-advance layout). */
public class TextGenerator {
    final List_1 verts = new List_1(), chars = new List_1(), lines = new List_1();
    Rect extents = new Rect();
    int visible;
    public TextGenerator() {}
    public TextGenerator(int cap) {}
    public void Invalidate() {}
    public float GetPreferredWidth(String s, TextGenerationSettings st) { return s == null ? 0 : s.length() * st.fontSize * .55f; }
    public float GetPreferredHeight(String s, TextGenerationSettings st) { return st.fontSize * 1.15f * Math.max(1, s == null ? 1 : s.split("\n", -1).length); }
    public boolean PopulateWithErrors(String s, TextGenerationSettings st, GameObject ctx) { return Populate(s, st); }
    public boolean Populate(String s, TextGenerationSettings st) {
        verts.Clear(); chars.Clear(); lines.Clear();
        visible = 0;
        UILineInfo l = new UILineInfo();
        l.height = st.fontSize;
        lines.Add(l);
        extents = new Rect(0, 0, GetPreferredWidth(s, st), GetPreferredHeight(s, st));
        return true;
    }
    public cs.System.Collections.Generic.IList_1 get_verts() { return verts; }
    public cs.System.Collections.Generic.IList_1 get_characters() { return chars; }
    public cs.System.Collections.Generic.IList_1 get_lines() { return lines; }
    public Rect get_rectExtents() { return extents.$copy(); }
    public int get_vertexCount() { return verts.get_Count(); }
    public int get_characterCount() { return chars.get_Count(); }
    public int get_characterCountVisible() { return visible; }
    public int get_lineCount() { return lines.get_Count(); }
    public int get_fontSizeUsedForBestFit() { return 0; }
}
