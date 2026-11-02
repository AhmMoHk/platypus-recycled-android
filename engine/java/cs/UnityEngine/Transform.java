package cs.UnityEngine;

import java.util.ArrayList;

/** UnityEngine.Transform: local TRS + cached local-to-world matrix. */
public class Transform extends Component implements cs.System.Collections.IEnumerable {
    final Vector3 lp = new Vector3();
    final Quaternion lr = new Quaternion(0, 0, 0, 1);
    final Vector3 ls = new Vector3(1, 1, 1);
    Transform parent;
    final ArrayList<Transform> children = new ArrayList<>(2);
    private final float[] w = new float[16];
    private boolean wDirty = true;
    boolean m_HasChanged = true;

    // ------------------------------------------------------------------ world matrix cache
    void dirty() {
        m_HasChanged = true;
        if (wDirty) return;
        wDirty = true;
        for (int i = 0, n = children.size(); i < n; i++) children.get(i).dirty();
    }

    /** column-major local-to-world matrix */
    final float[] world() {
        if (wDirty) {
            float[] l = local();
            if (parent == null) System.arraycopy(l, 0, w, 0, 16);
            else mul(parent.world(), l, w);
            wDirty = false;
        }
        return w;
    }

    private final float[] lm = new float[16];
    private float[] local() {
        float x = lr.x * 2, y = lr.y * 2, z = lr.z * 2;
        float xx = lr.x * x, yy = lr.y * y, zz = lr.z * z, xy = lr.x * y, xz = lr.x * z, yz = lr.y * z, wx = lr.w * x, wy = lr.w * y, wz = lr.w * z;
        float[] m = lm;
        m[0] = (1 - (yy + zz)) * ls.x; m[1] = (xy + wz) * ls.x; m[2] = (xz - wy) * ls.x; m[3] = 0;
        m[4] = (xy - wz) * ls.y; m[5] = (1 - (xx + zz)) * ls.y; m[6] = (yz + wx) * ls.y; m[7] = 0;
        m[8] = (xz + wy) * ls.z; m[9] = (yz - wx) * ls.z; m[10] = (1 - (xx + yy)) * ls.z; m[11] = 0;
        m[12] = lp.x; m[13] = lp.y; m[14] = lp.z; m[15] = 1;
        return m;
    }

    static void mul(float[] a, float[] b, float[] r) {
        for (int c = 0; c < 4; c++) {
            float b0 = b[c * 4], b1 = b[c * 4 + 1], b2 = b[c * 4 + 2], b3 = b[c * 4 + 3];
            r[c * 4] = a[0] * b0 + a[4] * b1 + a[8] * b2 + a[12] * b3;
            r[c * 4 + 1] = a[1] * b0 + a[5] * b1 + a[9] * b2 + a[13] * b3;
            r[c * 4 + 2] = a[2] * b0 + a[6] * b1 + a[10] * b2 + a[14] * b3;
            r[c * 4 + 3] = a[3] * b0 + a[7] * b1 + a[11] * b2 + a[15] * b3;
        }
    }

    // ------------------------------------------------------------------ local
    public Vector3 get_localPosition() { return lp.$copy(); }
    public void set_localPosition(Vector3 v) { lp.x = v.x; lp.y = v.y; lp.z = v.z; dirty(); }
    public Quaternion get_localRotation() { return lr.$copy(); }
    public void set_localRotation(Quaternion q) { lr.$set(q); dirty(); }
    public Vector3 get_localScale() { return ls.$copy(); }
    public void set_localScale(Vector3 v) { ls.$set(v); dirty(); $scaleChanged(); }
    public void $scaleChanged() {}
    public Vector3 get_localEulerAngles() { return lr.get_eulerAngles(); }
    public void set_localEulerAngles(Vector3 e) { set_localRotation(Quaternion.Euler(e)); }

    // ------------------------------------------------------------------ world
    public Vector3 get_position() { float[] m = world(); return new Vector3(m[12], m[13], m[14]); }
    public void set_position(Vector3 v) {
        if (parent == null) set_localPosition(v);
        else set_localPosition(parent.InverseTransformPoint(v));
    }
    public Quaternion get_rotation() {
        Quaternion q = lr.$copy();
        for (Transform p = parent; p != null; p = p.parent) q = Quaternion.op_Multiply(p.lr, q);
        return q;
    }
    public void set_rotation(Quaternion q) {
        if (parent == null) set_localRotation(q);
        else set_localRotation(Quaternion.op_Multiply(Quaternion.Inverse(parent.get_rotation()), q));
    }
    public Vector3 get_eulerAngles() { return get_rotation().get_eulerAngles(); }
    public void set_eulerAngles(Vector3 e) { set_rotation(Quaternion.Euler(e)); }
    public Vector3 get_lossyScale() {
        Vector3 s = ls.$copy();
        for (Transform p = parent; p != null; p = p.parent) { s.x *= p.ls.x; s.y *= p.ls.y; s.z *= p.ls.z; }
        return s;
    }
    public Vector3 get_right() { return Quaternion.op_Multiply(get_rotation(), Vector3.get_right()); }
    public void set_right(Vector3 v) { set_rotation(Quaternion.FromToRotation(Vector3.get_right(), v)); }
    public Vector3 get_up() { return Quaternion.op_Multiply(get_rotation(), Vector3.get_up()); }
    public void set_up(Vector3 v) { set_rotation(Quaternion.FromToRotation(Vector3.get_up(), v)); }
    public Vector3 get_forward() { return Quaternion.op_Multiply(get_rotation(), Vector3.get_forward()); }
    public void set_forward(Vector3 v) { set_rotation(Quaternion.LookRotation(v)); }
    public void SetPositionAndRotation(Vector3 p, Quaternion q) { set_position(p); set_rotation(q); }
    public void SetLocalPositionAndRotation(Vector3 p, Quaternion q) { set_localPosition(p); set_localRotation(q); }

