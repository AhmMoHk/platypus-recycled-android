package cs.System;

import java.lang.reflect.Array;

/** System.Array methods on Java arrays. */
public final class ArrayX {
    private ArrayX() {}

    public static int get_Length(Object a) { return Array.getLength(a); }
    public static long get_LongLength(Object a) { return Array.getLength(a); }
    public static int get_Rank(Object a) { return a instanceof cs.rt.MDArray ? ((cs.rt.MDArray) a).dims.length : 1; }
    public static int GetLength(Object a, int d) { return a instanceof cs.rt.MDArray ? ((cs.rt.MDArray) a).dims[d] : Array.getLength(a); }
    public static int GetUpperBound(Object a, int d) { return GetLength(a, d) - 1; }
    public static int GetLowerBound(Object a, int d) { return 0; }

    public static Object GetValue(Object a, int i) {
        Object v = Array.get(a, i);
        return v instanceof cs.rt.Struct ? ((cs.rt.Struct) v).$copy() : v;
    }
    public static Object GetValue(Object a, int[] idx) {
        if (a instanceof cs.rt.MDArray) { cs.rt.MDArray m = (cs.rt.MDArray) a; return m.data[m.index(idx)]; }
        return GetValue(a, idx[0]);
    }
    public static void SetValue(Object a, Object v, int i) { cs.rt.Rt.arraySet(a, i, v); }
    public static void SetValue(Object a, Object v, int[] idx) {
        if (a instanceof cs.rt.MDArray) { cs.rt.MDArray m = (cs.rt.MDArray) a; m.data[m.index(idx)] = v; return; }
        SetValue(a, v, idx[0]);
    }

    public static Object Clone(Object a) {
        int n = Array.getLength(a);
        Object c = Array.newInstance(a.getClass().getComponentType(), n);
        System.arraycopy(a, 0, c, 0, n);
        return c;
    }

    public static void Clear(Object a, int start, int len) {
        Class<?> ct = a.getClass().getComponentType();
        for (int i = start; i < start + len; i++) {
            if (ct.isPrimitive()) {
                if (ct == boolean.class) Array.setBoolean(a, i, false);
                else if (ct == char.class) Array.setChar(a, i, (char) 0);
                else Array.set(a, i, zero(ct));
            } else if (cs.rt.Struct.class.isAssignableFrom(ct)) {
                ((cs.rt.Struct) Array.get(a, i)).$clear();
            } else Array.set(a, i, null);
        }
    }
    public static void Clear(Object a) { Clear(a, 0, Array.getLength(a)); }

    private static Object zero(Class<?> c) {
        if (c == int.class) return 0;
        if (c == float.class) return 0f;
        if (c == long.class) return 0L;
        if (c == double.class) return 0d;
        if (c == byte.class) return (byte) 0;
        if (c == short.class) return (short) 0;
        return null;
    }

    public static void Copy(Object src, Object dst, int len) { Copy(src, 0, dst, 0, len); }
    public static void Copy(Object src, int si, Object dst, int di, int len) {
        Class<?> ct = dst.getClass().getComponentType();
        if (cs.rt.Struct.class.isAssignableFrom(ct)) {
            if (src == dst && si < di) for (int i = len - 1; i >= 0; i--) ((cs.rt.Struct) Array.get(dst, di + i)).$set(Array.get(src, si + i));
            else for (int i = 0; i < len; i++) ((cs.rt.Struct) Array.get(dst, di + i)).$set(Array.get(src, si + i));
            return;
        }
        if (src.getClass().getComponentType() == ct || !ct.isPrimitive() && !src.getClass().getComponentType().isPrimitive()) {
            System.arraycopy(src, si, dst, di, len);
            return;
        }
        for (int i = 0; i < len; i++) cs.rt.Rt.arraySet(dst, di + i, Array.get(src, si + i));
    }
    public static void ConstrainedCopy(Object src, int si, Object dst, int di, int len) { Copy(src, si, dst, di, len); }
    public static void CopyTo(Object src, Object dst, int di) { Copy(src, 0, dst, di, Array.getLength(src)); }

    public static Object CreateInstance(Type t, int n) {
        if (t.isStruct()) {
            Object a = Array.newInstance(t.cls, n);
            for (int i = 0; i < n; i++) Array.set(a, i, t.createInstance());
            return a;
        }
        return Array.newInstance(t.arrayElementClass(), n);
    }

