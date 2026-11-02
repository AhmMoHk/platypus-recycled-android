package cs.UnityEngine;

public class Bounds implements cs.rt.Struct {
    public Vector3 m_Center = new Vector3(), m_Extent = new Vector3();
    public Bounds() {}
    public Bounds(Vector3 center, Vector3 size) { m_Center.$set(center); m_Extent.Set(size.x * 0.5f, size.y * 0.5f, size.z * 0.5f); }
    public Bounds $copy() { Bounds b = new Bounds(); b.m_Center.$set(m_Center); b.m_Extent.$set(m_Extent); return b; }
    public void $set(Object o) { Bounds b = (Bounds) o; m_Center.$set(b.m_Center); m_Extent.$set(b.m_Extent); }
    public void $clear() { m_Center.$clear(); m_Extent.$clear(); }
    public static Bounds[] $newArray(int n) { Bounds[] a = new Bounds[n]; for (int i = 0; i < n; i++) a[i] = new Bounds(); return a; }
    public Vector3 get_center() { return m_Center.$copy(); }
    public void set_center(Vector3 c) { m_Center.$set(c); }
    public Vector3 get_size() { return new Vector3(m_Extent.x * 2, m_Extent.y * 2, m_Extent.z * 2); }
    public void set_size(Vector3 s) { m_Extent.Set(s.x * 0.5f, s.y * 0.5f, s.z * 0.5f); }
    public Vector3 get_extents() { return m_Extent.$copy(); }
    public void set_extents(Vector3 e) { m_Extent.$set(e); }
    public Vector3 get_min() { return Vector3.op_Subtraction(m_Center, m_Extent); }
    public Vector3 get_max() { return Vector3.op_Addition(m_Center, m_Extent); }
    public void SetMinMax(Vector3 min, Vector3 max) {
        m_Extent.Set((max.x - min.x) * 0.5f, (max.y - min.y) * 0.5f, (max.z - min.z) * 0.5f);
        m_Center.Set(min.x + m_Extent.x, min.y + m_Extent.y, min.z + m_Extent.z);
    }
    public void set_min(Vector3 v) { SetMinMax(v, get_max()); }
    public void set_max(Vector3 v) { SetMinMax(get_min(), v); }
    public void Encapsulate(Vector3 p) { SetMinMax(Vector3.Min(get_min(), p), Vector3.Max(get_max(), p)); }
    public void Encapsulate(Bounds b) { Encapsulate(Vector3.op_Subtraction(b.m_Center, b.m_Extent)); Encapsulate(Vector3.op_Addition(b.m_Center, b.m_Extent)); }
    public void Expand(float amount) { amount *= .5f; m_Extent.Set(m_Extent.x + amount, m_Extent.y + amount, m_Extent.z + amount); }
    public boolean Contains(Vector3 p) {
        Vector3 mn = get_min(), mx = get_max();
        return p.x >= mn.x && p.x <= mx.x && p.y >= mn.y && p.y <= mx.y && p.z >= mn.z && p.z <= mx.z;
    }
    public boolean Intersects(Bounds b) {
        Vector3 amn = get_min(), amx = get_max(), bmn = b.get_min(), bmx = b.get_max();
        return amn.x <= bmx.x && amx.x >= bmn.x && amn.y <= bmx.y && amx.y >= bmn.y && amn.z <= bmx.z && amx.z >= bmn.z;
    }
    public Vector3 ClosestPoint(Vector3 p) {
        Vector3 mn = get_min(), mx = get_max();
        return new Vector3(Mathf.Clamp(p.x, mn.x, mx.x), Mathf.Clamp(p.y, mn.y, mx.y), Mathf.Clamp(p.z, mn.z, mx.z));
    }
    public static boolean op_Equality(Bounds a, Bounds b) { return Vector3.op_Equality(a.m_Center, b.m_Center) && Vector3.op_Equality(a.m_Extent, b.m_Extent); }
    public static boolean op_Inequality(Bounds a, Bounds b) { return !op_Equality(a, b); }
    @Override public boolean equals(Object o) { return o instanceof Bounds && ((Bounds) o).m_Center.equals(m_Center) && ((Bounds) o).m_Extent.equals(m_Extent); }
    @Override public int hashCode() { return m_Center.hashCode() ^ (m_Extent.hashCode() << 2); }
    @Override public String toString() { return "Center: " + m_Center + ", Extents: " + m_Extent; }
}
