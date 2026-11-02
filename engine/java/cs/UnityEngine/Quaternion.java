package cs.UnityEngine;

public class Quaternion implements cs.rt.Struct {
    public float x, y, z, w;
    public static final float kEpsilon = 0.000001F;

    public Quaternion() {}
    public Quaternion(float x, float y, float z, float w) { this.x = x; this.y = y; this.z = z; this.w = w; }
    public Quaternion $copy() { return new Quaternion(x, y, z, w); }
    public void $set(Object o) { Quaternion q = (Quaternion) o; x = q.x; y = q.y; z = q.z; w = q.w; }
    public void $clear() { x = y = z = w = 0; }
    public static Quaternion[] $newArray(int n) { Quaternion[] a = new Quaternion[n]; for (int i = 0; i < n; i++) a[i] = new Quaternion(); return a; }

    public static Quaternion get_identity() { return new Quaternion(0, 0, 0, 1); }

    /** Unity: rotation around z, then x, then y (degrees). */
    public static Quaternion Euler(float ex, float ey, float ez) {
        double hx = ex * Math.PI / 360.0, hy = ey * Math.PI / 360.0, hz = ez * Math.PI / 360.0;
        double cx = Math.cos(hx), sx = Math.sin(hx), cy = Math.cos(hy), sy = Math.sin(hy), cz = Math.cos(hz), sz = Math.sin(hz);
        return new Quaternion(
            (float) (sx * cy * cz + cx * sy * sz),
            (float) (cx * sy * cz - sx * cy * sz),
            (float) (cx * cy * sz - sx * sy * cz),
            (float) (cx * cy * cz + sx * sy * sz));
    }
    public static Quaternion Euler(Vector3 e) { return Euler(e.x, e.y, e.z); }
    public static Quaternion AngleAxis(float angle, Vector3 axis) {
        Vector3 n = Vector3.Normalize(axis);
        double h = angle * Math.PI / 360.0, s = Math.sin(h);
        return new Quaternion((float) (n.x * s), (float) (n.y * s), (float) (n.z * s), (float) Math.cos(h));
    }

    public Vector3 get_eulerAngles() {
        // inverse of Euler (ZXY): returns angles in [0,360)
        float sinX = 2f * (w * x - y * z);
        float ex, ey, ez;
        if (Math.abs(sinX) >= 0.99999f) {
            ex = (float) Math.copySign(Math.PI / 2, sinX);
            ey = (float) Math.atan2(-2f * (x * z - w * y), 1f - 2f * (y * y + z * z));
            ez = 0;
        } else {
            ex = (float) Math.asin(sinX);
            ey = (float) Math.atan2(2f * (w * y + x * z), 1f - 2f * (x * x + y * y));
            ez = (float) Math.atan2(2f * (w * z + x * y), 1f - 2f * (x * x + z * z));
        }
        return new Vector3(norm(ex * Mathf.Rad2Deg), norm(ey * Mathf.Rad2Deg), norm(ez * Mathf.Rad2Deg));
    }
    private static float norm(float a) {
        a %= 360f;
        if (a < 0) a += 360f;
        if (a >= 360f) a -= 360f;
        return a;
    }
    public void set_eulerAngles(Vector3 e) { $set(Euler(e)); }

