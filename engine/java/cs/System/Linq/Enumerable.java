package cs.System.Linq;

import cs.System.Collections.Generic.IEnumerable_1;
import cs.System.Collections.Generic.IEnumerator_1;
import cs.System.Collections.Generic.List_1;
import cs.System.Func_2;
import cs.System.Type;

/** System.Linq.Enumerable with .NET semantics (deferred execution, stable OrderBy). */
public final class Enumerable {
    private Enumerable() {}

    /** A lazily evaluated sequence. */
    abstract static class Seq implements IEnumerable_1 {
        abstract Iter iter();
        public IEnumerator_1 GetEnumerator() { return iter(); }
    }

    abstract static class Iter implements IEnumerator_1 {
        Object current;
        public Object get_Current() { return current; }
        public void Reset() { throw new cs.System.NotSupportedException("Specified method is not supported."); }
        public void Dispose() {}
    }

    private static boolean truth(Object o) { return cs.rt.Rt.unboxZ(o); }

    private static IEnumerator_1 en(cs.System.Collections.IEnumerable src) {
        if (src == null) throw new cs.System.ArgumentNullException("source");
        cs.System.Collections.IEnumerator e = src.GetEnumerator();
        if (e instanceof IEnumerator_1) return (IEnumerator_1) e;
        final cs.System.Collections.IEnumerator f = e;
        return new Iter() {
            public boolean MoveNext() { if (f.MoveNext()) { current = f.get_Current(); return true; } return false; }
        };
    }

    private static List_1 list(cs.System.Collections.IEnumerable src) {
        List_1 l = new List_1();
        IEnumerator_1 e = en(src);
        while (e.MoveNext()) l.Add(e.get_Current());
        return l;
    }

    // ------------------------------------------------------------------ deferred operators
    public static IEnumerable_1 Where(Type t, final IEnumerable_1 src, final Func_2 pred) {
        if (src == null) throw new cs.System.ArgumentNullException("source");
        return new Seq() {
            Iter iter() {
                final IEnumerator_1 e = en(src);
                return new Iter() {
                    public boolean MoveNext() {
                        while (e.MoveNext()) { Object v = e.get_Current(); if (truth(pred.Invoke(v))) { current = v; return true; } }
                        return false;
                    }
                };
            }
        };
    }

    public static IEnumerable_1 Select(Type t, Type r, final IEnumerable_1 src, final Func_2 sel) {
        if (src == null) throw new cs.System.ArgumentNullException("source");
        return new Seq() {
            Iter iter() {
                final IEnumerator_1 e = en(src);
                return new Iter() {
                    public boolean MoveNext() {
                        if (e.MoveNext()) { current = sel.Invoke(e.get_Current()); return true; }
                        return false;
                    }
                };
            }
        };
    }

    public static IEnumerable_1 SelectMany(Type t, Type r, final IEnumerable_1 src, final Func_2 sel) {
        return new Seq() {
            Iter iter() {
                final IEnumerator_1 e = en(src);
                return new Iter() {
                    IEnumerator_1 inner;
                    public boolean MoveNext() {
                        while (true) {
                            if (inner != null && inner.MoveNext()) { current = inner.get_Current(); return true; }
                            if (!e.MoveNext()) return false;
                            Object s = sel.Invoke(e.get_Current());
                            inner = en(s instanceof cs.System.Collections.IEnumerable ? (cs.System.Collections.IEnumerable) s : cs.rt.Rt.asList(s));
                        }
                    }
                };
            }
        };
    }

    public static IEnumerable_1 Cast(Type t, final cs.System.Collections.IEnumerable src) {
        if (src instanceof IEnumerable_1) return (IEnumerable_1) src;
        return new Seq() {
            Iter iter() {
                final IEnumerator_1 e = en(src);
                return new Iter() {
                    public boolean MoveNext() { if (e.MoveNext()) { current = e.get_Current(); return true; } return false; }
                };
            }
        };
    }

