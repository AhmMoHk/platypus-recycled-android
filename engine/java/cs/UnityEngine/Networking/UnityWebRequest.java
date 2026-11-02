package cs.UnityEngine.Networking;

/** Local file requests only (file:// URIs or paths); Result: InProgress 0, Success 1, ConnectionError 2, ProtocolError 3, DataProcessingError 4 */
public class UnityWebRequest implements cs.System.IDisposable {
    String url, error;
    int result;
    boolean done;
    DownloadHandler handler;
    public UnityWebRequest() {}
    public UnityWebRequest(String url) { this.url = url; handler = new DownloadHandler(); }
    public static UnityWebRequest Get(String url) { return new UnityWebRequest(url); }
    public UnityWebRequestAsyncOperation SendWebRequest() {
        String p = url;
        if (p.startsWith("file://")) p = p.substring(7);
        p = java.net.URLDecoder.decode(p.replace("+", "%2B"));
        java.io.File f = new java.io.File(p);
        if (!f.exists()) {
            result = 2;
            error = "Cannot connect to destination host";
        } else {
            try {
                byte[] b = new byte[(int) f.length()];
                java.io.FileInputStream in = new java.io.FileInputStream(f);
                try { int off = 0, n; while (off < b.length && (n = in.read(b, off, b.length - off)) > 0) off += n; } finally { in.close(); }
                handler.data = b;
                if (handler instanceof DownloadHandlerAudioClip) {
                    cs.UnityEngine.AudioClip c = new cs.UnityEngine.AudioClip();
                    c.m_Name = f.getName();
                    c.$bytes = b;
                    ((DownloadHandlerAudioClip) handler).clip = c;
                }
                result = 1;
            } catch (java.io.IOException e) {
                result = 4;
                error = e.toString();
            }
        }
        done = true;
        UnityWebRequestAsyncOperation op = new UnityWebRequestAsyncOperation();
        op.req = this;
        op.$finish();
        return op;
    }
    public String get_url() { return url; }
    public void set_url(String u) { url = u; }
    public String get_error() { return error; }
    public boolean get_isDone() { return done; }
    public int get_result() { return result; }
    public long get_responseCode() { return result == 1 ? 200 : 0; }
    public boolean get_isNetworkError() { return result == 2; }
    public boolean get_isHttpError() { return result == 3; }
    public DownloadHandler get_downloadHandler() { return handler; }
    public void set_downloadHandler(DownloadHandler h) { handler = h; }
    public float get_downloadProgress() { return done ? 1 : 0; }
    public void Dispose() {}
    public void Abort() {}
}
