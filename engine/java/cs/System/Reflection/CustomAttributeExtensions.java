package cs.System.Reflection;

public class CustomAttributeExtensions {
    /** GetCustomAttribute<T>(this MemberInfo) */
    public static Object GetCustomAttribute(cs.System.Type t, MemberInfo m) {
        Object[] a = m.GetCustomAttributes(t, true);
        return a.length > 0 ? a[0] : null;
    }
    public static Object GetCustomAttribute(MemberInfo m, cs.System.Type t) { return GetCustomAttribute(t, m); }
}
