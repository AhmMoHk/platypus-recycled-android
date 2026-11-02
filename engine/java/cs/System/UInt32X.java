package cs.System;

import cs.rt.Fmt;

public final class UInt32X {
    private UInt32X() {}
    public static String ToString(int v) { return Long.toString(v & 0xffffffffL); }
    public static String ToString(int v, String f) { return Fmt.formatLong(v & 0xffffffffL, f, true); }
    public static String ToString(int v, IFormatProvider p) { return ToString(v); }
    public static int Parse(String s) { return (int) Fmt.parseLong(s); }
    public static boolean TryParse(String s, cs.rt.RefI r) {
        try { r.v = (int) Fmt.parseLong(s); return true; } catch (RuntimeException e) { r.v = 0; return false; }
    }
    public static int CompareTo(int a, int b) { return cs.rt.Rt.cmpu(a, b); }
    public static boolean Equals(int a, int b) { return a == b; }
    public static int GetHashCode(int a) { return a; }
}
