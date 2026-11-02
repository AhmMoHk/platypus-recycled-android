package cs.System;

import java.util.ArrayList;

/**
 * System.Delegate / MulticastDelegate. A translated delegate type is a subclass with Invoke(...) and $call(...);
 * each bound method is an anonymous subclass overriding $call. Combined delegates keep the invocation list in $list.
 */
public class Delegate {
    public Object $target;
    public String $key;
    public Delegate[] $list;

    public Delegate $bind(Object target, String key) {
        $target = target;
        $key = key;
        return this;
    }

    protected Delegate $empty() { throw new IllegalStateException("delegate type without $empty"); }

    private Delegate[] items() { return $list != null ? $list : new Delegate[]{this}; }

    public static Delegate Combine(Delegate a, Delegate b) {
        if (a == null) return b;
        if (b == null) return a;
        Delegate[] x = a.items(), y = b.items();
        Delegate[] all = new Delegate[x.length + y.length];
        System.arraycopy(x, 0, all, 0, x.length);
        System.arraycopy(y, 0, all, x.length, y.length);
        Delegate r = a.$empty();
        r.$list = all;
        return r;
    }

    public static Delegate Combine(Delegate[] ds) {
        Delegate r = null;
        for (Delegate d : ds) r = Combine(r, d);
        return r;
    }

    /** Removes the last occurrence of value's invocation list from source. */
    public static Delegate Remove(Delegate source, Delegate value) {
        if (source == null) return null;
        if (value == null) return source;
        Delegate[] s = source.items(), v = value.items();
        for (int i = s.length - v.length; i >= 0; i--) {
            boolean match = true;
            for (int k = 0; k < v.length; k++) if (!same(s[i + k], v[k])) { match = false; break; }
            if (match) {
                ArrayList<Delegate> rest = new ArrayList<>();
                for (int k = 0; k < s.length; k++) if (k < i || k >= i + v.length) rest.add(s[k]);
                if (rest.isEmpty()) return null;
                if (rest.size() == 1) return rest.get(0);
                Delegate r = source.$empty();
                r.$list = rest.toArray(new Delegate[0]);
                return r;
            }
        }
        return source;
    }

    public static Delegate RemoveAll(Delegate source, Delegate value) {
        Delegate prev;
        do { prev = source; source = Remove(source, value); } while (source != prev && source != null);
        return source;
    }

    private static boolean same(Delegate a, Delegate b) {
        if (a == b) return true;
        if (a.$list != null || b.$list != null) return false;
        return a.$key != null && a.$key.equals(b.$key) && a.$target == b.$target;
    }

    @Override public boolean equals(Object o) {
        if (!(o instanceof Delegate)) return false;
        Delegate d = (Delegate) o;
        Delegate[] x = items(), y = d.items();
        if (x.length != y.length) return false;
        for (int i = 0; i < x.length; i++) if (!same(x[i], y[i])) return false;
        return true;
    }

    @Override public int hashCode() {
        return ($key == null ? 0 : $key.hashCode()) ^ System.identityHashCode($target);
    }

    public static boolean op_Equality(Delegate a, Delegate b) { return a == null ? b == null : a.equals(b); }
    public static boolean op_Inequality(Delegate a, Delegate b) { return !op_Equality(a, b); }

    public Object get_Target() { return $list != null ? $list[$list.length - 1].$target : $target; }
    public Delegate[] GetInvocationList() { return items().clone(); }

    public static Delegate CreateDelegate(Type t, cs.System.Reflection.MethodInfo m) { throw new NotSupportedException("Delegate.CreateDelegate"); }
    public static Delegate CreateDelegate(Type t, Object target, cs.System.Reflection.MethodInfo m) { throw new NotSupportedException("Delegate.CreateDelegate"); }

    public cs.System.Reflection.MethodInfo get_Method() {
        return cs.System.Reflection.MethodInfo.ofDelegate(this);
    }

    /** DynamicInvoke: calls Invoke(...) reflectively. */
    public Object DynamicInvoke(Object[] args) {
        for (java.lang.reflect.Method m : getClass().getMethods()) {
            if (m.getName().equals("Invoke") && m.getParameterTypes().length == (args == null ? 0 : args.length)) {
                try {
                    return m.invoke(this, args);
                } catch (java.lang.reflect.InvocationTargetException e) {
                    throw cs.rt.Rt.toThrowable(e.getCause());
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        throw new InvalidOperationException("no Invoke");
    }
}
