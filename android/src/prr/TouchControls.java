package prr;

import cs.UnityEngine.Gfx;
import cs.UnityEngine.Matrix4x4;
import cs.UnityEngine.Screen;
import cs.UnityEngine.Texture2D;
import cs.Rewired.TouchInput;

/**
 * On-screen clay controls: a joystick (knob follows the thumb 360°, clamped to maxRadius, snaps back on
 * release) and a hold-to-fire button that scales to 0.92 while pressed. Each uses its own pointer id, so
 * moving and firing at the same time works.
 */
final class TouchControls {
    private Texture2D base, knob, fire, gear;
    private float gearX, gearY, gearR;
    private int gearPtr = -1;
    Runnable onSettings;
    private final Gfx.Item item = new Gfx.Item();
    /** layout in pixels (recomputed on resize) */
    private float baseX, baseY, baseR, knobR, fireX, fireY, fireR, maxRadius;
    private int stickPtr = -1, firePtr = -1;
    private float knobX, knobY;
    private boolean visible = true;
    private long firePressNanos;

    void load(AndroidHost host) {
        base = tex(host, "1000045163.png");   // hollow red clay ring: fixed joystick base (bottom-left)
        knob = tex(host, "1000045164.png");   // solid blue clay disc: knob, 47.5% of the ring, follows the thumb 360°
        fire = tex(host, "1000045153.png");   // red ring + yellow crosshair + blue dot: fire button (bottom-right)
        gear = gearTexture();
        layout();
    }

    /** small settings button: a gear glyph rendered with the platform font */
    private static Texture2D gearTexture() {
        int n = 128;
        android.graphics.Bitmap b = android.graphics.Bitmap.createBitmap(n, n, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas c = new android.graphics.Canvas(b);
        android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        p.setColor(0x99000000);
        c.drawCircle(n / 2f, n / 2f, n / 2f - 2, p);
        p.setColor(0xFFFFE14D);
        p.setTextSize(n * 0.72f);
        p.setTextAlign(android.graphics.Paint.Align.CENTER);
        android.graphics.Paint.FontMetrics fm = p.getFontMetrics();
        c.drawText("\u2699", n / 2f, n / 2f - (fm.ascent + fm.descent) / 2, p);
        int[] px = new int[n * n];
        b.getPixels(px, 0, n, 0, 0, n, n);
        b.recycle();
        Texture2D t = new Texture2D(n, n, 4, false);
        int[] dst = t.$pixels();
        for (int y = 0; y < n; y++) System.arraycopy(px, (n - 1 - y) * n, dst, y * n, n);
        t.Apply();
        return t;
    }

    private static Texture2D tex(AndroidHost host, String name) {
        byte[] b = host.asset(name);
        if (b == null) return null;
        Object[] im = host.decodeImage(b);
        if (im == null) return null;
        Texture2D t = new Texture2D((Integer) im[0], (Integer) im[1], 4, false);
        t.m_Name = name;
        cs.UnityEngine.ImageConversion.LoadImage(t, b);
        return t;
    }

    void setVisible(boolean v) {
        visible = v;
        if (!v) {
            stickPtr = firePtr = -1;
            knobX = knobY = 0;
            TouchInput.moveX = TouchInput.moveY = 0;
            TouchInput.fire = false;
        }
    }

    /** sizes relative to the short screen edge so the layout holds at any aspect ratio */
    void layout() {
        float sw = Screen.get_width(), sh = Screen.get_height();
        float unit = Math.min(sw, sh);
        baseR = unit * 0.21f;
        knobR = baseR * 0.475f;
        maxRadius = baseR * 0.5f;
        float margin = unit * 0.06f;
        baseX = margin + baseR;
        baseY = margin + baseR;
        fireR = unit * 0.13f;
        fireX = sw - margin - fireR;
        fireY = margin + fireR;
        gearR = unit * 0.042f;
        gearX = sw - unit * 0.03f - gearR;
        gearY = sh - unit * 0.03f - gearR;
    }

    /** Android MotionEvent coordinates are top-left based; y is flipped to screen space by the caller */
    boolean down(int ptr, float x, float y) {
        if (gearPtr < 0 && dist(x, y, gearX, gearY) <= gearR * 1.4f) {
            gearPtr = ptr;
            return true;
        }
        if (!visible) return false;
        if (stickPtr < 0 && dist(x, y, baseX, baseY) <= baseR * 1.35f) {
            stickPtr = ptr;
            move(ptr, x, y);
            return true;
        }
        if (firePtr < 0 && dist(x, y, fireX, fireY) <= fireR * 1.3f) {
            firePtr = ptr;
            TouchInput.fire = true;
            firePressNanos = System.nanoTime();
            return true;
        }
        return false;
    }

    boolean move(int ptr, float x, float y) {
        if (ptr == gearPtr) return true;
        if (ptr == stickPtr) {
            float dx = x - baseX, dy = y - baseY;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len > maxRadius && len > 0) {
                dx = dx / len * maxRadius;
                dy = dy / len * maxRadius;
            }
            knobX = dx;
            knobY = dy;
            TouchInput.moveX = dx / maxRadius;
            TouchInput.moveY = dy / maxRadius;
            return true;
        }
        if (ptr == firePtr) {
            if (dist(x, y, fireX, fireY) > fireR * 2.2f) { up(ptr); return true; }
            return true;
        }
        return false;
    }

