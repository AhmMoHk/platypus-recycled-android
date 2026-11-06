package prr;

import cs.UnityEngine.*;

/** Headless desktop runner: boots the game, runs frames, reports exceptions and the scene state. */
public final class Harness {
    static int errors;

    public static void main(String[] a) throws Exception {
        final int frames = a.length > 0 ? Integer.parseInt(a[0]) : 300;
        Host.set(new Host() {
            @Override public void log(int type, String msg) {
                if (type == 0 || type == 4) errors++;
                if (type == 3 && !verbose) return;
                System.out.println(new String[]{"E", "A", "W", "I", "X"}[type] + " [" + Time.get_frameCount() + "] " + (msg.length() > 3000 ? msg.substring(0, 3000) : msg));
            }
            @Override public String dataPath() { return System.getProperty("prr.data"); }
            @Override public String persistentDataPath() { return System.getProperty("prr.save", "/tmp/prr-save"); }
            @Override public Object[] decodeImage(byte[] data) {
                try {
                    java.awt.image.BufferedImage im = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(data));
                    if (im == null) return null;
                    int w = im.getWidth(), h = im.getHeight();
                    return new Object[]{w, h, im.getRGB(0, 0, w, h, null, 0, w)};
                } catch (Exception e) { return null; }
            }
        });
        long t0 = System.nanoTime();
        Boot.start(0);
        System.out.println("boot " + (System.nanoTime() - t0) / 1000000 + " ms, errors " + errors);
        for (int i = 0; i < frames; i++) {
            Eng.frame(1f / 60);
            Render.renderFrame();
            Eng.endOfFrame();
            if (i % 60 == 59) status();
        }
        System.out.println("done, errors " + errors);
    }

    static boolean verbose = Boolean.getBoolean("prr.verbose");

    static void status() {
        cs.UnityEngine.SceneManagement.Scene s = cs.UnityEngine.SceneManagement.SceneManager.GetActiveScene();
        int roots = s.GetRootGameObjects().length;
        System.out.println("-- frame " + Time.get_frameCount() + " t=" + Time.get_time() + " scene=" + s.get_name() + " roots=" + roots + " errors=" + errors);
    }
}
