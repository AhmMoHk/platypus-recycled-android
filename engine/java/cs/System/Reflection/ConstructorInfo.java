package cs.System.Reflection;

/** Constructors: Java no-arg constructor + the translated $ctor_X(...) method. */
public class ConstructorInfo extends MethodBase {
    public ConstructorInfo() {}
    ConstructorInfo(cs.System.Type t, java.lang.reflect.Method ctor, Object[] row) {
        MethodInfo mi = new MethodInfo(t, ctor, row);
        declaring = t; method = ctor; name = ".ctor"; flags = mi.flags; params = mi.params; attrKey = mi.attrKey;
    }
    public Object Invoke(Object[] args) {
        Object o;
        try { o = declaring.cls.getDeclaredConstructor().newInstance(); } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        if (method != null) super.Invoke(o, args);
        return o;
    }
    @Override public boolean get_IsConstructor() { return true; }
}
