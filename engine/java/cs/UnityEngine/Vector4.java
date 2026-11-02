package cs.UnityEngine;

public class Vector4 implements cs.rt.Struct {
    public float x, y, z, w;
    public Vector4() {}
    public Vector4(float x, float y, float z, float w) { this.x = x; this.y = y; this.z = z; this.w = w; }
    public Vector4(float x, float y, float z) { this(x, y, z, 0); }
    public Vector4(float x, float y) { this(x, y, 0, 0); }
    public Vector4 $copy() { return new Vector4(x, y, z, w); }
    public void $set(Object o) { Vector4 v = (Vector4) o; x = v.x; y = v.y; z = v.z; w = v.w; }
    public void $clear() { x = y = z = w = 0; }
    public static Vector4[] $newArray(int n) { Vector4[] a = new Vector4[n]; for (int i = 0; i < n; i++) a[i] = new Vector4(); return a; }
    public float get_Item(int i) {
        switch (i) { case 0: return x; case 1: return y; case 2: return z; case 3: return w; }
        throw new cs.System.IndexOutOfRangeException("Invalid Vector4 index!");
    }
    public void set_Item(int i, float v) {
        switch (i) { case 0: x = v; return; case 1: y = v; return; case 2: z = v; return; case 3: w = v; return; }
        throw new cs.System.IndexOutOfRangeException("Invalid Vector4 index!");
    }
    public float get_sqrMagnitude() { return x * x + y * y + z * z + w * w; }
    public float get_magnitude() { return (float) Math.sqrt(get_sqrMagnitude()); }
    public static Vector4 get_zero() { return new Vector4(); }
    public static Vector4 get_one() { return new Vector4(1, 1, 1, 1); }
    public static Vector4 Lerp(Vector4 a, Vector4 b, float t) {
        t = Mathf.Clamp01(t);
        return new Vector4(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t, a.w + (b.w - a.w) * t);
    }
    public static Vector4 op_Addition(Vector4 a, Vector4 b) { return new Vector4(a.x + b.x, a.y + b.y, a.z + b.z, a.w + b.w); }
    public static Vector4 op_Subtraction(Vector4 a, Vector4 b) { return new Vector4(a.x - b.x, a.y - b.y, a.z - b.z, a.w - b.w); }
    public static Vector4 op_Multiply(Vector4 a, float d) { return new Vector4(a.x * d, a.y * d, a.z * d, a.w * d); }
    public static Vector4 op_Multiply(float d, Vector4 a) { return op_Multiply(a, d); }
    public static Vector4 op_Division(Vector4 a, float d) { return new Vector4(a.x / d, a.y / d, a.z / d, a.w / d); }
    public static boolean op_Equality(Vector4 l, Vector4 r) {
        float dx = l.x - r.x, dy = l.y - r.y, dz = l.z - r.z, dw = l.w - r.w;
        return dx * dx + dy * dy + dz * dz + dw * dw < 9.99999944E-11f;
    }
    public static boolean op_Inequality(Vector4 l, Vector4 r) { return !op_Equality(l, r); }
    public static Vector4 op_Implicit$Vector4(Vector3 v) { return new Vector4(v.x, v.y, v.z, 0); }
    public static Vector4 op_Implicit$Vector4(Vector2 v) { return new Vector4(v.x, v.y, 0, 0); }
    public static Vector3 op_Implicit$Vector3(Vector4 v) { return new Vector3(v.x, v.y, v.z); }
    public static Vector2 op_Implicit$Vector2(Vector4 v) { return new Vector2(v.x, v.y); }
    public boolean Equals(Vector4 o) { return x == o.x && y == o.y && z == o.z && w == o.w; }
    @Override public boolean equals(Object o) { return o instanceof Vector4 && Equals((Vector4) o); }
    @Override public int hashCode() { return Float.floatToIntBits(x) ^ (Float.floatToIntBits(y) << 2) ^ (Float.floatToIntBits(z) >> 2) ^ (Float.floatToIntBits(w) >> 1); }
    @Override public String toString() { return "(" + Vector3.f2(x) + ", " + Vector3.f2(y) + ", " + Vector3.f2(z) + ", " + Vector3.f2(w) + ")"; }
}
