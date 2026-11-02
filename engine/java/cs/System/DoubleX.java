package cs.System;

import cs.rt.Fmt;

public final class DoubleX {
    private DoubleX() {}
    public static String ToString(double v) { return Fmt.str(v); }
    public static String ToString(double v, String f) { return Fmt.formatDouble(v, f, false); }
    public static String ToString(double v, IFormatProvider p) { return Fmt.str(v); }
    public static String ToString(double v, String f, IFormatProvider p) { return Fmt.formatDouble(v, f, false); }
    public static double Parse(String s) { return Fmt.parseDouble(s); }
    public static double Parse(String s, IFormatProvider p) { return Fmt.parseDouble(s); }
    public static double Parse(String s, int styles, IFormatProvider p) { return Fmt.parseDouble(s); }
    public static boolean TryParse(String s, cs.rt.RefD r) { return Fmt.tryDouble(s, r); }
    public static boolean TryParse(String s, int styles, IFormatProvider p, cs.rt.RefD r) {
        if ((styles & 0x200) != 0) {
            try { r.v = Long.parseLong(s.trim(), 16); return true; } catch (RuntimeException e) { r.v = 0; return false; }
        }
        return Fmt.tryDouble(s, r);
    }
    public static boolean IsNaN(double f) { return f != f; }
    public static boolean IsInfinity(double f) { return Double.isInfinite(f); }
    public static boolean IsPositiveInfinity(double f) { return f == Double.POSITIVE_INFINITY; }
    public static boolean IsNegativeInfinity(double f) { return f == Double.NEGATIVE_INFINITY; }
    public static int CompareTo(double a, double b) { return Double.compare(a, b); }
    public static int CompareTo(double a, Object b) { return b == null ? 1 : Double.compare(a, cs.rt.Rt.unboxD(b)); }
    public static boolean Equals(double a, double b) { return a == b || (a != a && b != b); }
    public static boolean Equals(double a, Object b) { return b instanceof Double && Equals(a, (double) (Double) b); }
    public static int GetHashCode(double a) { long b = a == 0 ? 0 : Double.doubleToLongBits(a); return (int) b ^ (int) (b >> 32); }
}