    public static IEnumerable_1 OfType(final Type t, final cs.System.Collections.IEnumerable src) {
        return new Seq() {
            Iter iter() {
                final IEnumerator_1 e = en(src);
                return new Iter() {
                    public boolean MoveNext() {
                        while (e.MoveNext()) { Object v = e.get_Current(); if (t.IsInstanceOfType(v)) { current = v; return true; } }
                        return false;
                    }
                };
            }
        };
    }

    public static IEnumerable_1 Skip(Type t, final IEnumerable_1 src, final int n) {
        return new Seq() {
            Iter iter() {
                final IEnumerator_1 e = en(src);
                return new Iter() {
                    int skipped;
                    public boolean MoveNext() {
                        while (skipped < n) { if (!e.MoveNext()) return false; skipped++; }
                        if (e.MoveNext()) { current = e.get_Current(); return true; }
                        return false;
                    }
                };
            }
        };
    }

    public static IEnumerable_1 Take(Type t, final IEnumerable_1 src, final int n) {
        return new Seq() {
            Iter iter() {
                final IEnumerator_1 e = en(src);
                return new Iter() {
                    int taken;
                    public boolean MoveNext() {
                        if (taken >= n || !e.MoveNext()) return false;
                        taken++;
                        current = e.get_Current();
                        return true;
                    }
                };
            }
        };
    }

    public static IEnumerable_1 Append(Type t, final IEnumerable_1 src, final Object v) {
        return new Seq() {
            Iter iter() {
                final IEnumerator_1 e = en(src);
                return new Iter() {
                    int state;
                    public boolean MoveNext() {
                        if (state == 0) { if (e.MoveNext()) { current = e.get_Current(); return true; } state = 1; current = v; return true; }
                        state = 2;
                        return false;
                    }
                };
            }
        };
    }

    public static IEnumerable_1 Prepend(Type t, final IEnumerable_1 src, final Object v) {
        return Concat(t, ArrayList(v), src);
    }

    private static IEnumerable_1 ArrayList(Object v) { List_1 l = new List_1(); l.Add(v); return l; }

    public static IEnumerable_1 Concat(Type t, final IEnumerable_1 a, final IEnumerable_1 b) {
        return new Seq() {
            Iter iter() {
                final IEnumerator_1 ea = en(a);
                return new Iter() {
                    IEnumerator_1 eb;
                    public boolean MoveNext() {
                        if (eb == null) { if (ea.MoveNext()) { current = ea.get_Current(); return true; } eb = en(b); }
                        if (eb.MoveNext()) { current = eb.get_Current(); return true; }
                        return false;
                    }
                };
            }
        };
    }

    public static IEnumerable_1 Union(Type t, final IEnumerable_1 a, final IEnumerable_1 b) {
        return new Seq() {
            Iter iter() {
                final IEnumerator_1 e = en(Concat(t, a, b));
                final java.util.HashSet<Object> seen = new java.util.HashSet<>();
                return new Iter() {
                    public boolean MoveNext() {
                        while (e.MoveNext()) { Object v = e.get_Current(); if (seen.add(v)) { current = v; return true; } }
                        return false;
                    }
                };
            }
        };
    }

    public static IEnumerable_1 Distinct(Type t, final IEnumerable_1 src) {
        return new Seq() {
            Iter iter() {
                final IEnumerator_1 e = en(src);
                final java.util.HashSet<Object> seen = new java.util.HashSet<>();
                return new Iter() {
                    public boolean MoveNext() {
                        while (e.MoveNext()) { Object v = e.get_Current(); if (seen.add(v)) { current = v; return true; } }
                        return false;
                    }
                };
            }
        };
    }

    public static IEnumerable_1 Except(Type t, final IEnumerable_1 a, final IEnumerable_1 b) {
        return new Seq() {
            Iter iter() {
                final java.util.HashSet<Object> ex = new java.util.HashSet<>();
                IEnumerator_1 eb = en(b);
                while (eb.MoveNext()) ex.add(eb.get_Current());
                final IEnumerator_1 e = en(a);
                return new Iter() {
                    public boolean MoveNext() {
                        while (e.MoveNext()) { Object v = e.get_Current(); if (ex.add(v)) { current = v; return true; } }
                        return false;
                    }
                };
            }
        };
    }