    boolean up(int ptr) {
        if (ptr == gearPtr) {
            gearPtr = -1;
            if (onSettings != null) onSettings.run();
            return true;
        }
        if (ptr == stickPtr) {
            stickPtr = -1;
            knobX = knobY = 0;
            TouchInput.moveX = TouchInput.moveY = 0;
            return true;
        }
        if (ptr == firePtr) {
            firePtr = -1;
            TouchInput.fire = false;
            return true;
        }
        return false;
    }

    void cancelAll() {
        up(stickPtr);
        up(firePtr);
    }

    private static float dist(float x, float y, float cx, float cy) {
        float dx = x - cx, dy = y - cy;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /** drawn after the game, in screen pixels */
    void draw(Gfx g) {
        float[] vp = Matrix4x4.Ortho(0, Screen.get_width(), 0, Screen.get_height(), -1, 1).m;
        quad(g, vp, gear, gearX, gearY, gearR * (gearPtr >= 0 ? 0.92f : 1f), 0.8f);
        if (!visible) return;
        quad(g, vp, base, baseX, baseY, baseR, 0.55f);
        quad(g, vp, knob, baseX + knobX, baseY + knobY, knobR, 0.85f);
        float fs = firePtr >= 0 ? 0.92f : 1f;
        quad(g, vp, fire, fireX, fireY, fireR * fs, firePtr >= 0 ? 1f : 0.75f);
    }

    private void quad(Gfx g, float[] vp, Texture2D t, float cx, float cy, float r, float alpha) {
        if (t == null) return;
        Gfx.Item it = item;
        System.arraycopy(vp, 0, it.mvp, 0, 16);
        System.arraycopy(Matrix4x4.get_identity().m, 0, it.model, 0, 16);
        it.shader = "Sprites/Default";
        it.mat = null;
        it.block = null;
        it.tex = t;
        it.col = null;
        it.scissor = false;
        it.stencilRef = 0;
        it.stencilComp = 8;
        it.colorMask = 15;
        it.tint[0] = it.tint[1] = it.tint[2] = 1;
        it.tint[3] = alpha;
        it.count = 4;
        it.pos = new float[]{cx - r, cy - r, 0, cx + r, cy - r, 0, cx - r, cy + r, 0, cx + r, cy + r, 0};
        it.uv0 = new float[]{0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0};
        it.uv1 = null;
        it.idx = QUAD;
        it.idxCount = 6;
        g.draw(it);
    }

    private static final int[] QUAD = {0, 1, 3, 3, 2, 0};
}
