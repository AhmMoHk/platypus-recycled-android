package cs.System.Collections.Generic;

public class Comparer_1 implements IComparer_1, cs.System.Collections.IComparer {
    private static final Comparer_1 DEFAULT = new Comparer_1();
    public static Comparer_1 get_Default() { return DEFAULT; }
    public int Compare(Object a, Object b) { return cs.System.ObjectX.compare(a, b); }
}
