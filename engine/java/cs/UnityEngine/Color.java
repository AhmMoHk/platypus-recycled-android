package cs.UnityEngine;

public class Color implements cs.rt.Struct {
    public float r, g, b, a;
    public Color() {}
    public Color(float r, float g, float b, float a) { this.r = r; this.g = g; this.b = b; this.a = a; }
    public Color(float r, float g, float b) { this(r, g, b, 1f); }
    public Color $copy() { return new Color(r, g, b, a); }
    public void $set(Object o) { Color c = (Color) o; r = c.r; g = c.g; b = c.b; a = c.a; }
    public void $clear() { r = g = b = a = 0; }
    public static Color[] $newArray(int n) { Color[] x = new Color[n]; for (int i = 0; i < n; i++) x[i] = new Color(); return x; }

    public static Color get_red() { return new Color(1, 0, 0, 1); }
    public static Color get_green() { return new Color(0, 1, 0, 1); }
    public static Color get_blue() { return new Color(0, 0, 1, 1); }
    public static Color get_white() { return new Color(1, 1, 1, 1); }
    public static Color get_black() { return new Color(0, 0, 0, 1); }
    public static Color get_yellow() { return new Color(1, 0.921568632F, 0.0156862754F, 1); }
    public static Color get_cyan() { return new Color(0, 1, 1, 1); }
    public static Color get_magenta() { return new Color(1, 0, 1, 1); }
    public static Color get_gray() { return new Color(.5f, .5f, .5f, 1); }
    public static Color get_grey() { return get_gray(); }
    public static Color get_clear() { return new Color(0, 0, 0, 0); }

    public float get_Item(int i) {
        switch (i) { case 0: return r; case 1: return g; case 2: return b; case 3: return a; }
        throw new cs.System.IndexOutOfRangeException("Invalid Color index(" + i + ")!");
    }
    public void set_Item(int i, float v) {
        switch (i) { case 0: r = v; return; case 1: g = v; return; case 2: b = v; return; case 3: a = v; return; }
        throw new cs.System.IndexOutOfRangeException("Invalid Color index(" + i + ")!");
    }
    public float get_grayscale() { return 0.299F * r + 0.587F * g + 0.114F * b; }
    public float get_maxColorComponent() { return Math.max(Math.max(r, g), b); }

    static float toLinear(float c) {
        if (c <= 0.04045F) return c / 12.92F;
        if (c < 1.0F) return (float) Math.pow((c + 0.055F) / 1.055F, 2.4F);
        return (float) Math.pow(c, 2.2F);
    }
    static float toGamma(float c) {
        if (c <= 0.0F) return 0.0F;
        if (c <= 0.0031308F) return 12.92F * c;
        if (c < 1.0F) return 1.055F * (float) Math.pow(c, 0.4166667F) - 0.055F;
        return (float) Math.pow(c, 0.45454545F);
    }
    public Color get_linear() { return new Color(toLinear(r), toLinear(g), toLinear(b), a); }
    public Color get_gamma() { return new Color(toGamma(r), toGamma(g), toGamma(b), a); }

