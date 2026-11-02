package cs.System.Text;

public class StringBuilder {
    private final java.lang.StringBuilder sb;
    public StringBuilder() { sb = new java.lang.StringBuilder(); }
    public StringBuilder(int cap) { sb = new java.lang.StringBuilder(cap); }
    public StringBuilder(String s) { sb = new java.lang.StringBuilder(s == null ? "" : s); }
    public StringBuilder(String s, int cap) { sb = new java.lang.StringBuilder(s == null ? "" : s); }
    public StringBuilder Append(String s) { if (s != null) sb.append(s); return this; }
    public StringBuilder Append(char c) { sb.append(c); return this; }
    public StringBuilder Append(char c, int n) { for (int i = 0; i < n; i++) sb.append(c); return this; }
    public StringBuilder Append(char[] c) { if (c != null) sb.append(c); return this; }
    public StringBuilder Append(char[] c, int o, int n) { sb.append(c, o, n); return this; }
    public StringBuilder Append(String s, int o, int n) { sb.append(s, o, o + n); return this; }
    public StringBuilder Append(int v) { sb.append(v); return this; }
    public StringBuilder Append(long v) { sb.append(v); return this; }
    public StringBuilder Append(float v) { sb.append(cs.rt.Fmt.str(v)); return this; }
    public StringBuilder Append(double v) { sb.append(cs.rt.Fmt.str(v)); return this; }
    public StringBuilder Append(boolean v) { sb.append(v ? "True" : "False"); return this; }
    public StringBuilder Append(Object o) { if (o != null) sb.append(cs.System.ObjectX.ToString(o)); return this; }
    public StringBuilder Append(StringBuilder o) { if (o != null) sb.append(o.sb); return this; }
    public StringBuilder AppendLine() { sb.append('\n'); return this; }
    public StringBuilder AppendLine(String s) { if (s != null) sb.append(s); sb.append('\n'); return this; }
    public StringBuilder AppendFormat(String f, Object a) { sb.append(cs.rt.Fmt.compose(f, new Object[]{a})); return this; }
    public StringBuilder AppendFormat(String f, Object a, Object b) { sb.append(cs.rt.Fmt.compose(f, new Object[]{a, b})); return this; }
    public StringBuilder AppendFormat(String f, Object a, Object b, Object c) { sb.append(cs.rt.Fmt.compose(f, new Object[]{a, b, c})); return this; }
    public StringBuilder AppendFormat(String f, Object[] a) { sb.append(cs.rt.Fmt.compose(f, a)); return this; }
    public StringBuilder Insert(int i, String s) { sb.insert(i, s); return this; }
    public StringBuilder Insert(int i, char c) { sb.insert(i, c); return this; }
    public StringBuilder Remove(int i, int n) { sb.delete(i, i + n); return this; }
    public StringBuilder Replace(String a, String b) {
        String s = sb.toString().replace(a, b == null ? "" : b);
        sb.setLength(0);
        sb.append(s);
        return this;
    }
    public StringBuilder Replace(char a, char b) { for (int i = 0; i < sb.length(); i++) if (sb.charAt(i) == a) sb.setCharAt(i, b); return this; }
    public StringBuilder Clear() { sb.setLength(0); return this; }
    public char get_Chars(int i) { return sb.charAt(i); }
    public void set_Chars(int i, char c) { sb.setCharAt(i, c); }
    public int get_Length() { return sb.length(); }
    public void set_Length(int n) { sb.setLength(n); }
    public int get_Capacity() { return sb.capacity(); }
    public String ToString(int s, int n) { return sb.substring(s, s + n); }
    @Override public String toString() { return sb.toString(); }
}
