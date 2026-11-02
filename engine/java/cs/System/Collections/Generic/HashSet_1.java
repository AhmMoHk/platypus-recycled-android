package cs.System.Collections.Generic;

import java.util.LinkedHashSet;

public class HashSet_1 implements ICollection_1, IReadOnlyCollection_1 {
    final LinkedHashSet<Object> set = new LinkedHashSet<>();
    int version;

    public HashSet_1() {}
    public HashSet_1(IEnumerable_1 src) { UnionWith(src); }
    public HashSet_1(IEqualityComparer_1 c) {}
    public HashSet_1(int capacity) {}

    public int get_Count() { return set.size(); }
    public boolean Add$bool(Object v) { boolean r = set.add(v); if (r) version++; return r; }
    public void Add(Object v) { Add$bool(v); }
    public boolean Contains(Object v) { return set.contains(v); }
    public boolean Remove(Object v) { boolean r = set.remove(v); if (r) version++; return r; }
    public void Clear() { set.clear(); version++; }
    public void UnionWith(IEnumerable_1 src) { IEnumerator_1 e = src.GetEnumerator(); while (e.MoveNext()) Add$bool(e.get_Current()); }
    public void ExceptWith(IEnumerable_1 src) { IEnumerator_1 e = src.GetEnumerator(); while (e.MoveNext()) Remove(e.get_Current()); }
    public void IntersectWith(IEnumerable_1 src) {
        HashSet_1 o = new HashSet_1(src);
        set.retainAll(o.set);
        version++;
    }
    public boolean SetEquals(IEnumerable_1 src) { HashSet_1 o = new HashSet_1(src); return set.equals(o.set); }
    public boolean Overlaps(IEnumerable_1 src) { IEnumerator_1 e = src.GetEnumerator(); while (e.MoveNext()) if (set.contains(e.get_Current())) return true; return false; }
    public boolean IsSubsetOf(IEnumerable_1 src) { HashSet_1 o = new HashSet_1(src); return o.set.containsAll(set); }
    public int RemoveWhere(cs.System.Predicate_1 p) {
        int n = 0;
        for (Object o : set.toArray()) if (p.Invoke(o)) { set.remove(o); n++; }
        if (n > 0) version++;
        return n;
    }
    public boolean TryGetValue(Object v, cs.rt.RefO out) {
        for (Object o : set) if (cs.System.ObjectX.Equals(o, v)) { out.v = o; return true; }
        out.v = null;
        return false;
    }
    public void CopyTo(Object arr) { int i = 0; for (Object o : set) cs.rt.Rt.arraySet(arr, i++, o); }
    public HashSet_1$Enumerator GetEnumerator() { return new HashSet_1$Enumerator(this); }
}
