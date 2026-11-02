package cs.System.IO;

public class StreamWriter extends TextWriter {
    private final Stream stream;
    private boolean autoFlush;
    private final StringBuilder pending = new StringBuilder();
    public StreamWriter(Stream st) { stream = st; }
    public StreamWriter(Stream st, cs.System.Text.Encoding e) { stream = st; }
    public StreamWriter(String path) { stream = new FileStream(path, 2, 2, 0); }
    public StreamWriter(String path, boolean append) { stream = new FileStream(path, append ? 6 : 2, 2, 0); }
    public void set_AutoFlush(boolean v) { autoFlush = v; }
    public boolean get_AutoFlush() { return autoFlush; }
    public void Write(String s) {
        if (s == null) return;
        pending.append(s);
        if (autoFlush) Flush();
    }
    public void Flush() {
        if (pending.length() == 0) return;
        byte[] b = pending.toString().getBytes(File.UTF8);
        pending.setLength(0);
        stream.Write(b, 0, b.length);
        stream.Flush();
    }
    public void Dispose() { Flush(); stream.Dispose(); }
}
