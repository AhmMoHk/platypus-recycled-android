package cs.UnityEngine;

public class Vector2 implements cs.rt.Struct {
    public float x, y;
    public static final float kEpsilon = 0.00001F;
    public static final float kEpsilonNormalSqrt = 1e-15f;

    public Vector2() {}
    public Vector2(float x, float y) { this.x = x; this.y = y; }

    public Vector2 $copy() { return new Vector2(x, y); }
    public void $set(Object o) { Vector2 v = (Vector2) o; x = v.x; y = v.y; }
    public void $clear() { x = y = 0; }
    public static Vector2[] $newArray(int n) { Vector2[] a = new Vector2[n]; for (int i = 0; i < n; i++) a[i] = new Vector2(); return a; }
    /** in-place constructor */
    public void $ctor(float x, float y) { this.x = x; this.y = y; }

    public float get_Item(int i) {
        if (i == 0) return x;
        if (i == 1) return y;
        throw new cs.System.IndexOutOfRangeException("Invalid Vector2 index!");
    }
    public void set_Item(int i, float v) {
        if (i == 0) x = v; else if (i == 1) y = v; else throw new cs.System.IndexOutOfRangeException("Invalid Vector2 index!");
    }
    public void Set(float nx, float ny) { x = nx; y = ny; }

    public float get_magnitude() { return (float) Math.sqrt(x * x + y * y); }
    public float get_sqrMagnitude() { return x * x + y * y; }
    public Vector2 get_normalized() { Vector2 v = new Vector2(x, y); v.Normalize(); return v; }
    public void Normalize() {
        float mag = get_magnitude();
        if (mag > kEpsilon) { x /= mag; y /= mag; } else { x = 0; y = 0; }
    }
    public static Vector2 get_zero() { return new Vector2(0, 0); }
    public static Vector2 get_one() { return new Vector2(1, 1); }
    public static Vector2 get_up() { return new Vector2(0, 1); }
    public static Vector2 get_down() { return new Vector2(0, -1); }
    public static Vector2 get_left() { return new Vector2(-1, 0); }
    public static Vector2 get_right() { return new Vector2(1, 0); }
    public static Vector2 get_positiveInfinity() { return new Vector2(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY); }
    public static Vector2 get_negativeInfinity() { return new Vector2(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY); }

    public static Vector2 Lerp(Vector2 a, Vector2 b, float t) {
        t = Mathf.Clamp01(t);
        return new Vector2(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t);
    }
    public static Vector2 LerpUnclamped(Vector2 a, Vector2 b, float t) { return new Vector2(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t); }
    public static Vector2 MoveTowards(Vector2 current, Vector2 target, float maxDistanceDelta) {
        float toX = target.x - current.x, toY = target.y - current.y;
        float sqDist = toX * toX + toY * toY;
        if (sqDist == 0 || (maxDistanceDelta >= 0 && sqDist <= maxDistanceDelta * maxDistanceDelta)) return target.$copy();
        float dist = (float) Math.sqrt(sqDist);
        return new Vector2(current.x + toX / dist * maxDistanceDelta, current.y + toY / dist * maxDistanceDelta);
    }
    public static Vector2 Scale(Vector2 a, Vector2 b) { return new Vector2(a.x * b.x, a.y * b.y); }
    public void Scale(Vector2 s) { x *= s.x; y *= s.y; }
    public static float Dot(Vector2 a, Vector2 b) { return a.x * b.x + a.y * b.y; }
    public static Vector2 Perpendicular(Vector2 d) { return new Vector2(-d.y, d.x); }
    public static Vector2 Reflect(Vector2 inDirection, Vector2 inNormal) {
        float f = -2F * Dot(inNormal, inDirection);
        return new Vector2(f * inNormal.x + inDirection.x, f * inNormal.y + inDirection.y);
    }
    public static float Angle(Vector2 from, Vector2 to) {
        float denominator = (float) Math.sqrt(from.get_sqrMagnitude() * to.get_sqrMagnitude());
        if (denominator < kEpsilonNormalSqrt) return 0F;
        float dot = Mathf.Clamp(Dot(from, to) / denominator, -1F, 1F);
        return (float) Math.acos(dot) * Mathf.Rad2Deg;
    }
    public static float SignedAngle(Vector2 from, Vector2 to) {
        float unsigned = Angle(from, to);
        float sign = Mathf.Sign(from.x * to.y - from.y * to.x);
        return unsigned * sign;
    }
    public static float Distance(Vector2 a, Vector2 b) {
        float dx = a.x - b.x, dy = a.y - b.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    public static Vector2 ClampMagnitude(Vector2 v, float max) {
        float sq = v.get_sqrMagnitude();
        if (sq > max * max) { float m = (float) Math.sqrt(sq); return new Vector2(v.x / m * max, v.y / m * max); }
        return v.$copy();
    }
    public static Vector2 Min(Vector2 a, Vector2 b) { return new Vector2(Math.min(a.x, b.x), Math.min(a.y, b.y)); }
    public static Vector2 Max(Vector2 a, Vector2 b) { return new Vector2(Math.max(a.x, b.x), Math.max(a.y, b.y)); }

    public static Vector2 op_Addition(Vector2 a, Vector2 b) { return new Vector2(a.x + b.x, a.y + b.y); }
    public static Vector2 op_Subtraction(Vector2 a, Vector2 b) { return new Vector2(a.x - b.x, a.y - b.y); }
    public static Vector2 op_Multiply(Vector2 a, Vector2 b) { return new Vector2(a.x * b.x, a.y * b.y); }
    public static Vector2 op_Division(Vector2 a, Vector2 b) { return new Vector2(a.x / b.x, a.y / b.y); }
    public static Vector2 op_UnaryNegation(Vector2 a) { return new Vector2(-a.x, -a.y); }
    public static Vector2 op_Multiply(Vector2 a, float d) { return new Vector2(a.x * d, a.y * d); }
    public static Vector2 op_Multiply(float d, Vector2 a) { return new Vector2(a.x * d, a.y * d); }
    public static Vector2 op_Division(Vector2 a, float d) { return new Vector2(a.x / d, a.y / d); }
    public static boolean op_Equality(Vector2 l, Vector2 r) {
        float dx = l.x - r.x, dy = l.y - r.y;
        return (dx * dx + dy * dy) < kEpsilon * kEpsilon;
    }
    public static boolean op_Inequality(Vector2 l, Vector2 r) { return !op_Equality(l, r); }
    public static Vector2 op_Implicit$Vector2(Vector3 v) { return new Vector2(v.x, v.y); }
    public static Vector3 op_Implicit$Vector3(Vector2 v) { return new Vector3(v.x, v.y, 0); }

    public boolean Equals(Vector2 o) { return x == o.x && y == o.y; }
    @Override public boolean equals(Object o) { return o instanceof Vector2 && Equals((Vector2) o); }
    @Override public int hashCode() { return Float.floatToIntBits(x) ^ (Float.floatToIntBits(y) << 2); }
    @Override public String toString() { return "(" + Vector3.f2(x) + ", " + Vector3.f2(y) + ")"; }
    public String ToString(String f) { return "(" + cs.rt.Fmt.formatDouble(x, f, true) + ", " + cs.rt.Fmt.formatDouble(y, f, true) + ")"; }
}
