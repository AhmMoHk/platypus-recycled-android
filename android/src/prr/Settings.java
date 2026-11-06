package prr;

import android.content.Context;
import android.content.SharedPreferences;

/** Port settings: frame rate, asset quality, fully-unlocked progression, on-screen controls. */
public final class Settings {
    public static final int[] FPS_OPTIONS = {60, 90, 120, 144};
    private static SharedPreferences p;

    static void init(Context c) { p = c.getSharedPreferences("prr", Context.MODE_PRIVATE); }

    public static int fps() { return p == null ? 60 : p.getInt("fps", 60); }
    public static void setFps(int v) { put("fps", v); }
    /** BundleQuality: HighEnd 0 (4K), LowEnd 1 (1080p) */
    public static int quality() { return p == null ? 0 : p.getInt("quality", 0); }
    public static void setQuality(int v) { put("quality", v); }
    public static boolean unlockAll() { return p != null && p.getBoolean("unlockAll", false); }
    public static void setUnlockAll(boolean v) { if (p != null) p.edit().putBoolean("unlockAll", v).apply(); }
    public static boolean showControls() { return p == null || p.getBoolean("controls", true); }
    public static void setShowControls(boolean v) { if (p != null) p.edit().putBoolean("controls", v).apply(); }

    private static void put(String k, int v) { if (p != null) p.edit().putInt(k, v).apply(); }

    /** applied on the GL thread once the engine is up */
    static void apply() {
        cs.UnityEngine.Application.set_targetFrameRate(fps());
        setStatic("cs.global.ModManager", "bundleQuality", quality());
        if (unlockAll()) unlockEverything();
    }

    private static void setStatic(String cls, String field, int value) {
        try {
            Class<?> c = Class.forName(cls);
            java.lang.reflect.Field f = c.getDeclaredField(field);
            f.setAccessible(true);
            f.setInt(null, value);
        } catch (ReflectiveOperationException e) {
            cs.UnityEngine.Debug.LogWarning("settings: cannot set " + cls + "." + field + ": " + e);
        }
    }

    /** write the progression unlock flags for every installed mod through the game's own save API */
    static void unlockEverything() {
        String[] methods = {"SaveAllContentUnlocked", "SavePodsUnlocked", "SaveNastyUnlocked", "SaveDoublerUnlocked"};
        try {
            Class<?> save = Class.forName("cs.Platypus.Core.Save");
            for (String mod : mods()) {
                for (String m : methods) {
                    try {
                        java.lang.reflect.Method mm = save.getDeclaredMethod(m, String.class, boolean.class);
                        mm.setAccessible(true);
                        mm.invoke(null, mod, true);
                    } catch (NoSuchMethodException ignored) {
                    } catch (ReflectiveOperationException e) {
                        cs.UnityEngine.Debug.LogWarning("unlock " + m + ": " + e);
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            cs.UnityEngine.Debug.LogWarning("unlock: Save class missing");
        }
    }

    private static java.util.ArrayList<String> mods() {
        java.util.ArrayList<String> r = new java.util.ArrayList<>();
        try {
            Class<?> mm = Class.forName("cs.global.ModManager");
            java.lang.reflect.Method g = mm.getDeclaredMethod("get_AvailableModPartials");
            g.setAccessible(true);
            Object arr = g.invoke(null);
            int n = arr == null ? 0 : java.lang.reflect.Array.getLength(arr);
            for (int i = 0; i < n; i++) {
                Object meta = java.lang.reflect.Array.get(arr, i);
                if (meta == null) continue;
                java.lang.reflect.Field f = meta.getClass().getDeclaredField("modName");
                f.setAccessible(true);
                Object v = f.get(meta);
                if (v instanceof String) r.add((String) v);
            }
        } catch (ReflectiveOperationException e) {
            cs.UnityEngine.Debug.LogWarning("unlock: mod list unavailable: " + e);
        }
        if (r.isEmpty()) r.add("Platypus Reclayed");
        return r;
    }
}
