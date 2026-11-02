package cs.System;

public final class IntPtrX {
    private IntPtrX() {}
    public static final long Zero = 0;
    public static String ToString(long v) { return Long.toString(v); }
    public static String ToString(long v, String f) { return cs.rt.Fmt.formatLong(v, f, false); }
    public static boolean op_Equality(long a, long b) { return a == b; }
    public static boolean op_Inequality(long a, long b) { return a != b; }
}
