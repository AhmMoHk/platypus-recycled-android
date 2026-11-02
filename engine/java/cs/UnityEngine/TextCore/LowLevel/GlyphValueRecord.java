package cs.UnityEngine.TextCore.LowLevel;

public class GlyphValueRecord implements cs.rt.Struct {
    public static final Object[] $fmeta = {"m_XPlacement", "m_XPlacement", "r4", 8, "m_YPlacement", "m_YPlacement", "r4", 8, "m_XAdvance", "m_XAdvance", "r4", 8, "m_YAdvance", "m_YAdvance", "r4", 8};
    public float m_XPlacement;
    public float m_YPlacement;
    public float m_XAdvance;
    public float m_YAdvance;
    public GlyphValueRecord() {}
    public GlyphValueRecord(float xp, float yp, float xa, float ya) { m_XPlacement = xp; m_YPlacement = yp; m_XAdvance = xa; m_YAdvance = ya; }
    public float get_xPlacement() { return m_XPlacement; }
    public void set_xPlacement(float v) { m_XPlacement = v; }
    public float get_yPlacement() { return m_YPlacement; }
    public void set_yPlacement(float v) { m_YPlacement = v; }
    public float get_xAdvance() { return m_XAdvance; }
    public void set_xAdvance(float v) { m_XAdvance = v; }
    public float get_yAdvance() { return m_YAdvance; }
    public void set_yAdvance(float v) { m_YAdvance = v; }
    public static GlyphValueRecord op_Addition(GlyphValueRecord a, GlyphValueRecord b) { return new GlyphValueRecord(a.m_XPlacement + b.m_XPlacement, a.m_YPlacement + b.m_YPlacement, a.m_XAdvance + b.m_XAdvance, a.m_YAdvance + b.m_YAdvance); }
    public GlyphValueRecord $copy() { GlyphValueRecord c = new GlyphValueRecord(); c.$set(this); return c; }
    public void $set(Object o) { GlyphValueRecord s = (GlyphValueRecord) o; m_XPlacement = s.m_XPlacement; m_YPlacement = s.m_YPlacement; m_XAdvance = s.m_XAdvance; m_YAdvance = s.m_YAdvance; }
    public void $clear() { $set(new GlyphValueRecord()); }
    public static GlyphValueRecord[] $newArray(int n) { GlyphValueRecord[] a = new GlyphValueRecord[n]; for (int i = 0; i < n; i++) a[i] = new GlyphValueRecord(); return a; }
}
