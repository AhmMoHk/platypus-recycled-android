package cs.System.Collections.Generic;

/** Immutable key/value pair (a .NET struct used by reference here: it is never mutated). */
public final class KeyValuePair_2 {
    public final Object key, value;
    public KeyValuePair_2() { key = null; value = null; }
    public KeyValuePair_2(Object k, Object v) { key = k; value = v; }
    public Object get_Key() { return key; }
    public Object get_Value() { return value; }
    public void Deconstruct(cs.rt.RefO k, cs.rt.RefO v) { k.v = key; v.v = value; }
    @Override public String toString() { return "[" + (key == null ? "" : cs.System.ObjectX.ToString(key)) + ", " + (value == null ? "" : cs.System.ObjectX.ToString(value)) + "]"; }
    @Override public boolean equals(Object o) {
        if (!(o instanceof KeyValuePair_2)) return false;
        KeyValuePair_2 p = (KeyValuePair_2) o;
        return cs.System.ObjectX.Equals(key, p.key) && cs.System.ObjectX.Equals(value, p.value);
    }
    @Override public int hashCode() { return cs.System.ObjectX.hash(key) * 31 + cs.System.ObjectX.hash(value); }
}
