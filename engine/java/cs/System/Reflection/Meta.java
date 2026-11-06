package cs.System.Reflection;

import java.util.ArrayList;
import java.util.HashMap;

/** Access to the translator's per-class reflection tables ($mmeta, $pmeta, $fmeta, $cattrs). */
public final class Meta {
    private Meta() {}

    private static final HashMap<Class<?>, Object[][]> mm = new HashMap<>();
    private static final HashMap<Class<?>, Object[][]> pm = new HashMap<>();
    private static final HashMap<Class<?>, Object[]> attrs = new HashMap<>();

    static synchronized Object[][] methods(Class<?> c) {
        if (!mm.containsKey(c)) mm.put(c, (Object[][]) staticField(c, "$mmeta"));
        return mm.get(c);
    }

    static synchronized Object[][] props(Class<?> c) {
        if (!pm.containsKey(c)) pm.put(c, (Object[][]) staticField(c, "$pmeta"));
        return pm.get(c);
    }

    static synchronized Object[] attrs(Class<?> c) {
        if (!attrs.containsKey(c)) {
            Object[] a = null;
            try {
                java.lang.reflect.Method m = c.getDeclaredMethod("$cattrs");
                a = (Object[]) m.invoke(null);
            } catch (NoSuchMethodException ignored) {
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
            attrs.put(c, a == null ? new Object[0] : a);
        }
        return attrs.get(c);
    }

    /** attribute instances attached to member key ("" type, "F:name", "M:javaName", "P:name") */
    static Object[] attrsOf(Class<?> c, String key) {
        Object[] a = attrs(c);
        ArrayList<Object> out = new ArrayList<>();
        for (int i = 0; i + 1 < a.length; i += 2) if (key.equals(a[i])) out.add(a[i + 1]);
        return out.toArray();
    }

    private static Object staticField(Class<?> c, String n) {
        try {
            java.lang.reflect.Field f = c.getDeclaredField(n);
            return f.get(null);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    static java.lang.reflect.Method findJava(Class<?> c, String javaName, int nparams) { return findJava(c, javaName, nparams, null); }

    /** the Java method for a $mmeta row. Overloads share a Java name, and getDeclaredMethods() order differs
     *  between VMs (HotSpot vs Android ART), so candidates are ranked by their parameter types. */
    static java.lang.reflect.Method findJava(Class<?> c, String javaName, int nparams, String[] csTypes) {
        java.lang.reflect.Method best = null;
        int bestScore = -1;
        String bestKey = null;
        for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
            if (!m.getName().equals(javaName) || m.getParameterTypes().length != nparams || m.isBridge() || m.isSynthetic()) continue;
            int score = 0;
            Class<?>[] jp = m.getParameterTypes();
            if (csTypes != null) {
                int off = nparams - csTypes.length;       // leading cs.System.Type args of generic methods
                for (int i = 0; i < csTypes.length && i + off < jp.length; i++) {
                    Class<?> want = csClass(csTypes[i]);
                    Class<?> have = jp[i + off];
                    if (want == null) continue;
                    if (want == have) score += 3;
                    else if (have.isAssignableFrom(want)) score += 1;
                    else score -= 4;
                }
            }
            String key = m.toGenericString();
            if (score > bestScore || score == bestScore && (bestKey == null || key.compareTo(bestKey) < 0)) {
                best = m; bestScore = score; bestKey = key;
            }
        }
        return best;
    }

    private static final java.util.HashMap<String, Class<?>> csCache = new java.util.HashMap<>();

    /** Java class of a C# type name from $mmeta (null when it cannot be judged) */
    static synchronized Class<?> csClass(String n) {
        if (n == null) return null;
        if (csCache.containsKey(n)) return csCache.get(n);
        Class<?> c = null;
        try {
            if (n.endsWith("&")) {
                switch (n.substring(0, n.length() - 1)) {
                    case "i4": case "u4": c = cs.rt.RefI.class; break;
                    case "r4": c = cs.rt.RefF.class; break;
                    case "bool": c = cs.rt.RefZ.class; break;
                    case "i8": case "u8": c = cs.rt.RefJ.class; break;
                    case "r8": c = cs.rt.RefD.class; break;
                    default: c = cs.rt.RefO.class;
                }
            } else if (n.contains("!!") || n.contains("`") && !n.contains("<")) {
                c = null;                                     // open generic parameter
            } else {
                cs.System.Type t = Assembly.resolveType(n);
                c = t == null ? null : t.cls;
                if (c == Object.class) c = null;
            }
        } catch (Throwable e) { c = null; }
        csCache.put(n, c);
        return c;
    }

    public static java.lang.reflect.Method findJavaPublic(Class<?> c, String javaName, int nparams, String[] csTypes) { return findJava(c, javaName, nparams, csTypes); }
    public static Class<?> csClassPublic(String n) { return csClass(n); }
}
