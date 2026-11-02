package cs.System;

import cs.rt.BoxedEnum;
import cs.rt.Fmt;

/** Methods of System.Object (and ValueType) for any Java object, with .NET semantics for boxed values. */
public final class ObjectX {
    private ObjectX() {}

    public static String ToString(Object o) {
        if (o == null) throw new NullReferenceException("Object reference not set to an instance of an object.");
        if (o instanceof String) return (String) o;
        if (o instanceof Float) return Fmt.str((Float) o);
        if (o instanceof Double) return Fmt.str((Double) o);
        if (o instanceof Boolean) return ((Boolean) o) ? "True" : "False";
        if (o instanceof Character) return String.valueOf((char) (Character) o);
        if (o instanceof Object[] || o != null && o.getClass().isArray()) return Type.ofObject(o).get_FullName();
        return o.toString();
    }

    /** ToString for string interpolation / concatenation: null -> "" */
    public static String str(Object o) { return o == null ? "" : ToString(o); }

    public static String baseToString(Object o) { return Type.ofObject(o).get_FullName(); }

    public static boolean Equals(Object a, Object b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a instanceof Float && b instanceof Float) {
            float x = (Float) a, y = (Float) b;
            return x == y || (x != x && y != y);
        }
        return a.equals(b);
    }

    public static boolean baseEquals(Object a, Object b) {
        if (a instanceof cs.rt.Struct) return a.equals(b);
        return a == b;
    }

    public static boolean ReferenceEquals(Object a, Object b) { return a == b; }

    public static int GetHashCode(Object o) {
        if (o == null) throw new NullReferenceException("Object reference not set to an instance of an object.");
        return hash(o);
    }

    public static int baseGetHashCode(Object o) { return System.identityHashCode(o); }

    public static Type GetType(Object o) {
        if (o == null) throw new NullReferenceException("Object reference not set to an instance of an object.");
        return Type.ofObject(o);
    }

    public static Object MemberwiseClone(Object o) {
        if (o instanceof cs.rt.Struct) return ((cs.rt.Struct) o).$copy();
        try {
            Object c = o.getClass().getDeclaredConstructor().newInstance();
            for (Class<?> k = o.getClass(); k != null && k != Object.class; k = k.getSuperclass()) {
                for (java.lang.reflect.Field f : k.getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                    f.setAccessible(true);
                    f.set(c, f.get(o));
                }
            }
            return c;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void Finalize(Object o) {}

    // ------------------------------------------------------------------ hashing helpers for generated structs
    public static int hash(Object o) {
        if (o == null) return 0;
        if (o instanceof String) return stringHash((String) o);
        return o.hashCode();
    }
    public static int hash(int v) { return v; }
    public static int hash(long v) { return (int) v ^ (int) (v >> 32); }
    public static int hash(float v) { return v == 0 ? 0 : Float.floatToIntBits(v); }
    public static int hash(double v) { long b = v == 0 ? 0 : Double.doubleToLongBits(v); return (int) b ^ (int) (b >> 32); }
    public static int hash(boolean v) { return v ? 1 : 0; }
    public static int hash(char v) { return v | (v << 16); }
    public static int hash(byte v) { return v; }
    public static int hash(short v) { return v; }

    public static int stringHash(String s) { return s.hashCode(); }

    public static boolean Equals(int a, int b) { return a == b; }
    public static boolean Equals(float a, float b) { return a == b || (a != a && b != b); }
    public static boolean Equals(boolean a, boolean b) { return a == b; }
    public static boolean Equals(long a, long b) { return a == b; }
    public static boolean Equals(double a, double b) { return a == b || (a != a && b != b); }
    public static boolean Equals(char a, char b) { return a == b; }
    public static boolean Equals(byte a, byte b) { return a == b; }
    public static boolean Equals(short a, short b) { return a == b; }

    /** Comparer<T>.Default */
    @SuppressWarnings("unchecked")
    public static int compare(Object a, Object b) {
        if (a == b) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        if (a instanceof String && b instanceof String) return StringX.Compare((String) a, (String) b);
        if (a instanceof IComparable_1) return ((IComparable_1) a).CompareTo(b);
        if (a instanceof IComparable) return ((IComparable) a).CompareTo(b);
        if (a instanceof Integer && b instanceof Integer) return Integer.compare((Integer) a, (Integer) b);
        if (a instanceof Float && b instanceof Float) return Float.compare((Float) a, (Float) b);
        if (a instanceof Number && b instanceof Number) return Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue());
        if (a instanceof Comparable) return ((Comparable<Object>) a).compareTo(b);
        throw new ArgumentException("At least one object must implement IComparable.");
    }
}
