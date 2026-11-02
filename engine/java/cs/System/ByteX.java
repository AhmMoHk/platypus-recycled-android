package cs.System;

public final class ByteX {
    private ByteX() {}
    public static String ToString(int v) { return Integer.toString(v & 0xff); }
    public static String ToString(int v, String f) { return cs.rt.Fmt.formatLong(v & 0xff, f, true); }
    public static byte Parse(String s) {
        int v = cs.rt.Fmt.parseInt(s);
        if (v < 0 || v > 255) throw new OverflowException("Value was either too large or too small for an unsigned byte.");
        return (byte) v;
    }
    public static boolean TryParse(String s, cs.rt.RefB r) {
        try { r.v = Parse(s); return true; } catch (RuntimeException e) { r.v = 0; return false; }
    }
    public static boolean Equals(int a, int b) { return (a & 0xff) == (b & 0xff); }
    public static int GetHashCode(int a) { return a & 0xff; }
}
