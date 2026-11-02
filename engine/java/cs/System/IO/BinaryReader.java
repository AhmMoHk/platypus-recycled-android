package cs.System.IO;

public class BinaryReader implements cs.System.IDisposable {
    private Stream s;
    public BinaryReader() {}
    public BinaryReader(Stream s) { this.s = s; }
    public BinaryReader(Stream s, cs.System.Text.Encoding e) { this.s = s; }
    public void $init(Stream st) { s = st; }
    public void $init(Stream st, cs.System.Text.Encoding e) { s = st; }
    private int b() { int v = s.ReadByte(); if (v < 0) throw new EndOfStreamException("Unable to read beyond the end of the stream."); return v; }
    public boolean ReadBoolean() { return b() != 0; }
    public byte ReadByte() { return (byte) b(); }
    public byte ReadSByte() { return (byte) b(); }
    public short ReadInt16() { return (short) (b() | b() << 8); }
    public char ReadUInt16() { return (char) (b() | b() << 8); }
    public int ReadInt32() { return b() | b() << 8 | b() << 16 | b() << 24; }
    public int ReadUInt32() { return ReadInt32(); }
    public long ReadInt64() { long lo = ReadInt32() & 0xffffffffL; long hi = ReadInt32() & 0xffffffffL; return lo | hi << 32; }
    public long ReadUInt64() { return ReadInt64(); }
    public float ReadSingle() { return Float.intBitsToFloat(ReadInt32()); }
    public double ReadDouble() { return Double.longBitsToDouble(ReadInt64()); }
    public byte[] ReadBytes(int n) { byte[] d = new byte[n]; int k = s.Read(d, 0, n); return k == n ? d : java.util.Arrays.copyOf(d, k); }
    public String ReadString() {
        int len = 0, shift = 0, v;
        do { v = b(); len |= (v & 0x7f) << shift; shift += 7; } while ((v & 0x80) != 0);
        byte[] d = ReadBytes(len);
        return new String(d, File.UTF8);
    }
    public void Dispose() { s.Dispose(); }
}
