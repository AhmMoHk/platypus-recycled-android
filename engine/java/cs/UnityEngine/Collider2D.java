package cs.UnityEngine;

import java.util.ArrayList;

/** Base 2D collider: world-space convex parts (polygons / circles / capsules) for the contact solver. */
public class Collider2D extends Behaviour {
    final Vector2 offset = new Vector2();
    boolean isTrigger, usedByEffector, usedByComposite;
    float density = 1;
    /** contacts found by the last Physics2D.Simulate */
    final ArrayList<Collider2D> contacts = new ArrayList<>();
    // cached world shape
    final ArrayList<float[]> polys = new ArrayList<>();      // convex polygons: x0,y0,x1,y1,...
    final ArrayList<float[]> circles = new ArrayList<>();    // x, y, r
    final ArrayList<float[]> capsules = new ArrayList<>();   // x0, y0, x1, y1, r
    float minX, minY, maxX, maxY;
    int shapeVersion = -1;
    boolean registered;
    Rigidbody2D cachedBody;

    public Vector2 get_offset() { return offset.$copy(); }
    public void set_offset(Vector2 v) { offset.$set(v); shapeChanged(); }
    public boolean get_isTrigger() { return isTrigger; }
    public void set_isTrigger(boolean v) { isTrigger = v; }
    public float get_density() { return density; }
    public void set_density(float v) { density = v; }
    public boolean get_usedByEffector() { return usedByEffector; }
    public void set_usedByEffector(boolean v) { usedByEffector = v; }
    public boolean get_usedByComposite() { return usedByComposite; }
    public void set_usedByComposite(boolean v) { usedByComposite = v; }
    public int get_shapeCount() { rebuild(); return polys.size() + circles.size() + capsules.size(); }

    public Rigidbody2D get_attachedRigidbody() {
        for (Transform t = gameObject == null ? null : gameObject.transform; t != null; t = t.parent) {
            Rigidbody2D rb = (Rigidbody2D) t.gameObject.GetComponent(cs.System.Type.of(Rigidbody2D.class));
            if (rb != null) return rb;
        }
        return null;
    }

    public Bounds get_bounds() {
        rebuild();
        Bounds b = new Bounds();
        b.m_Center.Set((minX + maxX) / 2, (minY + maxY) / 2, gameObject.transform.get_position().z);
        b.m_Extent.Set((maxX - minX) / 2, (maxY - minY) / 2, 0);
        return b;
    }

    public int GetContacts(ContactPoint2D[] out) {
        int n = 0;
        Rigidbody2D mine = get_attachedRigidbody();
        for (Collider2D c : contacts) {
            if (n >= out.length) break;
            if (c.$destroyed || isTrigger || c.isTrigger) continue;
            ContactPoint2D p = out[n++];
            p.m_Collider = c;
            p.m_OtherCollider = this;
            p.m_Rigidbody = c.get_attachedRigidbody();
            p.m_OtherRigidbody = mine;
            Physics2D.contactPoint(this, c, p);
        }
        return n;
    }
    public int GetContacts(cs.System.Collections.Generic.List_1 out) {
        out.Clear();
        ContactPoint2D[] a = ContactPoint2D.$newArray(contacts.size());
        int n = GetContacts(a);
        for (int i = 0; i < n; i++) out.Add(a[i]);
        return n;
    }
    public int GetContacts(Collider2D[] out) {
        int n = 0;
        for (Collider2D c : contacts) { if (n >= out.length) break; if (!c.$destroyed) out[n++] = c; }
        return n;
    }
    public boolean IsTouching(Collider2D c) { return contacts.contains(c); }
    public boolean IsTouchingLayers(int mask) {
        for (Collider2D c : contacts) if ((mask & (1 << c.gameObject.m_Layer)) != 0) return true;
        return false;
    }
    public boolean OverlapPoint(Vector2 p) {
        rebuild();
        return Physics2D.pointInCollider(this, p.x, p.y);
    }
    public Vector2 ClosestPoint(Vector2 p) { return p.$copy(); }

    // ------------------------------------------------------------------ shape
    int shapeDirty = 1;
    void shapeChanged() { shapeDirty++; }

    /** local-space shape (before offset/transform); subclasses append parts */
    void localShape(ArrayList<float[]> polys, ArrayList<float[]> circles, ArrayList<float[]> capsules) {}

