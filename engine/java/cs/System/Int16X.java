package cs.System;

public final class Int16X {
    private Int16X() {}
    public static String ToString(short v) { return Short.toString(v); }
    public static short Parse(String s) { return (short) cs.rt.Fmt.parseInt(s); }
}
