package cs.UnityEngine.TextCore;

public class GlyphRect implements cs.rt.Struct {
    public static final Object[] $fmeta = {"m_X", "m_X", "i4", 8, "m_Y", "m_Y", "i4", 8, "m_Width", "m_Width", "i4", 8, "m_Height", "m_Height", "i4", 8};
    public int m_X;
    public int m_Y;
    public int m_Width;
    public int m_Height;
    public GlyphRect() {}
    public GlyphRect(int x, int y, int w, int h) { m_X = x; m_Y = y; m_Width = w; m_Height = h; }
    public GlyphRect(cs.UnityEngine.Rect r) { this((int) r.m_XMin, (int) r.m_YMin, (int) r.m_Width, (int) r.m_Height); }
    public int get_x() { return m_X; }
    public void set_x(int v) { m_X = v; }
    public int get_y() { return m_Y; }
    public void set_y(int v) { m_Y = v; }
    public int get_width() { return m_Width; }
    public void set_width(int v) { m_Width = v; }
    public int get_height() { return m_Height; }
    public void set_height(int v) { m_Height = v; }
    public static GlyphRect get_zero() { return new GlyphRect(); }
    @Override public boolean equals(Object o) { if (!(o instanceof GlyphRect)) return false; GlyphRect r = (GlyphRect) o; return r.m_X == m_X && r.m_Y == m_Y && r.m_Width == m_Width && r.m_Height == m_Height; }
    @Override public int hashCode() { return ((m_X * 31 + m_Y) * 31 + m_Width) * 31 + m_Height; }
    public boolean Equals(GlyphRect r) { return equals(r); }
    public GlyphRect $copy() { GlyphRect c = new GlyphRect(); c.$set(this); return c; }
    public void $set(Object o) { GlyphRect s = (GlyphRect) o; m_X = s.m_X; m_Y = s.m_Y; m_Width = s.m_Width; m_Height = s.m_Height; }
    public void $clear() { $set(new GlyphRect()); }
    public static GlyphRect[] $newArray(int n) { GlyphRect[] a = new GlyphRect[n]; for (int i = 0; i < n; i++) a[i] = new GlyphRect(); return a; }
}
