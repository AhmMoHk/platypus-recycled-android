package cs.UnityEngine;

/** Services provided by the host (Android activity / desktop harness). */
public abstract class Host {
    private static Host instance = new Host() {};
    public static Host get() { return instance; }
    public static void set(Host h) { instance = h; }

    /** LogType: Error 0, Assert 1, Warning 2, Log 3, Exception 4 */
    public void log(int type, String msg) {
        (type == 3 ? System.out : System.err).println(new String[]{"E", "A", "W", "I", "X"}[type] + " " + msg);
    }
    /** root directory of the converted game data */
    public String dataPath() { return System.getProperty("prr.data", "data"); }
    /** writable directory for saves */
    public String persistentDataPath() { return System.getProperty("prr.save", "save"); }
    public int screenWidth() { return 1920; }
    public int screenHeight() { return 1080; }
    public float dpi() { return 160f; }
    public void openURL(String url) {}
    public void quit() {}
    public void setTargetFrameRate(int fps) {}
    /** the clipboard */
    public String clipboard() { return ""; }
    public void setClipboard(String s) {}
    public boolean isMobile() { return false; }
    /** decode an image file (PNG/JPG) to {width, height, argb pixels top-down}; null on failure (hosts override) */
    public Object[] decodeImage(byte[] data) { return null; }
    /** encode argb pixels (top-down) as PNG (hosts override) */
    public byte[] encodePNG(int w, int h, int[] argb) { return new byte[0]; }
    /** decode compressed audio (OGG Vorbis etc.) to {channels, sampleRate, short[] interleaved pcm}; null if unsupported */
    public Object[] decodeAudio(byte[] data) { return null; }
    /** output sample rate of the audio device */
    public int audioRate() { return 48000; }
    /** read a data file relative to the data root */
    public byte[] readData(String rel) {
        try {
            java.io.File f = new java.io.File(dataPath(), rel);
            if (!f.exists()) return null;
            java.io.FileInputStream in = new java.io.FileInputStream(f);
            try {
                byte[] b = new byte[(int) f.length()];
                int off = 0, n;
                while (off < b.length && (n = in.read(b, off, b.length - off)) > 0) off += n;
                return b;
            } finally { in.close(); }
        } catch (java.io.IOException e) {
            return null;
        }
    }
}
