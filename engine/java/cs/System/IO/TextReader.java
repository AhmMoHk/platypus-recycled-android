package cs.System.IO;

public abstract class TextReader implements cs.System.IDisposable {
    public abstract int Read();
    public abstract int Peek();
    public int Read(char[] b, int off, int n) {
        int k = 0;
        while (k < n) { int c = Read(); if (c < 0) break; b[off + k++] = (char) c; }
        return k;
    }
    public int ReadBlock(char[] b, int off, int n) { return Read(b, off, n); }
    public String ReadLine() {
        StringBuilder sb = new StringBuilder();
        int c = Read();
        if (c < 0) return null;
        while (c >= 0 && c != '\n' && c != '\r') { sb.append((char) c); c = Read(); }
        if (c == '\r' && Peek() == '\n') Read();
        return sb.toString();
    }
    public String ReadToEnd() {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = Read()) >= 0) sb.append((char) c);
        return sb.toString();
    }
    public void Close() { Dispose(); }
    public void Dispose() {}
}