    public static Color Lerp(Color x, Color y, float t) {
        t = Mathf.Clamp01(t);
        return new Color(x.r + (y.r - x.r) * t, x.g + (y.g - x.g) * t, x.b + (y.b - x.b) * t, x.a + (y.a - x.a) * t);
    }
    public static Color LerpUnclamped(Color x, Color y, float t) {
        return new Color(x.r + (y.r - x.r) * t, x.g + (y.g - x.g) * t, x.b + (y.b - x.b) * t, x.a + (y.a - x.a) * t);
    }
    public static Color HSVToRGB(float H, float S, float V) { return HSVToRGB(H, S, V, true); }
    public static Color HSVToRGB(float H, float S, float V, boolean hdr) {
        Color c = get_white();
        if (S == 0) { c.r = V; c.g = V; c.b = V; return c; }
        if (V == 0) { c.r = c.g = c.b = 0; return c; }
        float t_S, t_V, h_to_floor;
        t_S = S; t_V = V; h_to_floor = H * 6.0f;
        int temp = (int) Math.floor(h_to_floor);
        float t = h_to_floor - temp;
        float var_1 = t_V * (1 - t_S), var_2 = t_V * (1 - t_S * t), var_3 = t_V * (1 - t_S * (1 - t));
        switch (temp) {
            case 0: c.r = t_V; c.g = var_3; c.b = var_1; break;
            case 1: c.r = var_2; c.g = t_V; c.b = var_1; break;
            case 2: c.r = var_1; c.g = t_V; c.b = var_3; break;
            case 3: c.r = var_1; c.g = var_2; c.b = t_V; break;
            case 4: c.r = var_3; c.g = var_1; c.b = t_V; break;
            case 5: c.r = t_V; c.g = var_1; c.b = var_2; break;
            case 6: c.r = t_V; c.g = var_3; c.b = var_1; break;
            case -1: c.r = t_V; c.g = var_1; c.b = var_2; break;
        }
        if (!hdr) { c.r = Mathf.Clamp(c.r, 0, 1); c.g = Mathf.Clamp(c.g, 0, 1); c.b = Mathf.Clamp(c.b, 0, 1); }
        return c;
    }
    public static void RGBToHSV(Color rgb, cs.rt.RefF H, cs.rt.RefF S, cs.rt.RefF V) {
        float max = Math.max(rgb.r, Math.max(rgb.g, rgb.b)), min = Math.min(rgb.r, Math.min(rgb.g, rgb.b));
        float d = max - min;
        V.v = max;
        S.v = max > 0 ? d / max : 0;
        float h = 0;
        if (d > 0) {
            if (max == rgb.r) h = ((rgb.g - rgb.b) / d) % 6f;
            else if (max == rgb.g) h = (rgb.b - rgb.r) / d + 2f;
            else h = (rgb.r - rgb.g) / d + 4f;
            h /= 6f;
            if (h < 0) h += 1f;
        }
        H.v = h;
    }

    public static Color op_Addition(Color x, Color y) { return new Color(x.r + y.r, x.g + y.g, x.b + y.b, x.a + y.a); }
    public static Color op_Subtraction(Color x, Color y) { return new Color(x.r - y.r, x.g - y.g, x.b - y.b, x.a - y.a); }
    public static Color op_Multiply(Color x, Color y) { return new Color(x.r * y.r, x.g * y.g, x.b * y.b, x.a * y.a); }
    public static Color op_Multiply(Color x, float f) { return new Color(x.r * f, x.g * f, x.b * f, x.a * f); }
    public static Color op_Multiply(float f, Color x) { return op_Multiply(x, f); }
    public static Color op_Division(Color x, float f) { return new Color(x.r / f, x.g / f, x.b / f, x.a / f); }
    public static boolean op_Equality(Color l, Color r) {
        float dr = l.r - r.r, dg = l.g - r.g, db = l.b - r.b, da = l.a - r.a;
        return dr * dr + dg * dg + db * db + da * da < 9.99999944E-11f;
    }
    public static boolean op_Inequality(Color l, Color r) { return !op_Equality(l, r); }
    public static Vector4 op_Implicit$Vector4(Color c) { return new Vector4(c.r, c.g, c.b, c.a); }
    public static Color op_Implicit$Color(Vector4 v) { return new Color(v.x, v.y, v.z, v.w); }
    public boolean Equals(Color o) { return r == o.r && g == o.g && b == o.b && a == o.a; }
    @Override public boolean equals(Object o) { return o instanceof Color && Equals((Color) o); }
    @Override public int hashCode() { return Float.floatToIntBits(r) ^ (Float.floatToIntBits(g) << 2) ^ (Float.floatToIntBits(b) >> 2) ^ (Float.floatToIntBits(a) >> 1); }
    @Override public String toString() { return "RGBA(" + cs.rt.Fmt.formatDouble(r, "F3", true) + ", " + cs.rt.Fmt.formatDouble(g, "F3", true) + ", " + cs.rt.Fmt.formatDouble(b, "F3", true) + ", " + cs.rt.Fmt.formatDouble(a, "F3", true) + ")"; }
}
