package cs.System.Collections.Generic;

/** List<T>.Enumerator */
public class List_1$Enumerator implements IEnumerator_1 {
    private final List_1 list;
    private int index = -1;
    private final int ver;
    private Object current;

    List_1$Enumerator(List_1 l) { list = l; ver = l.version; }
    public List_1$Enumerator() { list = null; ver = 0; }

    public boolean MoveNext() {
        if (list == null) return false;
        if (ver != list.version) throw new cs.System.InvalidOperationException("Collection was modified; enumeration operation may not execute.");
        index++;
        if (index < list.size) { current = list.items[index]; return true; }
        current = null;
        index = list.size;
        return false;
    }
    public Object get_Current() { return current; }
    public void Reset() { index = -1; current = null; }
    public void Dispose() {}
}
