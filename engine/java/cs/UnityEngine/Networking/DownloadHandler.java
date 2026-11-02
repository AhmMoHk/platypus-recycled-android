package cs.UnityEngine.Networking;

public class DownloadHandler implements cs.System.IDisposable {
    byte[] data;
    public byte[] get_data() { return data; }
    public String get_text() { try { return data == null ? null : new String(data, "UTF-8"); } catch (java.io.UnsupportedEncodingException e) { return null; } }
    public boolean get_isDone() { return true; }
    public void Dispose() {}
}
