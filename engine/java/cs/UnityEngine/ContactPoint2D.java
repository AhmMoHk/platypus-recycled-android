package cs.UnityEngine;

public class ContactPoint2D implements cs.rt.Struct {
    public Collider2D m_Collider, m_OtherCollider;
    public Rigidbody2D m_Rigidbody, m_OtherRigidbody;
    public final Vector2 m_Point = new Vector2(), m_Normal = new Vector2(), m_RelativeVelocity = new Vector2();
    public float m_Separation, m_NormalImpulse, m_TangentImpulse;
    public ContactPoint2D() {}
    public Collider2D get_collider() { return m_Collider; }
    public Collider2D get_otherCollider() { return m_OtherCollider; }
    public Rigidbody2D get_rigidbody() { return m_Rigidbody; }
    public Rigidbody2D get_otherRigidbody() { return m_OtherRigidbody; }
    public Vector2 get_point() { return m_Point.$copy(); }
    public Vector2 get_normal() { return m_Normal.$copy(); }
    public Vector2 get_relativeVelocity() { return m_RelativeVelocity.$copy(); }
    public float get_separation() { return m_Separation; }
    public boolean get_enabled() { return true; }
    public ContactPoint2D $copy() { ContactPoint2D c = new ContactPoint2D(); c.$set(this); return c; }
    public void $set(Object o) {
        ContactPoint2D c = (ContactPoint2D) o;
        m_Collider = c.m_Collider; m_OtherCollider = c.m_OtherCollider; m_Rigidbody = c.m_Rigidbody; m_OtherRigidbody = c.m_OtherRigidbody;
        m_Point.$set(c.m_Point); m_Normal.$set(c.m_Normal); m_RelativeVelocity.$set(c.m_RelativeVelocity);
        m_Separation = c.m_Separation; m_NormalImpulse = c.m_NormalImpulse; m_TangentImpulse = c.m_TangentImpulse;
    }
    public void $clear() { $set(new ContactPoint2D()); }
    public static ContactPoint2D[] $newArray(int n) { ContactPoint2D[] a = new ContactPoint2D[n]; for (int i = 0; i < n; i++) a[i] = new ContactPoint2D(); return a; }
}
