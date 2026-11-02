package cs.System.Reflection;

public class MethodBase extends MemberInfo {
    protected java.lang.reflect.Method method;
    protected ParameterInfo[] params;
    protected int flags;              // translator method flags
    protected String retType;
    protected int ntype;              // leading cs.System.Type parameters (generic methods)

    public ParameterInfo[] GetParameters() {
        if (params == null) {
            if (method == null) return params = new ParameterInfo[0];
            Class<?>[] ps = method.getParameterTypes();
            params = new ParameterInfo[ps.length];
            for (int i = 0; i < ps.length; i++) params[i] = new ParameterInfo("arg" + i, cs.System.Type.of(ps[i]), i);
        }
        return params.clone();
    }

    public boolean get_IsStatic() { return (flags & 1) != 0 || method != null && flags == 0 && java.lang.reflect.Modifier.isStatic(method.getModifiers()); }
    public boolean get_IsPublic() { return (flags & 2) != 0 || flags == 0; }
    public boolean get_IsVirtual() { return (flags & 4) != 0; }
    public boolean get_IsAbstract() { return (flags & 8) != 0; }
    public boolean get_IsSpecialName() { return (flags & 16) != 0; }
    public boolean get_IsConstructor() { return (flags & 32) != 0; }
    public boolean get_IsPrivate() { return (flags & 64) != 0; }
    public boolean get_IsFamily() { return (flags & 128) != 0; }
    public boolean get_IsAssembly() { return (flags & 256) != 0; }
    public boolean get_IsFamilyOrAssembly() { return (flags & 512) != 0; }
    public boolean get_IsFamilyAndAssembly() { return (flags & 1024) != 0; }
    public boolean get_IsGenericMethod() { return (flags & 2048) != 0; }
    public boolean get_IsGenericMethodDefinition() { return (flags & 2048) != 0; }
    public boolean get_ContainsGenericParameters() { return (flags & 2048) != 0; }
    public boolean get_IsFinal() { return (flags & 4096) != 0; }
    public cs.System.Type[] GetGenericArguments() { return new cs.System.Type[0]; }

    /** Invoke with .NET argument conventions (boxed values, structs, enums as BoxedEnum). */
    public Object Invoke(Object obj, Object[] args) {
        try {
            Class<?>[] pt = method.getParameterTypes();
            Object[] a = new Object[pt.length];
            int k = 0;
            for (int i = 0; i < ntype; i++) a[k++] = cs.System.Type.OBJECT;
            for (int i = 0; k < pt.length; i++, k++) a[k] = toJava(args == null || i >= args.length ? null : args[i], pt[k]);
            Object r = method.invoke(get_IsStatic() ? null : obj, a);
            return fromJava(r, retType);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw new TargetInvocationException(cs.System.Exception.wrap(e.getCause()));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object toJava(Object v, Class<?> c) {
        if (c.isPrimitive()) {
            if (c == int.class) return cs.rt.Rt.unboxI(v);
            if (c == float.class) return cs.rt.Rt.unboxF(v);
            if (c == boolean.class) return cs.rt.Rt.unboxZ(v);
            if (c == double.class) return cs.rt.Rt.unboxD(v);
            if (c == long.class) return cs.rt.Rt.unboxJ(v);
            if (c == char.class) return cs.rt.Rt.unboxC(v);
            if (c == byte.class) return cs.rt.Rt.unboxB(v);
            if (c == short.class) return cs.rt.Rt.unboxS(v);
        }
        if (v instanceof cs.rt.Struct) return ((cs.rt.Struct) v).$copy();
        return v;
    }

    static Object fromJava(Object r, String retType) {
        if (r == null || retType == null) return r;
        if (r instanceof Number || r instanceof Boolean || r instanceof Character) {
            cs.System.Type t = Assembly.resolveType(retType);
            if (t != null && t.enumInfo != null) return t.enumInfo.box(((Number) r).longValue());
        }
        return r;
    }
}
