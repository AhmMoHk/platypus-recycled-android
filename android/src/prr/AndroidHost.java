package prr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import cs.UnityEngine.Host;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/** Host services backed by the Android platform. */
public final class AndroidHost extends Host {
    final Context ctx;
    final File data, save;
    int w = 1920, h = 1080;
    float dpi = 160;
    MainActivity activity;

    AndroidHost(Context c) {
        ctx = c;
        data = new File(c.getFilesDir(), "data");     // internal storage: filled by DataImport from the zip parts
        save = new File(c.getFilesDir(), "save");
        data.mkdirs();
        save.mkdirs();
        android.util.DisplayMetrics dm = c.getResources().getDisplayMetrics();
        dpi = dm.densityDpi;
    }

    @Override public void log(int type, String msg) {
        switch (type) {
            case 0: case 4: Log.e("prr", msg); break;
            case 1: case 2: Log.w("prr", msg); break;
            default: Log.i("prr", msg);
        }
    }
    @Override public String dataPath() { return data.getAbsolutePath(); }
    @Override public String persistentDataPath() { return save.getAbsolutePath(); }
    @Override public int screenWidth() { return w; }
    @Override public int screenHeight() { return h; }
    @Override public float dpi() { return dpi; }
    @Override public boolean isMobile() { return true; }
    @Override public void openURL(String url) {
        try {
            ctx.startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (RuntimeException e) {
            log(2, "openURL failed: " + e);
        }
    }
    @Override public void quit() { if (activity != null) activity.finish(); }
    /** the port's own frame-rate setting wins over values the game sets */
    @Override public void setTargetFrameRate(int fps) { if (activity != null) activity.setTargetFps(Settings.fps()); }
    @Override public String clipboard() {
        android.content.ClipboardManager cm = (android.content.ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData d = cm == null ? null : cm.getPrimaryClip();
        return d == null || d.getItemCount() == 0 ? "" : String.valueOf(d.getItemAt(0).coerceToText(ctx));
    }
    @Override public void setClipboard(String s) {
        android.content.ClipboardManager cm = (android.content.ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(android.content.ClipData.newPlainText("prr", s));
    }

    @Override public Object[] decodeImage(byte[] bytes) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inPreferredConfig = Bitmap.Config.ARGB_8888;
        o.inScaled = false;
        Bitmap b = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, o);
        if (b == null) return null;
        int bw = b.getWidth(), bh = b.getHeight();
        int[] px = new int[bw * bh];
        b.getPixels(px, 0, bw, 0, 0, bw, bh);
        b.recycle();
        return new Object[]{bw, bh, px};
    }

    @Override public byte[] encodePNG(int width, int height, int[] argb) {
        Bitmap b = Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888);
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.PNG, 100, bo);
        b.recycle();
        return bo.toByteArray();
    }

    @Override public byte[] readData(String rel) {
        File f = new File(data, rel);
        if (!f.exists()) return null;
        try {
            byte[] b = new byte[(int) f.length()];
            FileInputStream in = new FileInputStream(f);
            try {
                int off = 0, n;
                while (off < b.length && (n = in.read(b, off, b.length - off)) > 0) off += n;
            } finally { in.close(); }
            return b;
        } catch (java.io.IOException e) {
            return null;
        }
    }

    /** OGG/MP3/WAV via MediaCodec (see AudioDecode) */
    @Override public Object[] decodeAudio(byte[] bytes) { return AudioDecode.decode(bytes); }
    @Override public int audioRate() { return 48000; }

    byte[] asset(String name) {
        try {
            InputStream in = ctx.getAssets().open(name);
            try {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                byte[] buf = new byte[65536];
                int n;
                while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
                return bo.toByteArray();
            } finally { in.close(); }
        } catch (java.io.IOException e) {
            log(2, "asset missing: " + name);
            return null;
        }
    }
}
