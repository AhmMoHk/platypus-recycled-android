package cs.UnityEngine;

public class Collision2D {
    Collider2D collider, otherCollider;
    public Collider2D get_collider() { return collider; }
    public Collider2D get_otherCollider() { return otherCollider; }
    public Rigidbody2D get_rigidbody() { return collider == null ? null : collider.get_attachedRigidbody(); }
    public Rigidbody2D get_otherRigidbody() { return otherCollider == null ? null : otherCollider.get_attachedRigidbody(); }
    public GameObject get_gameObject() { return collider == null ? null : collider.gameObject; }
    public Transform get_transform() { return collider == null ? null : collider.get_transform(); }
}
