package cs.System;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;

import cs.rt.EnumInfo;

/**
 * System.Type for translated code. A Type wraps the Java class of a translated .NET type (or a primitive),
 * plus element type for arrays and type arguments for generic instantiations.
 */
public class Type extends cs.System.Reflection.MemberInfo {
    public final Class<?> cls;          // java class (wrapper class for primitives, enum class for enums)
    public final String csName;         // .NET full name
    public final Class<?> prim;         // java primitive class for primitives / enums (null otherwise)
    public final Type elem;             // array element
    public final Type[] gargs;          // generic arguments (null if not generic)
    public final EnumInfo enumInfo;
    public boolean byref;

    private Type(Class<?> cls, String csName, Class<?> prim, Type elem, Type[] gargs, EnumInfo enumInfo) {
        this.cls = cls;
        this.csName = csName;
        this.prim = prim;
        this.elem = elem;
        this.gargs = gargs;
        this.enumInfo = enumInfo;
    }

    public Type() { this(Object.class, "System.Object", null, null, null, null); }

    private static final HashMap<Class<?>, Type> byClass = new HashMap<>();
    private static final HashMap<Type, Type> arrays = new HashMap<>();

    private static Type prim(Class<?> wrapper, Class<?> p, String name) {
        Type t = new Type(wrapper, name, p, null, null, null);
        return t;
    }

    public static final Type BOOLEAN = prim(Boolean.class, boolean.class, "System.Boolean");
    public static final Type CHAR = prim(Character.class, char.class, "System.Char");
    public static final Type SBYTE = prim(Byte.class, byte.class, "System.SByte");
    public static final Type BYTE = prim(Byte.class, byte.class, "System.Byte");
    public static final Type INT16 = prim(Short.class, short.class, "System.Int16");
    public static final Type UINT16 = prim(Character.class, char.class, "System.UInt16");
    public static final Type INT32 = prim(Integer.class, int.class, "System.Int32");
    public static final Type UINT32 = prim(Integer.class, int.class, "System.UInt32");
    public static final Type INT64 = prim(Long.class, long.class, "System.Int64");
    public static final Type UINT64 = prim(Long.class, long.class, "System.UInt64");
    public static final Type SINGLE = prim(Float.class, float.class, "System.Single");
    public static final Type DOUBLE = prim(Double.class, double.class, "System.Double");
    public static final Type INTPTR = prim(Long.class, long.class, "System.IntPtr");
    public static final Type UINTPTR = prim(Long.class, long.class, "System.UIntPtr");
    public static final Type STRING = new Type(String.class, "System.String", null, null, null, null);
    public static final Type OBJECT = new Type(Object.class, "System.Object", null, null, null, null);
    public static final Type VOID = new Type(void.class, "System.Void", null, null, null, null);

    static {
        byClass.put(String.class, STRING);
        byClass.put(Object.class, OBJECT);
        byClass.put(Boolean.class, BOOLEAN);
        byClass.put(Character.class, CHAR);
        byClass.put(Byte.class, BYTE);
        byClass.put(Short.class, INT16);
        byClass.put(Integer.class, INT32);
        byClass.put(Long.class, INT64);
        byClass.put(Float.class, SINGLE);
        byClass.put(Double.class, DOUBLE);
        byClass.put(boolean.class, BOOLEAN);
        byClass.put(char.class, CHAR);
        byClass.put(byte.class, BYTE);
        byClass.put(short.class, INT16);
        byClass.put(int.class, INT32);
        byClass.put(long.class, INT64);
        byClass.put(float.class, SINGLE);
        byClass.put(double.class, DOUBLE);
    }

    /** .NET full name of a translated Java class (cs.global.A$B -> A+B, cs.X.Y.List_1 -> X.Y.List`1). */
    public static String csNameOf(Class<?> c) {
        String n = c.getName();
        if (n.startsWith("cs.global.")) n = n.substring(10);
        else if (n.startsWith("cs.")) n = n.substring(3);
        n = n.replace('$', '+');
        int k = n.length() - 1;
        while (k > 0 && Character.isDigit(n.charAt(k))) k--;
        if (k > 0 && k < n.length() - 1 && n.charAt(k) == '_') n = n.substring(0, k) + "`" + n.substring(k + 1);
        if (n.equals("UnityEngine.UObject")) n = "UnityEngine.Object";
        return n;
    }

