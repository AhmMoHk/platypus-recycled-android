package cs.System;

public class Guid implements cs.rt.Struct {
    public byte[] b = new byte[16];
    public static Guid Empty = new Guid();
    public Guid() {}
    public Guid(byte[] d) { System.arraycopy(d, 0, b, 0, 16); }
    public Guid(String s) {
        String h = s.replace("-", "").replace("{", "").replace("}", "");
        // .NET byte order: first three groups little-endian
        byte[] raw = new byte[16];
        for (int i = 0; i < 16; i++) raw[i] = (byte) Integer.parseInt(h.substring(i * 2, i * 2 + 2), 16);
        b = new byte[]{raw[3], raw[2], raw[1], raw[0], raw[5], raw[4], raw[7], raw[6], raw[8], raw[9], raw[10], raw[11], raw[12], raw[13], raw[14], raw[15]};
    }
    public static Guid NewGuid() {
        Guid g = new Guid();
        new java.security.SecureRandom().nextBytes(g.b);
        g.b[7] = (byte) ((g.b[7] & 0x0f) | 0x40);
        g.b[8] = (byte) ((g.b[8] & 0x3f) | 0x80);
        return g;
    }
    public byte[] ToByteArray() { return b.clone(); }
    private static String hx(int v) { String s = Integer.toHexString(v & 255); return s.length() == 1 ? "0" + s : s; }
    @Override public String toString() { return ToString("D"); }
    public String ToString(String f) {
        StringBuilder sb = new StringBuilder();
        int[] order = {3, 2, 1, 0, -1, 5, 4, -1, 7, 6, -1, 8, 9, -1, 10, 11, 12, 13, 14, 15};
        boolean dashes = f == null || f.isEmpty() || f.equalsIgnoreCase("D") || f.equalsIgnoreCase("B");
        for (int i : order) { if (i < 0) { if (dashes) sb.append('-'); } else sb.append(hx(b[i])); }
        return f != null && f.equalsIgnoreCase("B") ? "{" + sb + "}" : sb.toString();
    }
    public static boolean op_Equality(Guid a, Guid c) { return java.util.Arrays.equals(a.b, c.b); }
    public static boolean op_Inequality(Guid a, Guid c) { return !op_Equality(a, c); }
    @Override public boolean equals(Object o) { return o instanceof Guid && java.util.Arrays.equals(b, ((Guid) o).b); }
    @Override public int hashCode() { return java.util.Arrays.hashCode(b); }
    public Guid $copy() { return new Guid(b); }
    public void $set(Object o) { System.arraycopy(((Guid) o).b, 0, b, 0, 16); }
    public void $clear() { java.util.Arrays.fill(b, (byte) 0); }
    public static Guid[] $newArray(int n) { Guid[] a = new Guid[n]; for (int i = 0; i < n; i++) a[i] = new Guid(); return a; }
}
