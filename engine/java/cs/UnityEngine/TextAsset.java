package cs.UnityEngine;

public class TextAsset extends UObject {
    byte[] bytes;
    String text;
    public TextAsset() { bytes = new byte[0]; }
    public TextAsset(String t) { text = t; bytes = null; }
    public static TextAsset $of(String name, byte[] data) { TextAsset t = new TextAsset(); t.m_Name = name; t.bytes = data; return t; }
    public String get_text() {
        if (text == null) {
            try {
                int off = bytes.length >= 3 && (bytes[0] & 0xff) == 0xEF && (bytes[1] & 0xff) == 0xBB && (bytes[2] & 0xff) == 0xBF ? 3 : 0;
                text = new String(bytes, off, bytes.length - off, "UTF-8");
            } catch (java.io.UnsupportedEncodingException e) { text = ""; }
        }
        return text;
    }
    public byte[] get_bytes() {
        if (bytes == null) {
            try { bytes = text.getBytes("UTF-8"); } catch (java.io.UnsupportedEncodingException e) { bytes = new byte[0]; }
        }
        return bytes;
    }
    public long get_dataSize() { return get_bytes().length; }
    @Override public String toString() { return get_text(); }
    @Override public void $loadAsset(java.util.HashMap<String, Object> d, Loader.Ctx ctx) {
        Object s = d.get("m_Script");
        if (s instanceof String) { text = (String) s; bytes = null; }
    }
}
