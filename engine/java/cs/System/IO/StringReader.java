package cs.System.IO;

public class StringReader extends TextReader {
    private final String s;
    private int pos;
    public StringReader(String s) { if (s == null) throw new cs.System.ArgumentNullException("s"); this.s = s; }
    public int Read() { return pos < s.length() ? s.charAt(pos++) : -1; }
    public int Peek() { return pos < s.length() ? s.charAt(pos) : -1; }
    @Override public String ReadToEnd() { String r = s.substring(pos); pos = s.length(); return r; }
}
