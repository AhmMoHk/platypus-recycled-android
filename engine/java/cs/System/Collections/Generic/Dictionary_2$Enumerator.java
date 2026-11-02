package cs.System.Collections.Generic;

public class Dictionary_2$Enumerator implements IEnumerator_1, cs.System.Collections.IDictionaryEnumerator {
    private final Dictionary_2 d;
    private final Object[] e;
    private final int ver;
    private int i = -1;

    Dictionary_2$Enumerator(Dictionary_2 d) { this.d = d; this.e = d.entries(); this.ver = d.version; }
    public Dictionary_2$Enumerator() { d = null; e = new Object[0]; ver = 0; }

    public boolean MoveNext() {
        if (d != null && ver != d.version) throw new cs.System.InvalidOperationException("Collection was modified; enumeration operation may not execute.");
        if (i + 1 < e.length / 2) { i++; return true; }
        i = e.length / 2;
        return false;
    }
    public KeyValuePair_2 get_Current() {
        if (i < 0 || i >= e.length / 2) return new KeyValuePair_2();
        return new KeyValuePair_2(e[i * 2], value());
    }
    private Object value() { return d != null ? d.map.get(d.wrap(e[i * 2])) : e[i * 2 + 1]; }
    public Object get_Key() { return e[i * 2]; }
    public Object get_Value() { return value(); }
    public cs.System.Collections.DictionaryEntry get_Entry() { return new cs.System.Collections.DictionaryEntry(e[i * 2], value()); }
    public void Reset() { i = -1; }
    public void Dispose() {}
}
