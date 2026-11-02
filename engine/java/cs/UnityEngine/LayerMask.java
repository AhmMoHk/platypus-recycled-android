package cs.UnityEngine;

public class LayerMask implements cs.rt.Struct {
    static final String[] names = new String[32];
    public int m_Mask;
    public LayerMask() {}
    public int get_value() { return m_Mask; }
    public void set_value(int v) { m_Mask = v; }
    public static LayerMask op_Implicit$LayerMask(int v) { LayerMask m = new LayerMask(); m.m_Mask = v; return m; }
    public static int op_Implicit$int_(LayerMask m) { return m.m_Mask; }
    public static int NameToLayer(String n) {
        for (int i = 0; i < 32; i++) if (n.equals(names[i])) return i;
        return -1;
    }
    public static String LayerToName(int l) { return l < 0 || l >= 32 || names[l] == null ? "" : names[l]; }
    public static int GetMask(String[] ns) {
        int m = 0;
        for (String n : ns) { int l = NameToLayer(n); if (l >= 0) m |= 1 << l; }
        return m;
    }
    public LayerMask $copy() { return op_Implicit$LayerMask(m_Mask); }
    public void $set(Object o) { m_Mask = ((LayerMask) o).m_Mask; }
    public void $clear() { m_Mask = 0; }
    public static LayerMask[] $newArray(int n) { LayerMask[] a = new LayerMask[n]; for (int i = 0; i < n; i++) a[i] = new LayerMask(); return a; }
    @Override public boolean equals(Object o) { return o instanceof LayerMask && ((LayerMask) o).m_Mask == m_Mask; }
    @Override public int hashCode() { return m_Mask; }
}