    public static IEnumerable_1 Reverse(Type t, final IEnumerable_1 src) {
        return new Seq() {
            Iter iter() {
                final List_1 l = list(src);
                return new Iter() {
                    int i = l.get_Count();
                    public boolean MoveNext() { if (i > 0) { current = l.get_Item(--i); return true; } return false; }
                };
            }
        };
    }

    // ------------------------------------------------------------------ ordering (stable)
    static final class Ordered extends Seq implements IOrderedEnumerable_1 {
        final IEnumerable_1 src;
        final java.util.ArrayList<Func_2> keys = new java.util.ArrayList<>();
        final java.util.ArrayList<Boolean> desc = new java.util.ArrayList<>();
        Ordered(IEnumerable_1 src) { this.src = src; }
        Iter iter() {
            List_1 l = list(src);
            final int n = l.get_Count();
            final Object[] items = new Object[n];
            final Object[][] k = new Object[keys.size()][n];
            for (int i = 0; i < n; i++) {
                items[i] = l.get_Item(i);
                for (int j = 0; j < keys.size(); j++) k[j][i] = keys.get(j).Invoke(items[i]);
            }
            Integer[] idx = new Integer[n];
            for (int i = 0; i < n; i++) idx[i] = i;
            java.util.Arrays.sort(idx, new java.util.Comparator<Integer>() {
                public int compare(Integer a, Integer b) {
                    for (int j = 0; j < keys.size(); j++) {
                        int c = cs.System.ObjectX.compare(k[j][a], k[j][b]);
                        if (c != 0) return desc.get(j) ? -c : c;
                    }
                    return Integer.compare(a, b);
                }
            });
            final Object[] sorted = new Object[n];
            for (int i = 0; i < n; i++) sorted[i] = items[idx[i]];
            return new Iter() {
                int i = -1;
                public boolean MoveNext() { if (i + 1 < n) { current = sorted[++i]; return true; } i = n; return false; }
            };
        }
    }

    public static IOrderedEnumerable_1 OrderBy(Type t, Type k, IEnumerable_1 src, Func_2 key) {
        Ordered o = new Ordered(src);
        o.keys.add(key);
        o.desc.add(false);
        return o;
    }
    public static IOrderedEnumerable_1 OrderByDescending(Type t, Type k, IEnumerable_1 src, Func_2 key) {
        Ordered o = new Ordered(src);
        o.keys.add(key);
        o.desc.add(true);
        return o;
    }
    public static IOrderedEnumerable_1 ThenBy(Type t, Type k, IOrderedEnumerable_1 src, Func_2 key) {
        Ordered p = (Ordered) src, o = new Ordered(p.src);
        o.keys.addAll(p.keys); o.desc.addAll(p.desc);
        o.keys.add(key); o.desc.add(false);
        return o;
    }
    public static IOrderedEnumerable_1 ThenByDescending(Type t, Type k, IOrderedEnumerable_1 src, Func_2 key) {
        Ordered p = (Ordered) src, o = new Ordered(p.src);
        o.keys.addAll(p.keys); o.desc.addAll(p.desc);
        o.keys.add(key); o.desc.add(true);
        return o;
    }

