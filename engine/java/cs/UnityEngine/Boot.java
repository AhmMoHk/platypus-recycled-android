package cs.UnityEngine;

import cs.rt.Json;
import java.util.ArrayList;
import java.util.HashMap;

/** Player startup: RuntimeInitializeOnLoad methods by phase, platform init, first scene. */
public final class Boot {
    private Boot() {}
    static boolean booted;

    /** SubsystemRegistration 4, AfterAssembliesLoaded 2, BeforeSplashScreen 3, BeforeSceneLoad 1, AfterSceneLoad 0 */
    public static void start(int firstScene) {
        if (booted) return;
        booted = true;
        Loader.meta();
        Screen.$setSize(Host.get().screenWidth(), Host.get().screenHeight());
        ArrayList<HashMap<String, Object>> inits = new ArrayList<>();
        String txt = Loader.readText(new java.io.File(Loader.unityDir() + "/RuntimeInitializeOnLoads.json"));
        if (txt != null) for (Object o : Loader.l(Json.map(Json.parse(txt)), "root")) inits.add(Json.map(o));
        runInits(inits, 4);
        runInits(inits, 2);
        cs.Blackdrop.Platforms.Platform.get_Instance().$afterLoad();
        runInits(inits, 3);
        runInits(inits, 1);
        SceneLoads.boot(firstScene);
        runInits(inits, 0);
    }

    private static void runInits(ArrayList<HashMap<String, Object>> inits, int phase) {
        for (HashMap<String, Object> m : inits) {
            if (Json.i(m.get("loadTypes")) != phase) continue;
            String ns = (String) m.get("nameSpace"), cn = (String) m.get("className"), mn = (String) m.get("methodName");
            String full = (ns == null || ns.isEmpty() ? "" : ns + ".") + cn;
            Class<?> c;
            try {
                c = Class.forName(cs.System.Reflection.Assembly.javaName(full));
            } catch (ClassNotFoundException e) {
                continue;   // Rewired / Blackdrop internals replaced by facades
            }
            invokeStatic(c, mn);
        }
    }

    static void invokeStatic(Class<?> c, String name) {
        try {
            java.lang.reflect.Method m = c.getDeclaredMethod(name);
            m.setAccessible(true);
            m.invoke(null);
        } catch (NoSuchMethodException e) {
            Debug.LogWarning("init method missing: " + c.getName() + "." + name);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Debug.logException(e.getCause(), null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /** invoke static parameterless methods carrying an attribute of class `attr` */
    public static void invokeAttributed(Class<?> attr) {
        for (String n : cs.rt.GameTypes.NAMES) {
            Class<?> c;
            try { c = Class.forName(n); } catch (ClassNotFoundException e) { continue; }
            Object[] a;
            try {
                java.lang.reflect.Method cm = c.getDeclaredMethod("$cattrs");
                a = (Object[]) cm.invoke(null);
            } catch (NoSuchMethodException e) {
                continue;
            } catch (ReflectiveOperationException e) {
                continue;
            }
            for (int i = 0; i + 1 < a.length; i += 2) {
                String k = (String) a[i];
                if (attr.isInstance(a[i + 1]) && k.startsWith("M:")) {
                    try {
                        java.lang.reflect.Method m = c.getDeclaredMethod(k.substring(2));
                        if (java.lang.reflect.Modifier.isStatic(m.getModifiers())) invokeStatic(c, k.substring(2));
                    } catch (NoSuchMethodException ignored) {}
                }
            }
        }
    }
}