    public Matrix4x4 get_localToWorldMatrix() { Matrix4x4 m = new Matrix4x4(); System.arraycopy(world(), 0, m.m, 0, 16); return m; }
    public Matrix4x4 get_worldToLocalMatrix() { return Matrix4x4.Inverse(get_localToWorldMatrix()); }

    public Vector3 TransformPoint(Vector3 v) {
        float[] m = world();
        return new Vector3(m[0] * v.x + m[4] * v.y + m[8] * v.z + m[12], m[1] * v.x + m[5] * v.y + m[9] * v.z + m[13], m[2] * v.x + m[6] * v.y + m[10] * v.z + m[14]);
    }
    public Vector3 TransformPoint(float x, float y, float z) { return TransformPoint(new Vector3(x, y, z)); }
    public Vector3 TransformVector(Vector3 v) {
        float[] m = world();
        return new Vector3(m[0] * v.x + m[4] * v.y + m[8] * v.z, m[1] * v.x + m[5] * v.y + m[9] * v.z, m[2] * v.x + m[6] * v.y + m[10] * v.z);
    }
    public Vector3 TransformDirection(Vector3 v) { return Quaternion.op_Multiply(get_rotation(), v); }
    public Vector3 InverseTransformPoint(Vector3 v) {
        // inverse of TRS chain: apply inverse of each level from root to this
        Vector3 p = parent == null ? v.$copy() : parent.InverseTransformPoint(v);
        p = Vector3.op_Subtraction(p, lp);
        p = Quaternion.op_Multiply(Quaternion.Inverse(lr), p);
        p.x = ls.x == 0 ? 0 : p.x / ls.x;
        p.y = ls.y == 0 ? 0 : p.y / ls.y;
        p.z = ls.z == 0 ? 0 : p.z / ls.z;
        return p;
    }
    public Vector3 InverseTransformPoint(float x, float y, float z) { return InverseTransformPoint(new Vector3(x, y, z)); }
    public Vector3 InverseTransformVector(Vector3 v) {
        Vector3 p = parent == null ? v.$copy() : parent.InverseTransformVector(v);
        p = Quaternion.op_Multiply(Quaternion.Inverse(lr), p);
        p.x = ls.x == 0 ? 0 : p.x / ls.x;
        p.y = ls.y == 0 ? 0 : p.y / ls.y;
        p.z = ls.z == 0 ? 0 : p.z / ls.z;
        return p;
    }
    public Vector3 InverseTransformDirection(Vector3 v) { return Quaternion.op_Multiply(Quaternion.Inverse(get_rotation()), v); }

    public void Translate(Vector3 v) { Translate(v, 1); }
    /** Space: 0 World, 1 Self */
    public void Translate(Vector3 v, int space) {
        if (space == 0) set_position(Vector3.op_Addition(get_position(), v));
        else set_position(Vector3.op_Addition(get_position(), TransformDirection(v)));
    }
    public void Translate(float x, float y, float z) { Translate(new Vector3(x, y, z), 1); }
    public void Translate(Vector3 v, Transform relativeTo) {
        if (relativeTo != null) set_position(Vector3.op_Addition(get_position(), relativeTo.TransformDirection(v)));
        else Translate(v, 0);
    }
    public void Rotate(Vector3 euler) { Rotate(euler, 1); }
    public void Rotate(Vector3 euler, int space) {
        Quaternion q = Quaternion.Euler(euler);
        if (space == 1) set_localRotation(Quaternion.op_Multiply(lr, q));
        else set_rotation(Quaternion.op_Multiply(q, get_rotation()));
    }
    public void Rotate(float x, float y, float z) { Rotate(new Vector3(x, y, z), 1); }
    public void Rotate(Vector3 axis, float angle) { set_localRotation(Quaternion.op_Multiply(lr, Quaternion.AngleAxis(angle, axis))); }
    public void RotateAround(Vector3 point, Vector3 axis, float angle) {
        Quaternion q = Quaternion.AngleAxis(angle, axis);
        Vector3 d = Vector3.op_Subtraction(get_position(), point);
        set_position(Vector3.op_Addition(point, Quaternion.op_Multiply(q, d)));
        set_rotation(Quaternion.op_Multiply(q, get_rotation()));
    }
    public void LookAt(Transform t) { LookAt(t.get_position()); }
    public void LookAt(Vector3 p) { set_rotation(Quaternion.LookRotation(Vector3.op_Subtraction(p, get_position()))); }

