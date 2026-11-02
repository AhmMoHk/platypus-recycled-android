package cs.System.Reflection;

import java.util.ArrayList;
import java.util.HashMap;

/** Access to the translator's per-class reflection tables ($mmeta, $pmeta, $fmeta, $cattrs). */
final class Meta {
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

    static java.lang.reflect.Method findJava(Class<?> c, String javaName, int nparams) {
        for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
            if (m.getName().equals(javaName) && m.getParameterTypes().length == nparams) return m;
        }
        return null;
    }
}