    private static final java.util.HashMap<Type, Object> empties = new java.util.HashMap<>();
    public static synchronized Object Empty(Type t) {
        Object e = empties.get(t);
        if (e == null) empties.put(t, e = Array.newInstance(t.arrayElementClass(), 0));
        return e;
    }

    public static int IndexOf(Type t, Object a, Object v) { return IndexOf(a, v); }
    public static int IndexOf(Object a, Object v) {
        int n = Array.getLength(a);
        for (int i = 0; i < n; i++) if (ObjectX.Equals(box(a, i), v)) return i;
        return -1;
    }
    public static int IndexOf(Type t, Object a, Object v, int start) {
        int n = Array.getLength(a);
        for (int i = start; i < n; i++) if (ObjectX.Equals(box(a, i), v)) return i;
        return -1;
    }
    public static int LastIndexOf(Type t, Object a, Object v) {
        for (int i = Array.getLength(a) - 1; i >= 0; i--) if (ObjectX.Equals(box(a, i), v)) return i;
        return -1;
    }
    public static boolean Exists(Type t, Object a, cs.System.Predicate_1 p) { return FindIndex(t, a, p) >= 0; }
    public static int FindIndex(Type t, Object a, cs.System.Predicate_1 p) {
        int n = Array.getLength(a);
        for (int i = 0; i < n; i++) if (p.Invoke(box(a, i))) return i;
        return -1;
    }
    public static Object Find(Type t, Object a, cs.System.Predicate_1 p) {
        int i = FindIndex(t, a, p);
        return i < 0 ? t.defaultValue() : box(a, i);
    }

    /** element boxed with the enum type when the array is of an enum (int[] keeps ints). */
    private static Object box(Object a, int i) { return Array.get(a, i); }

    public static void Reverse(Type t, Object a) { Reverse(a); }
    public static void Reverse(Object a) { Reverse(a, 0, Array.getLength(a)); }
    public static void Reverse(Object a, int start, int len) {
        for (int i = start, j = start + len - 1; i < j; i++, j--) {
            Object x = Array.get(a, i);
            Array.set(a, i, Array.get(a, j));
            Array.set(a, j, x);
        }
    }

    public static void Resize(Type t, cs.rt.RefO ref, int n) {
        Object old = ref.v;
        Object na = CreateInstance(t, n);
        if (old != null) Copy(old, 0, na, 0, java.lang.Math.min(n, Array.getLength(old)));
        ref.v = na;
    }

    public static void Sort(Type t, Object a) { Sort(a); }
    public static void Sort(Object a) { Sort(a, 0, Array.getLength(a), null); }
    public static void Sort(Type t, Object a, final cs.System.Comparison_1 c) {
        cs.rt.IntroSort.sort(a, 0, Array.getLength(a), new java.util.Comparator<Object>() {
            public int compare(Object x, Object y) { return c.Invoke(x, y); }
        });
    }
    public static void Sort(Type t, Object a, cs.System.Collections.Generic.IComparer_1 c) { Sort(a, 0, Array.getLength(a), c); }
    public static void Sort(Type t, Object a, int start, int len, cs.System.Collections.Generic.IComparer_1 c) { Sort(a, start, len, c); }
    public static void Sort(Object a, int start, int len, cs.System.Collections.Generic.IComparer_1 c) {
        cs.rt.IntroSort.sort(a, start, len, cs.rt.IntroSort.comparer(c));
    }

    public static cs.System.Collections.IEnumerator GetEnumerator(Object a) { return asList(a).GetEnumerator(); }

    public static cs.System.Collections.Generic.List_1 asList(Object a) {
        cs.System.Collections.Generic.List_1 l = new cs.System.Collections.Generic.List_1();
        int n = Array.getLength(a);
        for (int i = 0; i < n; i++) l.Add(Array.get(a, i));
        return l;
    }

    public static boolean TrueForAll(Type t, Object a, cs.System.Predicate_1 p) {
        int n = Array.getLength(a);
        for (int i = 0; i < n; i++) if (!p.Invoke(box(a, i))) return false;
        return true;
    }

    public static void Fill(Type t, Object a, Object v) {
        for (int i = 0; i < Array.getLength(a); i++) cs.rt.Rt.arraySet(a, i, v);
    }
}