    public static synchronized Type of(Class<?> c) {
        if (c == null) return null;
        Type t = byClass.get(c);
        if (t != null) return t;
        if (c.isArray()) {
            t = arrayOf(of(c.getComponentType()));
            byClass.put(c, t);
            return t;
        }
        EnumInfo ei = null;
        Class<?> p = null;
        try {
            java.lang.reflect.Field f = c.getField("$info");
            if (f.getType() == EnumInfo.class && Modifier.isStatic(f.getModifiers())) ei = (EnumInfo) f.get(null);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
        if (ei != null) {
            p = int.class;
            for (java.lang.reflect.Field f : c.getFields()) {
                if (Modifier.isStatic(f.getModifiers()) && f.getType().isPrimitive()) { p = f.getType(); break; }
            }
        }
        t = new Type(c, csNameOf(c), p, null, null, ei);
        byClass.put(c, t);
        return t;
    }

    public static synchronized Type arrayOf(Type e) {
        Type t = arrays.get(e);
        if (t == null) {
            Class<?> ac = Array.newInstance(e.arrayElementClass(), 0).getClass();
            t = new Type(ac, e.csName + "[]", null, e, null, null);
            arrays.put(e, t);
        }
        return t;
    }

    public static Type generic(Class<?> c, Type... args) {
        Type base = of(c);
        return new Type(c, base.csName, base.prim, null, args, base.enumInfo);
    }

    /** Type of a runtime object (GetType()). */
    public static Type ofObject(Object o) {
        if (o instanceof cs.rt.BoxedEnum) return of(((cs.rt.BoxedEnum) o).info.cls);
        return of(o.getClass());
    }

    // ------------------------------------------------------------------ runtime helpers
    public boolean isPrimitive() { return prim != null && enumInfo == null; }
    public boolean isStruct() { return cs.rt.Struct.class.isAssignableFrom(cls); }

    public Class<?> arrayElementClass() {
        if (prim != null) return prim;
        if (elem != null) return cls;
        return cls;
    }

    public void storePrimitive(Object arr, int i, Object v) {
        Class<?> p = arr.getClass().getComponentType();
        if (p == int.class) Array.setInt(arr, i, cs.rt.Rt.unboxI(v));
        else if (p == float.class) Array.setFloat(arr, i, cs.rt.Rt.unboxF(v));
        else if (p == boolean.class) Array.setBoolean(arr, i, cs.rt.Rt.unboxZ(v));
        else if (p == long.class) Array.setLong(arr, i, cs.rt.Rt.unboxJ(v));
        else if (p == double.class) Array.setDouble(arr, i, cs.rt.Rt.unboxD(v));
        else if (p == char.class) Array.setChar(arr, i, cs.rt.Rt.unboxC(v));
        else if (p == byte.class) Array.setByte(arr, i, cs.rt.Rt.unboxB(v));
        else if (p == short.class) Array.setShort(arr, i, cs.rt.Rt.unboxS(v));
        else Array.set(arr, i, v);
    }

    public Object defaultValue() {
        if (enumInfo != null) return enumInfo.box(0);
        if (prim != null) return boxZero(prim, this);
        if (isStruct()) return createInstance();
        return null;
    }

    private static Object boxZero(Class<?> p, Type t) {
        if (p == int.class) return 0;
        if (p == float.class) return 0f;
        if (p == boolean.class) return false;
        if (p == long.class) return 0L;
        if (p == double.class) return 0d;
        if (p == char.class) return (char) 0;
        if (p == byte.class) return (byte) 0;
        if (p == short.class) return (short) 0;
        return null;
    }

    /** Activator.CreateInstance: Java no-arg constructor, then the translated .NET constructor if present. */
    public Object createInstance() {
        if (prim != null) return defaultValue();
        try {
            Object o = cls.getDeclaredConstructor().newInstance();
            construct(o, cls);
            return o;
        } catch (ReflectiveOperationException e) {
            throw new MissingMethodException("cannot create " + csName + ": " + e);
        }
    }

    /** Runs the parameterless translated constructor ($ctor_Simple) of class c on o, if c declares one. */
    public static void construct(Object o, Class<?> c) {
        try {
            // the nearest class (hand-written facades may not declare one) with a field-initializer constructor
            for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
                String simple = k.getName().substring(k.getName().lastIndexOf('.') + 1);
                java.lang.reflect.Method m;
                try { m = k.getMethod("$ctor_" + simple); } catch (NoSuchMethodException e) { continue; }
                m.invoke(o);
                break;
            }
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw cs.rt.Rt.toThrowable(e.getCause());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean IsInstanceOfType(Object o) {
        if (o == null) return false;
        if (enumInfo != null) return o instanceof cs.rt.BoxedEnum && ((cs.rt.BoxedEnum) o).info == enumInfo;
        return cls.isInstance(o);
    }

    // ------------------------------------------------------------------ System.Type API
    public String get_Name() {
        String n = csName;
        int k = java.lang.Math.max(n.lastIndexOf('.'), n.lastIndexOf('+'));
        return n.substring(k + 1);
    }
    public String get_FullName() { return csName; }
    public String get_Namespace() {
        int k = csName.lastIndexOf('.');
        return k < 0 ? null : csName.substring(0, k);
    }
    @Override public String toString() { return csName; }
    @Override public int get_MemberType() { return 32; }
    @Override protected Object[] attributes() { return cs.System.Reflection.Assembly.typeAttrs(this); }
    public String ToString() { return csName; }

    public boolean get_IsArray() { return elem != null; }
    public boolean get_IsEnum() { return enumInfo != null; }
    public boolean get_IsClass() { return prim == null && !isStruct() && !cls.isInterface(); }
    public boolean get_IsValueType() { return prim != null || isStruct(); }
    public boolean get_IsInterface() { return cls.isInterface(); }
    public boolean get_IsAbstract() { return Modifier.isAbstract(cls.getModifiers()); }
    public boolean get_IsPrimitive() { return isPrimitive() && this != STRING; }
    public boolean get_IsGenericType() { return gargs != null; }
    public Type[] get_GenericTypeArguments() { return gargs != null ? gargs.clone() : new Type[0]; }
    public Type[] GetGenericArguments() { return get_GenericTypeArguments(); }
    public Type GetElementType() { return elem; }
    public Type get_BaseType() {
        if (prim != null || elem != null) return OBJECT;
        Class<?> s = cls.getSuperclass();
        return s == null ? null : of(s);
    }

    public boolean IsAssignableFrom(Type c) {
        if (c == null) return false;
        if (this == c || equals(c)) return true;
        if (this == OBJECT) return true;
        if (prim != null || c.prim != null) return false;
        return cls.isAssignableFrom(c.cls);
    }

    public boolean IsSubclassOf(Type c) {
        return c != null && !equals(c) && c.cls.isAssignableFrom(cls) && !c.cls.isInterface();
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Type)) return false;
        Type t = (Type) o;
        if (cls != t.cls || !csName.equals(t.csName)) return false;
        if (elem != null ? !elem.equals(t.elem) : t.elem != null) return false;
        return java.util.Arrays.equals(gargs, t.gargs);
    }

