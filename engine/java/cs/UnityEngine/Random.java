package cs.UnityEngine;

/** UnityEngine.Random: xorshift128 like Unity's implementation. */
public final class Random {
    private Random() {}
    private static int s0, s1, s2, s3;
    static { InitState((int) System.nanoTime()); }

    public static void InitState(int seed) {
        s0 = seed;
        s1 = s0 * 1812433253 + 1;
        s2 = s1 * 1812433253 + 1;
        s3 = s2 * 1812433253 + 1;
    }

    private static int next() {
        int t = s0 ^ (s0 << 11);
        s0 = s1; s1 = s2; s2 = s3;
        s3 = s3 ^ (s3 >>> 19) ^ t ^ (t >>> 8);
        return s3;
    }

    /** [0,1] inclusive */
    public static float get_value() { return (next() & 0x7FFFFF) * (1.0f / 8388607.0f); }

    public static float Range(float min, float max) {
        float t = get_value();
        return t * min + (1f - t) * max;
    }

    public static int Range(int min, int max) {
        if (min == max) return min;
        if (min > max) {
            int d = min - max;
            return min - (int) ((next() & 0xffffffffL) % d);
        }
        long d = (long) max - min;
        return (int) (min + (next() & 0xffffffffL) % d);
    }

    public static Vector2 get_insideUnitCircle() {
        float a = Range(0f, (float) (Math.PI * 2));
        float r = (float) Math.sqrt(get_value());
        return new Vector2((float) Math.cos(a) * r, (float) Math.sin(a) * r);
    }

    public static Vector3 get_insideUnitSphere() {
        Vector3 v;
        do { v = new Vector3(Range(-1f, 1f), Range(-1f, 1f), Range(-1f, 1f)); } while (v.get_sqrMagnitude() > 1f);
        return v;
    }

    public static Vector3 get_onUnitSphere() { return Vector3.Normalize(get_insideUnitSphere()); }

    public static Quaternion get_rotation() { return Quaternion.Euler(Range(0f, 360f), Range(0f, 360f), Range(0f, 360f)); }

    public static Color ColorHSV() { return Color.HSVToRGB(get_value(), get_value(), get_value()); }

}
