package cs.System;

public class TimeZoneInfo {
    final java.util.TimeZone tz;
    TimeZoneInfo(java.util.TimeZone t) { tz = t; }
    public static TimeZoneInfo get_Local() { return new TimeZoneInfo(java.util.TimeZone.getDefault()); }
    public static TimeZoneInfo get_Utc() { return new TimeZoneInfo(java.util.TimeZone.getTimeZone("UTC")); }
    public String get_Id() { return tz.getID(); }
    public String get_DisplayName() { return tz.getDisplayName(); }
    public TimeSpan GetUtcOffset(DateTime d) { return TimeSpan.FromMilliseconds(tz.getOffset(System.currentTimeMillis())); }
    public static DateTime ConvertTimeFromUtc(DateTime d, TimeZoneInfo z) { return DateTime.op_Addition(d, TimeSpan.FromMilliseconds(z.tz.getOffset(System.currentTimeMillis()))); }
}
