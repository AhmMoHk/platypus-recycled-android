package cs.System.Collections.Generic;

public class HashSet_1$Enumerator implements IEnumerator_1 {
    private final HashSet_1 s;
    private final Object[] items;
    private final int ver;
    private int i = -1;
    HashSet_1$Enumerator(HashSet_1 s) { this.s = s; items = s.set.toArray(); ver = s.version; }
    public HashSet_1$Enumerator() { s = null; items = new Object[0]; ver = 0; }
    public boolean MoveNext() {
        if (s != null && ver != s.version) throw new cs.System.InvalidOperationException("Collection was modified; enumeration operation may not execute.");
        if (i + 1 < items.length) { i++; return true; }
        i = items.length;
        return false;
    }
    public Object get_Current() { return i >= 0 && i < items.length ? items[i] : null; }
    public void Reset() { i = -1; }
    public void Dispose() {}
}
