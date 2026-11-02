package cs.System.Reflection;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

/** Properties from the translator's $pmeta table (or get_X/set_X pairs for runtime classes). */
public class PropertyInfo extends MemberInfo {
    private MethodInfo getter, setter;
    private String typeName;

    public PropertyInfo() {}

    public static void collect(cs.System.Type t, int bf, ArrayList<PropertyInfo> out) {
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        for (Class<?> c = t.cls; c != null && c != Object.class; c = c.getSuperclass()) {
            Object[][] meta = Meta.props(c);
            Object[][] mm = Meta.methods(c);
            if (meta != null) {
                for (Object[] row : meta) {
                    String n = (String) row[0];
                    if (!seen.add(n)) continue;
                    PropertyInfo p = new PropertyInfo();
                    p.name = n;
                    p.typeName = (String) row[1];
                    p.declaring = cs.System.Type.of(c);
                    p.attrKey = "P:" + n;
                    p.getter = method(c, mm, (String) row[2]);
                    p.setter = method(c, mm, (String) row[3]);
                    MethodInfo any = p.getter != null ? p.getter : p.setter;
                    if (any == null) continue;
                    boolean st = any.get_IsStatic(), pub = (p.getter != null && p.getter.get_IsPublic()) || (p.setter != null && p.setter.get_IsPublic());
                    if (st && (bf & 8) == 0 || !st && (bf & 4) == 0 || pub && (bf & 16) == 0 || !pub && (bf & 32) == 0) continue;
                    out.add(p);
                }
            } else {
                java.util.LinkedHashMap<String, PropertyInfo> map = new java.util.LinkedHashMap<>();
                for (Method m : c.getDeclaredMethods()) {
                    String n = m.getName();
                    if (!Modifier.isPublic(m.getModifiers())) continue;
                    boolean st = Modifier.isStatic(m.getModifiers());
                    if (st && (bf & 8) == 0 || !st && (bf & 4) == 0) continue;
                    boolean get = n.startsWith("get_") && m.getParameterTypes().length == 0;
                    boolean set = n.startsWith("set_") && m.getParameterTypes().length == 1;
                    if (!get && !set) continue;
                    String pn = n.substring(4);
                    if (seen.contains(pn)) continue;
                    PropertyInfo p = map.get(pn);
                    if (p == null) { map.put(pn, p = new PropertyInfo()); p.name = pn; p.declaring = cs.System.Type.of(c); }
                    if (get) p.getter = new MethodInfo(cs.System.Type.of(c), m); else p.setter = new MethodInfo(cs.System.Type.of(c), m);
                }
                for (PropertyInfo p : map.values()) { seen.add(p.name); out.add(p); }
            }
            if ((bf & 2) != 0) break;
        }
    }

    private static MethodInfo method(Class<?> c, Object[][] mm, String javaName) {
        if (javaName == null || mm == null) return null;
        for (Object[] row : mm) {
            if (javaName.equals(row[0])) {
                Method jm = Meta.findJava(c, javaName, ((String[]) row[4]).length);
                return jm == null ? null : new MethodInfo(cs.System.Type.of(c), jm, row);
            }
        }
        return null;
    }

    public cs.System.Type get_PropertyType() {
        if (typeName != null) { cs.System.Type t = Assembly.resolveType(typeName); if (t != null) return t; }
        if (getter != null) return getter.get_ReturnType();
        if (setter != null) return setter.GetParameters()[0].get_ParameterType();
        return cs.System.Type.OBJECT;
    }

    public boolean get_CanRead() { return getter != null; }
    public boolean get_CanWrite() { return setter != null; }
    public boolean get_IsSpecialName() { return false; }
    public MethodInfo GetGetMethod() { return getter != null && getter.get_IsPublic() ? getter : null; }
    public MethodInfo GetGetMethod(boolean nonPublic) { return nonPublic ? getter : GetGetMethod(); }
    public MethodInfo GetSetMethod() { return setter != null && setter.get_IsPublic() ? setter : null; }
    public MethodInfo GetSetMethod(boolean nonPublic) { return nonPublic ? setter : GetSetMethod(); }
    public MethodInfo get_GetMethod() { return getter; }
    public MethodInfo get_SetMethod() { return setter; }
    public ParameterInfo[] GetIndexParameters() { return new ParameterInfo[0]; }

    public Object GetValue(Object obj) { return getter.Invoke(obj, new Object[0]); }
    public Object GetValue(Object obj, Object[] idx) { return GetValue(obj); }
    public void SetValue(Object obj, Object v) { setter.Invoke(obj, new Object[]{v}); }
    public void SetValue(Object obj, Object v, Object[] idx) { SetValue(obj, v); }

    public static boolean op_Equality(PropertyInfo a, PropertyInfo b) { return a == null ? b == null : a.equals(b); }
    public static boolean op_Inequality(PropertyInfo a, PropertyInfo b) { return !op_Equality(a, b); }
}
