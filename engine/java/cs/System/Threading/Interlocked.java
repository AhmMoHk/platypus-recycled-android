package cs.System.Threading;

public final class Interlocked {
    private Interlocked() {}
    public static synchronized Object CompareExchange(cs.System.Type t, cs.rt.RefO loc, Object value, Object comparand) {
        Object old = loc.v;
        if (old == comparand) loc.v = value;
        return old;
    }
    public static synchronized Object Exchange(cs.System.Type t, cs.rt.RefO loc, Object value) { Object o = loc.v; loc.v = value; return o; }
    public static synchronized int Increment(cs.rt.RefI r) { return ++r.v; }
    public static synchronized int Decrement(cs.rt.RefI r) { return --r.v; }
    public static synchronized int Add(cs.rt.RefI r, int v) { return r.v += v; }
    public static synchronized int CompareExchange(cs.rt.RefI loc, int value, int comparand) { int o = loc.v; if (o == comparand) loc.v = value; return o; }
    public static synchronized int Exchange(cs.rt.RefI loc, int value) { int o = loc.v; loc.v = value; return o; }
}
