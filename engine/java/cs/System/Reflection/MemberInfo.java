package cs.System.Reflection;

import java.util.ArrayList;

/** Base of reflection members. Custom attributes come from the translator's $cattrs metadata. */
public class MemberInfo {
    protected String name;
    protected cs.System.Type declaring;
    protected Object[] attrs;           // attribute instances
    protected String attrKey;           // key into $cattrs of the declaring class

    public String get_Name() { return name; }
    public cs.System.Type get_DeclaringType() { return declaring; }
    public cs.System.Type get_ReflectedType() { return declaring; }
    public int get_MemberType() { return 0; }

    protected Object[] attributes() {
        if (attrs == null) attrs = (declaring != null && attrKey != null) ? Meta.attrsOf(declaring.cls, attrKey) : new Object[0];
        return attrs;
    }

    public Object[] GetCustomAttributes(boolean inherit) { return attributes().clone(); }

    public Object[] GetCustomAttributes(cs.System.Type type, boolean inherit) {
        ArrayList<Object> out = new ArrayList<>();
        for (Object a : attributes()) if (type.IsInstanceOfType(a)) out.add(a);
        Object arr = java.lang.reflect.Array.newInstance(type.cls, out.size());
        for (int i = 0; i < out.size(); i++) java.lang.reflect.Array.set(arr, i, out.get(i));
        return (Object[]) arr;
    }

    public boolean IsDefined(cs.System.Type type, boolean inherit) { return GetCustomAttributes(type, inherit).length > 0; }

    public cs.System.Collections.Generic.IEnumerable_1 get_CustomAttributes() {
        cs.System.Collections.Generic.List_1 l = new cs.System.Collections.Generic.List_1();
        for (Object a : attributes()) l.Add(new CustomAttributeData(a));
        return l;
    }

    public static boolean op_Equality(MemberInfo a, MemberInfo b) { return a == null ? b == null : a.equals(b); }
    public static boolean op_Inequality(MemberInfo a, MemberInfo b) { return !op_Equality(a, b); }

    @Override public String toString() { return name; }
}
