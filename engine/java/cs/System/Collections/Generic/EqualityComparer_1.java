package cs.System.Collections.Generic;

public class EqualityComparer_1 implements IEqualityComparer_1 {
    private static final EqualityComparer_1 DEFAULT = new EqualityComparer_1();
    public static EqualityComparer_1 get_Default() { return DEFAULT; }
    public static EqualityComparer_1 get_Default(cs.System.Type t) { return DEFAULT; }
    public boolean Equals(Object a, Object b) { return cs.System.ObjectX.Equals(a, b); }
    public int GetHashCode(Object a) { return a == null ? 0 : cs.System.ObjectX.hash(a); }
}
