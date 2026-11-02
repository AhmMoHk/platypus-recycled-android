package cs.System.IO;

public class MemoryStream extends Stream {
    private byte[] buf;
    private int len, pos;
    public MemoryStream() { buf = new byte[256]; }
    public MemoryStream(int cap) { buf = new byte[java.lang.Math.max(cap, 16)]; }
    public MemoryStream(byte[] d) { buf = d; len = d.length; }
    public boolean get_CanWrite() { return true; }
    public boolean get_CanSeek() { return true; }
    public long get_Length() { return len; }
    public long get_Position() { return pos; }
    public void set_Position(long p) { pos = (int) p; }
    public int Read(byte[] b, int off, int n) {
        int k = java.lang.Math.max(0, java.lang.Math.min(n, len - pos));
        System.arraycopy(buf, pos, b, off, k);
        pos += k;
        return k;
    }
    public void Write(byte[] b, int off, int n) {
        if (pos + n > buf.length) buf = java.util.Arrays.copyOf(buf, java.lang.Math.max(buf.length * 2, pos + n));
        System.arraycopy(b, off, buf, pos, n);
        pos += n;
        if (pos > len) len = pos;
    }
    public long Seek(long off, int origin) { pos = (int) ((origin == 0 ? 0 : origin == 1 ? pos : len) + off); return pos; }
    public void SetLength(long n) { if (n > buf.length) buf = java.util.Arrays.copyOf(buf, (int) n); len = (int) n; }
    public byte[] ToArray() { return java.util.Arrays.copyOf(buf, len); }
    public byte[] GetBuffer() { return buf; }
}
