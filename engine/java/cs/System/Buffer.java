package cs.System;

public final class Buffer {
    private Buffer() {}
    public static void BlockCopy(Object src, int so, Object dst, int dO, int n) {
        if (src instanceof byte[] && dst instanceof byte[]) { System.arraycopy(src, so, dst, dO, n); return; }
        throw cs.rt.Rt.todo("Buffer.BlockCopy " + src.getClass());
    }
}
