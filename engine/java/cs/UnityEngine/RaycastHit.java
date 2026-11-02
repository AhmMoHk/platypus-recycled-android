package cs.UnityEngine;

public class RaycastHit implements cs.rt.Struct {
    public final Vector3 m_Point = new Vector3(), m_Normal = new Vector3();
    public float m_Distance;
    public Collider m_Collider;
    public RaycastHit() {}
    public Collider get_collider() { return m_Collider; }
    public float get_distance() { return m_Distance; }
    public Vector3 get_normal() { return m_Normal.$copy(); }
    public Vector3 get_point() { return m_Point.$copy(); }
    public RaycastHit $copy() { RaycastHit r = new RaycastHit(); r.$set(this); return r; }
    public void $set(Object o) { RaycastHit h = (RaycastHit) o; m_Point.$set(h.m_Point); m_Normal.$set(h.m_Normal); m_Distance = h.m_Distance; m_Collider = h.m_Collider; }
    public void $clear() { $set(new RaycastHit()); }
    public static RaycastHit[] $newArray(int n) { RaycastHit[] a = new RaycastHit[n]; for (int i = 0; i < n; i++) a[i] = new RaycastHit(); return a; }
}
