package cs.UnityEngine;

public class Vector3 implements cs.rt.Struct {
    public float x, y, z;
    public static final float kEpsilon = 0.00001F;
    public static final float kEpsilonNormalSqrt = 1e-15F;

    public Vector3() {}
    public Vector3(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }
    public Vector3(float x, float y) { this.x = x; this.y = y; }

    public Vector3 $copy() { return new Vector3(x, y, z); }
    public void $set(Object o) { Vector3 v = (Vector3) o; x = v.x; y = v.y; z = v.z; }
    public void $clear() { x = y = z = 0; }
    public static Vector3[] $newArray(int n) { Vector3[] a = new Vector3[n]; for (int i = 0; i < n; i++) a[i] = new Vector3(); return a; }
    public void Set(float nx, float ny, float nz) { x = nx; y = ny; z = nz; }

    static String f2(float v) { return cs.rt.Fmt.formatDouble(v, "F2", true); }

    public float get_Item(int i) {
        switch (i) { case 0: return x; case 1: return y; case 2: return z; }
        throw new cs.System.IndexOutOfRangeException("Invalid Vector3 index!");
    }
    public void set_Item(int i, float v) {
        switch (i) { case 0: x = v; return; case 1: y = v; return; case 2: z = v; return; }
        throw new cs.System.IndexOutOfRangeException("Invalid Vector3 index!");
    }

    public float get_magnitude() { return (float) Math.sqrt(x * x + y * y + z * z); }
    public float get_sqrMagnitude() { return x * x + y * y + z * z; }
    public static float Magnitude(Vector3 v) { return v.get_magnitude(); }
    public static float SqrMagnitude(Vector3 v) { return v.get_sqrMagnitude(); }
    public Vector3 get_normalized() { return Normalize(this); }
    public static Vector3 Normalize(Vector3 v) {
        float mag = v.get_magnitude();
        if (mag > kEpsilon) return new Vector3(v.x / mag, v.y / mag, v.z / mag);
        return new Vector3();
    }
    public void Normalize() {
        float mag = get_magnitude();
        if (mag > kEpsilon) { x /= mag; y /= mag; z /= mag; } else { x = y = z = 0; }
    }

    public static Vector3 get_zero() { return new Vector3(); }
    public static Vector3 get_one() { return new Vector3(1, 1, 1); }
    public static Vector3 get_up() { return new Vector3(0, 1, 0); }
    public static Vector3 get_down() { return new Vector3(0, -1, 0); }
    public static Vector3 get_left() { return new Vector3(-1, 0, 0); }
    public static Vector3 get_right() { return new Vector3(1, 0, 0); }
    public static Vector3 get_forward() { return new Vector3(0, 0, 1); }
    public static Vector3 get_back() { return new Vector3(0, 0, -1); }
    public static Vector3 get_positiveInfinity() { return new Vector3(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY); }
    public static Vector3 get_negativeInfinity() { return new Vector3(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY); }

