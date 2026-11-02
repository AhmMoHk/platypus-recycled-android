package cs.System.Collections.Generic;

public class CollectionExtensions {
    public static Object GetValueOrDefault(cs.System.Type tk, cs.System.Type tv, IReadOnlyDictionary_2 d, Object key, Object def) {
        cs.rt.RefO r = new cs.rt.RefO();
        return d.TryGetValue(key, r) ? r.v : def;
    }
    public static Object GetValueOrDefault(cs.System.Type tk, cs.System.Type tv, IReadOnlyDictionary_2 d, Object key) {
        cs.rt.RefO r = new cs.rt.RefO();
        return d.TryGetValue(key, r) ? r.v : tv.defaultValue();
    }
    public static boolean TryAdd(cs.System.Type tk, cs.System.Type tv, IDictionary_2 d, Object key, Object v) {
        if (d.ContainsKey(key)) return false;
        d.Add(key, v);
        return true;
    }
}
