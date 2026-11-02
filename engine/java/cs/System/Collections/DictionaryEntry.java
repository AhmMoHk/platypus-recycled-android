package cs.System.Collections;

public class DictionaryEntry implements cs.rt.Struct {
    public Object _key, _value;
    public DictionaryEntry() {}
    public DictionaryEntry(Object k, Object v) { _key = k; _value = v; }
    public Object get_Key() { return _key; }
    public Object get_Value() { return _value; }
    public DictionaryEntry $copy() { return new DictionaryEntry(_key, _value); }
    public void $set(Object o) { DictionaryEntry d = (DictionaryEntry) o; _key = d._key; _value = d._value; }
    public void $clear() { _key = _value = null; }
    public static DictionaryEntry[] $newArray(int n) { DictionaryEntry[] a = new DictionaryEntry[n]; for (int i = 0; i < n; i++) a[i] = new DictionaryEntry(); return a; }
}
