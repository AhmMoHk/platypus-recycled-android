package cs.rt;

import java.lang.reflect.Array;
import java.util.Comparator;

/** .NET's (unstable) introspective sort, so equal elements end up in the same order as in the original game. */
public final class IntroSort {
    private IntroSort() {}

    public static Comparator<Object> comparer(final cs.System.Collections.Generic.IComparer_1 c) {
        if (c == null) {
            return new Comparator<Object>() {
                public int compare(Object a, Object b) { return cs.System.ObjectX.compare(a, b); }
            };
        }
        return new Comparator<Object>() {
            public int compare(Object a, Object b) { return c.Compare(a, b); }
        };
    }

    public static Comparator<Object> comparison(final cs.System.Comparison_1 c) {
        return new Comparator<Object>() {
            public int compare(Object a, Object b) { return c.Invoke(a, b); }
        };
    }

    /** Sort any java array (primitive arrays are sorted through boxing). */
    public static void sort(Object arr, int lo, int len, Comparator<Object> c) {
        if (arr instanceof Object[]) {
            sort((Object[]) arr, lo, len, c);
            return;
        }
        Object[] tmp = new Object[len];
        for (int i = 0; i < len; i++) tmp[i] = Array.get(arr, lo + i);
        sort(tmp, 0, len, c);
        for (int i = 0; i < len; i++) Rt.arraySet(arr, lo + i, tmp[i]);
    }

    public static void sort(Object[] k, int left, int length, Comparator<Object> c) {
        if (length < 2) return;
        introSort(k, left, length + left - 1, 2 * floorLog2(length), c);
    }

    private static int floorLog2(int n) {
        int r = 0;
        while (n >= 1) { r++; n /= 2; }
        return r;
    }

    private static void introSort(Object[] k, int lo, int hi, int depth, Comparator<Object> c) {
        while (hi > lo) {
            int size = hi - lo + 1;
            if (size <= 16) {
                if (size == 1) return;
                if (size == 2) { swapIfGreater(k, c, lo, hi); return; }
                if (size == 3) { swapIfGreater(k, c, lo, hi - 1); swapIfGreater(k, c, lo, hi); swapIfGreater(k, c, hi - 1, hi); return; }
                insertionSort(k, lo, hi, c);
                return;
            }
            if (depth == 0) { heapsort(k, lo, hi, c); return; }
            depth--;
            int p = pickPivotAndPartition(k, lo, hi, c);
            introSort(k, p + 1, hi, depth, c);
            hi = p - 1;
        }
    }

    private static void swapIfGreater(Object[] k, Comparator<Object> c, int a, int b) {
        if (a != b && c.compare(k[a], k[b]) > 0) { Object t = k[a]; k[a] = k[b]; k[b] = t; }
    }

    private static void swap(Object[] k, int i, int j) { Object t = k[i]; k[i] = k[j]; k[j] = t; }

    private static int pickPivotAndPartition(Object[] k, int lo, int hi, Comparator<Object> c) {
        int mid = lo + ((hi - lo) >> 1);
        swapIfGreater(k, c, lo, mid);
        swapIfGreater(k, c, lo, hi);
        swapIfGreater(k, c, mid, hi);
        Object pivot = k[mid];
        swap(k, mid, hi - 1);
        int left = lo, right = hi - 1;
        while (left < right) {
            while (c.compare(k[++left], pivot) < 0) {}
            while (c.compare(pivot, k[--right]) < 0) {}
            if (left >= right) break;
            swap(k, left, right);
        }
        if (left != hi - 1) swap(k, left, hi - 1);
        return left;
    }

    private static void insertionSort(Object[] k, int lo, int hi, Comparator<Object> c) {
        for (int i = lo; i < hi; i++) {
            int j = i;
            Object t = k[i + 1];
            while (j >= lo && c.compare(t, k[j]) < 0) { k[j + 1] = k[j]; j--; }
            k[j + 1] = t;
        }
    }

    private static void heapsort(Object[] k, int lo, int hi, Comparator<Object> c) {
        int n = hi - lo + 1;
        for (int i = n >> 1; i >= 1; i--) downHeap(k, i, n, lo, c);
        for (int i = n; i > 1; i--) { swap(k, lo, lo + i - 1); downHeap(k, 1, i - 1, lo, c); }
    }

    private static void downHeap(Object[] k, int i, int n, int lo, Comparator<Object> c) {
        Object d = k[lo + i - 1];
        while (i <= n >> 1) {
            int child = 2 * i;
            if (child < n && c.compare(k[lo + child - 1], k[lo + child]) < 0) child++;
            if (!(c.compare(d, k[lo + child - 1]) < 0)) break;
            k[lo + i - 1] = k[lo + child - 1];
            i = child;
        }
        k[lo + i - 1] = d;
    }
}
