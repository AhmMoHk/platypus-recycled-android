package cs.UnityEngine.TextCore.LowLevel;

public class GlyphPairAdjustmentRecord implements cs.rt.Struct {
    public static final Object[] $fmeta = {"m_FirstAdjustmentRecord", "m_FirstAdjustmentRecord", "UnityEngine.TextCore.LowLevel.GlyphAdjustmentRecord", 8, "m_SecondAdjustmentRecord", "m_SecondAdjustmentRecord", "UnityEngine.TextCore.LowLevel.GlyphAdjustmentRecord", 8, "m_FeatureLookupFlags", "m_FeatureLookupFlags", "i4", 8};
    public GlyphAdjustmentRecord m_FirstAdjustmentRecord = new GlyphAdjustmentRecord();
    public GlyphAdjustmentRecord m_SecondAdjustmentRecord = new GlyphAdjustmentRecord();
    public int m_FeatureLookupFlags;
    public GlyphPairAdjustmentRecord() {}
    public GlyphPairAdjustmentRecord(GlyphAdjustmentRecord a, GlyphAdjustmentRecord b) { m_FirstAdjustmentRecord.$set(a); m_SecondAdjustmentRecord.$set(b); }
    public GlyphAdjustmentRecord get_firstAdjustmentRecord() { return m_FirstAdjustmentRecord.$copy(); }
    public void set_firstAdjustmentRecord(GlyphAdjustmentRecord v) { m_FirstAdjustmentRecord.$set(v); }
    public GlyphAdjustmentRecord get_secondAdjustmentRecord() { return m_SecondAdjustmentRecord.$copy(); }
    public void set_secondAdjustmentRecord(GlyphAdjustmentRecord v) { m_SecondAdjustmentRecord.$set(v); }
    public int get_featureLookupFlags() { return m_FeatureLookupFlags; }
    public void set_featureLookupFlags(int v) { m_FeatureLookupFlags = v; }

    public GlyphPairAdjustmentRecord $copy() { GlyphPairAdjustmentRecord c = new GlyphPairAdjustmentRecord(); c.$set(this); return c; }
    public void $set(Object o) { GlyphPairAdjustmentRecord s = (GlyphPairAdjustmentRecord) o; m_FirstAdjustmentRecord.$set(s.m_FirstAdjustmentRecord); m_SecondAdjustmentRecord.$set(s.m_SecondAdjustmentRecord); m_FeatureLookupFlags = s.m_FeatureLookupFlags; }
    public void $clear() { $set(new GlyphPairAdjustmentRecord()); }
    public static GlyphPairAdjustmentRecord[] $newArray(int n) { GlyphPairAdjustmentRecord[] a = new GlyphPairAdjustmentRecord[n]; for (int i = 0; i < n; i++) a[i] = new GlyphPairAdjustmentRecord(); return a; }
}
