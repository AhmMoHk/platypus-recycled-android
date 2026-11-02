package cs.UnityEngine.TextCore;

public class GlyphMetrics implements cs.rt.Struct {
    public static final Object[] $fmeta = {"m_Width", "m_Width", "r4", 8, "m_Height", "m_Height", "r4", 8, "m_HorizontalBearingX", "m_HorizontalBearingX", "r4", 8, "m_HorizontalBearingY", "m_HorizontalBearingY", "r4", 8, "m_HorizontalAdvance", "m_HorizontalAdvance", "r4", 8};
    public float m_Width;
    public float m_Height;
    public float m_HorizontalBearingX;
    public float m_HorizontalBearingY;
    public float m_HorizontalAdvance;
    public GlyphMetrics() {}
    public GlyphMetrics(float w, float h, float bx, float by, float adv) { m_Width = w; m_Height = h; m_HorizontalBearingX = bx; m_HorizontalBearingY = by; m_HorizontalAdvance = adv; }
    public float get_width() { return m_Width; }
    public void set_width(float v) { m_Width = v; }
    public float get_height() { return m_Height; }
    public void set_height(float v) { m_Height = v; }
    public float get_horizontalBearingX() { return m_HorizontalBearingX; }
    public void set_horizontalBearingX(float v) { m_HorizontalBearingX = v; }
    public float get_horizontalBearingY() { return m_HorizontalBearingY; }
    public void set_horizontalBearingY(float v) { m_HorizontalBearingY = v; }
    public float get_horizontalAdvance() { return m_HorizontalAdvance; }
    public void set_horizontalAdvance(float v) { m_HorizontalAdvance = v; }

    public GlyphMetrics $copy() { GlyphMetrics c = new GlyphMetrics(); c.$set(this); return c; }
    public void $set(Object o) { GlyphMetrics s = (GlyphMetrics) o; m_Width = s.m_Width; m_Height = s.m_Height; m_HorizontalBearingX = s.m_HorizontalBearingX; m_HorizontalBearingY = s.m_HorizontalBearingY; m_HorizontalAdvance = s.m_HorizontalAdvance; }
    public void $clear() { $set(new GlyphMetrics()); }
    public static GlyphMetrics[] $newArray(int n) { GlyphMetrics[] a = new GlyphMetrics[n]; for (int i = 0; i < n; i++) a[i] = new GlyphMetrics(); return a; }
}
