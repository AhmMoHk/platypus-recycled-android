package cs.System;

import cs.rt.Fmt;

public final class SingleX {
    private SingleX() {}
    public static String ToString(float v) { return Fmt.str(v); }
    public static String ToString(float v, String f) { return Fmt.formatDouble(v, f, true); }
    public static String ToString(float v, IFormatProvider p) { return Fmt.str(v); }
    public static String ToString(float v, String f, IFormatProvider p) { return Fmt.formatDouble(v, f, true); }
    public static float Parse(String s) { return (float) Fmt.parseDouble(s); }
    public static float Parse(String s, IFormatProvider p) { return (float) Fmt.parseDouble(s); }
    public static float Parse(String s, int styles, IFormatProvider p) { return (float) Fmt.parseDouble(s); }
    public static boolean TryParse(String s, cs.rt.RefF r) { return Fmt.tryFloat(s, r); }
    public static boolean TryParse(String s, int styles, IFormatProvider p, cs.rt.RefF r) { return Fmt.tryFloat(s, r); }
    public static boolean IsNaN(float f) { return f != f; }
    public static boolean IsInfinity(float f) { return Float.isInfinite(f); }
    public static boolean IsPositiveInfinity(float f) { return f == Float.POSITIVE_INFINITY; }
    public static boolean IsNegativeInfinity(float f) { return f == Float.NEGATIVE_INFINITY; }
    public static boolean IsFinite(float f) { return !Float.isNaN(f) && !Float.isInfinite(f); }
    public static int CompareTo(float a, float b) { return Float.compare(a, b); }
    public static int CompareTo(float a, Object b) { return b == null ? 1 : Float.compare(a, cs.rt.Rt.unboxF(b)); }
    public static boolean Equals(float a, float b) { return a == b || (a != a && b != b); }
    public static boolean Equals(float a, Object b) { return b instanceof Float && Equals(a, (float) (Float) b); }
    public static int GetHashCode(float a) { return a == 0 ? 0 : Float.floatToIntBits(a); }
}
