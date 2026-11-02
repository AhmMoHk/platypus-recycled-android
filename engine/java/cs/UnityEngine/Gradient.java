package cs.UnityEngine;

import cs.rt.Json;
import java.util.HashMap;

/** GradientMode: Blend 0, Fixed 1 */
public class Gradient {
    GradientColorKey[] colorKeys = {new GradientColorKey(new Color(1, 1, 1, 1), 0), new GradientColorKey(new Color(1, 1, 1, 1), 1)};
    GradientAlphaKey[] alphaKeys = {new GradientAlphaKey(1, 0), new GradientAlphaKey(1, 1)};
    int mode;
    public Gradient() {}
    public void $ctor_Gradient() {}
    public GradientColorKey[] get_colorKeys() { GradientColorKey[] r = new GradientColorKey[colorKeys.length]; for (int i = 0; i < r.length; i++) r[i] = colorKeys[i].$copy(); return r; }
    public void set_colorKeys(GradientColorKey[] k) { colorKeys = new GradientColorKey[k.length]; for (int i = 0; i < k.length; i++) colorKeys[i] = k[i].$copy(); }
    public GradientAlphaKey[] get_alphaKeys() { GradientAlphaKey[] r = new GradientAlphaKey[alphaKeys.length]; for (int i = 0; i < r.length; i++) r[i] = alphaKeys[i].$copy(); return r; }
    public void set_alphaKeys(GradientAlphaKey[] k) { alphaKeys = new GradientAlphaKey[k.length]; for (int i = 0; i < k.length; i++) alphaKeys[i] = k[i].$copy(); }
    public void SetKeys(GradientColorKey[] c, GradientAlphaKey[] a) { set_colorKeys(c); set_alphaKeys(a); }
    public int get_mode() { return mode; }
    public void set_mode(int m) { mode = m; }

    public Color Evaluate(float t) {
        Color c = new Color();
        int n = colorKeys.length;
        if (n == 0) { c.r = c.g = c.b = 1; }
        else if (t <= colorKeys[0].time) c.$set(colorKeys[0].color);
        else if (t >= colorKeys[n - 1].time) c.$set(colorKeys[n - 1].color);
        else {
            for (int i = 0; i < n - 1; i++) {
                GradientColorKey a = colorKeys[i], b = colorKeys[i + 1];
                if (t >= a.time && t <= b.time) {
                    if (mode == 1) { c.$set(b.color); break; }
                    float s = b.time == a.time ? 0 : (t - a.time) / (b.time - a.time);
                    c.r = a.color.r + (b.color.r - a.color.r) * s;
                    c.g = a.color.g + (b.color.g - a.color.g) * s;
                    c.b = a.color.b + (b.color.b - a.color.b) * s;
                    break;
                }
            }
        }
        int m = alphaKeys.length;
        if (m == 0) c.a = 1;
        else if (t <= alphaKeys[0].time) c.a = alphaKeys[0].alpha;
        else if (t >= alphaKeys[m - 1].time) c.a = alphaKeys[m - 1].alpha;
        else {
            for (int i = 0; i < m - 1; i++) {
                GradientAlphaKey a = alphaKeys[i], b = alphaKeys[i + 1];
                if (t >= a.time && t <= b.time) {
                    if (mode == 1) { c.a = b.alpha; break; }
                    float s = b.time == a.time ? 0 : (t - a.time) / (b.time - a.time);
                    c.a = a.alpha + (b.alpha - a.alpha) * s;
                    break;
                }
            }
        }
        return c;
    }

    Gradient $clone() {
        Gradient g = new Gradient();
        g.set_colorKeys(colorKeys);
        g.set_alphaKeys(alphaKeys);
        g.mode = mode;
        return g;
    }

    static Gradient $fromJson(Object v) {
        Gradient g = new Gradient();
        if (!(v instanceof HashMap)) return g;
        HashMap<String, Object> m = Json.map(v);
        int nc = Json.i(m.get("m_NumColorKeys")), na = Json.i(m.get("m_NumAlphaKeys"));
        g.colorKeys = new GradientColorKey[nc];
        g.alphaKeys = new GradientAlphaKey[na];
        for (int i = 0; i < nc; i++) {
            Color c = new Color();
            Loader.color(m.get("key" + i), c);
            g.colorKeys[i] = new GradientColorKey(c, Json.i(m.get("ctime" + i)) / 65535f);
        }
        for (int i = 0; i < na; i++) {
            Color c = new Color();
            Loader.color(m.get("key" + i), c);
            g.alphaKeys[i] = new GradientAlphaKey(c.a, Json.i(m.get("atime" + i)) / 65535f);
        }
        g.mode = Json.i(m.get("m_Mode"));
        return g;
    }
}
