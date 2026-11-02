package cs.System.IO;

public class BinaryWriter implements cs.System.IDisposable {
    private Stream s;
    public BinaryWriter() {}
    public BinaryWriter(Stream s) { this.s = s; }
    public void $init(Stream st) { s = st; }
    public void $init(Stream st, cs.System.Text.Encoding e) { s = st; }
    private void b(int v) { s.WriteByte((byte) v); }
    public void Write(boolean v) { b(v ? 1 : 0); }
    public void Write(byte v) { b(v); }
    public void Write(char v) { b(v); b(v >> 8); }
    public void Write(short v) { b(v); b(v >> 8); }
    public void Write(int v) { b(v); b(v >> 8); b(v >> 16); b(v >> 24); }
    public void Write(long v) { Write((int) v); Write((int) (v >> 32)); }
    public void Write(float v) { Write(Float.floatToIntBits(v)); }
    public void Write(double v) { Write(Double.doubleToLongBits(v)); }
    public void Write(byte[] d) { s.Write(d, 0, d.length); }
    public void Write(String v) {
        byte[] d = v.getBytes(File.UTF8);
        int n = d.length;
        while (n >= 0x80) { b((n & 0x7f) | 0x80); n >>>= 7; }
        b(n);
        s.Write(d, 0, d.length);
    }
    public void Flush() { s.Flush(); }
    public void Dispose() { s.Dispose(); }
}
