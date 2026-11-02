package cs.System.Collections.Generic;

public class Stack_1 implements IEnumerable_1, IReadOnlyCollection_1, cs.System.Collections.ICollection {
    final java.util.ArrayList<Object> s = new java.util.ArrayList<>();
    int version;

    public Stack_1() {}
    public Stack_1(int capacity) {}
    public Stack_1(IEnumerable_1 src) { IEnumerator_1 e = src.GetEnumerator(); while (e.MoveNext()) Push(e.get_Current()); }

    public int get_Count() { return s.size(); }
    public void Push(Object v) { s.add(v); version++; }
    public Object Pop() {
        if (s.isEmpty()) throw new cs.System.InvalidOperationException("Stack empty.");
        version++;
        return s.remove(s.size() - 1);
    }
    public Object Peek() {
        if (s.isEmpty()) throw new cs.System.InvalidOperationException("Stack empty.");
        return s.get(s.size() - 1);
    }
    public boolean TryPop(cs.rt.RefO r) { if (s.isEmpty()) { r.v = null; return false; } r.v = Pop(); return true; }
    public boolean TryPeek(cs.rt.RefO r) { if (s.isEmpty()) { r.v = null; return false; } r.v = Peek(); return true; }
    public boolean Contains(Object v) { for (Object o : s) if (cs.System.ObjectX.Equals(o, v)) return true; return false; }
    public void Clear() { s.clear(); version++; }
    /** top first, like .NET */
    Object[] snapshot() {
        Object[] a = new Object[s.size()];
        for (int i = 0; i < a.length; i++) a[i] = s.get(s.size() - 1 - i);
        return a;
    }
    public Object ToArray() { return snapshot(); }
    public Stack_1$Enumerator GetEnumerator() { return new Stack_1$Enumerator(this); }
}
