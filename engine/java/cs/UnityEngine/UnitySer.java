package cs.UnityEngine;

import cs.rt.Json;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

/** JSON <-> object using serialized-field metadata (JsonUtility semantics; UnityEngine.Object refs as instance ids). */
final class UnitySer {
    private UnitySer() {}

    static ArrayList<Object[]> fields(Class<?> c) {
        ArrayList<ArrayList<Object[]>> chain = new ArrayList<>();
        for (Class<?> k = c; k != null && k != Object.class && k != MonoBehaviour.class && k != ScriptableObject.class; k = k.getSuperclass()) {
            ArrayList<Object[]> l = new ArrayList<>();
            try {
                Object[] fm = (Object[]) k.getDeclaredField("$fmeta").get(null);
                for (int i = 0; i + 3 < fm.length; i += 4) {
                    int fl = (Integer) fm[i + 3];
                    if ((fl & 1) != 0 || ((fl & 8) == 0)) continue;
                    try {
                        java.lang.reflect.Field f = k.getDeclaredField((String) fm[i]);
                        f.setAccessible(true);
                        l.add(new Object[]{f, fm[i + 1], fm[i + 2]});
                    } catch (NoSuchFieldException ignored) {}
                }
            } catch (ReflectiveOperationException ignored) {}
            chain.add(0, l);
        }
        ArrayList<Object[]> r = new ArrayList<>();
        for (ArrayList<Object[]> l : chain) r.addAll(l);
        return r;
    }

    static Object toTree(Object o) {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        for (Object[] f : fields(o.getClass())) {
            try { m.put((String) f[1], value(((java.lang.reflect.Field) f[0]).get(o))); } catch (IllegalAccessException e) { throw new RuntimeException(e); }
        }
        return m;
    }

    private static Object value(Object v) {
        if (v == null) return null;
        if (v instanceof String || v instanceof Boolean) return v;
        if (v instanceof Float || v instanceof Double) return ((Number) v).doubleValue();
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof Character) return (long) (Character) v;
        if (v instanceof UObject) { LinkedHashMap<String, Object> r = new LinkedHashMap<>(); r.put("instanceID", (long) ((UObject) v).GetInstanceID()); return r; }
        if (v instanceof Vector2) { Vector2 x = (Vector2) v; return map("x", x.x, "y", x.y); }
        if (v instanceof Vector3) { Vector3 x = (Vector3) v; return map("x", x.x, "y", x.y, "z", x.z); }
        if (v instanceof Vector4) { Vector4 x = (Vector4) v; return map("x", x.x, "y", x.y, "z", x.z, "w", x.w); }
        if (v instanceof Quaternion) { Quaternion x = (Quaternion) v; return map("x", x.x, "y", x.y, "z", x.z, "w", x.w); }
        if (v instanceof Color) { Color x = (Color) v; return map("r", x.r, "g", x.g, "b", x.b, "a", x.a); }
        if (v.getClass().isArray()) {
            ArrayList<Object> l = new ArrayList<>();
            for (int i = 0, n = java.lang.reflect.Array.getLength(v); i < n; i++) l.add(value(java.lang.reflect.Array.get(v, i)));
            return l;
        }
        if (v instanceof cs.System.Collections.Generic.List_1) {
            cs.System.Collections.Generic.List_1 s = (cs.System.Collections.Generic.List_1) v;
            ArrayList<Object> l = new ArrayList<>();
            for (int i = 0; i < s.get_Count(); i++) l.add(value(s.get_Item(i)));
            return l;
        }
        return toTree(v);
    }

    private static LinkedHashMap<String, Object> map(Object... kv) {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], ((Number) kv[i + 1]).doubleValue());
        return m;
    }

    static void fromTree(Object o, HashMap<String, Object> d) {
        Loader.Ctx ctx = null;
        for (Object[] f : fields(o.getClass())) {
            String n = (String) f[1];
            if (!d.containsKey(n)) continue;
            java.lang.reflect.Field jf = (java.lang.reflect.Field) f[0];
            try {
                Object v = Loader.convert(d.get(n), (String) f[2], jf.getType(), jf.get(o), ctx);
                if (v != Loader.NOVALUE) jf.set(o, v);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (RuntimeException e) {
                Debug.LogWarning("JsonUtility: field " + n + ": " + e);
            }
        }
    }
}
