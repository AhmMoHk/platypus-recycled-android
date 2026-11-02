package cs.System;

public class ValueTuple_2 implements cs.rt.Struct {
    public Object Item1, Item2;
    public ValueTuple_2() {}
    public ValueTuple_2(Object a0, Object a1) { Item1 = a0; Item2 = a1; }
    public ValueTuple_2 $copy() { ValueTuple_2 t = new ValueTuple_2(); t.$set(this); return t; }
    public void $set(Object o) { ValueTuple_2 t = (ValueTuple_2) o; Item1 = t.Item1 instanceof cs.rt.Struct ? ((cs.rt.Struct) t.Item1).$copy() : t.Item1; Item2 = t.Item2 instanceof cs.rt.Struct ? ((cs.rt.Struct) t.Item2).$copy() : t.Item2; }
    public void $clear() { Item1 = null; Item2 = null; }
    public static ValueTuple_2[] $newArray(int n) { ValueTuple_2[] a = new ValueTuple_2[n]; for (int i = 0; i < n; i++) a[i] = new ValueTuple_2(); return a; }
    @Override public boolean equals(Object o) { if (!(o instanceof ValueTuple_2)) return false; ValueTuple_2 t = (ValueTuple_2) o; return ObjectX.Equals(Item1, t.Item1) && ObjectX.Equals(Item2, t.Item2); }
    @Override public int hashCode() { int h = 0; h = h * 31 + ObjectX.hash(Item1); h = h * 31 + ObjectX.hash(Item2); return h; }
    @Override public String toString() { return "(" + ObjectX.str(Item1) + ", " + ObjectX.str(Item2) + ")"; }
}
