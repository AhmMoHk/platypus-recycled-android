package cs.UnityEngine;

public class Plane implements cs.rt.Struct {
    public Vector3 m_Normal = new Vector3();
    public float m_Distance;
    public Plane() {}
    public Plane(Vector3 inNormal, Vector3 inPoint) { m_Normal.$set(Vector3.Normalize(inNormal)); m_Distance = -Vector3.Dot(m_Normal, inPoint); }
    public Plane(Vector3 inNormal, float d) { m_Normal.$set(Vector3.Normalize(inNormal)); m_Distance = d; }
    public Plane $copy() { Plane p = new Plane(); p.m_Normal.$set(m_Normal); p.m_Distance = m_Distance; return p; }
    public void $set(Object o) { Plane p = (Plane) o; m_Normal.$set(p.m_Normal); m_Distance = p.m_Distance; }
    public void $clear() { m_Normal.$clear(); m_Distance = 0; }
    public static Plane[] $newArray(int n) { Plane[] a = new Plane[n]; for (int i = 0; i < n; i++) a[i] = new Plane(); return a; }
    public Vector3 get_normal() { return m_Normal.$copy(); }
    public float get_distance() { return m_Distance; }
    public float GetDistanceToPoint(Vector3 p) { return Vector3.Dot(m_Normal, p) + m_Distance; }
    public boolean Raycast(Ray ray, cs.rt.RefF enter) {
        float vdot = Vector3.Dot(ray.m_Direction, m_Normal);
        float ndot = -Vector3.Dot(ray.m_Origin, m_Normal) - m_Distance;
        if (Mathf.Approximately(vdot, 0.0f)) { enter.v = 0.0F; return false; }
        enter.v = ndot / vdot;
        return enter.v > 0.0F;
    }
}
