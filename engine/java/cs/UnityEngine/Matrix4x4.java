package cs.UnityEngine;

/** Column-major 4x4 matrix (m[row + col*4]). */
public class Matrix4x4 implements cs.rt.Struct {
    public final float[] m = new float[16];
    public Matrix4x4() {}
    public Matrix4x4 $copy() { Matrix4x4 c = new Matrix4x4(); System.arraycopy(m, 0, c.m, 0, 16); return c; }
    public void $set(Object o) { System.arraycopy(((Matrix4x4) o).m, 0, m, 0, 16); }
    public void $clear() { java.util.Arrays.fill(m, 0); }
    public static Matrix4x4[] $newArray(int n) { Matrix4x4[] a = new Matrix4x4[n]; for (int i = 0; i < n; i++) a[i] = new Matrix4x4(); return a; }
    public float get_Item(int i) { return m[i]; }
    public void set_Item(int i, float v) { m[i] = v; }
    public float get_Item(int r, int c) { return m[r + c * 4]; }
    public void set_Item(int r, int c, float v) { m[r + c * 4] = v; }
    public static Matrix4x4 get_identity() { Matrix4x4 x = new Matrix4x4(); x.m[0] = x.m[5] = x.m[10] = x.m[15] = 1; return x; }
    public static Matrix4x4 get_zero() { return new Matrix4x4(); }
    public static Matrix4x4 TRS(Vector3 p, Quaternion q, Vector3 s) {
        Matrix4x4 r = new Matrix4x4();
        float x = q.x * 2, y = q.y * 2, z = q.z * 2;
        float xx = q.x * x, yy = q.y * y, zz = q.z * z, xy = q.x * y, xz = q.x * z, yz = q.y * z, wx = q.w * x, wy = q.w * y, wz = q.w * z;
        r.m[0] = (1 - (yy + zz)) * s.x; r.m[1] = (xy + wz) * s.x; r.m[2] = (xz - wy) * s.x;
        r.m[4] = (xy - wz) * s.y; r.m[5] = (1 - (xx + zz)) * s.y; r.m[6] = (yz + wx) * s.y;
        r.m[8] = (xz + wy) * s.z; r.m[9] = (yz - wx) * s.z; r.m[10] = (1 - (xx + yy)) * s.z;
        r.m[12] = p.x; r.m[13] = p.y; r.m[14] = p.z; r.m[15] = 1;
        return r;
    }
    public static Matrix4x4 Translate(Vector3 v) { Matrix4x4 r = get_identity(); r.m[12] = v.x; r.m[13] = v.y; r.m[14] = v.z; return r; }
    public static Matrix4x4 Scale(Vector3 v) { Matrix4x4 r = new Matrix4x4(); r.m[0] = v.x; r.m[5] = v.y; r.m[10] = v.z; r.m[15] = 1; return r; }
    public static Matrix4x4 Ortho(float l, float r, float b, float t, float n, float f) {
        Matrix4x4 o = new Matrix4x4();
        o.m[0] = 2 / (r - l); o.m[5] = 2 / (t - b); o.m[10] = -2 / (f - n);
        o.m[12] = -(r + l) / (r - l); o.m[13] = -(t + b) / (t - b); o.m[14] = -(f + n) / (f - n); o.m[15] = 1;
        return o;
    }
    public static Matrix4x4 Perspective(float fov, float aspect, float n, float f) {
        Matrix4x4 r = new Matrix4x4();
        float c = (float) (1.0 / Math.tan(Math.toRadians(fov) / 2));
        r.m[0] = c / aspect; r.m[5] = c; r.m[10] = -(f + n) / (f - n); r.m[11] = -1; r.m[14] = -2 * f * n / (f - n);
        return r;
    }
    public static Matrix4x4 op_Multiply(Matrix4x4 a, Matrix4x4 b) {
        Matrix4x4 r = new Matrix4x4();
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++) {
                float s = 0;
                for (int k = 0; k < 4; k++) s += a.m[i + k * 4] * b.m[k + j * 4];
                r.m[i + j * 4] = s;
            }
        return r;
    }
    public static Vector4 op_Multiply(Matrix4x4 a, Vector4 v) {
        return new Vector4(a.m[0] * v.x + a.m[4] * v.y + a.m[8] * v.z + a.m[12] * v.w,
                           a.m[1] * v.x + a.m[5] * v.y + a.m[9] * v.z + a.m[13] * v.w,
                           a.m[2] * v.x + a.m[6] * v.y + a.m[10] * v.z + a.m[14] * v.w,
                           a.m[3] * v.x + a.m[7] * v.y + a.m[11] * v.z + a.m[15] * v.w);
    }
    public Vector3 MultiplyPoint(Vector3 v) {
        float x = m[0] * v.x + m[4] * v.y + m[8] * v.z + m[12];
        float y = m[1] * v.x + m[5] * v.y + m[9] * v.z + m[13];
        float z = m[2] * v.x + m[6] * v.y + m[10] * v.z + m[14];
        float w = m[3] * v.x + m[7] * v.y + m[11] * v.z + m[15];
        w = 1f / w;
        return new Vector3(x * w, y * w, z * w);
    }
    public Vector3 MultiplyPoint3x4(Vector3 v) {
        return new Vector3(m[0] * v.x + m[4] * v.y + m[8] * v.z + m[12], m[1] * v.x + m[5] * v.y + m[9] * v.z + m[13], m[2] * v.x + m[6] * v.y + m[10] * v.z + m[14]);
    }
    public Vector3 MultiplyVector(Vector3 v) {
        return new Vector3(m[0] * v.x + m[4] * v.y + m[8] * v.z, m[1] * v.x + m[5] * v.y + m[9] * v.z, m[2] * v.x + m[6] * v.y + m[10] * v.z);
    }
    public Vector3 get_lossyScale() {
        return new Vector3((float) Math.sqrt(m[0] * m[0] + m[1] * m[1] + m[2] * m[2]),
                           (float) Math.sqrt(m[4] * m[4] + m[5] * m[5] + m[6] * m[6]),
                           (float) Math.sqrt(m[8] * m[8] + m[9] * m[9] + m[10] * m[10]));
    }
    public Matrix4x4 get_inverse() { return Inverse(this); }
    public static Matrix4x4 Inverse(Matrix4x4 a) {
        float[] s = a.m, inv = new float[16];
        inv[0] = s[5] * s[10] * s[15] - s[5] * s[11] * s[14] - s[9] * s[6] * s[15] + s[9] * s[7] * s[14] + s[13] * s[6] * s[11] - s[13] * s[7] * s[10];
        inv[4] = -s[4] * s[10] * s[15] + s[4] * s[11] * s[14] + s[8] * s[6] * s[15] - s[8] * s[7] * s[14] - s[12] * s[6] * s[11] + s[12] * s[7] * s[10];
        inv[8] = s[4] * s[9] * s[15] - s[4] * s[11] * s[13] - s[8] * s[5] * s[15] + s[8] * s[7] * s[13] + s[12] * s[5] * s[11] - s[12] * s[7] * s[9];
        inv[12] = -s[4] * s[9] * s[14] + s[4] * s[10] * s[13] + s[8] * s[5] * s[14] - s[8] * s[6] * s[13] - s[12] * s[5] * s[10] + s[12] * s[6] * s[9];
        inv[1] = -s[1] * s[10] * s[15] + s[1] * s[11] * s[14] + s[9] * s[2] * s[15] - s[9] * s[3] * s[14] - s[13] * s[2] * s[11] + s[13] * s[3] * s[10];
        inv[5] = s[0] * s[10] * s[15] - s[0] * s[11] * s[14] - s[8] * s[2] * s[15] + s[8] * s[3] * s[14] + s[12] * s[2] * s[11] - s[12] * s[3] * s[10];
        inv[9] = -s[0] * s[9] * s[15] + s[0] * s[11] * s[13] + s[8] * s[1] * s[15] - s[8] * s[3] * s[13] - s[12] * s[1] * s[11] + s[12] * s[3] * s[9];
        inv[13] = s[0] * s[9] * s[14] - s[0] * s[10] * s[13] - s[8] * s[1] * s[14] + s[8] * s[2] * s[13] + s[12] * s[1] * s[10] - s[12] * s[2] * s[9];
        inv[2] = s[1] * s[6] * s[15] - s[1] * s[7] * s[14] - s[5] * s[2] * s[15] + s[5] * s[3] * s[14] + s[13] * s[2] * s[7] - s[13] * s[3] * s[6];
        inv[6] = -s[0] * s[6] * s[15] + s[0] * s[7] * s[14] + s[4] * s[2] * s[15] - s[4] * s[3] * s[14] - s[12] * s[2] * s[7] + s[12] * s[3] * s[6];
        inv[10] = s[0] * s[5] * s[15] - s[0] * s[7] * s[13] - s[4] * s[1] * s[15] + s[4] * s[3] * s[13] + s[12] * s[1] * s[7] - s[12] * s[3] * s[5];
        inv[14] = -s[0] * s[5] * s[14] + s[0] * s[6] * s[13] + s[4] * s[1] * s[14] - s[4] * s[2] * s[13] - s[12] * s[1] * s[6] + s[12] * s[2] * s[5];
        inv[3] = -s[1] * s[6] * s[11] + s[1] * s[7] * s[10] + s[5] * s[2] * s[11] - s[5] * s[3] * s[10] - s[9] * s[2] * s[7] + s[9] * s[3] * s[6];
        inv[7] = s[0] * s[6] * s[11] - s[0] * s[7] * s[10] - s[4] * s[2] * s[11] + s[4] * s[3] * s[10] + s[8] * s[2] * s[7] - s[8] * s[3] * s[6];
        inv[11] = -s[0] * s[5] * s[11] + s[0] * s[7] * s[9] + s[4] * s[1] * s[11] - s[4] * s[3] * s[9] - s[8] * s[1] * s[7] + s[8] * s[3] * s[5];
        inv[15] = s[0] * s[5] * s[10] - s[0] * s[6] * s[9] - s[4] * s[1] * s[10] + s[4] * s[2] * s[9] + s[8] * s[1] * s[6] - s[8] * s[2] * s[5];
        float det = s[0] * inv[0] + s[1] * inv[4] + s[2] * inv[8] + s[3] * inv[12];
        Matrix4x4 r = new Matrix4x4();
        if (det == 0) return r;
        det = 1f / det;
        for (int i = 0; i < 16; i++) r.m[i] = inv[i] * det;
        return r;
    }
    public Vector4 GetColumn(int i) { return new Vector4(m[i * 4], m[i * 4 + 1], m[i * 4 + 2], m[i * 4 + 3]); }
    public Vector4 GetRow(int i) { return new Vector4(m[i], m[i + 4], m[i + 8], m[i + 12]); }
    public void SetColumn(int i, Vector4 v) { m[i * 4] = v.x; m[i * 4 + 1] = v.y; m[i * 4 + 2] = v.z; m[i * 4 + 3] = v.w; }
    public void SetRow(int i, Vector4 v) { m[i] = v.x; m[i + 4] = v.y; m[i + 8] = v.z; m[i + 12] = v.w; }
    @Override public boolean equals(Object o) { return o instanceof Matrix4x4 && java.util.Arrays.equals(m, ((Matrix4x4) o).m); }
    @Override public int hashCode() { return java.util.Arrays.hashCode(m); }
}
