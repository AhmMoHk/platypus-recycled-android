package cs.System;

public class Index implements cs.rt.Struct {
    public int value;
    public boolean fromEnd;
    public Index() {}
    public Index(int v, boolean e) { value = v; fromEnd = e; }
    public static Index op_Implicit$Index(int v) { return new Index(v, false); }
    public int GetOffset(int len) { return fromEnd ? len - value : value; }
    public int get_Value() { return value; }
    public boolean get_IsFromEnd() { return fromEnd; }
    public Index $copy() { return new Index(value, fromEnd); }
    public void $set(Object o) { Index i = (Index) o; value = i.value; fromEnd = i.fromEnd; }
    public void $clear() { value = 0; fromEnd = false; }
    public static Index[] $newArray(int n) { Index[] a = new Index[n]; for (int i = 0; i < n; i++) a[i] = new Index(); return a; }
}