    @Override public int hashCode() { return csName.hashCode() * 31 + cls.hashCode(); }

    public static boolean op_Equality(Type a, Type b) { return a == null ? b == null : a.equals(b); }
    public static boolean op_Inequality(Type a, Type b) { return !op_Equality(a, b); }

    // ------------------------------------------------------------------ reflection
    public cs.System.Reflection.FieldInfo GetField(String name) {
        return GetField(name, 4 | 8 | 16);
    }

    public cs.System.Reflection.FieldInfo GetField(String name, int bindingFlags) {
        for (cs.System.Reflection.FieldInfo f : GetFields(bindingFlags)) if (f.get_Name().equals(name)) return f;
        return null;
    }

    public cs.System.Reflection.FieldInfo[] GetFields() { return GetFields(4 | 8 | 16); }

    /** BindingFlags: Instance=4 Static=8 Public=16 NonPublic=32 */
    public cs.System.Reflection.FieldInfo[] GetFields(int bindingFlags) {
        ArrayList<cs.System.Reflection.FieldInfo> out = new ArrayList<>();
        cs.System.Reflection.FieldInfo.collect(this, bindingFlags, out);
        return out.toArray(new cs.System.Reflection.FieldInfo[0]);
    }

    public cs.System.Reflection.MethodInfo[] GetMethods() { return GetMethods(4 | 8 | 16); }

