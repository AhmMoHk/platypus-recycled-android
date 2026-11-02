package cs.UnityEngine;

public class Rect implements cs.rt.Struct {
    public float m_XMin, m_YMin, m_Width, m_Height;
    public Rect() {}
    public Rect(float x, float y, float w, float h) { m_XMin = x; m_YMin = y; m_Width = w; m_Height = h; }
    public Rect(Vector2 pos, Vector2 size) { this(pos.x, pos.y, size.x, size.y); }
    public Rect(Rect r) { this(r.m_XMin, r.m_YMin, r.m_Width, r.m_Height); }
    public Rect $copy() { return new Rect(m_XMin, m_YMin, m_Width, m_Height); }
    public void $set(Object o) { Rect r = (Rect) o; m_XMin = r.m_XMin; m_YMin = r.m_YMin; m_Width = r.m_Width; m_Height = r.m_Height; }
    public void $clear() { m_XMin = m_YMin = m_Width = m_Height = 0; }
    public static Rect[] $newArray(int n) { Rect[] a = new Rect[n]; for (int i = 0; i < n; i++) a[i] = new Rect(); return a; }

    public static Rect get_zero() { return new Rect(); }
    public static Rect MinMaxRect(float xmin, float ymin, float xmax, float ymax) { return new Rect(xmin, ymin, xmax - xmin, ymax - ymin); }
    public void Set(float x, float y, float w, float h) { m_XMin = x; m_YMin = y; m_Width = w; m_Height = h; }

    public float get_x() { return m_XMin; }
    public void set_x(float v) { m_XMin = v; }
    public float get_y() { return m_YMin; }
    public void set_y(float v) { m_YMin = v; }
    public float get_width() { return m_Width; }
    public void set_width(float v) { m_Width = v; }
    public float get_height() { return m_Height; }
    public void set_height(float v) { m_Height = v; }
    public Vector2 get_position() { return new Vector2(m_XMin, m_YMin); }
    public void set_position(Vector2 v) { m_XMin = v.x; m_YMin = v.y; }
    public Vector2 get_size() { return new Vector2(m_Width, m_Height); }
    public void set_size(Vector2 v) { m_Width = v.x; m_Height = v.y; }
    public Vector2 get_center() { return new Vector2(m_XMin + m_Width / 2f, m_YMin + m_Height / 2f); }
    public void set_center(Vector2 v) { m_XMin = v.x - m_Width / 2f; m_YMin = v.y - m_Height / 2f; }
    public Vector2 get_min() { return new Vector2(get_xMin(), get_yMin()); }
    public void set_min(Vector2 v) { set_xMin(v.x); set_yMin(v.y); }
    public Vector2 get_max() { return new Vector2(get_xMax(), get_yMax()); }
    public void set_max(Vector2 v) { set_xMax(v.x); set_yMax(v.y); }
    public float get_xMin() { return m_XMin; }
    public void set_xMin(float v) { float oldxmax = get_xMax(); m_XMin = v; m_Width = oldxmax - m_XMin; }
    public float get_yMin() { return m_YMin; }
    public void set_yMin(float v) { float oldymax = get_yMax(); m_YMin = v; m_Height = oldymax - m_YMin; }
    public float get_xMax() { return m_Width + m_XMin; }
    public void set_xMax(float v) { m_Width = v - m_XMin; }
    public float get_yMax() { return m_Height + m_YMin; }
    public void set_yMax(float v) { m_Height = v - m_YMin; }

    public boolean Contains(Vector2 p) { return p.x >= get_xMin() && p.x < get_xMax() && p.y >= get_yMin() && p.y < get_yMax(); }
    public boolean Contains(Vector3 p) { return p.x >= get_xMin() && p.x < get_xMax() && p.y >= get_yMin() && p.y < get_yMax(); }
    private static Rect orderMinMax(Rect r) {
        Rect o = r.$copy();
        if (o.get_xMin() > o.get_xMax()) { float t = o.get_xMin(); o.set_xMin(o.get_xMax()); o.set_xMax(t); }
        if (o.get_yMin() > o.get_yMax()) { float t = o.get_yMin(); o.set_yMin(o.get_yMax()); o.set_yMax(t); }
        return o;
    }
    public boolean Overlaps(Rect o) {
        return o.get_xMax() > get_xMin() && o.get_xMin() < get_xMax() && o.get_yMax() > get_yMin() && o.get_yMin() < get_yMax();
    }
    public boolean Overlaps(Rect o, boolean allowInverse) {
        Rect self = this;
        if (allowInverse) { self = orderMinMax(this); o = orderMinMax(o); }
        return self.Overlaps(o);
    }
    public static Vector2 NormalizedToPoint(Rect r, Vector2 n) { return new Vector2(Mathf.Lerp(r.m_XMin, r.get_xMax(), n.x), Mathf.Lerp(r.m_YMin, r.get_yMax(), n.y)); }
    public static Vector2 PointToNormalized(Rect r, Vector2 p) { return new Vector2(Mathf.InverseLerp(r.m_XMin, r.get_xMax(), p.x), Mathf.InverseLerp(r.m_YMin, r.get_yMax(), p.y)); }

    public static boolean op_Equality(Rect a, Rect b) { return a.m_XMin == b.m_XMin && a.m_YMin == b.m_YMin && a.m_Width == b.m_Width && a.m_Height == b.m_Height; }
    public static boolean op_Inequality(Rect a, Rect b) { return !op_Equality(a, b); }
    public boolean Equals(Rect o) { return op_Equality(this, o); }
    @Override public boolean equals(Object o) { return o instanceof Rect && Equals((Rect) o); }
    @Override public int hashCode() { return Float.floatToIntBits(m_XMin) ^ (Float.floatToIntBits(m_Width) << 2) ^ (Float.floatToIntBits(m_YMin) >> 2) ^ (Float.floatToIntBits(m_Height) >> 1); }
    @Override public String toString() { return "(x:" + Vector3.f2(m_XMin) + ", y:" + Vector3.f2(m_YMin) + ", width:" + Vector3.f2(m_Width) + ", height:" + Vector3.f2(m_Height) + ")"; }
}
