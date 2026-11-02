package cs.System;

/** System.Math (note: inside package cs.System, java.lang.Math must be written in full). */
public final class Math {
    private Math() {}
    public static final double PI = java.lang.Math.PI, E = java.lang.Math.E;
    public static double Abs(double a) { return java.lang.Math.abs(a); }
    public static float Abs(float a) { return java.lang.Math.abs(a); }
    public static int Abs(int a) { if (a == Integer.MIN_VALUE) throw new OverflowException("Negating the minimum value of a twos complement number is invalid."); return java.lang.Math.abs(a); }
    public static long Abs(long a) { return java.lang.Math.abs(a); }
    public static double Acos(double a) { return java.lang.Math.acos(a); }
    public static double Asin(double a) { return java.lang.Math.asin(a); }
    public static double Atan(double a) { return java.lang.Math.atan(a); }
    public static double Atan2(double y, double x) { return java.lang.Math.atan2(y, x); }
    public static double Ceiling(double a) { return java.lang.Math.ceil(a); }
    public static double Floor(double a) { return java.lang.Math.floor(a); }
    public static double Cos(double a) { return java.lang.Math.cos(a); }
    public static double Sin(double a) { return java.lang.Math.sin(a); }
    public static double Tan(double a) { return java.lang.Math.tan(a); }
    public static double Cosh(double a) { return java.lang.Math.cosh(a); }
    public static double Sinh(double a) { return java.lang.Math.sinh(a); }
    public static double Tanh(double a) { return java.lang.Math.tanh(a); }
    public static double Exp(double a) { return java.lang.Math.exp(a); }
    public static double Log(double a) { return java.lang.Math.log(a); }
    public static double Log(double a, double b) { return java.lang.Math.log(a) / java.lang.Math.log(b); }
    public static double Log10(double a) { return java.lang.Math.log10(a); }
    public static double Pow(double a, double b) { return java.lang.Math.pow(a, b); }
    public static double Sqrt(double a) { return java.lang.Math.sqrt(a); }
    public static double IEEERemainder(double a, double b) { return java.lang.Math.IEEEremainder(a, b); }
    public static double Max(double a, double b) { return (a > b || Double.isNaN(a)) ? a : b; }
    public static float Max(float a, float b) { return (a > b || Float.isNaN(a)) ? a : b; }
    public static int Max(int a, int b) { return a > b ? a : b; }
    public static long Max(long a, long b) { return a > b ? a : b; }
    public static double Min(double a, double b) { return (a < b || Double.isNaN(a)) ? a : b; }
    public static float Min(float a, float b) { return (a < b || Float.isNaN(a)) ? a : b; }
    public static int Min(int a, int b) { return a < b ? a : b; }
    public static long Min(long a, long b) { return a < b ? a : b; }
    /** banker's rounding */
    public static double Round(double a) { return java.lang.Math.rint(a); }
    public static double Round(double a, int digits) {
        if (Double.isNaN(a) || Double.isInfinite(a)) return a;
        return new java.math.BigDecimal(a).setScale(digits, java.math.RoundingMode.HALF_EVEN).doubleValue();
    }
    public static double Round(double a, int mode, int dummy) { return Round(a); }
    public static Decimal Round(Decimal d) { return Decimal.Round(d, 0); }
    public static int Sign(double a) { if (Double.isNaN(a)) throw new ArithmeticException("Function does not accept floating point Not-a-Number values."); return a > 0 ? 1 : a < 0 ? -1 : 0; }
    public static int Sign(float a) { return Sign((double) a); }
    public static int Sign(int a) { return Integer.signum(a); }
    public static double Truncate(double a) { return a < 0 ? java.lang.Math.ceil(a) : java.lang.Math.floor(a); }
    public static int Clamp(int v, int a, int b) { return v < a ? a : v > b ? b : v; }
    public static float Clamp(float v, float a, float b) { return v < a ? a : v > b ? b : v; }
    public static double Clamp(double v, double a, double b) { return v < a ? a : v > b ? b : v; }
}
