package cs.System.Text;

public class UTF8Encoding extends Encoding {
    public UTF8Encoding() { super(java.nio.charset.Charset.forName("UTF-8")); }
    public UTF8Encoding(boolean bom) { super(java.nio.charset.Charset.forName("UTF-8")); }
    public UTF8Encoding(boolean bom, boolean thr) { super(java.nio.charset.Charset.forName("UTF-8")); }
}
