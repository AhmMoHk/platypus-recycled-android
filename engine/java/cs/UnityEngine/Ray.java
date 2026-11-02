package cs.UnityEngine;

public class Ray implements cs.rt.Struct {
    public Vector3 m_Origin = new Vector3(), m_Direction = new Vector3();
    public Ray() {}
    public Ray(Vector3 o, Vector3 d) { m_Origin.$set(o); m_Direction.$set(d.get_normalized()); }
    public Ray $copy() { Ray r = new Ray(); r.m_Origin.$set(m_Origin); r.m_Direction.$set(m_Direction); return r; }
    public void $set(Object o) { Ray r = (Ray) o; m_Origin.$set(r.m_Origin); m_Direction.$set(r.m_Direction); }
    public void $clear() { m_Origin.$clear(); m_Direction.$clear(); }
    public static Ray[] $newArray(int n) { Ray[] a = new Ray[n]; for (int i = 0; i < n; i++) a[i] = new Ray(); return a; }
    public Vector3 get_origin() { return m_Origin.$copy(); }
    public Vector3 get_direction() { return m_Direction.$copy(); }
    public Vector3 GetPoint(float d) { return Vector3.op_Addition(m_Origin, Vector3.op_Multiply(m_Direction, d)); }
}
