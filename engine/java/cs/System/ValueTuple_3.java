package cs.System;

public class ValueTuple_3 implements cs.rt.Struct {
    public Object Item1, Item2, Item3;
    public ValueTuple_3() {}
    public ValueTuple_3(Object a0, Object a1, Object a2) { Item1 = a0; Item2 = a1; Item3 = a2; }
    public ValueTuple_3 $copy() { ValueTuple_3 t = new ValueTuple_3(); t.$set(this); return t; }
    public void $set(Object o) { ValueTuple_3 t = (ValueTuple_3) o; Item1 = t.Item1 instanceof cs.rt.Struct ? ((cs.rt.Struct) t.Item1).$copy() : t.Item1; Item2 = t.Item2 instanceof cs.rt.Struct ? ((cs.rt.Struct) t.Item2).$copy() : t.Item2; Item3 = t.Item3 instanceof cs.rt.Struct ? ((cs.rt.Struct) t.Item3).$copy() : t.Item3; }
    public void $clear() { Item1 = null; Item2 = null; Item3 = null; }
    public static ValueTuple_3[] $newArray(int n) { ValueTuple_3[] a = new ValueTuple_3[n]; for (int i = 0; i < n; i++) a[i] = new ValueTuple_3(); return a; }
    @Override public boolean equals(Object o) { if (!(o instanceof ValueTuple_3)) return false; ValueTuple_3 t = (ValueTuple_3) o; return ObjectX.Equals(Item1, t.Item1) && ObjectX.Equals(Item2, t.Item2) && ObjectX.Equals(Item3, t.Item3); }
    @Override public int hashCode() { int h = 0; h = h * 31 + ObjectX.hash(Item1); h = h * 31 + ObjectX.hash(Item2); h = h * 31 + ObjectX.hash(Item3); return h; }
    @Override public String toString() { return "(" + ObjectX.str(Item1) + ", " + ObjectX.str(Item2) + ", " + ObjectX.str(Item3) + ")"; }
}
