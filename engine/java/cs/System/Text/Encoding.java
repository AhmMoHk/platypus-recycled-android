package cs.System.Text;

public class Encoding {
    protected final java.nio.charset.Charset cs;
    private static final Encoding UTF8 = new UTF8Encoding(false);
    private static final Encoding ASCII = new Encoding(java.nio.charset.Charset.forName("US-ASCII"));
    private static final Encoding UNICODE = new Encoding(java.nio.charset.Charset.forName("UTF-16LE"));
    protected Encoding(java.nio.charset.Charset c) { cs = c; }
    protected Encoding() { cs = java.nio.charset.Charset.forName("ISO-8859-1"); }
    public static Encoding get_UTF8() { return UTF8; }
    public static Encoding get_ASCII() { return ASCII; }
    public static Encoding get_Unicode() { return UNICODE; }
    public static Encoding get_Default() { return UTF8; }
    public static Encoding GetEncoding(String n) { return new Encoding(java.nio.charset.Charset.forName(n)); }
    public static Encoding GetEncoding(int cp) { return cp == 65001 ? UTF8 : cp == 1200 ? UNICODE : new Encoding(java.nio.charset.Charset.forName("ISO-8859-1")); }
    public byte[] GetBytes(String s) { return s.getBytes(cs); }
    public byte[] GetBytes(char[] c) { return new String(c).getBytes(cs); }
    public String GetString(byte[] d) { return new String(d, cs); }
    public String GetString(byte[] d, int off, int n) { return new String(d, off, n, cs); }
    public int GetByteCount(String s) { return GetBytes(s).length; }
    public char[] GetChars(byte[] d) { return GetString(d).toCharArray(); }
}
