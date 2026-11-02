package cs.System;

public class ValueTuple_5 implements cs.rt.Struct {
    public Object Item1, Item2, Item3, Item4, Item5;
    public ValueTuple_5() {}
    public ValueTuple_5(Object a0, Object a1, Object a2, Object a3, Object a4) { Item1 = a0; Item2 = a1; Item3 = a2; Item4 = a3; Item5 = a4; }
    public ValueTuple_5 $copy() { ValueTuple_5 t = new ValueTuple_5(); t.$set(this); return t; }
    public void $set(Object o) { ValueTuple_5 t = (ValueTuple_5) o; Item1 = t.Item1 instanceof cs.rt.Struct ? ((cs.rt.Struct) t.Item1).$copy() : t.Item1; Item2 = t.Item2 instanceof cs.rt.Struct ? ((cs.rt.Struct) t.Item2).$copy() : t.Item2; Item3 = t.Item3 instanceof cs.rt.Struct ? ((cs.rt.Struct) t.Item3).$copy() : t.Item3; Item4 = t.Item4 instanceof cs.rt.Struct ? ((cs.rt.Struct) t.Item4).$copy() : t.Item4; Item5 = t.Item5 instanceof cs.rt.Struct ? ((cs.rt.Struct) t.Item5).$copy() : t.Item5; }
    public void $clear() { Item1 = null; Item2 = null; Item3 = null; Item4 = null; Item5 = null; }
    public static ValueTuple_5[] $newArray(int n) { ValueTuple_5[] a = new ValueTuple_5[n]; for (int i = 0; i < n; i++) a[i] = new ValueTuple_5(); return a; }
    @Override public boolean equals(Object o) { if (!(o instanceof ValueTuple_5)) return false; ValueTuple_5 t = (ValueTuple_5) o; return ObjectX.Equals(Item1, t.Item1) && ObjectX.Equals(Item2, t.Item2) && ObjectX.Equals(Item3, t.Item3) && ObjectX.Equals(Item4, t.Item4) && ObjectX.Equals(Item5, t.Item5); }
    @Override public int hashCode() { int h = 0; h = h * 31 + ObjectX.hash(Item1); h = h * 31 + ObjectX.hash(Item2); h = h * 31 + ObjectX.hash(Item3); h = h * 31 + ObjectX.hash(Item4); h = h * 31 + ObjectX.hash(Item5); return h; }
    @Override public String toString() { return "(" + ObjectX.str(Item1) + ", " + ObjectX.str(Item2) + ", " + ObjectX.str(Item3) + ", " + ObjectX.str(Item4) + ", " + ObjectX.str(Item5) + ")"; }
}
