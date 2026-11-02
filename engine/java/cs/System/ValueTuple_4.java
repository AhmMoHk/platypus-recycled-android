package cs.System;

public class ValueTuple_4 implements cs.rt.Struct {
    public Object Item1, Item2, Item3, Item4;
    public ValueTuple_4() {}
    public ValueTuple_4(Object a0, Object a1, Object a2, Object a3) { Item1 = a0; Item2 = a1; Item3 = a2; Item4 = a3; }
    public ValueTuple_4 $copy() { ValueTuple_4 t = new ValueTuple_4(); t.$set(this); return t; }
    public void $set(Object o) { ValueTuple_4 t = (ValueTuple_4) o; Item1 = t.Item1 instanceof cs.rt.Struct ? ((cs.rt.Struct) t.Item1).$copy() : t.Item1; Item2 = t.Item2 instanceof cs.rt.Struct ? ((cs.rt.Struct) t.Item2).$copy() : t.Item2; Item3 = t.Item3 instanceof cs.rt.Struct ? ((cs.rt.Struct) t.Item3).$copy() : t.Item3; Item4 = t.Item4 instanceof cs.rt.Struct ? ((cs.rt.Struct) t.Item4).$copy() : t.Item4; }
    public void $clear() { Item1 = null; Item2 = null; Item3 = null; Item4 = null; }
    public static ValueTuple_4[] $newArray(int n) { ValueTuple_4[] a = new ValueTuple_4[n]; for (int i = 0; i < n; i++) a[i] = new ValueTuple_4(); return a; }
    @Override public boolean equals(Object o) { if (!(o instanceof ValueTuple_4)) return false; ValueTuple_4 t = (ValueTuple_4) o; return ObjectX.Equals(Item1, t.Item1) && ObjectX.Equals(Item2, t.Item2) && ObjectX.Equals(Item3, t.Item3) && ObjectX.Equals(Item4, t.Item4); }
    @Override public int hashCode() { int h = 0; h = h * 31 + ObjectX.hash(Item1); h = h * 31 + ObjectX.hash(Item2); h = h * 31 + ObjectX.hash(Item3); h = h * 31 + ObjectX.hash(Item4); return h; }
    @Override public String toString() { return "(" + ObjectX.str(Item1) + ", " + ObjectX.str(Item2) + ", " + ObjectX.str(Item3) + ", " + ObjectX.str(Item4) + ")"; }
}
