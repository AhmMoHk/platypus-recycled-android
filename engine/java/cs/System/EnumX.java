package cs.System;

import cs.rt.BoxedEnum;
import cs.rt.EnumInfo;

/** System.Enum (enum values are boxed as cs.rt.BoxedEnum). */
public final class EnumX {
    private EnumX() {}

    private static EnumInfo info(Type t) {
        if (t == null || t.enumInfo == null) throw new ArgumentException("Type provided must be an Enum.", "enumType");
        return t.enumInfo;
    }

    private static long value(Object o) {
        if (o instanceof BoxedEnum) return ((BoxedEnum) o).value;
        if (o instanceof Number) return ((Number) o).longValue();
        throw new ArgumentException("not an enum value");
    }

    public static String ToString(Object e) { return e.toString(); }
    public static String ToString(Object e, String fmt) { return cs.rt.Fmt.format(e, fmt); }
    public static boolean HasFlag(Object e, Object flag) { long f = value(flag); return (value(e) & f) == f; }
    public static int CompareTo(Object a, Object b) { return Long.compare(value(a), value(b)); }
    public static boolean Equals(Object a, Object b) { return a.equals(b); }
    public static int GetHashCode(Object a) { return a.hashCode(); }

    public static String[] GetNames(Type t) { return info(t).names.clone(); }
    public static Object GetValues(Type t) {
        EnumInfo ei = info(t);
        Object arr = java.lang.reflect.Array.newInstance(t.arrayElementClass(), ei.values.length);
        for (int i = 0; i < ei.values.length; i++) t.storePrimitive(arr, i, ei.box(ei.values[i]));
        return arr;
    }
    public static String GetName(Type t, Object v) {
        EnumInfo ei = info(t);
        long x = value(v);
        for (int i = 0; i < ei.values.length; i++) if (ei.values[i] == x) return ei.names[i];
        return null;
    }
    public static Type GetUnderlyingType(Type t) { return Type.of(info(t).cls == null ? int.class : t.prim); }
    public static boolean IsDefined(Type t, Object v) {
        if (v instanceof String) return info(t).parse((String) v, false) != null && java.util.Arrays.asList(info(t).names).contains(v);
        return info(t).isDefined(value(v));
    }
    public static Object Parse(Type t, String s) { return Parse(t, s, false); }
    public static Object Parse(Type t, String s, boolean ignoreCase) {
        if (s == null) throw new ArgumentNullException("value");
        Long v = info(t).parse(s, ignoreCase);
        if (v == null) throw new ArgumentException("Requested value '" + s + "' was not found.");
        return info(t).box(v);
    }
    /** Enum.Parse<T>(string) */
    public static Object Parse(Type t, String s, Type unused) { return Parse(t, s, false); }
    public static boolean TryParse(Type t, String s, cs.rt.RefO r) { return TryParse(t, s, false, r); }
    public static boolean TryParse(Type t, String s, boolean ignoreCase, cs.rt.RefO r) {
        Long v = s == null ? null : info(t).parse(s, ignoreCase);
        r.v = info(t).box(v == null ? 0 : v);
        return v != null;
    }
    public static Object ToObject(Type t, int v) { return info(t).box(v); }
    public static Object ToObject(Type t, long v) { return info(t).box(v); }
    public static Object ToObject(Type t, Object v) { return info(t).box(value(v)); }
}
