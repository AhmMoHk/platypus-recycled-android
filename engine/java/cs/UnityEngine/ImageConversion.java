package cs.UnityEngine;

public final class ImageConversion {
    private ImageConversion() {}
    public static boolean LoadImage(Texture2D t, byte[] data) { return LoadImage(t, data, false); }
    public static boolean LoadImage(Texture2D t, byte[] data, boolean markNonReadable) {
        Object[] im = Host.get().decodeImage(data);
        if (im == null) return false;
        t.$src = null;
        t.$compressed = null;
        t.setFromTopDown((Integer) im[0], (Integer) im[1], (int[]) im[2]);
        t.format = 4;
        if (markNonReadable) t.readable = false;
        return true;
    }
    public static byte[] EncodeToPNG(Texture2D t) { return Host.get().encodePNG(t.width, t.height, t.$topDown()); }
    public static byte[] EncodeToJPG(Texture2D t) { return EncodeToPNG(t); }
    public static byte[] EncodeToJPG(Texture2D t, int q) { return EncodeToPNG(t); }
}
