package cs.rt;

import java.lang.reflect.Array;

/** Helpers called by translated code. */
public final class Rt {
    private Rt() {}

    public static RuntimeException todo(String what) {
        return new UnsupportedOperationException("not implemented: " + what);
    }

    // ------------------------------------------------------------------ unboxing (C# unbox / unbox.any)
    private static RuntimeException badUnbox(Object o, String to) {
        if (o == null) return new cs.System.NullReferenceException("unbox null to " + to);
        return new cs.System.InvalidCastException("cannot unbox " + o.getClass().getName() + " to " + to);
    }

    public static int unboxI(Object o) {
        if (o == null) return 0;
        if (o instanceof Integer) return (Integer) o;
        if (o instanceof BoxedEnum) return (int) ((BoxedEnum) o).value;
        if (o instanceof Number) return ((Number) o).intValue();
        if (o instanceof Character) return (Character) o;
        if (o instanceof Boolean) return ((Boolean) o) ? 1 : 0;
        throw badUnbox(o, "int");
    }

    public static long unboxJ(Object o) {
        if (o == null) return 0L;
        if (o instanceof Long) return (Long) o;
        if (o instanceof BoxedEnum) return ((BoxedEnum) o).value;
        if (o instanceof Number) return ((Number) o).longValue();
        if (o instanceof Character) return (Character) o;
        throw badUnbox(o, "long");
    }

    public static float unboxF(Object o) {
        if (o == null) return 0f;
        if (o instanceof Float) return (Float) o;
        if (o instanceof Number) return ((Number) o).floatValue();
        throw badUnbox(o, "float");
    }

