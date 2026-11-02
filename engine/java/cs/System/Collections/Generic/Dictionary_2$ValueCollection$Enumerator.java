package cs.System.Collections.Generic;

public class Dictionary_2$ValueCollection$Enumerator implements IEnumerator_1 {
    private final Dictionary_2 d;
    private final Object[] e;
    private final int ver;
    private int i = -1;
    Dictionary_2$ValueCollection$Enumerator(Dictionary_2 d) { this.d = d; e = d.entries(); ver = d.version; }
    public Dictionary_2$ValueCollection$Enumerator() { d = null; e = new Object[0]; ver = 0; }
    public boolean MoveNext() {
        if (d != null && ver != d.version) throw new cs.System.InvalidOperationException("Collection was modified; enumeration operation may not execute.");
        if (i + 1 < e.length / 2) { i++; return true; }
        i = e.length / 2;
        return false;
    }
    public Object get_Current() { return i >= 0 && i < e.length / 2 ? (d != null ? d.map.get(d.wrap(e[i * 2])) : e[i * 2 + 1]) : null; }
    public void Reset() { i = -1; }
    public void Dispose() {}
}
