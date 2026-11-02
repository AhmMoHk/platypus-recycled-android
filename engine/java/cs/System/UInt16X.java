package cs.System;

public final class UInt16X {
    private UInt16X() {}
    public static String ToString(char v) { return Integer.toString(v); }
    public static char Parse(String s) { return (char) cs.rt.Fmt.parseInt(s); }
}
