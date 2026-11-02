package cs.System.Collections.Generic;

public class Queue_1$Enumerator implements IEnumerator_1 {
    private final Queue_1 q;
    private final Object[] items;
    private final int ver;
    private int i = -1;
    Queue_1$Enumerator(Queue_1 q) { this.q = q; items = q.snapshot(); ver = q.version; }
    public Queue_1$Enumerator() { q = null; items = new Object[0]; ver = 0; }
    public boolean MoveNext() {
        if (q != null && ver != q.version) throw new cs.System.InvalidOperationException("Collection was modified; enumeration operation may not execute.");
        if (i + 1 < items.length) { i++; return true; }
        i = items.length;
        return false;
    }
    public Object get_Current() { return i >= 0 && i < items.length ? items[i] : null; }
    public void Reset() { i = -1; }
    public void Dispose() {}
}