    public cs.System.Reflection.MethodInfo[] GetMethods(int bindingFlags) {
        ArrayList<cs.System.Reflection.MethodInfo> out = new ArrayList<>();
        cs.System.Reflection.MethodInfo.collect(this, bindingFlags, out);
        return out.toArray(new cs.System.Reflection.MethodInfo[0]);
    }

    public cs.System.Reflection.PropertyInfo[] GetProperties() { return GetProperties(4 | 8 | 16); }

    public cs.System.Reflection.PropertyInfo[] GetProperties(int bindingFlags) {
        ArrayList<cs.System.Reflection.PropertyInfo> out = new ArrayList<>();
        cs.System.Reflection.PropertyInfo.collect(this, bindingFlags, out);
        return out.toArray(new cs.System.Reflection.PropertyInfo[0]);
    }

    public Type makeByRef() {
        Type t = new Type(cls, csName + "&", prim, elem, gargs, enumInfo);
        t.byref = true;
        t.base = this;
        return t;
    }
    private Type base;

    public boolean get_IsByRef() { return byref; }
    public boolean get_IsPointer() { return false; }
    public boolean get_IsGenericParameter() { return false; }
    public boolean get_IsGenericTypeDefinition() { return false; }
    public boolean get_ContainsGenericParameters() { return false; }
    public boolean get_IsSealed() { return Modifier.isFinal(cls.getModifiers()); }
    public boolean get_IsPublic() { return !get_IsNested() && Modifier.isPublic(cls.getModifiers()); }
    public boolean get_IsNotPublic() { return !get_IsPublic() && !get_IsNested(); }
    public boolean get_IsNested() { return cls.getName().indexOf('$', cls.getName().lastIndexOf('.')) > 0 && cls.getName().startsWith("cs."); }
    public boolean get_IsNestedPublic() { return get_IsNested(); }
    public boolean get_IsNestedPrivate() { return false; }
    public boolean get_IsNestedFamily() { return false; }
    public boolean get_IsNestedAssembly() { return false; }
    public boolean get_IsNestedFamANDAssem() { return false; }
    public boolean get_IsNestedFamORAssem() { return false; }
    public cs.System.Reflection.Assembly get_Assembly() { return cs.System.Reflection.Assembly.game(); }
    public int GetArrayRank() { return 1; }
    public Type GetGenericTypeDefinition() { return of(cls); }
    public Type MakeGenericType(Type[] args) { return generic(cls, args); }
    public Type MakeArrayType() { return arrayOf(this); }
    public Type MakeByRefType() { return makeByRef(); }
    public Type GetEnumUnderlyingType() { return enumInfo == null ? null : of(prim); }
    public Type[] GetInterfaces() {
        java.util.LinkedHashSet<Class<?>> all = new java.util.LinkedHashSet<>();
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) collectIf(c, all);
        Type[] r = new Type[all.size()];
        int i = 0;
        for (Class<?> c : all) r[i++] = of(c);
        return r;
    }
    private static void collectIf(Class<?> c, java.util.Set<Class<?>> out) {
        for (Class<?> i : c.getInterfaces()) if (out.add(i)) collectIf(i, out);
    }
    public Type GetInterface(String name) {
        for (Type t : GetInterfaces()) if (t.get_Name().equals(name) || t.get_FullName().equals(name)) return t;
        return null;
    }
    public Type[] GetNestedTypes() { return GetNestedTypes(0); }
    public Type[] GetNestedTypes(int bf) {
        ArrayList<Type> out = new ArrayList<>();
        String prefix = cls.getName() + "$";
        for (String n : cs.rt.GameTypes.NAMES) {
            if (n.startsWith(prefix) && n.indexOf('$', prefix.length()) < 0) {
                try { out.add(of(cs.rt.Rt.cls(n))); } catch (ClassNotFoundException ignored) {}
            }
        }
        return out.toArray(new Type[0]);
    }
    public cs.System.Reflection.ConstructorInfo[] GetConstructors() { return GetConstructors(4 | 16); }
    public cs.System.Reflection.ConstructorInfo[] GetConstructors(int bf) { return new cs.System.Reflection.ConstructorInfo[0]; }
    public cs.System.Reflection.EventInfo[] GetEvents() { return new cs.System.Reflection.EventInfo[0]; }
    public cs.System.Reflection.EventInfo[] GetEvents(int bf) { return new cs.System.Reflection.EventInfo[0]; }
    public cs.System.Reflection.MethodInfo GetMethod(String name) {
        cs.System.Reflection.MethodInfo found = null;
        for (cs.System.Reflection.MethodInfo m : GetMethods(4 | 8 | 16)) if (m.get_Name().equals(name)) found = found == null ? m : found;
        return found;
    }
    public cs.System.Reflection.MethodInfo GetMethod(String name, int bf) {
        for (cs.System.Reflection.MethodInfo m : GetMethods(bf)) if (m.get_Name().equals(name)) return m;
        return null;
    }
    public cs.System.Reflection.MethodInfo GetMethod(String name, Type[] types) {
        for (cs.System.Reflection.MethodInfo m : GetMethods(4 | 8 | 16)) {
            if (!m.get_Name().equals(name)) continue;
            cs.System.Reflection.ParameterInfo[] ps = m.GetParameters();
            if (ps.length != types.length) continue;
            boolean ok = true;
            for (int i = 0; i < ps.length; i++) if (!ps[i].get_ParameterType().equals(types[i])) { ok = false; break; }
            if (ok) return m;
        }
        return null;
    }
    public cs.System.Reflection.PropertyInfo GetProperty(String name) {
        for (cs.System.Reflection.PropertyInfo p : GetProperties(4 | 8 | 16)) if (p.get_Name().equals(name)) return p;
        return null;
    }
    public cs.System.Reflection.PropertyInfo GetProperty(String name, int bf) {
        for (cs.System.Reflection.PropertyInfo p : GetProperties(bf)) if (p.get_Name().equals(name)) return p;
        return null;
    }
    public Object[] GetCustomAttributes(boolean inherit) { return cs.System.Reflection.Assembly.typeAttrs(this); }
    public Object[] GetCustomAttributes(Type t, boolean inherit) {
        ArrayList<Object> out = new ArrayList<>();
        for (Object a : cs.System.Reflection.Assembly.typeAttrs(this)) if (t.IsInstanceOfType(a)) out.add(a);
        Object arr = Array.newInstance(t.cls, out.size());
        for (int i = 0; i < out.size(); i++) Array.set(arr, i, out.get(i));
        return (Object[]) arr;
    }
    public boolean IsDefined(Type t, boolean inherit) { return GetCustomAttributes(t, inherit).length > 0; }
    public static Type GetType(String name) { return cs.System.Reflection.Assembly.resolveType(name.split(",")[0].trim().replace('+', '/')); }
    public static Type GetTypeFromHandle(Type t) { return t; }
    public Type get_UnderlyingSystemType() { return this; }
    public Type get_DeclaringType() {
        String n = cls.getName();
        int k = n.lastIndexOf('$');
        if (k < 0 || !n.startsWith("cs.")) return null;
        try { return of(cs.rt.Rt.cls(n.substring(0, k))); } catch (ClassNotFoundException e) { return null; }
    }

    /** .NET name of a type for resolution from metadata strings ("System.Int32", "Foo", "List`1<...>"). */
    public static Type forCsName(String n) {
        return cs.System.Reflection.Assembly.resolveType(n);
    }
}