    public boolean get_hasChanged() { return m_HasChanged; }
    public void set_hasChanged(boolean v) { m_HasChanged = v; }

    // ------------------------------------------------------------------ hierarchy
    public Transform get_parent() { return parent; }
    public void set_parent(Transform p) { SetParent(p, true); }
    public void SetParent(Transform p) { SetParent(p, true); }
    public void SetParent(Transform p, boolean worldPositionStays) {
        if (p == parent) return;
        for (Transform q = p; q != null; q = q.parent) if (q == this) return;   // would create a cycle
        Vector3 wp = null, wsc = null;
        Quaternion wr = null;
        if (worldPositionStays) { wp = get_position(); wr = get_rotation(); wsc = get_lossyScale(); }
        Transform old = parent;
        if (old != null) old.children.remove(this);
        else Eng.removeRoot(gameObject);
        parent = p;
        if (p != null) p.children.add(this);
        else Eng.addRoot(gameObject);
        dirty();
        if (worldPositionStays) {
            set_position(wp);
            set_rotation(wr);
            Vector3 ps = p == null ? new Vector3(1, 1, 1) : p.get_lossyScale();
            set_localScale(new Vector3(ps.x == 0 ? 0 : wsc.x / ps.x, ps.y == 0 ? 0 : wsc.y / ps.y, ps.z == 0 ? 0 : wsc.z / ps.z));
        }
        if (p != null && gameObject != null && p.gameObject != null && gameObject.scene != p.gameObject.scene) Eng.moveToScene(gameObject, p.gameObject.scene);
        $parentChanged();
        Eng.hierarchyChanged(this, old);
    }
    public void $parentChanged() {}

    public int get_childCount() { return children.size(); }
    public Transform GetChild(int i) {
        if (i < 0 || i >= children.size()) throw new UnityException("Transform child out of bounds");
        return children.get(i);
    }
    public int GetSiblingIndex() { return parent == null ? Eng.rootIndex(gameObject) : parent.children.indexOf(this); }
    public void SetSiblingIndex(int i) {
        if (parent == null) { Eng.setRootIndex(gameObject, i); return; }
        ArrayList<Transform> c = parent.children;
        c.remove(this);
        if (i < 0) i = 0;
        if (i > c.size()) i = c.size();
        c.add(i, this);
        Eng.siblingOrderChanged(parent);
    }
    public void SetAsFirstSibling() { SetSiblingIndex(0); }
    public void SetAsLastSibling() { SetSiblingIndex(Integer.MAX_VALUE); }
    public Transform get_root() { Transform t = this; while (t.parent != null) t = t.parent; return t; }
    public boolean IsChildOf(Transform p) {
        for (Transform t = this; t != null; t = t.parent) if (t == p) return true;
        return false;
    }
    public Transform Find(String path) {
        Transform cur = this;
        for (String part : path.split("/")) {
            Transform next = null;
            for (Transform c : cur.children) if (c.gameObject.m_Name.equals(part)) { next = c; break; }
            if (next == null) return null;
            cur = next;
        }
        return cur;
    }
    public Transform FindChild(String n) { return Find(n); }
    public void DetachChildren() {
        for (Transform c : new ArrayList<>(children)) c.SetParent(null, true);
    }
    public int get_hierarchyCount() { int n = 1; for (Transform c : children) n += c.get_hierarchyCount(); return n; }

    /** foreach over children (by index, like Unity's Transform enumerator) */
    public cs.System.Collections.IEnumerator GetEnumerator() {
        return new cs.System.Collections.IEnumerator() {
            int i = -1;
            public boolean MoveNext() { return ++i < children.size(); }
            public Object get_Current() { return children.get(i); }
            public void Reset() { i = -1; }
        };
    }

    @Override public void $load(java.util.HashMap<String, Object> d, Loader.Ctx ctx) {
        Loader.vec3(d.get("m_LocalPosition"), lp);
        Loader.quat(d.get("m_LocalRotation"), lr);
        Loader.vec3(d.get("m_LocalScale"), ls);
        dirty();
    }

    @Override public void $cloneFrom(Component src, Eng.CloneCtx ctx) {
        Transform s = (Transform) src;
        lp.$set(s.lp);
        lr.$set(s.lr);
        ls.$set(s.ls);
        dirty();
    }
}
