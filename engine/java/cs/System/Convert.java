package cs.System;

public final class Convert {
    private Convert() {}
    public static byte[] FromBase64String(String s) { return cs.rt.Rt.b64(s); }
    public static String ToBase64String(byte[] d) { return cs.rt.Rt.b64encode(d); }
    public static boolean IsDBNull(Object o) { return false; }
    public static byte ToByte(String s, int base) { return (byte) Integer.parseInt(s.trim(), base); }
    public static char ToChar(Object o) {
        if (o instanceof Character) return (Character) o;
        if (o instanceof String && ((String) o).length() == 1) return ((String) o).charAt(0);
        return (char) cs.rt.Rt.unboxI(o);
    }
    public static char ToChar(byte b) { return (char) (b & 0xff); }
    public static char ToChar(int b) { return (char) b; }
    public static int ToInt32(String s) { return s == null ? 0 : cs.rt.Fmt.parseInt(s); }
    public static int ToInt32(String s, int base) { return (int) Long.parseLong(s.trim(), base); }
    public static int ToInt32(Object o) {
        if (o == null) return 0;
        if (o instanceof String) return ToInt32((String) o);
        if (o instanceof Float || o instanceof Double) return (int) java.lang.Math.rint(((Number) o).doubleValue());
        if (o instanceof Boolean) return ((Boolean) o) ? 1 : 0;
        return cs.rt.Rt.unboxI(o);
    }
    public static int ToInt32(float f) { return (int) java.lang.Math.rint(f); }
    public static int ToInt32(double f) { return (int) java.lang.Math.rint(f); }
    public static int ToInt32(boolean b) { return b ? 1 : 0; }
    public static long ToInt64(String s, IFormatProvider p) { return cs.rt.Fmt.parseLong(s); }
    public static long ToInt64(Object o) { return o instanceof String ? cs.rt.Fmt.parseLong((String) o) : cs.rt.Rt.unboxJ(o); }
    public static int ToUInt32(String s, int base) { return (int) Long.parseLong(s.trim(), base); }
    public static double ToDouble(String s, IFormatProvider p) { return cs.rt.Fmt.parseDouble(s); }
    public static double ToDouble(Object o) { return o instanceof String ? cs.rt.Fmt.parseDouble((String) o) : cs.rt.Rt.unboxD(o); }
    public static float ToSingle(Object o) { return o instanceof String ? (float) cs.rt.Fmt.parseDouble((String) o) : cs.rt.Rt.unboxF(o); }
    public static boolean ToBoolean(Object o) {
        if (o instanceof String) return BooleanX.Parse((String) o);
        if (o instanceof Number) return ((Number) o).doubleValue() != 0;
        return cs.rt.Rt.unboxZ(o);
    }
    public static String ToString(long v, int base) {
        if (base == 16) return Long.toHexString(v);
        if (base == 2) return Long.toBinaryString(v);
        if (base == 8) return Long.toOctalString(v);
        return Long.toString(v);
    }
    public static String ToString(int v, int base) { return base == 10 ? Integer.toString(v) : ToString(v & 0xffffffffL, base); }
    public static String ToString(Object o) { return o == null ? "" : ObjectX.ToString(o); }
    public static Object ChangeType(Object o, Type t) {
        if (t == Type.INT32) return ToInt32(o);
        if (t == Type.SINGLE) return ToSingle(o);
        if (t == Type.DOUBLE) return ToDouble(o);
        if (t == Type.BOOLEAN) return ToBoolean(o);
        if (t == Type.STRING) return ToString(o);
        if (t == Type.INT64) return ToInt64(o);
        return o;
    }
}
