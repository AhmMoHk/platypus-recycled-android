package cs.UnityEngine;

public class RaycastHit2D implements cs.rt.Struct {
    public final Vector2 m_Centroid = new Vector2(), m_Point = new Vector2(), m_Normal = new Vector2();
    public float m_Distance, m_Fraction;
    public Collider2D m_Collider;
    public RaycastHit2D() {}
    public Collider2D get_collider() { return m_Collider; }
    public Rigidbody2D get_rigidbody() { return m_Collider == null ? null : m_Collider.get_attachedRigidbody(); }
    public Transform get_transform() { return m_Collider == null ? null : m_Collider.get_transform(); }
    public Vector2 get_point() { return m_Point.$copy(); }
    public Vector2 get_centroid() { return m_Centroid.$copy(); }
    public Vector2 get_normal() { return m_Normal.$copy(); }
    public float get_distance() { return m_Distance; }
    public float get_fraction() { return m_Fraction; }
    public static boolean op_Implicit$boolean_(RaycastHit2D h) { return h.m_Collider != null; }
    public RaycastHit2D $copy() { RaycastHit2D c = new RaycastHit2D(); c.$set(this); return c; }
    public void $set(Object o) {
        RaycastHit2D h = (RaycastHit2D) o;
        m_Centroid.$set(h.m_Centroid); m_Point.$set(h.m_Point); m_Normal.$set(h.m_Normal);
        m_Distance = h.m_Distance; m_Fraction = h.m_Fraction; m_Collider = h.m_Collider;
    }
    public void $clear() { $set(new RaycastHit2D()); }
    public static RaycastHit2D[] $newArray(int n) { RaycastHit2D[] a = new RaycastHit2D[n]; for (int i = 0; i < n; i++) a[i] = new RaycastHit2D(); return a; }
}
