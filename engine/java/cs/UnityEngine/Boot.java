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
                c = cs.rt.Rt.cls(cs.System.Reflection.Assembly.javaName(full));
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

    /** invoke static parameterless methods carrying an attribute of class `attr` (index written by the translator) */
    public static void invokeAttributed(Class<?> attr) {
        String an = cs.System.Type.csNameOf(attr);
        for (String e : cs.rt.GameTypes.ATTRIBUTED) {
            String[] p = e.split("\\|");
            if (!p[2].equals(an)) continue;
            Class<?> c;
            try { c = cs.rt.Rt.cls(p[0]); } catch (ClassNotFoundException ex) { continue; }
            for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
                if (!java.lang.reflect.Modifier.isStatic(m.getModifiers()) || m.getParameterTypes().length != 0) continue;
                if (m.getName().equals(p[1]) || m.getName().startsWith(p[1] + "$")) { invokeStatic(c, m.getName()); break; }
            }
        }
    }
}
