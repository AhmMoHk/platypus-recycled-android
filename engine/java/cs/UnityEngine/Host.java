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
    /** decode an image file (PNG/JPG) to {width, height, argb pixels top-down}; null on failure */
    public Object[] decodeImage(byte[] data) {
        try {
            java.awt.image.BufferedImage im = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(data));
            if (im == null) return null;
            int w = im.getWidth(), h = im.getHeight();
            int[] px = im.getRGB(0, 0, w, h, null, 0, w);
            return new Object[]{w, h, px};
        } catch (Throwable e) {
            return null;
        }
    }
    /** encode argb pixels (top-down) as PNG */
    public byte[] encodePNG(int w, int h, int[] argb) {
        try {
            java.awt.image.BufferedImage im = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            im.setRGB(0, 0, w, h, argb, 0, w);
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(im, "png", bo);
            return bo.toByteArray();
        } catch (Throwable e) {
            return new byte[0];
        }
    }
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