    public static Quaternion Inverse(Quaternion q) { return new Quaternion(-q.x, -q.y, -q.z, q.w); }
    public static float Dot(Quaternion a, Quaternion b) { return a.x * b.x + a.y * b.y + a.z * b.z + a.w * b.w; }
    public static float Angle(Quaternion a, Quaternion b) {
        float dot = Math.min(Math.abs(Dot(a, b)), 1.0F);
        return dot > 1.0f - kEpsilon ? 0.0f : (float) Math.acos(dot) * 2.0F * Mathf.Rad2Deg;
    }
    public static Quaternion Normalize(Quaternion q) {
        float mag = (float) Math.sqrt(Dot(q, q));
        if (mag < Mathf.Epsilon) return get_identity();
        return new Quaternion(q.x / mag, q.y / mag, q.z / mag, q.w / mag);
    }
    public Quaternion get_normalized() { return Normalize(this); }
    public static Quaternion Slerp(Quaternion a, Quaternion b, float t) { return SlerpUnclamped(a, b, Mathf.Clamp01(t)); }
    public static Quaternion SlerpUnclamped(Quaternion a, Quaternion b, float t) {
        float cos = Dot(a, b);
        float bx = b.x, by = b.y, bz = b.z, bw = b.w;
        if (cos < 0) { cos = -cos; bx = -bx; by = -by; bz = -bz; bw = -bw; }
        float k0, k1;
        if (cos > 0.9995f) { k0 = 1 - t; k1 = t; }
        else {
            double th = Math.acos(cos), s = Math.sin(th);
            k0 = (float) (Math.sin((1 - t) * th) / s);
            k1 = (float) (Math.sin(t * th) / s);
        }
        return Normalize(new Quaternion(a.x * k0 + bx * k1, a.y * k0 + by * k1, a.z * k0 + bz * k1, a.w * k0 + bw * k1));
    }
    public static Quaternion Lerp(Quaternion a, Quaternion b, float t) {
        t = Mathf.Clamp01(t);
        float s = Dot(a, b) < 0 ? -1 : 1;
        return Normalize(new Quaternion(a.x + (b.x * s - a.x) * t, a.y + (b.y * s - a.y) * t, a.z + (b.z * s - a.z) * t, a.w + (b.w * s - a.w) * t));
    }
    public static Quaternion RotateTowards(Quaternion from, Quaternion to, float maxDegreesDelta) {
        float angle = Angle(from, to);
        if (angle == 0.0f) return to.$copy();
        return SlerpUnclamped(from, to, Math.min(1.0f, maxDegreesDelta / angle));
    }
    public static Quaternion LookRotation(Vector3 forward, Vector3 up) {
        Vector3 f = Vector3.Normalize(forward);
        Vector3 r = Vector3.Normalize(Vector3.Cross(up, f));
        Vector3 u = Vector3.Cross(f, r);
        float m00 = r.x, m01 = u.x, m02 = f.x, m10 = r.y, m11 = u.y, m12 = f.y, m20 = r.z, m21 = u.z, m22 = f.z;
        float tr = m00 + m11 + m22;
        Quaternion q = new Quaternion();
        if (tr > 0) {
            float s = (float) Math.sqrt(tr + 1.0) * 2;
            q.w = 0.25f * s; q.x = (m21 - m12) / s; q.y = (m02 - m20) / s; q.z = (m10 - m01) / s;
        } else if (m00 > m11 && m00 > m22) {
            float s = (float) Math.sqrt(1.0 + m00 - m11 - m22) * 2;
            q.w = (m21 - m12) / s; q.x = 0.25f * s; q.y = (m01 + m10) / s; q.z = (m02 + m20) / s;
        } else if (m11 > m22) {
            float s = (float) Math.sqrt(1.0 + m11 - m00 - m22) * 2;
            q.w = (m02 - m20) / s; q.x = (m01 + m10) / s; q.y = 0.25f * s; q.z = (m12 + m21) / s;
        } else {
            float s = (float) Math.sqrt(1.0 + m22 - m00 - m11) * 2;
            q.w = (m10 - m01) / s; q.x = (m02 + m20) / s; q.y = (m12 + m21) / s; q.z = 0.25f * s;
        }
        return q;
    }
    public static Quaternion LookRotation(Vector3 forward) { return LookRotation(forward, Vector3.get_up()); }
    public static Quaternion FromToRotation(Vector3 from, Vector3 to) {
        Vector3 a = Vector3.Normalize(from), b = Vector3.Normalize(to);
        float d = Vector3.Dot(a, b);
        if (d >= 1f - 1e-6f) return get_identity();
        if (d <= -1f + 1e-6f) {
            Vector3 axis = Vector3.Cross(Vector3.get_right(), a);
            if (axis.get_sqrMagnitude() < 1e-6f) axis = Vector3.Cross(Vector3.get_up(), a);
            return AngleAxis(180, axis);
        }
        Vector3 c = Vector3.Cross(a, b);
        return Normalize(new Quaternion(c.x, c.y, c.z, 1 + d));
    }

    public static Quaternion op_Multiply(Quaternion l, Quaternion r) {
        return new Quaternion(
            l.w * r.x + l.x * r.w + l.y * r.z - l.z * r.y,
            l.w * r.y + l.y * r.w + l.z * r.x - l.x * r.z,
            l.w * r.z + l.z * r.w + l.x * r.y - l.y * r.x,
            l.w * r.w - l.x * r.x - l.y * r.y - l.z * r.z);
    }
    public static Vector3 op_Multiply(Quaternion r, Vector3 p) {
        float x = r.x * 2F, y = r.y * 2F, z = r.z * 2F;
        float xx = r.x * x, yy = r.y * y, zz = r.z * z, xy = r.x * y, xz = r.x * z, yz = r.y * z;
        float wx = r.w * x, wy = r.w * y, wz = r.w * z;
        return new Vector3(
            (1F - (yy + zz)) * p.x + (xy - wz) * p.y + (xz + wy) * p.z,
            (xy + wz) * p.x + (1F - (xx + zz)) * p.y + (yz - wx) * p.z,
            (xz - wy) * p.x + (yz + wx) * p.y + (1F - (xx + yy)) * p.z);
    }
    private static boolean isEqualUsingDot(float dot) { return dot > 1.0f - kEpsilon; }
    public static boolean op_Equality(Quaternion a, Quaternion b) { return isEqualUsingDot(Dot(a, b)); }
    public static boolean op_Inequality(Quaternion a, Quaternion b) { return !op_Equality(a, b); }
    public boolean Equals(Quaternion o) { return x == o.x && y == o.y && z == o.z && w == o.w; }
    @Override public boolean equals(Object o) { return o instanceof Quaternion && Equals((Quaternion) o); }
    @Override public int hashCode() { return Float.floatToIntBits(x) ^ (Float.floatToIntBits(y) << 2) ^ (Float.floatToIntBits(z) >> 2) ^ (Float.floatToIntBits(w) >> 1); }
    @Override public String toString() { return "(" + Vector3.f2(x) + ", " + Vector3.f2(y) + ", " + Vector3.f2(z) + ", " + Vector3.f2(w) + ")"; }
}
