package cs.System;

public class Tuple_2 {
    public final Object item1, item2;
    public Tuple_2(Object a, Object b) { item1 = a; item2 = b; }
    public Object get_Item1() { return item1; }
    public Object get_Item2() { return item2; }
    @Override public boolean equals(Object o) { return o instanceof Tuple_2 && ObjectX.Equals(item1, ((Tuple_2) o).item1) && ObjectX.Equals(item2, ((Tuple_2) o).item2); }
    @Override public int hashCode() { return ObjectX.hash(item1) * 31 + ObjectX.hash(item2); }
}
