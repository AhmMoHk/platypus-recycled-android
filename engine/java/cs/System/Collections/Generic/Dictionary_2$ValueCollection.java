package cs.System.Collections.Generic;

public class Dictionary_2$ValueCollection implements ICollection_1, IReadOnlyCollection_1 {
    final Dictionary_2 d;
    Dictionary_2$ValueCollection(Dictionary_2 d) { this.d = d; }
    public int get_Count() { return d.get_Count(); }
    public Dictionary_2$ValueCollection$Enumerator GetEnumerator() { return new Dictionary_2$ValueCollection$Enumerator(d); }
    public void Add(Object v) { throw new cs.System.NotSupportedException("Mutating a value collection derived from a dictionary is not allowed."); }
    public void Clear() { throw new cs.System.NotSupportedException("Mutating a value collection derived from a dictionary is not allowed."); }
    public boolean Contains(Object v) { return d.ContainsValue(v); }
    public boolean Remove(Object v) { throw new cs.System.NotSupportedException("Mutating a value collection derived from a dictionary is not allowed."); }
    public void CopyTo(Object arr, int at) { Object[] e = d.entries(); for (int i = 0; i < e.length / 2; i++) cs.rt.Rt.arraySet(arr, at + i, e[i * 2 + 1]); }
}
