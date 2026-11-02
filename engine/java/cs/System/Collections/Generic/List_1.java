package cs.System.Collections.Generic;

import java.util.Arrays;

/** System.Collections.Generic.List<T> (elements stored boxed / by reference). */
public class List_1 implements IList_1, IReadOnlyList_1, cs.System.Collections.IList {
    Object[] items;
    int size;
    int version;

    public List_1() { items = new Object[0]; }
    public List_1(int capacity) {
        if (capacity < 0) throw new cs.System.ArgumentOutOfRangeException("capacity");
        items = new Object[capacity];
    }
    public List_1(IEnumerable_1 src) { this(); AddRange(src); }
    public List_1(cs.System.Collections.IEnumerable src) { this(); addAll(src); }

    public void $init(int capacity) { items = new Object[java.lang.Math.max(capacity, 0)]; size = 0; }
    public void $init(IEnumerable_1 src) { items = new Object[0]; size = 0; AddRange(src); }

    private void grow(int min) {
        if (items.length < min) {
            int n = items.length == 0 ? 4 : items.length * 2;
            if (n < min) n = min;
            items = Arrays.copyOf(items, n);
        }
    }

    public int get_Count() { return size; }
    public int get_Capacity() { return items.length; }
    public void set_Capacity(int c) { if (c >= size) items = Arrays.copyOf(items, c); }

    private void check(int i) {
        if (i < 0 || i >= size) throw new cs.System.ArgumentOutOfRangeException("index", "Index was out of range. Must be non-negative and less than the size of the collection.");
    }

    public Object get_Item(int i) { check(i); return items[i]; }
    public void set_Item(int i, Object v) { check(i); items[i] = v; version++; }

    public void Add(Object v) {
        if (size == items.length) grow(size + 1);
        items[size++] = v;
        version++;
    }
    public int IList_Add(Object v) { Add(v); return size - 1; }

    private void addAll(cs.System.Collections.IEnumerable src) {
        cs.System.Collections.IEnumerator e = src.GetEnumerator();
        while (e.MoveNext()) Add(e.get_Current());
    }

    public void AddRange(IEnumerable_1 src) {
        if (src == null) throw new cs.System.ArgumentNullException("collection");
        if (src instanceof List_1) {
            List_1 o = (List_1) src;
            Object[] copy = Arrays.copyOf(o.items, o.size);
            grow(size + copy.length);
            System.arraycopy(copy, 0, items, size, copy.length);
            size += copy.length;
            version++;
            return;
        }
        addAll(src);
    }

    public void Insert(int i, Object v) {
        if (i < 0 || i > size) throw new cs.System.ArgumentOutOfRangeException("index");
        if (size == items.length) grow(size + 1);
        System.arraycopy(items, i, items, i + 1, size - i);
        items[i] = v;
        size++;
        version++;
    }

    public void InsertRange(int i, IEnumerable_1 src) {
        List_1 tmp = new List_1(src);
        if (i < 0 || i > size) throw new cs.System.ArgumentOutOfRangeException("index");
        grow(size + tmp.size);
        System.arraycopy(items, i, items, i + tmp.size, size - i);
        System.arraycopy(tmp.items, 0, items, i, tmp.size);
        size += tmp.size;
        version++;
    }

    public void Clear() {
        Arrays.fill(items, 0, size, null);
        size = 0;
        version++;
    }

    public int IndexOf(Object v) {
        for (int i = 0; i < size; i++) if (cs.System.ObjectX.Equals(items[i], v)) return i;
        return -1;
    }
    public int IndexOf(Object v, int start) {
        for (int i = start; i < size; i++) if (cs.System.ObjectX.Equals(items[i], v)) return i;
        return -1;
    }
    public int LastIndexOf(Object v) {
        for (int i = size - 1; i >= 0; i--) if (cs.System.ObjectX.Equals(items[i], v)) return i;
        return -1;
    }
    public boolean Contains(Object v) { return IndexOf(v) >= 0; }

    public boolean Remove(Object v) {
        int i = IndexOf(v);
        if (i < 0) return false;
        RemoveAt(i);
        return true;
    }

    public void RemoveAt(int i) {
        check(i);
        size--;
        System.arraycopy(items, i + 1, items, i, size - i);
        items[size] = null;
        version++;
    }