    private int builtFor = -1;
    private final float[] lastWorld = new float[16];

    final void rebuild() {
        if (gameObject == null) return;
        float[] m = gameObject.transform.world();
        boolean same = builtFor == shapeDirty;
        if (same) for (int i = 0; i < 16; i++) if (m[i] != lastWorld[i]) { same = false; break; }
        if (same) return;
        builtFor = shapeDirty;
        System.arraycopy(m, 0, lastWorld, 0, 16);
        polys.clear(); circles.clear(); capsules.clear();
        ArrayList<float[]> lp = new ArrayList<>(), lc = new ArrayList<>(), lk = new ArrayList<>();
        localShape(lp, lc, lk);
        float ox = offset.x, oy = offset.y;
        float sx = (float) Math.sqrt(m[0] * m[0] + m[1] * m[1]), sy = (float) Math.sqrt(m[4] * m[4] + m[5] * m[5]);
        float rs = Math.max(Math.abs(sx), Math.abs(sy));
        minX = minY = Float.POSITIVE_INFINITY;
        maxX = maxY = Float.NEGATIVE_INFINITY;
        for (float[] p : lp) {
            float[] w = new float[p.length];
            for (int i = 0; i < p.length; i += 2) {
                float x = p[i] + ox, y = p[i + 1] + oy;
                w[i] = m[0] * x + m[4] * y + m[12];
                w[i + 1] = m[1] * x + m[5] * y + m[13];
                grow(w[i], w[i + 1], 0);
            }
            if (signedArea(w) < 0) reverse(w);
            polys.add(w);
        }
        for (float[] c : lc) {
            float x = c[0] + ox, y = c[1] + oy;
            float[] w = {m[0] * x + m[4] * y + m[12], m[1] * x + m[5] * y + m[13], c[2] * rs};
            grow(w[0], w[1], w[2]);
            circles.add(w);
        }
        for (float[] k : lk) {
            float x0 = k[0] + ox, y0 = k[1] + oy, x1 = k[2] + ox, y1 = k[3] + oy;
            float[] w = {m[0] * x0 + m[4] * y0 + m[12], m[1] * x0 + m[5] * y0 + m[13], m[0] * x1 + m[4] * y1 + m[12], m[1] * x1 + m[5] * y1 + m[13], k[4] * rs};
            grow(w[0], w[1], w[4]);
            grow(w[2], w[3], w[4]);
            capsules.add(w);
        }
    }

    private void grow(float x, float y, float r) {
        minX = Math.min(minX, x - r); maxX = Math.max(maxX, x + r);
        minY = Math.min(minY, y - r); maxY = Math.max(maxY, y + r);
    }

    static float signedArea(float[] p) {
        float a = 0;
        for (int i = 0, n = p.length; i < n; i += 2) {
            int j = (i + 2) % n;
            a += p[i] * p[j + 1] - p[j] * p[i + 1];
        }
        return a / 2;
    }

    static void reverse(float[] p) {
        for (int i = 0, j = p.length - 2; i < j; i += 2, j -= 2) {
            float x = p[i], y = p[i + 1];
            p[i] = p[j]; p[i + 1] = p[j + 1];
            p[j] = x; p[j + 1] = y;
        }
    }

    // ------------------------------------------------------------------ lifecycle
    @Override public void $nativeEnable() { Physics2D.register(this); }
    @Override public void $nativeDisable() { Physics2D.unregister(this); }
    @Override public void $onDestroyNative() { Physics2D.unregister(this); }

    @Override public void $load(java.util.HashMap<String, Object> d, Loader.Ctx ctx) {
        Loader.vec2(d.get("m_Offset"), offset);
        isTrigger = Loader.b(d, "m_IsTrigger", false);
        density = Loader.f(d, "m_Density", 1);
        usedByEffector = Loader.b(d, "m_UsedByEffector", false);
        usedByComposite = Loader.b(d, "m_UsedByComposite", false);
        shapeChanged();
    }
    @Override public void $cloneFrom(Component src, Eng.CloneCtx ctx) {
        Collider2D s = (Collider2D) src;
        offset.$set(s.offset);
        isTrigger = s.isTrigger;
        density = s.density;
        usedByEffector = s.usedByEffector;
        usedByComposite = s.usedByComposite;
        shapeChanged();
    }
}
