package cs.UnityEngine.TextCore.LowLevel;

public class GlyphAdjustmentRecord implements cs.rt.Struct {
    public static final Object[] $fmeta = {"m_GlyphIndex", "m_GlyphIndex", "u4", 8, "m_GlyphValueRecord", "m_GlyphValueRecord", "UnityEngine.TextCore.LowLevel.GlyphValueRecord", 8};
    public int m_GlyphIndex;
    public GlyphValueRecord m_GlyphValueRecord = new GlyphValueRecord();
    public GlyphAdjustmentRecord() {}
    public GlyphAdjustmentRecord(int g, GlyphValueRecord v) { m_GlyphIndex = g; m_GlyphValueRecord.$set(v); }
    public int get_glyphIndex() { return m_GlyphIndex; }
    public void set_glyphIndex(int v) { m_GlyphIndex = v; }
    public GlyphValueRecord get_glyphValueRecord() { return m_GlyphValueRecord.$copy(); }
    public void set_glyphValueRecord(GlyphValueRecord v) { m_GlyphValueRecord.$set(v); }

    public GlyphAdjustmentRecord $copy() { GlyphAdjustmentRecord c = new GlyphAdjustmentRecord(); c.$set(this); return c; }
    public void $set(Object o) { GlyphAdjustmentRecord s = (GlyphAdjustmentRecord) o; m_GlyphIndex = s.m_GlyphIndex; m_GlyphValueRecord.$set(s.m_GlyphValueRecord); }
    public void $clear() { $set(new GlyphAdjustmentRecord()); }
    public static GlyphAdjustmentRecord[] $newArray(int n) { GlyphAdjustmentRecord[] a = new GlyphAdjustmentRecord[n]; for (int i = 0; i < n; i++) a[i] = new GlyphAdjustmentRecord(); return a; }
}
