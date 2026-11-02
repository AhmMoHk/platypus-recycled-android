package cs.System;

public final class BooleanX {
    private BooleanX() {}
    public static final String TrueString = "True", FalseString = "False";
    public static String ToString(boolean v) { return v ? "True" : "False"; }
    public static String ToString(boolean v, IFormatProvider p) { return ToString(v); }
    public static boolean Parse(String s) {
        String t = s.trim();
        if (t.equalsIgnoreCase("true")) return true;
        if (t.equalsIgnoreCase("false")) return false;
        throw new FormatException("String '" + s + "' was not recognized as a valid Boolean.");
    }
    public static boolean TryParse(String s, cs.rt.RefZ r) {
        if (s == null) { r.v = false; return false; }
        String t = s.trim();
        if (t.equalsIgnoreCase("true")) { r.v = true; return true; }
        r.v = false;
        return t.equalsIgnoreCase("false");
    }
    public static boolean Equals(boolean a, boolean b) { return a == b; }
    public static boolean Equals(boolean a, Object b) { return b instanceof Boolean && (Boolean) b == a; }
    public static int GetHashCode(boolean a) { return a ? 1 : 0; }
    public static int CompareTo(boolean a, boolean b) { return Boolean.compare(a, b); }
}