    public static double unboxD(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Double) return (Double) o;
        if (o instanceof Number) return ((Number) o).doubleValue();
        throw badUnbox(o, "double");
    }

    public static boolean unboxZ(Object o) {
        if (o == null) return false;
        if (o instanceof Boolean) return (Boolean) o;
        if (o instanceof Number) return ((Number) o).intValue() != 0;
        throw badUnbox(o, "bool");
    }

    public static char unboxC(Object o) {
        if (o == null) return '\0';
        if (o instanceof Character) return (Character) o;
        if (o instanceof Number) return (char) ((Number) o).intValue();
        throw badUnbox(o, "char");
    }

    public static byte unboxB(Object o) {
        if (o == null) return (byte) 0;
        if (o instanceof Byte) return (Byte) o;
        if (o instanceof BoxedEnum) return (byte) ((BoxedEnum) o).value;
        if (o instanceof Number) return ((Number) o).byteValue();
        throw badUnbox(o, "byte");
    }

    public static short unboxS(Object o) {
        if (o == null) return (short) 0;
        if (o instanceof Short) return (Short) o;
        if (o instanceof BoxedEnum) return (short) ((BoxedEnum) o).value;
        if (o instanceof Number) return ((Number) o).shortValue();
        throw badUnbox(o, "short");
    }

    /** isinst for an enum type: the object if it is a boxed value of that enum, else null. */
    public static Object isEnum(Object o, EnumInfo info) {
        if (o instanceof BoxedEnum && ((BoxedEnum) o).info == info) return o;
        return null;
    }

    public static Object defaultOf(cs.System.Type t) {
        return t == null ? null : t.defaultValue();
    }

    // unsigned arithmetic (Integer.*Unsigned is API 26)
    public static int cmpu(int a, int b) { return Integer.compare(a + Integer.MIN_VALUE, b + Integer.MIN_VALUE); }
    public static int cmpuL(long a, long b) { return Long.compare(a + Long.MIN_VALUE, b + Long.MIN_VALUE); }
    public static int divu(int a, int b) { return (int) ((a & 0xffffffffL) / (b & 0xffffffffL)); }
    public static int remu(int a, int b) { return (int) ((a & 0xffffffffL) % (b & 0xffffffffL)); }
    public static long divuL(long a, long b) {
        if (b < 0) return cmpuL(a, b) < 0 ? 0 : 1;
        if (a >= 0) return a / b;
        long q = ((a >>> 1) / b) << 1;
        long r = a - q * b;
        return q + (cmpuL(r, b) >= 0 ? 1 : 0);
    }
    public static long remuL(long a, long b) { return a - divuL(a, b) * b; }

    public static double ulongToDouble(long v) {
        if (v >= 0) return (double) v;
        return ((double) (v >>> 1)) * 2.0 + (v & 1);
    }

    // ------------------------------------------------------------------ exceptions
    public static RuntimeException toThrowable(Object o) {
        if (o instanceof RuntimeException) return (RuntimeException) o;
        if (o == null) return new cs.System.NullReferenceException("throw null");
        if (o instanceof Throwable) return cs.System.Exception.wrap((Throwable) o);
        return new cs.System.Exception("thrown object: " + o);
    }

    // ------------------------------------------------------------------ arrays
    private static final int[] B64 = new int[128];
    static {
        String a = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        java.util.Arrays.fill(B64, -1);
        for (int i = 0; i < 64; i++) B64[a.charAt(i)] = i;
    }

    /** base64 decode (no java.util.Base64 on older Android) */
    public static byte[] b64(String s) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(s.length() * 3 / 4);
        int buf = 0, bits = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '=') break;
            if (c >= 128 || B64[c] < 0) continue;
            buf = (buf << 6) | B64[c];
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                out.write((buf >> bits) & 255);
            }
        }
        return out.toByteArray();
    }

    private static final char[] B64C = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

    public static String b64encode(byte[] d) {
        StringBuilder sb = new StringBuilder((d.length + 2) / 3 * 4);
        for (int i = 0; i < d.length; i += 3) {
            int b0 = d[i] & 255, b1 = i + 1 < d.length ? d[i + 1] & 255 : 0, b2 = i + 2 < d.length ? d[i + 2] & 255 : 0;
            int v = (b0 << 16) | (b1 << 8) | b2;
            sb.append(B64C[(v >> 18) & 63]).append(B64C[(v >> 12) & 63]);
            sb.append(i + 1 < d.length ? B64C[(v >> 6) & 63] : '=');
            sb.append(i + 2 < d.length ? B64C[v & 63] : '=');
        }
        return sb.toString();
    }

    public static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }

    public static boolean allDigits(String s) {
        for (int i = 0; i < s.length(); i++) if (!Character.isDigit(s.charAt(i))) return false;
        return true;
    }

    /** RuntimeHelpers.InitializeArray: little-endian element data. */
    public static void initArray(Object arr, byte[] d) {
        if (arr instanceof MDArray) {
            MDArray m = (MDArray) arr;
            Object z = m.data.length > 0 ? m.data[0] : null;
            java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(d).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < m.data.length; i++) {
                if (z instanceof Integer) m.data[i] = bb.getInt(i * 4);
                else if (z instanceof Float) m.data[i] = bb.getFloat(i * 4);
                else if (z instanceof Long) m.data[i] = bb.getLong(i * 8);
                else if (z instanceof Double) m.data[i] = bb.getDouble(i * 8);
                else if (z instanceof Short) m.data[i] = bb.getShort(i * 2);
                else if (z instanceof Character) m.data[i] = bb.getChar(i * 2);
                else if (z instanceof Byte) m.data[i] = d[i];
                else if (z instanceof Boolean) m.data[i] = d[i] != 0;
                else throw todo("InitializeArray MDArray of " + z);
            }
            return;
        }
        if (arr instanceof byte[]) {
            byte[] a = (byte[]) arr;
            System.arraycopy(d, 0, a, 0, Math.min(a.length, d.length));
        } else if (arr instanceof int[]) {
            int[] a = (int[]) arr;
            for (int i = 0; i < a.length && i * 4 + 3 < d.length; i++)
                a[i] = (d[i * 4] & 255) | (d[i * 4 + 1] & 255) << 8 | (d[i * 4 + 2] & 255) << 16 | (d[i * 4 + 3] & 255) << 24;
        } else if (arr instanceof float[]) {
            float[] a = (float[]) arr;
            for (int i = 0; i < a.length && i * 4 + 3 < d.length; i++)
                a[i] = Float.intBitsToFloat((d[i * 4] & 255) | (d[i * 4 + 1] & 255) << 8 | (d[i * 4 + 2] & 255) << 16 | (d[i * 4 + 3] & 255) << 24);
        } else if (arr instanceof short[]) {
            short[] a = (short[]) arr;
            for (int i = 0; i < a.length && i * 2 + 1 < d.length; i++) a[i] = (short) ((d[i * 2] & 255) | (d[i * 2 + 1] & 255) << 8);
        } else if (arr instanceof char[]) {
            char[] a = (char[]) arr;
            for (int i = 0; i < a.length && i * 2 + 1 < d.length; i++) a[i] = (char) ((d[i * 2] & 255) | (d[i * 2 + 1] & 255) << 8);
        } else if (arr instanceof long[]) {
            long[] a = (long[]) arr;
            for (int i = 0; i < a.length && i * 8 + 7 < d.length; i++) {
                long v = 0;
                for (int k = 7; k >= 0; k--) v = (v << 8) | (d[i * 8 + k] & 255);
                a[i] = v;
            }
        } else if (arr instanceof double[]) {
            double[] a = (double[]) arr;
            for (int i = 0; i < a.length && i * 8 + 7 < d.length; i++) {
                long v = 0;
                for (int k = 7; k >= 0; k--) v = (v << 8) | (d[i * 8 + k] & 255);
                a[i] = Double.longBitsToDouble(v);
            }
        } else if (arr instanceof boolean[]) {
            boolean[] a = (boolean[]) arr;
            for (int i = 0; i < a.length && i < d.length; i++) a[i] = d[i] != 0;
        } else {
            throw todo("InitializeArray " + arr.getClass());
        }
    }

    /** An array produced by erased generic code (Object[] or any array) as an array of the given element type. */
    public static Object castArray(Object arr, cs.System.Type elem) {
        if (arr == null) return null;
        Class<?> ec = elem.arrayElementClass();
        Class<?> ac = arr.getClass();
        if (ac.getComponentType() == ec) return arr;
        int n = Array.getLength(arr);
        Object out = Array.newInstance(ec, n);
        for (int i = 0; i < n; i++) {
            Object v = Array.get(arr, i);
            if (ec.isPrimitive()) elem.storePrimitive(out, i, v);
            else Array.set(out, i, v);
        }
        return out;
    }

    /** load a class without running its static initializer (.NET reflection does not run type initializers) */
    public static Class<?> cls(String name) throws ClassNotFoundException {
        return Class.forName(name, false, Rt.class.getClassLoader());
    }

    /** A value used as IEnumerable / IList: arrays become a live list view, anything else passes through. */
    public static Object iface(Object o) {
        if (o == null || !o.getClass().isArray()) return o;
        if (o instanceof Object[]) return cs.System.Collections.Generic.List_1.$view((Object[]) o);
        return asList(o);
    }

    /** An array passed where an IEnumerable / IList is expected. */
    public static cs.System.Collections.Generic.List_1 asList(Object arr) {
        return cs.System.ArrayX.asList(arr);
    }

    /** stelem on an array typed as a generic parameter (any java array). */
    public static void arraySet(Object arr, int i, Object v) {
        Class<?> ct = arr.getClass().getComponentType();
        if (ct.isPrimitive()) {
            if (ct == int.class) Array.setInt(arr, i, unboxI(v));
            else if (ct == float.class) Array.setFloat(arr, i, unboxF(v));
            else if (ct == boolean.class) Array.setBoolean(arr, i, unboxZ(v));
            else if (ct == long.class) Array.setLong(arr, i, unboxJ(v));
            else if (ct == double.class) Array.setDouble(arr, i, unboxD(v));
            else if (ct == char.class) Array.setChar(arr, i, unboxC(v));
            else if (ct == byte.class) Array.setByte(arr, i, unboxB(v));
            else if (ct == short.class) Array.setShort(arr, i, unboxS(v));
        } else if (Struct.class.isAssignableFrom(ct) && v != null) {
            ((Struct) Array.get(arr, i)).$set(v);
        } else {
            Array.set(arr, i, v);
        }
    }

    public static Object copyStruct(Object o) {
        return o instanceof Struct ? ((Struct) o).$copy() : o;
    }

    public static Object[] boxArray(Object arr) {
        if (arr instanceof Object[]) return (Object[]) arr;
        int n = Array.getLength(arr);
        Object[] out = new Object[n];
        for (int i = 0; i < n; i++) out[i] = Array.get(arr, i);
        return out;
    }
}
