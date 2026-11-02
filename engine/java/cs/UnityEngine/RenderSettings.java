package cs.UnityEngine;

import java.util.HashMap;

public final class RenderSettings {
    private RenderSettings() {}
    static boolean fog;
    static final Color fogColor = new Color(.5f, .5f, .5f, 1f);
    static float fogStart, fogEnd = 300, fogDensity = .01f;
    static int fogMode = 3;
    static final Color ambientLight = new Color(.2f, .2f, .2f, 1f);

    static void load(HashMap<String, Object> d, Loader.Ctx ctx) {
        fog = Loader.b(d, "m_Fog", false);
        Loader.color(d.get("m_FogColor"), fogColor);
        fogMode = Loader.i(d, "m_FogMode", 3);
        fogDensity = Loader.f(d, "m_FogDensity", .01f);
        fogStart = Loader.f(d, "m_LinearFogStart", 0);
        fogEnd = Loader.f(d, "m_LinearFogEnd", 300);
        Loader.color(d.get("m_AmbientSkyColor"), ambientLight);
    }
    public static boolean get_fog() { return fog; }
    public static void set_fog(boolean v) { fog = v; }
    public static Color get_fogColor() { return fogColor.$copy(); }
    public static void set_fogColor(Color c) { fogColor.$set(c); }
    public static Color get_ambientLight() { return ambientLight.$copy(); }
    public static void set_ambientLight(Color c) { ambientLight.$set(c); }
}