    public static Vector3 Lerp(Vector3 a, Vector3 b, float t) {
        t = Mathf.Clamp01(t);
        return new Vector3(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t);
    }
    public static Vector3 LerpUnclamped(Vector3 a, Vector3 b, float t) {
        return new Vector3(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t);
    }
    public static Vector3 MoveTowards(Vector3 current, Vector3 target, float maxDistanceDelta) {
        float toX = target.x - current.x, toY = target.y - current.y, toZ = target.z - current.z;
        float sqdist = toX * toX + toY * toY + toZ * toZ;
        if (sqdist == 0 || (maxDistanceDelta >= 0 && sqdist <= maxDistanceDelta * maxDistanceDelta)) return target.$copy();
        float dist = (float) Math.sqrt(sqdist);
        return new Vector3(current.x + toX / dist * maxDistanceDelta, current.y + toY / dist * maxDistanceDelta, current.z + toZ / dist * maxDistanceDelta);
    }
    public static Vector3 Slerp(Vector3 a, Vector3 b, float t) {
        t = Mathf.Clamp01(t);
        return SlerpUnclamped(a, b, t);
    }
    public static Vector3 SlerpUnclamped(Vector3 a, Vector3 b, float t) {
        float ma = a.get_magnitude(), mb = b.get_magnitude();
        if (ma < kEpsilon || mb < kEpsilon) return LerpUnclamped(a, b, t);
        Vector3 na = op_Division(a, ma), nb = op_Division(b, mb);
        float dot = Mathf.Clamp(Dot(na, nb), -1f, 1f);
        float theta = (float) Math.acos(dot) * t;
        Vector3 rel = op_Subtraction(nb, op_Multiply(na, dot));
        rel.Normalize();
        Vector3 dir = op_Addition(op_Multiply(na, (float) Math.cos(theta)), op_Multiply(rel, (float) Math.sin(theta)));
        if (dot > 1 - 1e-6f) dir = Normalize(LerpUnclamped(na, nb, t));
        return op_Multiply(dir, ma + (mb - ma) * t);
    }
    public static Vector3 Scale(Vector3 a, Vector3 b) { return new Vector3(a.x * b.x, a.y * b.y, a.z * b.z); }
    public void Scale(Vector3 s) { x *= s.x; y *= s.y; z *= s.z; }
    public static Vector3 Cross(Vector3 l, Vector3 r) { return new Vector3(l.y * r.z - l.z * r.y, l.z * r.x - l.x * r.z, l.x * r.y - l.y * r.x); }
    public static float Dot(Vector3 l, Vector3 r) { return l.x * r.x + l.y * r.y + l.z * r.z; }
    public static Vector3 Project(Vector3 v, Vector3 n) {
        float sqrMag = Dot(n, n);
        if (sqrMag < Mathf.Epsilon) return new Vector3();
        float dot = Dot(v, n);
        return new Vector3(n.x * dot / sqrMag, n.y * dot / sqrMag, n.z * dot / sqrMag);
    }
    public static float Angle(Vector3 from, Vector3 to) {
        float denominator = (float) Math.sqrt(from.get_sqrMagnitude() * to.get_sqrMagnitude());
        if (denominator < kEpsilonNormalSqrt) return 0F;
        float dot = Mathf.Clamp(Dot(from, to) / denominator, -1F, 1F);
        return (float) Math.acos(dot) * Mathf.Rad2Deg;
    }
    public static float SignedAngle(Vector3 from, Vector3 to, Vector3 axis) {
        float ua = Angle(from, to);
        float cx = from.y * to.z - from.z * to.y, cy = from.z * to.x - from.x * to.z, cz = from.x * to.y - from.y * to.x;
        float sign = Mathf.Sign(axis.x * cx + axis.y * cy + axis.z * cz);
        return ua * sign;
    }
    public static float Distance(Vector3 a, Vector3 b) {
        float dx = a.x - b.x, dy = a.y - b.y, dz = a.z - b.z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    public static Vector3 ClampMagnitude(Vector3 v, float max) {
        float sq = v.get_sqrMagnitude();
        if (sq > max * max) { float m = (float) Math.sqrt(sq); return new Vector3(v.x / m * max, v.y / m * max, v.z / m * max); }
        return v.$copy();
    }
    public static Vector3 Min(Vector3 a, Vector3 b) { return new Vector3(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z)); }
    public static Vector3 Max(Vector3 a, Vector3 b) { return new Vector3(Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z)); }
    public static Vector3 Reflect(Vector3 inDirection, Vector3 inNormal) {
        float f = -2F * Dot(inNormal, inDirection);
        return new Vector3(f * inNormal.x + inDirection.x, f * inNormal.y + inDirection.y, f * inNormal.z + inDirection.z);
    }

    public static Vector3 op_Addition(Vector3 a, Vector3 b) { return new Vector3(a.x + b.x, a.y + b.y, a.z + b.z); }
    public static Vector3 op_Subtraction(Vector3 a, Vector3 b) { return new Vector3(a.x - b.x, a.y - b.y, a.z - b.z); }
    public static Vector3 op_UnaryNegation(Vector3 a) { return new Vector3(-a.x, -a.y, -a.z); }
    public static Vector3 op_Multiply(Vector3 a, float d) { return new Vector3(a.x * d, a.y * d, a.z * d); }
    public static Vector3 op_Multiply(float d, Vector3 a) { return new Vector3(a.x * d, a.y * d, a.z * d); }
    public static Vector3 op_Division(Vector3 a, float d) { return new Vector3(a.x / d, a.y / d, a.z / d); }
    public static boolean op_Equality(Vector3 l, Vector3 r) {
        float dx = l.x - r.x, dy = l.y - r.y, dz = l.z - r.z;
        return dx * dx + dy * dy + dz * dz < kEpsilon * kEpsilon;
    }
    public static boolean op_Inequality(Vector3 l, Vector3 r) { return !op_Equality(l, r); }

    public boolean Equals(Vector3 o) { return x == o.x && y == o.y && z == o.z; }
    @Override public boolean equals(Object o) { return o instanceof Vector3 && Equals((Vector3) o); }
    @Override public int hashCode() { return Float.floatToIntBits(x) ^ (Float.floatToIntBits(y) << 2) ^ (Float.floatToIntBits(z) >> 2); }
    @Override public String toString() { return "(" + f2(x) + ", " + f2(y) + ", " + f2(z) + ")"; }
    public String ToString(String f) { return "(" + cs.rt.Fmt.formatDouble(x, f, true) + ", " + cs.rt.Fmt.formatDouble(y, f, true) + ", " + cs.rt.Fmt.formatDouble(z, f, true) + ")"; }
}
