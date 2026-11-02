package cs.System;

public class TimeSpan implements cs.rt.Struct {
    public long ticks;
    public static final TimeSpan Zero = new TimeSpan();
    public TimeSpan() {}
    public TimeSpan(long ticks) { this.ticks = ticks; }
    public TimeSpan(int h, int m, int s) { ticks = ((h * 3600L) + m * 60L + s) * 10000000L; }
    public static TimeSpan FromSeconds(double s) { return new TimeSpan((long) (s * 1e7)); }
    public static TimeSpan FromMilliseconds(double s) { return new TimeSpan((long) (s * 1e4)); }
    public static TimeSpan FromMinutes(double s) { return new TimeSpan((long) (s * 6e8)); }
    public double get_TotalSeconds() { return ticks / 1e7; }
    public double get_TotalMilliseconds() { return ticks / 1e4; }
    public double get_TotalMinutes() { return ticks / 6e8; }
    public double get_TotalHours() { return ticks / 3.6e10; }
    public int get_Milliseconds() { return (int) (ticks / 10000 % 1000); }
    public int get_Seconds() { return (int) (ticks / 10000000 % 60); }
    public int get_Minutes() { return (int) (ticks / 600000000L % 60); }
    public int get_Hours() { return (int) (ticks / 36000000000L % 24); }
    public int get_Days() { return (int) (ticks / 864000000000L); }
    public long get_Ticks() { return ticks; }
    public static TimeSpan op_Addition(TimeSpan a, TimeSpan b) { return new TimeSpan(a.ticks + b.ticks); }
    public static TimeSpan op_Subtraction(TimeSpan a, TimeSpan b) { return new TimeSpan(a.ticks - b.ticks); }
    public static boolean op_GreaterThan(TimeSpan a, TimeSpan b) { return a.ticks > b.ticks; }
    public static boolean op_LessThan(TimeSpan a, TimeSpan b) { return a.ticks < b.ticks; }
    @Override public String toString() {
        long t = Math.Abs(ticks);
        String s = String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", t / 36000000000L % 24, t / 600000000L % 60, t / 10000000 % 60);
        long d = t / 864000000000L;
        if (d > 0) s = d + "." + s;
        if (t % 10000000 != 0) s += String.format(java.util.Locale.ROOT, ".%07d", t % 10000000);
        return ticks < 0 ? "-" + s : s;
    }
    public String ToString(String f) {
        long t = Math.Abs(ticks);
        if (f == null) return toString();
        String r = f.replace("\\", "");
        r = r.replace("hh", pad(t / 36000000000L % 24)).replace("mm", pad(t / 600000000L % 60)).replace("ss", pad(t / 10000000 % 60));
        r = r.replace("fff", String.format(java.util.Locale.ROOT, "%03d", t / 10000 % 1000)).replace("ff", String.format(java.util.Locale.ROOT, "%02d", t / 100000 % 100));
        return r;
    }
    private static String pad(long v) { return v < 10 ? "0" + v : Long.toString(v); }
    @Override public boolean equals(Object o) { return o instanceof TimeSpan && ((TimeSpan) o).ticks == ticks; }
    @Override public int hashCode() { return (int) ticks ^ (int) (ticks >> 32); }
    public TimeSpan $copy() { return new TimeSpan(ticks); }
    public void $set(Object o) { ticks = ((TimeSpan) o).ticks; }
    public void $clear() { ticks = 0; }
    public static TimeSpan[] $newArray(int n) { TimeSpan[] a = new TimeSpan[n]; for (int i = 0; i < n; i++) a[i] = new TimeSpan(); return a; }
}
