package cs.System;

public class ArraySegment_1 implements cs.rt.Struct {
    public Object array;
    public int offset, count;
    public ArraySegment_1() {}
    public ArraySegment_1(Object a) { array = a; offset = 0; count = java.lang.reflect.Array.getLength(a); }
    public ArraySegment_1(Object a, int o, int c) { array = a; offset = o; count = c; }
    public Object get_Array() { return array; }
    public int get_Count() { return count; }
    public int get_Offset() { return offset; }
    public Object get_Item(int i) { return java.lang.reflect.Array.get(array, offset + i); }
    public ArraySegment_1 $copy() { return new ArraySegment_1(array, offset, count); }
    public void $set(Object o) { ArraySegment_1 s = (ArraySegment_1) o; array = s.array; offset = s.offset; count = s.count; }
    public void $clear() { array = null; offset = count = 0; }
    public static ArraySegment_1[] $newArray(int n) { ArraySegment_1[] a = new ArraySegment_1[n]; for (int i = 0; i < n; i++) a[i] = new ArraySegment_1(); return a; }
}
