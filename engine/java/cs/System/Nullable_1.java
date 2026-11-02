package cs.System;

public class Nullable_1 implements cs.rt.Struct {
    public Object value;
    public boolean hasValue;
    public Nullable_1() {}
    public Nullable_1(Object v) { value = v; hasValue = true; }
    public boolean get_HasValue() { return hasValue; }
    public Object get_Value() { if (!hasValue) throw new InvalidOperationException("Nullable object must have a value."); return value; }
    public Object GetValueOrDefault() { return value; }
    public Object GetValueOrDefault(Object d) { return hasValue ? value : d; }
    public Nullable_1 $copy() { Nullable_1 n = new Nullable_1(); n.value = value; n.hasValue = hasValue; return n; }
    public void $set(Object o) { Nullable_1 n = (Nullable_1) o; value = n.value; hasValue = n.hasValue; }
    public void $clear() { value = null; hasValue = false; }
    public static Nullable_1[] $newArray(int n) { Nullable_1[] a = new Nullable_1[n]; for (int i = 0; i < n; i++) a[i] = new Nullable_1(); return a; }
    @Override public boolean equals(Object o) { if (!(o instanceof Nullable_1)) return !hasValue && o == null || hasValue && ObjectX.Equals(value, o); Nullable_1 n = (Nullable_1) o; return n.hasValue == hasValue && ObjectX.Equals(value, n.value); }
    @Override public int hashCode() { return hasValue ? ObjectX.hash(value) : 0; }
    @Override public String toString() { return hasValue ? ObjectX.ToString(value) : ""; }
}
