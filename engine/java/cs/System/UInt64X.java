package cs.System;

public final class UInt64X {
    private UInt64X() {}
    public static String ToString(long v) { return v >= 0 ? Long.toString(v) : new java.math.BigInteger(Long.toString(v >>> 1)).shiftLeft(1).add(java.math.BigInteger.valueOf(v & 1)).toString(); }
    public static boolean TryParse(String s, int styles, IFormatProvider p, cs.rt.RefJ r) {
        try {
            s = s.trim();
            r.v = new java.math.BigInteger(s, (styles & 0x200) != 0 ? 16 : 10).longValue();
            return true;
        } catch (RuntimeException e) { r.v = 0; return false; }
    }
    public static boolean TryParse(String s, cs.rt.RefJ r) { return TryParse(s, 0, null, r); }
    public static boolean Equals(long a, long b) { return a == b; }
    public static int GetHashCode(long a) { return (int) a ^ (int) (a >> 32); }
}
