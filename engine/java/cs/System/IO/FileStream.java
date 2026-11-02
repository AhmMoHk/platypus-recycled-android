package cs.System.IO;

/** FileMode: 1 CreateNew 2 Create 3 Open 4 OpenOrCreate 5 Truncate 6 Append; FileAccess: 1 Read 2 Write 3 ReadWrite */
public class FileStream extends Stream {
    private java.io.RandomAccessFile raf;
    private final boolean write;

    public FileStream(String path, int mode) { this(path, mode, mode == 3 ? 1 : 3, 1); }
    public FileStream(String path, int mode, int access) { this(path, mode, access, 1); }
    public FileStream(String path, int mode, int access, int share) {
        write = (access & 2) != 0;
        java.io.File f = File.f(path);
        try {
            if (mode == 3 && !f.exists()) throw new FileNotFoundException("Could not find file '" + path + "'");
            raf = new java.io.RandomAccessFile(f, write ? "rw" : "r");
            if (mode == 2 || mode == 5) raf.setLength(0);
            if (mode == 6) raf.seek(raf.length());
        } catch (java.io.IOException e) {
            throw new IOException(e.toString());
        }
    }
    public boolean get_CanWrite() { return write; }
    public boolean get_CanSeek() { return true; }
    public long get_Length() { try { return raf.length(); } catch (java.io.IOException e) { throw new IOException(e.toString()); } }
    public long get_Position() { try { return raf.getFilePointer(); } catch (java.io.IOException e) { throw new IOException(e.toString()); } }
    public int Read(byte[] b, int off, int n) {
        try { int r = raf.read(b, off, n); return r < 0 ? 0 : r; } catch (java.io.IOException e) { throw new IOException(e.toString()); }
    }
    public void Write(byte[] b, int off, int n) { try { raf.write(b, off, n); } catch (java.io.IOException e) { throw new IOException(e.toString()); } }
    public long Seek(long off, int origin) {
        try {
            long base = origin == 0 ? 0 : origin == 1 ? raf.getFilePointer() : raf.length();
            raf.seek(base + off);
            return raf.getFilePointer();
        } catch (java.io.IOException e) { throw new IOException(e.toString()); }
    }
    public void SetLength(long n) { try { raf.setLength(n); } catch (java.io.IOException e) { throw new IOException(e.toString()); } }
    public void Dispose() { try { if (raf != null) raf.close(); } catch (java.io.IOException ignored) {} raf = null; }
}
