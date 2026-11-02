package cs.System.IO;

public abstract class Stream implements cs.System.IDisposable {
    public boolean get_CanRead() { return true; }
    public boolean get_CanWrite() { return false; }
    public boolean get_CanSeek() { return false; }
    public boolean get_CanTimeout() { return false; }
    public long get_Length() { throw new cs.System.NotSupportedException(); }
    public long get_Position() { throw new cs.System.NotSupportedException(); }
    public void set_Position(long p) { Seek(p, 0); }
    public int get_ReadTimeout() { return -1; }
    public int get_WriteTimeout() { return -1; }
    public void set_ReadTimeout(int t) {}
    public void set_WriteTimeout(int t) {}
    public abstract int Read(byte[] b, int off, int n);
    public void Write(byte[] b, int off, int n) { throw new cs.System.NotSupportedException(); }
    public int ReadByte() { byte[] b = new byte[1]; return Read(b, 0, 1) == 1 ? b[0] & 255 : -1; }
    public void WriteByte(byte v) { Write(new byte[]{v}, 0, 1); }
    public long Seek(long off, int origin) { throw new cs.System.NotSupportedException(); }
    public void SetLength(long n) { throw new cs.System.NotSupportedException(); }
    public void Flush() {}
    public void Close() { Dispose(); }
    public void Dispose() {}
    public void CopyTo(Stream dst) {
        byte[] buf = new byte[65536];
        int n;
        while ((n = Read(buf, 0, buf.length)) > 0) dst.Write(buf, 0, n);
    }
    public cs.System.IAsyncResult BeginRead(byte[] b, int o, int n, cs.System.AsyncCallback cb, Object st) { throw new cs.System.NotSupportedException(); }
    public cs.System.IAsyncResult BeginWrite(byte[] b, int o, int n, cs.System.AsyncCallback cb, Object st) { throw new cs.System.NotSupportedException(); }
    public int EndRead(cs.System.IAsyncResult r) { throw new cs.System.NotSupportedException(); }
    public void EndWrite(cs.System.IAsyncResult r) { throw new cs.System.NotSupportedException(); }
}
