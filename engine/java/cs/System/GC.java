package cs.System;

public final class GC {
    private GC() {}
    public static void Collect() { System.gc(); }
    public static void Collect(int gen) { System.gc(); }
    public static void Collect(int gen, int mode) { System.gc(); }
    public static void ReRegisterForFinalize(Object o) {}
    public static void SuppressFinalize(Object o) {}
    public static void KeepAlive(Object o) {}
    public static long GetTotalMemory(boolean force) { Runtime r = Runtime.getRuntime(); return r.totalMemory() - r.freeMemory(); }
}
