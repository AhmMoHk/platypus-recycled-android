package cs.UnityEngine;

public final class ImageConversion {
    private ImageConversion() {}
    public static boolean LoadImage(Texture2D t, byte[] data) { return LoadImage(t, data, false); }
    public static boolean LoadImage(Texture2D t, byte[] data, boolean markNonReadable) {
        int[] wh = pngSize(data);
        t.$src = null;
        t.$compressed = null;
        t.$compressedSrc = null;
        t.$cpuDirty = false;
        t.format = 4;
        if (wh != null) {
            // PNG: keep the encoded bytes, decode lazily (pixel reads / first GPU upload)
            t.width = wh[0];
            t.height = wh[1];
            t.px = null;
            t.$encoded = data;
            t.$version++;
        } else {
            Object[] im = Host.get().decodeImage(data);
            if (im == null) return false;
            t.$encoded = null;
            t.setFromTopDown((Integer) im[0], (Integer) im[1], (int[]) im[2]);
        }
        if (markNonReadable) t.readable = false;
        return true;
    }

    /** width/height from a PNG IHDR chunk, or null */
    static int[] pngSize(byte[] b) {
        if (b == null || b.length < 24 || (b[0] & 0xff) != 0x89 || b[1] != 'P' || b[2] != 'N' || b[3] != 'G') return null;
        int w = ((b[16] & 0xff) << 24) | ((b[17] & 0xff) << 16) | ((b[18] & 0xff) << 8) | (b[19] & 0xff);
        int h = ((b[20] & 0xff) << 24) | ((b[21] & 0xff) << 16) | ((b[22] & 0xff) << 8) | (b[23] & 0xff);
        return w > 0 && h > 0 ? new int[]{w, h} : null;
    }
    public static byte[] EncodeToPNG(Texture2D t) { return Host.get().encodePNG(t.width, t.height, t.$topDown()); }
    public static byte[] EncodeToJPG(Texture2D t) { return EncodeToPNG(t); }
    public static byte[] EncodeToJPG(Texture2D t, int q) { return EncodeToPNG(t); }
}
