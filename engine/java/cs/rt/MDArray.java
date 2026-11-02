package cs.rt;

/** Multi-dimensional .NET array (row-major). */
public final class MDArray {
    public final int[] dims;
    public final Object[] data;

    public MDArray(int... dims) {
        this.dims = dims;
        int n = 1;
        for (int d : dims) n *= d;
        data = new Object[n];
    }

    public void fill(Object v) { java.util.Arrays.fill(data, v); }

    public int GetLength(int d) { return dims[d]; }
    public int get_Length() { return data.length; }

    public int index(int... idx) {
        int k = 0;
        for (int i = 0; i < dims.length; i++) k = k * dims[i] + idx[i];
        return k;
    }
}
