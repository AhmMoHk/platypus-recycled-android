package cs.UnityEngine;

public class Color32 implements cs.rt.Struct {
    public byte r, g, b, a;
    public Color32() {}
    public Color32(byte r, byte g, byte b, byte a) { this.r = r; this.g = g; this.b = b; this.a = a; }
    public Color32 $copy() { return new Color32(r, g, b, a); }
    public void $set(Object o) { Color32 c = (Color32) o; r = c.r; g = c.g; b = c.b; a = c.a; }
    public void $clear() { r = g = b = a = 0; }
    public static Color32[] $newArray(int n) { Color32[] x = new Color32[n]; for (int i = 0; i < n; i++) x[i] = new Color32(); return x; }
    public static Color32 op_Implicit$Color32(Color c) {
        return new Color32((byte) Math.round(Mathf.Clamp01(c.r) * 255f), (byte) Math.round(Mathf.Clamp01(c.g) * 255f),
                           (byte) Math.round(Mathf.Clamp01(c.b) * 255f), (byte) Math.round(Mathf.Clamp01(c.a) * 255f));
    }
    public static Color op_Implicit$Color(Color32 c) { return new Color((c.r & 255) / 255f, (c.g & 255) / 255f, (c.b & 255) / 255f, (c.a & 255) / 255f); }
    public static Color32 Lerp(Color32 a, Color32 b, float t) {
        t = Mathf.Clamp01(t);
        return new Color32((byte) ((a.r & 255) + ((b.r & 255) - (a.r & 255)) * t), (byte) ((a.g & 255) + ((b.g & 255) - (a.g & 255)) * t),
                           (byte) ((a.b & 255) + ((b.b & 255) - (a.b & 255)) * t), (byte) ((a.a & 255) + ((b.a & 255) - (a.a & 255)) * t));
    }
    public int rgba() { return (r & 255) << 24 | (g & 255) << 16 | (b & 255) << 8 | (a & 255); }
    @Override public boolean equals(Object o) { return o instanceof Color32 && ((Color32) o).rgba() == rgba(); }
    @Override public int hashCode() { return rgba(); }
    @Override public String toString() { return "RGBA(" + (r & 255) + ", " + (g & 255) + ", " + (b & 255) + ", " + (a & 255) + ")"; }
}
