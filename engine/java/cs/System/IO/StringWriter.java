package cs.System.IO;

public class StringWriter extends TextWriter {
    private final StringBuilder sb = new StringBuilder();
    public StringWriter() {}
    public void Write(String s) { if (s != null) sb.append(s); }
    @Override public String toString() { return sb.toString(); }
}
