package cs.System.Text.RegularExpressions;

public class Capture {
    final int index, length;
    final String value;
    Capture(int i, int l, String v) { index = i; length = l; value = v == null ? "" : v; }
    public int get_Index() { return index; }
    public int get_Length() { return length; }
    public String get_Value() { return value; }
    @Override public String toString() { return value; }
    public String ToString() { return value; }
}
