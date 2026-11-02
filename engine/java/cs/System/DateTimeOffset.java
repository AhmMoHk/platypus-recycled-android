package cs.System;

public class DateTimeOffset implements cs.rt.Struct {
    public long ms;
    public DateTimeOffset() {}
    public static DateTimeOffset get_UtcNow() { DateTimeOffset d = new DateTimeOffset(); d.ms = System.currentTimeMillis(); return d; }
    public static DateTimeOffset get_Now() { return get_UtcNow(); }
    public long ToUnixTimeMilliseconds() { return ms; }
    public long ToUnixTimeSeconds() { return ms / 1000; }
    public static DateTimeOffset FromUnixTimeSeconds(long s) { DateTimeOffset d = new DateTimeOffset(); d.ms = s * 1000; return d; }
    public static DateTimeOffset FromUnixTimeMilliseconds(long s) { DateTimeOffset d = new DateTimeOffset(); d.ms = s; return d; }
    public DateTimeOffset $copy() { DateTimeOffset d = new DateTimeOffset(); d.ms = ms; return d; }
    public void $set(Object o) { ms = ((DateTimeOffset) o).ms; }
    public void $clear() { ms = 0; }
    public static DateTimeOffset[] $newArray(int n) { DateTimeOffset[] a = new DateTimeOffset[n]; for (int i = 0; i < n; i++) a[i] = new DateTimeOffset(); return a; }
}
