package cs.System;

import java.util.Calendar;
import java.util.TimeZone;

/** System.DateTime (ticks since 0001-01-01, kind ignored except for UtcNow/Now). */
public class DateTime implements cs.rt.Struct {
    public long ticks;
    private static final long EPOCH = 621355968000000000L;
    public DateTime() {}
    public DateTime(long ticks) { this.ticks = ticks; }
    public DateTime(int y, int mo, int d) { this(y, mo, d, 0, 0, 0, 0); }
    public DateTime(int y, int mo, int d, int h, int mi, int s) { this(y, mo, d, h, mi, s, 0); }
    public DateTime(int y, int mo, int d, int h, int mi, int s, int ms) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.clear();
        c.set(y, mo - 1, d, h, mi, s);
        c.set(Calendar.MILLISECOND, ms);
        ticks = c.getTimeInMillis() * 10000 + EPOCH;
    }
    public static DateTime fromMillis(long ms) { return new DateTime(ms * 10000 + EPOCH); }
    public static DateTime get_UtcNow() { return fromMillis(System.currentTimeMillis()); }
    public static DateTime get_Now() { long ms = System.currentTimeMillis(); return fromMillis(ms + TimeZone.getDefault().getOffset(ms)); }
    public static DateTime get_Today() { DateTime n = get_Now(); return new DateTime(n.ticks - n.ticks % 864000000000L); }
    private Calendar cal() {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.setTimeInMillis((ticks - EPOCH) / 10000);
        return c;
    }
    public int get_Year() { return cal().get(Calendar.YEAR); }
    public int get_Month() { return cal().get(Calendar.MONTH) + 1; }
    public int get_Day() { return cal().get(Calendar.DAY_OF_MONTH); }
    public int get_Hour() { return cal().get(Calendar.HOUR_OF_DAY); }
    public int get_Minute() { return cal().get(Calendar.MINUTE); }
    public int get_Second() { return cal().get(Calendar.SECOND); }
    public int get_Millisecond() { return cal().get(Calendar.MILLISECOND); }
    public int get_DayOfWeek() { return cal().get(Calendar.DAY_OF_WEEK) - 1; }
    public int get_DayOfYear() { return cal().get(Calendar.DAY_OF_YEAR); }
    public long get_Ticks() { return ticks; }
    public boolean IsDaylightSavingTime() { return false; }
    public static DateTime op_Addition(DateTime a, TimeSpan b) { return new DateTime(a.ticks + b.ticks); }
    public static TimeSpan op_Subtraction(DateTime a, DateTime b) { return new TimeSpan(a.ticks - b.ticks); }
    public static boolean op_GreaterThan(DateTime a, DateTime b) { return a.ticks > b.ticks; }
    public static boolean op_LessThan(DateTime a, DateTime b) { return a.ticks < b.ticks; }
    public static boolean op_Equality(DateTime a, DateTime b) { return a.ticks == b.ticks; }
    public DateTime AddSeconds(double s) { return new DateTime(ticks + (long) (s * 1e7)); }
    public DateTime AddDays(double s) { return new DateTime(ticks + (long) (s * 864000000000L)); }
    public String ToString(String f) {
        Calendar c = cal();
        if (f == null || f.isEmpty()) return toString();
        String r = f;
        r = r.replace("yyyy", String.format(java.util.Locale.ROOT, "%04d", c.get(Calendar.YEAR)));
        r = r.replace("MM", String.format(java.util.Locale.ROOT, "%02d", c.get(Calendar.MONTH) + 1));
        r = r.replace("dd", String.format(java.util.Locale.ROOT, "%02d", c.get(Calendar.DAY_OF_MONTH)));
        r = r.replace("HH", String.format(java.util.Locale.ROOT, "%02d", c.get(Calendar.HOUR_OF_DAY)));
        r = r.replace("mm", String.format(java.util.Locale.ROOT, "%02d", c.get(Calendar.MINUTE)));
        r = r.replace("ss", String.format(java.util.Locale.ROOT, "%02d", c.get(Calendar.SECOND)));
        return r;
    }
    @Override public String toString() { return ToString("MM/dd/yyyy HH:mm:ss"); }
    @Override public boolean equals(Object o) { return o instanceof DateTime && ((DateTime) o).ticks == ticks; }
    @Override public int hashCode() { return (int) ticks ^ (int) (ticks >> 32); }
    public DateTime $copy() { return new DateTime(ticks); }
    public void $set(Object o) { ticks = ((DateTime) o).ticks; }
    public void $clear() { ticks = 0; }
    public static DateTime[] $newArray(int n) { DateTime[] a = new DateTime[n]; for (int i = 0; i < n; i++) a[i] = new DateTime(); return a; }
}
