package cs.System.IO;

public class StreamReader extends TextReader {
    private String s;
    private int pos;
    private Stream stream;
    public StreamReader(String contentOrPath) { this(contentOrPath, true); }
    StreamReader(String content, boolean isContent) { s = content; }
    public StreamReader(Stream st) { stream = st; }
    public StreamReader(Stream st, cs.System.Text.Encoding e) { stream = st; }
    public StreamReader(Stream st, boolean detect) { stream = st; }
    public static StreamReader ofPath(String p) { return new StreamReader(File.ReadAllText(p), true); }
    private void load() {
        if (s != null) return;
        MemoryStream m = new MemoryStream();
        stream.CopyTo(m);
        s = File.decode(m.ToArray());
    }
    public int Read() { load(); return pos < s.length() ? s.charAt(pos++) : -1; }
    public int Peek() { load(); return pos < s.length() ? s.charAt(pos) : -1; }
    public boolean get_EndOfStream() { load(); return pos >= s.length(); }
    public Stream get_BaseStream() { return stream; }
    public void Dispose() { if (stream != null) stream.Dispose(); }
}
