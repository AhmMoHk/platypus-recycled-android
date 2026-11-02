package cs.UnityEngine;

import cs.rt.Json;
import java.util.ArrayList;
import java.util.HashMap;

/** Unity built-in resources and value types for the loader. */
final class Builtins {
    private Builtins() {}
    private static final HashMap<String, UObject> cache = new HashMap<>();

    static UObject get(String file, long pid) {
        String k = file + "#" + pid;
        if (cache.containsKey(k)) return cache.get(k);
        UObject o = null;
        if (file.equals("unity default resources") || file.equals("unity_builtin_extra")) {
            String sh = SHADERS.get(pid);
            if (sh != null) o = Shader.Find(sh);
        }
        if (o != null) { o.$asset = true; o.$ref = k; }
        cache.put(k, o);
        return o;
    }

    private static final HashMap<Long, String> SHADERS = new HashMap<>();
    static {
        SHADERS.put(10101L, "Sprites/Default");
        SHADERS.put(10753L, "Sprites/Default");
        SHADERS.put(10770L, "UI/Default");
        SHADERS.put(7L, "Legacy Shaders/Diffuse");
    }

    /** built-in UI sprites (UISprite, Background, Knob...): white 32x32 with a 10px border */
    static Sprite uiSprite(String name) {
        String k = "sprite:" + name;
        if (cache.containsKey(k)) return (Sprite) cache.get(k);
        Texture2D t = new Texture2D(32, 32, 4, false);
        java.util.Arrays.fill(t.px, 0xFFFFFFFF);
        t.m_Name = name;
        Sprite s = Sprite.Create(t, new Rect(0, 0, 32, 32), new Vector2(.5f, .5f), 100f, 0, 0, new Vector4(10, 10, 10, 10), false);
        s.m_Name = name;
        s.$asset = true;
        cache.put(k, s);
        return s;
    }

    /** value types with fixed Unity layouts; returns Loader.NOVALUE when jt is not one */
    static Object valueType(Class<?> jt, Object v, Loader.Ctx ctx) {
        if (jt == Vector2.class) { Vector2 r = new Vector2(); Loader.vec2(v, r); return r; }
        if (jt == Vector3.class) { Vector3 r = new Vector3(); Loader.vec3(v, r); return r; }
        if (jt == Vector4.class) { Vector4 r = new Vector4(); Loader.vec4(v, r); return r; }
        if (jt == Quaternion.class) { Quaternion r = new Quaternion(); Loader.quat(v, r); return r; }
        if (jt == Color.class) { Color r = new Color(); Loader.color(v, r); return r; }
        if (jt == Color32.class) {
            Color32 r = new Color32();
            if (v instanceof HashMap) {
                long x = Json.lng(Json.map(v).get("rgba"));
                r.r = (byte) x; r.g = (byte) (x >> 8); r.b = (byte) (x >> 16); r.a = (byte) (x >> 24);
            }
            return r;
        }
        if (jt == Rect.class) { Rect r = new Rect(); Loader.rect(v, r); return r; }
        if (jt == Vector2Int.class) {
            Vector2Int r = new Vector2Int();
            if (v instanceof HashMap) { r.m_X = Json.i(Json.map(v).get("x")); r.m_Y = Json.i(Json.map(v).get("y")); }
            return r;
        }
        if (jt == LayerMask.class) {
            LayerMask r = new LayerMask();
            if (v instanceof HashMap) r.m_Mask = (int) Json.lng(Json.map(v).get("m_Bits"));
            return r;
        }
        if (jt == Bounds.class) {
            Bounds r = new Bounds();
            if (v instanceof HashMap) { Loader.vec3(Json.map(v).get("m_Center"), r.m_Center); Loader.vec3(Json.map(v).get("m_Extent"), r.m_Extent); }
            return r;
        }
        if (jt == RectOffset.class) {
            RectOffset r = new RectOffset();
            if (v instanceof HashMap) {
                HashMap<String, Object> m = Json.map(v);
                r.set_left(Json.i(m.get("m_Left"))); r.set_right(Json.i(m.get("m_Right")));
                r.set_top(Json.i(m.get("m_Top"))); r.set_bottom(Json.i(m.get("m_Bottom")));
            }
            return r;
        }
        if (jt == AnimationCurve.class) return AnimationCurve.$fromJson(v);
        if (jt == Gradient.class) return Gradient.$fromJson(v);
        if (jt == Matrix4x4.class) {
            Matrix4x4 r = new Matrix4x4();
            if (v instanceof HashMap) {
                HashMap<String, Object> m = Json.map(v);
                for (int c = 0; c < 4; c++) for (int rr = 0; rr < 4; rr++) r.m[c * 4 + rr] = Json.f(m.get("e" + rr + c));
            }
            return r;
        }
        return Loader.NOVALUE;
    }

    static Object cloneValueType(Object v) {
        if (v instanceof RectOffset) {
            RectOffset s = (RectOffset) v, r = new RectOffset();
            r.set_left(s.get_left()); r.set_right(s.get_right()); r.set_top(s.get_top()); r.set_bottom(s.get_bottom());
            return r;
        }
        if (v instanceof AnimationCurve) return ((AnimationCurve) v).$clone();
        if (v instanceof Gradient) return ((Gradient) v).$clone();
        return null;
    }
}
