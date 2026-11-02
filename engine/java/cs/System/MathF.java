package cs.System;

public final class MathF {
    private MathF() {}
    public static final float PI = (float) java.lang.Math.PI;
    public static float Max(float a, float b) { return (a > b || Float.isNaN(a)) ? a : b; }
    public static float Min(float a, float b) { return (a < b || Float.isNaN(a)) ? a : b; }
    public static float Abs(float a) { return java.lang.Math.abs(a); }
    public static float Sqrt(float a) { return (float) java.lang.Math.sqrt(a); }
    public static float Sin(float a) { return (float) java.lang.Math.sin(a); }
    public static float Cos(float a) { return (float) java.lang.Math.cos(a); }
    public static float Atan2(float y, float x) { return (float) java.lang.Math.atan2(y, x); }
    public static float Floor(float a) { return (float) java.lang.Math.floor(a); }
    public static float Ceiling(float a) { return (float) java.lang.Math.ceil(a); }
    public static float Round(float a) { return (float) java.lang.Math.rint(a); }
    public static float Pow(float a, float b) { return (float) java.lang.Math.pow(a, b); }
    public static float Clamp(float v, float a, float b) { return v < a ? a : v > b ? b : v; }
}
