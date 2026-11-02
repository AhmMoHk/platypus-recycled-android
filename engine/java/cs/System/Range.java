package cs.System;

public class Range implements cs.rt.Struct {
    public Index start = new Index(), end = new Index(0, true);
    public Range() {}
    public Range(Index s, Index e) { start = s.$copy(); end = e.$copy(); }
    public static Range StartAt(Index s) { return new Range(s, new Index(0, true)); }
    public static Range EndAt(Index e) { return new Range(new Index(0, false), e); }
    public Index get_Start() { return start.$copy(); }
    public Index get_End() { return end.$copy(); }
    public Range $copy() { return new Range(start, end); }
    public void $set(Object o) { Range r = (Range) o; start.$set(r.start); end.$set(r.end); }
    public void $clear() { start.$clear(); end.$clear(); }
    public static Range[] $newArray(int n) { Range[] a = new Range[n]; for (int i = 0; i < n; i++) a[i] = new Range(); return a; }
}
