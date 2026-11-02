package cs.UnityEngine;

/** JsonUtility over the Unity-serialized fields ($fmeta flag 8 or public) */
public final class JsonUtility {
    private JsonUtility() {}
    public static String ToJson(Object o) { return ToJson(o, false); }
    public static String ToJson(Object o, boolean pretty) {
        if (o == null) return "";
        return cs.rt.Json.write(UnitySer.toTree(o));
    }
    public static Object FromJson(String json, cs.System.Type t) {
        Object o = Loader.instantiate(t.cls);
        FromJsonOverwrite(json, o);
        return o;
    }
    public static Object FromJson(cs.System.Type g, String json) { return FromJson(json, g); }
    public static void FromJsonOverwrite(String json, Object o) {
        if (json == null || json.trim().isEmpty() || o == null) return;
        Object tree = cs.rt.Json.parse(json);
        if (tree instanceof java.util.HashMap) UnitySer.fromTree(o, cs.rt.Json.map(tree));
    }
}
