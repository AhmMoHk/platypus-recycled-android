package cs.UnityEngine;

/** UnityEngine.Mathf with Unity's exact formulas. */
public final class Mathf {
    private Mathf() {}
    public static final float PI = (float) Math.PI;
    public static final float Infinity = Float.POSITIVE_INFINITY;
    public static final float NegativeInfinity = Float.NEGATIVE_INFINITY;
    public static final float Deg2Rad = PI * 2f / 360f;
    public static final float Rad2Deg = 1f / Deg2Rad;
    public static float Epsilon = Float.MIN_VALUE;

    public static float Sin(float f) { return (float) Math.sin(f); }
    public static float Cos(float f) { return (float) Math.cos(f); }
    public static float Tan(float f) { return (float) Math.tan(f); }
    public static float Asin(float f) { return (float) Math.asin(f); }
    public static float Acos(float f) { return (float) Math.acos(f); }
    public static float Atan(float f) { return (float) Math.atan(f); }
    public static float Atan2(float y, float x) { return (float) Math.atan2(y, x); }
    public static float Sqrt(float f) { return (float) Math.sqrt(f); }
    public static float Abs(float f) { return Math.abs(f); }
    public static int Abs(int v) { return Math.abs(v); }
    public static float Min(float a, float b) { return a < b ? a : b; }
    public static int Min(int a, int b) { return a < b ? a : b; }
    public static float Min(float[] v) {
        if (v.length == 0) return 0;
        float m = v[0];
        for (int i = 1; i < v.length; i++) if (v[i] < m) m = v[i];
        return m;
    }
    public static float Max(float a, float b) { return a > b ? a : b; }
    public static int Max(int a, int b) { return a > b ? a : b; }
    public static float Max(float[] v) {
        if (v.length == 0) return 0;
        float m = v[0];
        for (int i = 1; i < v.length; i++) if (v[i] > m) m = v[i];
        return m;
    }
    public static int Max(int[] v) {
        if (v.length == 0) return 0;
        int m = v[0];
        for (int i = 1; i < v.length; i++) if (v[i] > m) m = v[i];
        return m;
    }
    public static float Pow(float f, float p) { return (float) Math.pow(f, p); }
    public static float Exp(float p) { return (float) Math.exp(p); }
    public static float Log(float f, float p) { return (float) (Math.log(f) / Math.log(p)); }
    public static float Log(float f) { return (float) Math.log(f); }
    public static float Log10(float f) { return (float) Math.log10(f); }
    public static float Ceil(float f) { return (float) Math.ceil(f); }
    public static float Floor(float f) { return (float) Math.floor(f); }
    /** banker's rounding like System.Math.Round */
    public static float Round(float f) { return (float) Math.rint(f); }
    public static int CeilToInt(float f) { return (int) Math.ceil(f); }
    public static int FloorToInt(float f) { return (int) Math.floor(f); }
    public static int RoundToInt(float f) { return (int) Math.rint(f); }
    public static float Sign(float f) { return f >= 0f ? 1f : -1f; }
    public static float Clamp(float v, float min, float max) { if (v < min) v = min; else if (v > max) v = max; return v; }
    public static int Clamp(int v, int min, int max) { if (v < min) v = min; else if (v > max) v = max; return v; }
    public static float Clamp01(float v) { if (v < 0f) return 0f; if (v > 1f) return 1f; return v; }
    public static float Lerp(float a, float b, float t) { return a + (b - a) * Clamp01(t); }
    public static float LerpUnclamped(float a, float b, float t) { return a + (b - a) * t; }
    public static float LerpAngle(float a, float b, float t) {
        float delta = Repeat(b - a, 360);
        if (delta > 180) delta -= 360;
        return a + delta * Clamp01(t);
    }
    public static float MoveTowards(float current, float target, float maxDelta) {
        if (Abs(target - current) <= maxDelta) return target;
        return current + Sign(target - current) * maxDelta;
    }
    public static float MoveTowardsAngle(float current, float target, float maxDelta) {
        float deltaAngle = DeltaAngle(current, target);
        if (-maxDelta < deltaAngle && deltaAngle < maxDelta) return target;
        target = current + deltaAngle;
        return MoveTowards(current, target, maxDelta);
    }
    public static float SmoothStep(float from, float to, float t) {
        t = Clamp01(t);
        t = -2.0F * t * t * t + 3.0F * t * t;
        return to * t + from * (1F - t);
    }
    public static float Gamma(float value, float absmax, float gamma) {
        boolean negative = value < 0F;
        float absval = Abs(value);
        if (absval > absmax) return negative ? -absval : absval;
        float result = Pow(absval / absmax, gamma) * absmax;
        return negative ? -result : result;
    }
    public static boolean Approximately(float a, float b) {
        return Abs(b - a) < Max(0.000001f * Max(Abs(a), Abs(b)), Epsilon * 8);
    }
    public static float SmoothDamp(float current, float target, cs.rt.RefF currentVelocity, float smoothTime, float maxSpeed, float deltaTime) {
        smoothTime = Max(0.0001F, smoothTime);
        float omega = 2F / smoothTime;
        float x = omega * deltaTime;
        float exp = 1F / (1F + x + 0.48F * x * x + 0.235F * x * x * x);
        float change = current - target;
        float originalTo = target;
        float maxChange = maxSpeed * smoothTime;
        change = Clamp(change, -maxChange, maxChange);
        target = current - change;
        float temp = (currentVelocity.v + omega * change) * deltaTime;
        currentVelocity.v = (currentVelocity.v - omega * temp) * exp;
        float output = target + (change + temp) * exp;
        if (originalTo - current > 0.0F == output > originalTo) {
            output = originalTo;
            currentVelocity.v = (output - originalTo) / deltaTime;
        }
        return output;
    }
    public static float SmoothDamp(float current, float target, cs.rt.RefF v, float smoothTime) {
        return SmoothDamp(current, target, v, smoothTime, Infinity, Time.get_deltaTime());
    }
    public static float SmoothDamp(float current, float target, cs.rt.RefF v, float smoothTime, float maxSpeed) {
        return SmoothDamp(current, target, v, smoothTime, maxSpeed, Time.get_deltaTime());
    }
    public static float Repeat(float t, float length) { return Clamp(t - Floor(t / length) * length, 0.0f, length); }
    public static float PingPong(float t, float length) {
        t = Repeat(t, length * 2F);
        return length - Abs(t - length);
    }
    public static float InverseLerp(float a, float b, float value) {
        if (a != b) return Clamp01((value - a) / (b - a));
        return 0.0f;
    }
    public static float DeltaAngle(float current, float target) {
        float delta = Repeat(target - current, 360.0F);
        if (delta > 180.0F) delta -= 360.0F;
        return delta;
    }
    public static int NextPowerOfTwo(int v) {
        v -= 1;
        v |= v >> 16; v |= v >> 8; v |= v >> 4; v |= v >> 2; v |= v >> 1;
        return v + 1;
    }
    public static int ClosestPowerOfTwo(int v) {
        int n = NextPowerOfTwo(v), p = n >> 1;
        return (n - v) > (v - p) ? p : n;
    }
    public static boolean IsPowerOfTwo(int v) { return (v & (v - 1)) == 0; }
    public static float PerlinNoise(float x, float y) { return Perlin.noise(x, y); }
}
