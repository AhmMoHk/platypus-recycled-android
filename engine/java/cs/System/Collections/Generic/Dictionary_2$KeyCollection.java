package cs.System.Collections.Generic;

public class Dictionary_2$KeyCollection implements ICollection_1, IReadOnlyCollection_1 {
    final Dictionary_2 d;
    Dictionary_2$KeyCollection(Dictionary_2 d) { this.d = d; }
    public Dictionary_2$KeyCollection(Object d) { this.d = (Dictionary_2) d; }
    public int get_Count() { return d.get_Count(); }
    public Dictionary_2$KeyCollection$Enumerator GetEnumerator() { return new Dictionary_2$KeyCollection$Enumerator(d); }
    public void Add(Object v) { throw new cs.System.NotSupportedException("Mutating a key collection derived from a dictionary is not allowed."); }
    public void Clear() { throw new cs.System.NotSupportedException("Mutating a key collection derived from a dictionary is not allowed."); }
    public boolean Contains(Object v) { return d.ContainsKey(v); }
    public boolean Remove(Object v) { throw new cs.System.NotSupportedException("Mutating a key collection derived from a dictionary is not allowed."); }
    public void CopyTo(Object arr, int at) { Object[] e = d.entries(); for (int i = 0; i < e.length / 2; i++) cs.rt.Rt.arraySet(arr, at + i, e[i * 2]); }
}
