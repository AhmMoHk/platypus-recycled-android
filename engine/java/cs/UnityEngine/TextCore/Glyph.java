package cs.UnityEngine.TextCore;

public class Glyph {
    public static final Object[] $fmeta = {"m_Index", "m_Index", "u4", 8, "m_Metrics", "m_Metrics", "UnityEngine.TextCore.GlyphMetrics", 8,
        "m_GlyphRect", "m_GlyphRect", "UnityEngine.TextCore.GlyphRect", 8, "m_Scale", "m_Scale", "r4", 8, "m_AtlasIndex", "m_AtlasIndex", "i4", 8, "m_ClassDefinitionType", "m_ClassDefinitionType", "i4", 8};
    public int m_Index, m_AtlasIndex, m_ClassDefinitionType;
    public GlyphMetrics m_Metrics = new GlyphMetrics();
    public GlyphRect m_GlyphRect = new GlyphRect();
    public float m_Scale = 1;
    public Glyph() {}
    public Glyph(Glyph g) { m_Index = g.m_Index; m_Metrics.$set(g.m_Metrics); m_GlyphRect.$set(g.m_GlyphRect); m_Scale = g.m_Scale; m_AtlasIndex = g.m_AtlasIndex; }
    public Glyph(int index, GlyphMetrics m, GlyphRect r) { this(index, m, r, 1, 0); }
    public Glyph(int index, GlyphMetrics m, GlyphRect r, float scale, int atlas) { m_Index = index; m_Metrics.$set(m); m_GlyphRect.$set(r); m_Scale = scale; m_AtlasIndex = atlas; }
    public int get_index() { return m_Index; }
    public void set_index(int v) { m_Index = v; }
    public GlyphMetrics get_metrics() { return m_Metrics.$copy(); }
    public void set_metrics(GlyphMetrics v) { m_Metrics.$set(v); }
    public GlyphRect get_glyphRect() { return m_GlyphRect.$copy(); }
    public void set_glyphRect(GlyphRect v) { m_GlyphRect.$set(v); }
    public float get_scale() { return m_Scale; }
    public void set_scale(float v) { m_Scale = v; }
    public int get_atlasIndex() { return m_AtlasIndex; }
    public void set_atlasIndex(int v) { m_AtlasIndex = v; }
    public int get_classDefinitionType() { return m_ClassDefinitionType; }
    public void set_classDefinitionType(int v) { m_ClassDefinitionType = v; }
    public boolean Compare(Glyph o) { return o != null && o.m_Index == m_Index && o.m_GlyphRect.equals(m_GlyphRect); }
}
