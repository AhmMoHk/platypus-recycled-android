package cs.System;

import cs.rt.Fmt;

public final class Int64X {
    private Int64X() {}
    public static String ToString(long v) { return Long.toString(v); }
    public static String ToString(long v, String f) { return Fmt.formatLong(v, f, false); }
    public static String ToString(long v, IFormatProvider p) { return Long.toString(v); }
    public static String ToString(long v, String f, IFormatProvider p) { return Fmt.formatLong(v, f, false); }
    public static long Parse(String s) { return Fmt.parseLong(s); }
    public static long Parse(String s, IFormatProvider p) { return Fmt.parseLong(s); }
    public static boolean TryParse(String s, cs.rt.RefJ r) {
        try { r.v = Fmt.parseLong(s); return true; } catch (RuntimeException e) { r.v = 0; return false; }
    }
    public static int CompareTo(long a, long b) { return Long.compare(a, b); }
    public static boolean Equals(long a, long b) { return a == b; }
    public static int GetHashCode(long a) { return (int) a ^ (int) (a >> 32); }
}
