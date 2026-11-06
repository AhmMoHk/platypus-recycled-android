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
            @Override public boolean isMobile() { return Boolean.getBoolean("prr.mobile"); }
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
        int sw = Integer.getInteger("prr.w", 1280), sh = Integer.getInteger("prr.h", 720);
        String shots = System.getProperty("prr.shots", "");
        java.util.HashSet<Integer> shotFrames = new java.util.HashSet<>();
        for (String x : shots.split(",")) if (!x.trim().isEmpty()) shotFrames.add(Integer.parseInt(x.trim()));
        SoftGfx soft = null;
        if (!shotFrames.isEmpty()) {
            soft = new SoftGfx(System.getProperty("prr.astcenc", "astcenc-avx2"), System.getProperty("prr.data"));
            Gfx.set(soft);
        }
        long t0 = System.nanoTime();
        Screen.$setSize(sw, sh);
        Boot.start(0);
        Screen.$setSize(sw, sh);
        System.out.println("boot " + (System.nanoTime() - t0) / 1000000 + " ms, errors " + errors);
        for (int i = 0; i < frames; i++) {
            Eng.frame(1f / 60);
            boolean shot = shotFrames.contains(i + 1);
            if (shot || soft == null) Render.renderFrame();
            Eng.endOfFrame();
            if (shot) {
                java.io.File f = new java.io.File(System.getProperty("prr.shotdir", "build/shots"), String.format("frame%05d.png", i + 1));
                f.getParentFile().mkdirs();
                soft.save(f);
                System.out.println("shot " + f);
            }
            if (i % 60 == 59) status();
            inject(i + 1);
            float[] up = pending.remove(i + 1);
            if (up != null) Input.$touch(1, 2, up[0], up[1]);
            Integer ku = keyUps.remove(i + 1);
            if (ku != null) Input.$key(ku, false);
        }
        System.out.println("done, errors " + errors);
    }

    static boolean verbose = Boolean.getBoolean("prr.verbose");

    /** scripted input: -Dprr.input=frame:action[,frame:action] with action tap@x;y | key@code | fire | right */
    static java.util.HashMap<Integer, String> script;
    static void inject(int frame) {
        if (script == null) {
            script = new java.util.HashMap<>();
            for (String e : System.getProperty("prr.input", "").split(",")) {
                int c = e.indexOf(':');
                if (c > 0) script.put(Integer.parseInt(e.substring(0, c).trim()), e.substring(c + 1).trim());
            }
        }
        String a = script.get(frame);
        if (a == null) return;
        System.out.println("input " + a);
        if (a.startsWith("tap@")) {
            String[] p = a.substring(4).split(";");
            float x = Float.parseFloat(p[0]), y = Float.parseFloat(p[1]);
            Input.$touch(1, 0, x, y);
            pending.put(frame + 3, new float[]{x, y});
        } else if (a.startsWith("key@")) {
            int k = Integer.parseInt(a.substring(4));
            Input.$key(k, true);
            keyUps.put(frame + 3, k);
        }
        float[] up = pending.remove(frame);
        if (up != null) Input.$touch(1, 2, up[0], up[1]);
    }
    static final java.util.HashMap<Integer, float[]> pending = new java.util.HashMap<>();
    static final java.util.HashMap<Integer, Integer> keyUps = new java.util.HashMap<>();
    static { new Thread(); }

    static void status() {
        cs.UnityEngine.SceneManagement.Scene s = cs.UnityEngine.SceneManagement.SceneManager.GetActiveScene();
        int roots = s.GetRootGameObjects().length;
        System.out.println("-- frame " + Time.get_frameCount() + " t=" + Time.get_time() + " scene=" + s.get_name() + " roots=" + roots + " errors=" + errors);
        cs.UnityEngine.EventSystems.EventSystem es = cs.UnityEngine.EventSystems.EventSystem.get_current();
        if (es != null && es.get_currentSelectedGameObject() != null) System.out.println("   ui: selected=" + es.get_currentSelectedGameObject().get_name());
    }
}
