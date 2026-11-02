package cs.UnityEngine;

public class RangeInt implements cs.rt.Struct {
    public int start, length;
    public RangeInt() {}
    public RangeInt(int s, int l) { start = s; length = l; }
    public int get_end() { return start + length; }
    public RangeInt $copy() { return new RangeInt(start, length); }
    public void $set(Object o) { RangeInt r = (RangeInt) o; start = r.start; length = r.length; }
    public void $clear() { start = length = 0; }
    public static RangeInt[] $newArray(int n) { RangeInt[] a = new RangeInt[n]; for (int i = 0; i < n; i++) a[i] = new RangeInt(); return a; }
}