    // ------------------------------------------------------------------ immediate operators
    public static boolean Any(Type t, IEnumerable_1 src) { return en(src).MoveNext(); }
    public static boolean Any(Type t, IEnumerable_1 src, Func_2 p) {
        IEnumerator_1 e = en(src);
        while (e.MoveNext()) if (truth(p.Invoke(e.get_Current()))) return true;
        return false;
    }
    public static boolean All(Type t, IEnumerable_1 src, Func_2 p) {
        IEnumerator_1 e = en(src);
        while (e.MoveNext()) if (!truth(p.Invoke(e.get_Current()))) return false;
        return true;
    }
    public static boolean Contains(Type t, IEnumerable_1 src, Object v) {
        if (src instanceof cs.System.Collections.Generic.ICollection_1) return ((cs.System.Collections.Generic.ICollection_1) src).Contains(v);
        IEnumerator_1 e = en(src);
        while (e.MoveNext()) if (cs.System.ObjectX.Equals(e.get_Current(), v)) return true;
        return false;
    }
    public static int Count(Type t, IEnumerable_1 src) {
        if (src instanceof cs.System.Collections.Generic.ICollection_1) return ((cs.System.Collections.Generic.ICollection_1) src).get_Count();
        int n = 0;
        IEnumerator_1 e = en(src);
        while (e.MoveNext()) n++;
        return n;
    }
    public static int Count(Type t, IEnumerable_1 src, Func_2 p) {
        int n = 0;
        IEnumerator_1 e = en(src);
        while (e.MoveNext()) if (truth(p.Invoke(e.get_Current()))) n++;
        return n;
    }
    public static Object ElementAt(Type t, IEnumerable_1 src, int i) {
        if (src instanceof List_1) return ((List_1) src).get_Item(i);
        IEnumerator_1 e = en(src);
        int k = 0;
        while (e.MoveNext()) if (k++ == i) return e.get_Current();
        throw new cs.System.ArgumentOutOfRangeException("index");
    }
    public static Object ElementAtOrDefault(Type t, IEnumerable_1 src, int i) {
        IEnumerator_1 e = en(src);
        int k = 0;
        while (e.MoveNext()) if (k++ == i) return e.get_Current();
        return t.defaultValue();
    }
    private static RuntimeException noElements() { return new cs.System.InvalidOperationException("Sequence contains no elements"); }
    public static Object First(Type t, IEnumerable_1 src) { IEnumerator_1 e = en(src); if (e.MoveNext()) return e.get_Current(); throw noElements(); }
    public static Object First(Type t, IEnumerable_1 src, Func_2 p) {
        IEnumerator_1 e = en(src);
        while (e.MoveNext()) { Object v = e.get_Current(); if (truth(p.Invoke(v))) return v; }
        throw new cs.System.InvalidOperationException("Sequence contains no matching element");
    }
    public static Object FirstOrDefault(Type t, IEnumerable_1 src) { IEnumerator_1 e = en(src); return e.MoveNext() ? e.get_Current() : t.defaultValue(); }
    public static Object FirstOrDefault(Type t, IEnumerable_1 src, Func_2 p) {
        IEnumerator_1 e = en(src);
        while (e.MoveNext()) { Object v = e.get_Current(); if (truth(p.Invoke(v))) return v; }
        return t.defaultValue();
    }
    public static Object Last(Type t, IEnumerable_1 src) {
        IEnumerator_1 e = en(src);
        if (!e.MoveNext()) throw noElements();
        Object v = e.get_Current();
        while (e.MoveNext()) v = e.get_Current();
        return v;
    }
    public static Object LastOrDefault(Type t, IEnumerable_1 src) {
        IEnumerator_1 e = en(src);
        Object v = t.defaultValue();
        while (e.MoveNext()) v = e.get_Current();
        return v;
    }
    public static Object LastOrDefault(Type t, IEnumerable_1 src, Func_2 p) {
        IEnumerator_1 e = en(src);
        Object v = t.defaultValue();
        while (e.MoveNext()) { Object x = e.get_Current(); if (truth(p.Invoke(x))) v = x; }
        return v;
    }
    public static Object Single(Type t, IEnumerable_1 src) {
        IEnumerator_1 e = en(src);
        if (!e.MoveNext()) throw noElements();
        Object v = e.get_Current();
        if (e.MoveNext()) throw new cs.System.InvalidOperationException("Sequence contains more than one element");
        return v;
    }
    public static Object SingleOrDefault(Type t, IEnumerable_1 src) {
        IEnumerator_1 e = en(src);
        if (!e.MoveNext()) return t.defaultValue();
        Object v = e.get_Current();
        if (e.MoveNext()) throw new cs.System.InvalidOperationException("Sequence contains more than one element");
        return v;
    }
    public static int Max(Type t, IEnumerable_1 src, Func_2 sel) {
        IEnumerator_1 e = en(src);
        if (!e.MoveNext()) throw noElements();
        int m = cs.rt.Rt.unboxI(sel.Invoke(e.get_Current()));
        while (e.MoveNext()) m = java.lang.Math.max(m, cs.rt.Rt.unboxI(sel.Invoke(e.get_Current())));
        return m;
    }
    public static int Min(Type t, IEnumerable_1 src, Func_2 sel) {
        IEnumerator_1 e = en(src);
        if (!e.MoveNext()) throw noElements();
        int m = cs.rt.Rt.unboxI(sel.Invoke(e.get_Current()));
        while (e.MoveNext()) m = java.lang.Math.min(m, cs.rt.Rt.unboxI(sel.Invoke(e.get_Current())));
        return m;
    }
    public static int Sum(Type t, IEnumerable_1 src, Func_2 sel) {
        int s = 0;
        IEnumerator_1 e = en(src);
        while (e.MoveNext()) s += cs.rt.Rt.unboxI(sel.Invoke(e.get_Current()));
        return s;
    }
    public static int Max(IEnumerable_1 src) {
        IEnumerator_1 e = en(src);
        if (!e.MoveNext()) throw noElements();
        int m = cs.rt.Rt.unboxI(e.get_Current());
        while (e.MoveNext()) m = java.lang.Math.max(m, cs.rt.Rt.unboxI(e.get_Current()));
        return m;
    }
    public static int Sum(IEnumerable_1 src) {
        int s = 0;
        IEnumerator_1 e = en(src);
        while (e.MoveNext()) s += cs.rt.Rt.unboxI(e.get_Current());
        return s;
    }
    public static Object ToArray(Type t, IEnumerable_1 src) {
        List_1 l = list(src);
        Object arr = java.lang.reflect.Array.newInstance(t.arrayElementClass(), l.get_Count());
        for (int i = 0; i < l.get_Count(); i++) cs.rt.Rt.arraySet(arr, i, l.get_Item(i));
        return arr;
    }
    public static List_1 ToList(Type t, IEnumerable_1 src) { return list(src); }
    public static cs.System.Collections.Generic.Dictionary_2 ToDictionary(Type t, Type k, IEnumerable_1 src, Func_2 key) {
        cs.System.Collections.Generic.Dictionary_2 d = new cs.System.Collections.Generic.Dictionary_2();
        IEnumerator_1 e = en(src);
        while (e.MoveNext()) { Object v = e.get_Current(); d.Add(key.Invoke(v), v); }
        return d;
    }
    public static cs.System.Collections.Generic.Dictionary_2 ToDictionary(Type t, Type k, Type vt, IEnumerable_1 src, Func_2 key, Func_2 val) {
        cs.System.Collections.Generic.Dictionary_2 d = new cs.System.Collections.Generic.Dictionary_2();
        IEnumerator_1 e = en(src);
        while (e.MoveNext()) { Object v = e.get_Current(); d.Add(key.Invoke(v), val.Invoke(v)); }
        return d;
    }
    public static boolean SequenceEqual(Type t, IEnumerable_1 a, IEnumerable_1 b) {
        IEnumerator_1 x = en(a), y = en(b);
        while (true) {
            boolean mx = x.MoveNext(), my = y.MoveNext();
            if (mx != my) return false;
            if (!mx) return true;
            if (!cs.System.ObjectX.Equals(x.get_Current(), y.get_Current())) return false;
        }
    }
    public static IEnumerable_1 Empty(Type t) { return new List_1(); }
    public static IEnumerable_1 Range(final int start, final int count) {
        List_1 l = new List_1(count);
        for (int i = 0; i < count; i++) l.Add(start + i);
        return l;
    }
    public static IEnumerable_1 Repeat(Type t, Object v, int count) {
        List_1 l = new List_1(count);
        for (int i = 0; i < count; i++) l.Add(v);
        return l;
    }
}
