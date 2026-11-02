package cs.System.Runtime.CompilerServices;

public final class RuntimeHelpers {
    private RuntimeHelpers() {}
    public static Object GetSubArray(cs.System.Type t, Object arr, cs.System.Range r) {
        int len = java.lang.reflect.Array.getLength(arr);
        int s = r.start.GetOffset(len), e = r.end.GetOffset(len);
        Object out = java.lang.reflect.Array.newInstance(arr.getClass().getComponentType(), e - s);
        System.arraycopy(arr, s, out, 0, e - s);
        return out;
    }
    public static int GetHashCode(Object o) { return System.identityHashCode(o); }
    public static void EnsureSufficientExecutionStack() {}
    public static void RunClassConstructor(Object handle) {}
}
