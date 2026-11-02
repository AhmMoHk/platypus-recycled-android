package cs.System;

import cs.rt.Fmt;

public final class Int32X {
    private Int32X() {}
    public static final int MaxValue = Integer.MAX_VALUE, MinValue = Integer.MIN_VALUE;
    public static String ToString(int v) { return Integer.toString(v); }
    public static String ToString(int v, String f) { return Fmt.formatLong(v, f, false); }
    public static String ToString(int v, IFormatProvider p) { return Integer.toString(v); }
    public static String ToString(int v, String f, IFormatProvider p) { return Fmt.formatLong(v, f, false); }
    public static int Parse(String s) { return Fmt.parseInt(s); }
    public static int Parse(String s, IFormatProvider p) { return Fmt.parseInt(s); }
    public static int Parse(String s, int styles) { return (styles & 0x200) != 0 ? (int) Long.parseLong(s.trim(), 16) : Fmt.parseInt(s); }
    public static int Parse(String s, int styles, IFormatProvider p) { return Parse(s, styles); }
    public static boolean TryParse(String s, cs.rt.RefI r) { return Fmt.tryInt(s, r); }
    public static boolean TryParse(String s, int styles, IFormatProvider p, cs.rt.RefI r) {
        try { r.v = Parse(s, styles); return true; } catch (RuntimeException e) { r.v = 0; return false; }
    }
    public static int CompareTo(int a, int b) { return Integer.compare(a, b); }
    public static int CompareTo(int a, Object b) { return b == null ? 1 : Integer.compare(a, cs.rt.Rt.unboxI(b)); }
    public static boolean Equals(int a, int b) { return a == b; }
    public static boolean Equals(int a, Object b) { return b instanceof Integer && (Integer) b == a; }
    public static int GetHashCode(int a) { return a; }
}
