package cs.System.Collections.Generic;

import java.util.LinkedHashMap;
import java.util.Map;

/** System.Collections.Generic.Dictionary<K,V> - insertion ordered like .NET's (without removals). */
public class Dictionary_2 implements IDictionary_2, IReadOnlyDictionary_2, cs.System.Collections.IDictionary {
    final LinkedHashMap<Object, Object> map;
    int version;
    private final IEqualityComparer_1 comparer;

    public Dictionary_2() { map = new LinkedHashMap<>(); comparer = null; }
    public Dictionary_2(int capacity) { map = new LinkedHashMap<>(java.lang.Math.max(4, capacity * 4 / 3 + 1)); comparer = null; }
    public Dictionary_2(IEqualityComparer_1 c) { map = new LinkedHashMap<>(); comparer = c instanceof EqualityComparer_1 ? null : c; }
    public Dictionary_2(int capacity, IEqualityComparer_1 c) { this(c); }
    public Dictionary_2(IDictionary_2 src) {
        this();
        IEnumerator_1 e = src.GetEnumerator();
        while (e.MoveNext()) { KeyValuePair_2 p = (KeyValuePair_2) e.get_Current(); Add(p.key, p.value); }
    }
    public Dictionary_2(IDictionary_2 src, IEqualityComparer_1 c) {
        this(c);
        IEnumerator_1 e = src.GetEnumerator();
        while (e.MoveNext()) { KeyValuePair_2 p = (KeyValuePair_2) e.get_Current(); Add(p.key, p.value); }
    }

    /** keys wrapped when a custom comparer is used */
    private static final class CKey {
        final Object k;
        final IEqualityComparer_1 c;
        CKey(Object k, IEqualityComparer_1 c) { this.k = k; this.c = c; }
        @Override public boolean equals(Object o) { return o instanceof CKey && c.Equals(k, ((CKey) o).k); }
        @Override public int hashCode() { return c.GetHashCode(k); }
    }

    Object wrap(Object k) {
        if (k == null) throw new cs.System.ArgumentNullException("key");
        return comparer == null ? k : new CKey(k, comparer);
    }

    static Object unwrap(Object k) { return k instanceof CKey ? ((CKey) k).k : k; }

    public int get_Count() { return map.size(); }

    public Object get_Item(Object k) {
        Object w = wrap(k);
        Object v = map.get(w);
        if (v == null && !map.containsKey(w))
            throw new KeyNotFoundException("The given key '" + cs.System.ObjectX.ToString(k) + "' was not present in the dictionary.");
        return v;
    }

    public void set_Item(Object k, Object v) {
        Object w = wrap(k);
        if (!map.containsKey(w)) version++;
        map.put(w, v);
    }

    public void Add(Object k, Object v) {
        Object w = wrap(k);
        if (map.containsKey(w)) throw new cs.System.ArgumentException("An item with the same key has already been added. Key: " + cs.System.ObjectX.ToString(k));
        map.put(w, v);
        version++;
    }
    public void Add(Object kvp) { KeyValuePair_2 p = (KeyValuePair_2) kvp; Add(p.key, p.value); }

    public boolean TryAdd(Object k, Object v) {
        Object w = wrap(k);
        if (map.containsKey(w)) return false;
        map.put(w, v);
        version++;
        return true;
    }

    public boolean ContainsKey(Object k) { return map.containsKey(wrap(k)); }
    public boolean ContainsValue(Object v) {
        for (Object x : map.values()) if (cs.System.ObjectX.Equals(x, v)) return true;
        return false;
    }
    public boolean Contains(Object kvp) {
        KeyValuePair_2 p = (KeyValuePair_2) kvp;
        return ContainsKey(p.key) && cs.System.ObjectX.Equals(map.get(wrap(p.key)), p.value);
    }

    public boolean TryGetValue(Object k, cs.rt.RefO out) {
        Object w = wrap(k);
        Object v = map.get(w);
        if (v == null && !map.containsKey(w)) { out.v = null; return false; }
        out.v = v;
        return true;
    }

    public boolean Remove(Object k) {
        Object w = wrap(k);
        if (!map.containsKey(w)) return false;
        map.remove(w);
        version++;
        return true;
    }
    public boolean Remove(Object k, cs.rt.RefO out) {
        Object w = wrap(k);
        if (!map.containsKey(w)) { out.v = null; return false; }
        out.v = map.remove(w);
        version++;
        return true;
    }

    public void Clear() { map.clear(); version++; }

    public Dictionary_2$KeyCollection get_Keys() { return new Dictionary_2$KeyCollection(this); }
    public Dictionary_2$ValueCollection get_Values() { return new Dictionary_2$ValueCollection(this); }

    public Dictionary_2$Enumerator GetEnumerator() { return new Dictionary_2$Enumerator(this); }

    public cs.System.Collections.IDictionaryEnumerator IDictionary_GetEnumerator() { return new Dictionary_2$Enumerator(this); }
    public Object IDictionary_get_Item(Object k) { Object w = wrap(k); return map.get(w); }
    public void IDictionary_set_Item(Object k, Object v) { set_Item(k, v); }

    /** snapshot of entries for enumerators */
    Object[] entries() {
        Object[] a = new Object[map.size() * 2];
        int i = 0;
        for (Map.Entry<Object, Object> e : map.entrySet()) { a[i++] = unwrap(e.getKey()); a[i++] = e.getValue(); }
        return a;
    }

    /** `x is Dictionary<K,V>` with erased generics: judged by the first entry (an empty one matches) */
    public boolean $argsMatch(cs.System.Type[] g) {
        if (g == null || g.length < 2) return true;
        for (Map.Entry<Object, Object> e : map.entrySet()) return cs.rt.Rt.instOf(g[0], unwrap(e.getKey())) && cs.rt.Rt.instOf(g[1], e.getValue());
        return true;
    }
}