    public void RemoveRange(int i, int n) {
        if (i < 0 || n < 0 || i + n > size) throw new cs.System.ArgumentException("Offset and length were out of bounds for the array or count is greater than the number of elements from index to the end of the source collection.");
        System.arraycopy(items, i + n, items, i, size - i - n);
        Arrays.fill(items, size - n, size, null);
        size -= n;
        version++;
    }

    public int RemoveAll(cs.System.Predicate_1 p) {
        int free = 0;
        while (free < size && !p.Invoke(items[free])) free++;
        if (free >= size) return 0;
        int cur = free + 1;
        while (cur < size) {
            while (cur < size && p.Invoke(items[cur])) cur++;
            if (cur < size) items[free++] = items[cur++];
        }
        Arrays.fill(items, free, size, null);
        int removed = size - free;
        size = free;
        version++;
        return removed;
    }

    public Object Find(cs.System.Predicate_1 p) {
        for (int i = 0; i < size; i++) if (p.Invoke(items[i])) return items[i];
        return null;
    }
    public Object Find(cs.System.Type t, cs.System.Predicate_1 p) {
        for (int i = 0; i < size; i++) if (p.Invoke(items[i])) return items[i];
        return t.defaultValue();
    }
    public int FindIndex(cs.System.Predicate_1 p) {
        for (int i = 0; i < size; i++) if (p.Invoke(items[i])) return i;
        return -1;
    }
    public Object FindLast(cs.System.Predicate_1 p) {
        for (int i = size - 1; i >= 0; i--) if (p.Invoke(items[i])) return items[i];
        return null;
    }
    public List_1 FindAll(cs.System.Predicate_1 p) {
        List_1 r = new List_1();
        for (int i = 0; i < size; i++) if (p.Invoke(items[i])) r.Add(items[i]);
        return r;
    }
    public boolean Exists(cs.System.Predicate_1 p) { return FindIndex(p) >= 0; }
    public boolean TrueForAll(cs.System.Predicate_1 p) {
        for (int i = 0; i < size; i++) if (!p.Invoke(items[i])) return false;
        return true;
    }
    public void ForEach(cs.System.Action_1 a) {
        int v = version;
        for (int i = 0; i < size; i++) {
            if (v != version) throw new cs.System.InvalidOperationException("Collection was modified; enumeration operation may not execute.");
            a.Invoke(items[i]);
        }
    }

    public List_1 GetRange(int i, int n) {
        if (i < 0 || n < 0 || i + n > size) throw new cs.System.ArgumentException("Offset and length were out of bounds.");
        List_1 r = new List_1(n);
        System.arraycopy(items, i, r.items, 0, n);
        r.size = n;
        return r;
    }

    public void Reverse() { Reverse(0, size); }
    public void Reverse(int i, int n) {
        for (int a = i, b = i + n - 1; a < b; a++, b--) { Object t = items[a]; items[a] = items[b]; items[b] = t; }
        version++;
    }

    public void Sort() { cs.rt.IntroSort.sort(items, 0, size, cs.rt.IntroSort.comparer(null)); version++; }
    public void Sort(IComparer_1 c) { cs.rt.IntroSort.sort(items, 0, size, cs.rt.IntroSort.comparer(c)); version++; }
    public void Sort(cs.System.Comparison_1 c) { cs.rt.IntroSort.sort(items, 0, size, cs.rt.IntroSort.comparison(c)); version++; }
    public void Sort(int i, int n, IComparer_1 c) { cs.rt.IntroSort.sort(items, i, n, cs.rt.IntroSort.comparer(c)); version++; }

    public int BinarySearch(Object v) {
        int lo = 0, hi = size - 1;
        while (lo <= hi) {
            int m = (lo + hi) >>> 1;
            int c = cs.System.ObjectX.compare(items[m], v);
            if (c == 0) return m;
            if (c < 0) lo = m + 1; else hi = m - 1;
        }
        return ~lo;
    }

    /** ToArray: an Object[] (the translator converts to the element type). */
    public Object ToArray() { return Arrays.copyOf(items, size); }
    public void CopyTo(Object arr) { CopyTo(arr, 0); }
    public void CopyTo(Object arr, int at) { for (int i = 0; i < size; i++) cs.rt.Rt.arraySet(arr, at + i, items[i]); }
    public void TrimExcess() { items = Arrays.copyOf(items, size); }

    public List_1$Enumerator GetEnumerator() { return new List_1$Enumerator(this); }

    // view for the debugger / toString
    @Override public String toString() { return "System.Collections.Generic.List`1[" + size + "]"; }
}
