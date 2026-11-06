package prr;

import cs.UnityEngine.Gfx;
import cs.UnityEngine.MaterialPropertyBlock;
import cs.UnityEngine.Shader;
import cs.UnityEngine.Texture;
import cs.UnityEngine.Texture2D;

import java.io.File;
import java.util.HashMap;

/**
 * Software rasterizer for the desktop harness (screenshots without a GPU). Mirrors the GLES backend's
 * shader families closely enough to check layout, sorting, masking and text.
 */
final class SoftGfx extends Gfx {
    int w, h;
    int[] fb;
    byte[] stencil;
    private int vx, vy, vw, vh;
    final String astcenc, dataRoot;

    SoftGfx(String astcenc, String dataRoot) { this.astcenc = astcenc; this.dataRoot = dataRoot; }

    @Override public void begin(int width, int height) {
        if (fb == null || w != width || h != height) {
            w = width;
            h = height;
            fb = new int[w * h];
            stencil = new byte[w * h];
        }
        vx = 0; vy = 0; vw = w; vh = h;
    }
    @Override public void viewport(int x, int y, int vw0, int vh0) { vx = x; vy = y; vw = vw0; vh = vh0; }
    @Override public void clear(boolean color, boolean depth, float r, float g, float b, float a) {
        int c = 0xFF000000 | (cl(r) << 16) | (cl(g) << 8) | cl(b);
        for (int y = Math.max(0, vy); y < Math.min(h, vy + vh); y++)
            for (int x = Math.max(0, vx); x < Math.min(w, vx + vw); x++) {
                if (color) fb[y * w + x] = c;
                if (depth) stencil[y * w + x] = 0;
            }
    }
    @Override public void end() {}

    private static int cl(float v) { return Math.max(0, Math.min(255, (int) (v * 255 + .5f))); }

    // ------------------------------------------------------------------ textures (CPU copies; ASTC decoded with astcenc -dl)
    private final HashMap<Texture, Object[]> cache = new HashMap<>();

    private Object[] tex(Texture t) {
        if (!(t instanceof Texture2D)) return null;
        Object[] e = cache.get(t);
        if (e != null && (Integer) e[3] == t.$version) return e;
        Texture2D t2 = (Texture2D) t;
        int[] px = null;
        int tw = t2.get_width(), th = t2.get_height();
        if (!t2.$hasPixels() && t2.$compressedSrc != null) px = decodeAstc(t2, tw, th);
        if (px == null) px = t2.$pixels();
        e = new Object[]{px, tw, th, t.$version};
        cache.put(t, e);
        return e;
    }

    private int[] decodeAstc(Texture2D t, int tw, int th) {
        try {
            byte[] blocks = t.$readCompressed();
            if (blocks == null) return null;
            File tmp = File.createTempFile("astc", ".astc"), png = File.createTempFile("astc", ".png");
            byte[] hdr = {0x13, (byte) 0xAB, (byte) 0xA1, 0x5C, 4, 4, 1, (byte) tw, (byte) (tw >> 8), (byte) (tw >> 16), (byte) th, (byte) (th >> 8), (byte) (th >> 16), 1, 0, 0};
            java.io.FileOutputStream o = new java.io.FileOutputStream(tmp);
            o.write(hdr);
            o.write(blocks);
            o.close();
            Process p = new ProcessBuilder(astcenc, "-dl", tmp.getPath(), png.getPath(), "-silent").redirectErrorStream(true).start();
            p.getInputStream().readAllBytes();
            p.waitFor();
            java.awt.image.BufferedImage im = javax.imageio.ImageIO.read(png);
            tmp.delete();
            png.delete();
            if (im == null) return null;
            // the PNG's top row is GL row 0 (bottom): keep rows as they are
            return im.getRGB(0, 0, tw, th, null, 0, tw);
        } catch (Exception ex) {
            return null;
        }
    }

    // ------------------------------------------------------------------ drawing
    private static final int P_FACECOLOR = Shader.PropertyToID("_FaceColor"), P_OUTLINECOLOR = Shader.PropertyToID("_OutlineColor"),
        P_OUTLINEWIDTH = Shader.PropertyToID("_OutlineWidth"), P_MAINTEX = Shader.PropertyToID("_MainTex"),
        P_HIGH = Shader.PropertyToID("_HighTint"), P_LOW = Shader.PropertyToID("_LowTint"), P_TMPCOLOR = Shader.PropertyToID("_TMPColor"),
        P_THRESHOLD = Shader.PropertyToID("_HighLowThreshold");

    private float[] vec(Item it, int id, float[] def) {
        if (it.block != null && it.block.$vector(id) != null) return it.block.$vector(id);
        if (it.mat != null && it.mat.$props().$vector(id) != null) return it.mat.$props().$vector(id);
        return def;
    }
    private float flt(Item it, int id, float def) {
        if (it.mat != null) { float v = it.mat.$props().$float(id, Float.NaN); if (v == v) return v; }
        return def;
    }

    static boolean drawLog = Boolean.getBoolean("prr.drawlog");
    static String path(cs.UnityEngine.Transform t) { return t == null ? "" : path(t.get_parent()) + "/" + t.get_gameObject().get_name(); }

    @Override public void draw(Item it) {
        if (it.count == 0 || it.idxCount == 0 || it.pos == null) return;
        Texture t = it.tex != null ? it.tex : it.mat != null ? it.mat.$props().$texture(P_MAINTEX) : null;
        Object[] te = t == null ? null : tex(t);
        if (drawLog) System.out.println("  draw " + it.shader + " tex=" + (t == null ? "-" : t.get_name() + " " + t.get_width() + "x" + t.get_height() + (te == null || te[0] == null ? " NOPX" : "")
            + (t instanceof Texture2D && ((Texture2D) t).$compressedSrc != null ? " astc" : "")) + " n=" + it.count + " uv=" + (it.uv0 == null ? "null" : it.uv0[0] + "," + it.uv0[1] + ".." + it.uv0[8] + "," + it.uv0[9]) + " col=" + (it.col == null ? "null" : Integer.toHexString(it.col[0])) + " tint=" + java.util.Arrays.toString(it.tint));
        int[] tp = te == null ? null : (int[]) te[0];
        int tw = te == null ? 1 : (Integer) te[1], th = te == null ? 1 : (Integer) te[2];
        String sh = it.shader == null ? "" : it.shader;
        boolean sdf = sh.contains("Distance Field"), morph = sh.contains("MorphText");
        float[] face = vec(it, P_FACECOLOR, new float[]{1, 1, 1, 1}), outline = vec(it, P_OUTLINECOLOR, new float[]{0, 0, 0, 1});
        float ow = flt(it, P_OUTLINEWIDTH, 0);
        float[] hi = vec(it, P_HIGH, new float[]{1, 1, 1, 1}), lo = vec(it, P_LOW, new float[]{1, 1, 1, 1}), tc = vec(it, P_TMPCOLOR, new float[]{1, 1, 1, 1});
        float thr = flt(it, P_THRESHOLD, .5f), sm = Math.max(.001f, flt(it, Shader.PropertyToID("_Smoothness"), .05f));
        boolean stencilTest = it.stencilRef != 0 || it.stencilComp != 8;
        int n = it.count;
        float[] sx = new float[n], sy = new float[n];
        float[] m = it.mvp;
        for (int i = 0; i < n; i++) {
            float x = it.pos[i * 3], y = it.pos[i * 3 + 1], z = it.pos[i * 3 + 2];
            float cx = m[0] * x + m[4] * y + m[8] * z + m[12], cy = m[1] * x + m[5] * y + m[9] * z + m[13], cw = m[3] * x + m[7] * y + m[11] * z + m[15];
            if (cw == 0) cw = 1e-6f;
            sx[i] = vx + (cx / cw + 1) * .5f * vw;
            sy[i] = vy + (cy / cw + 1) * .5f * vh;
        }
        if (drawLog) {
            float a0 = 1e9f, a1 = -1e9f, b0 = 1e9f, b1 = -1e9f;
            for (int i = 0; i < n; i++) { a0 = Math.min(a0, sx[i]); a1 = Math.max(a1, sx[i]); b0 = Math.min(b0, sy[i]); b1 = Math.max(b1, sy[i]); }
            System.out.println("    bbox " + (int) a0 + "," + (int) b0 + " - " + (int) a1 + "," + (int) b1 + (it.mat != null ? " mat=" + it.mat.get_name() : "") + (it.src != null ? " go=" + path(it.src.get_transform()) + " sl=" + ((cs.UnityEngine.Renderer) it.src).get_sortingLayerID() + "/" + ((cs.UnityEngine.Renderer) it.src).get_sortingOrder() + " z=" + it.src.get_transform().get_position().z : "") + (it.mat != null ? " q=" + it.mat.get_renderQueue() : ""));
        }
        int clipX0 = Math.max(0, vx), clipY0 = Math.max(0, vy), clipX1 = Math.min(w, vx + vw), clipY1 = Math.min(h, vy + vh);
        if (it.scissor) {
            clipX0 = Math.max(clipX0, it.sx); clipY0 = Math.max(clipY0, it.sy);
            clipX1 = Math.min(clipX1, it.sx + it.sw); clipY1 = Math.min(clipY1, it.sy + it.sh);
        }
        for (int k = 0; k + 2 < it.idxCount; k += 3) {
            int a = it.idx[k], b = it.idx[k + 1], c = it.idx[k + 2];
            float area = (sx[b] - sx[a]) * (sy[c] - sy[a]) - (sy[b] - sy[a]) * (sx[c] - sx[a]);
            if (area == 0) continue;
            int x0 = (int) Math.max(clipX0, Math.floor(Math.min(sx[a], Math.min(sx[b], sx[c]))));
            int x1 = (int) Math.min(clipX1 - 1, Math.ceil(Math.max(sx[a], Math.max(sx[b], sx[c]))));
            int y0 = (int) Math.max(clipY0, Math.floor(Math.min(sy[a], Math.min(sy[b], sy[c]))));
            int y1 = (int) Math.min(clipY1 - 1, Math.ceil(Math.max(sy[a], Math.max(sy[b], sy[c]))));
            for (int y = y0; y <= y1; y++) {
                float py = y + .5f;
                for (int x = x0; x <= x1; x++) {
                    float px = x + .5f;
                    float wa = ((sx[b] - px) * (sy[c] - py) - (sy[b] - py) * (sx[c] - px)) / area;
                    float wb = ((sx[c] - px) * (sy[a] - py) - (sy[c] - py) * (sx[a] - px)) / area;
                    float wc = 1 - wa - wb;
                    if (wa < 0 || wb < 0 || wc < 0) continue;
                    int fi = y * w + x;
                    if (stencilTest && !stencilPass(it, stencil[fi] & 0xff)) continue;
                    float r = 1, g = 1, bl = 1, al = 1;
                    if (it.col != null) {
                        r = lerp3(it.col, a, b, c, wa, wb, wc, 16); g = lerp3(it.col, a, b, c, wa, wb, wc, 8);
                        bl = lerp3(it.col, a, b, c, wa, wb, wc, 0); al = lerp3(it.col, a, b, c, wa, wb, wc, 24);
                    }
                    r *= it.tint[0]; g *= it.tint[1]; bl *= it.tint[2]; al *= it.tint[3];
                    if (tp != null && it.uv0 != null) {
                        float u = it.uv0[a * 4] * wa + it.uv0[b * 4] * wb + it.uv0[c * 4] * wc;
                        float v = it.uv0[a * 4 + 1] * wa + it.uv0[b * 4 + 1] * wb + it.uv0[c * 4 + 1] * wc;
                        int tx = Math.max(0, Math.min(tw - 1, (int) (u * tw))), ty = Math.max(0, Math.min(th - 1, (int) (v * th)));
                        int s = tp.length > ty * tw + tx ? tp[ty * tw + tx] : 0;
                        float ta = ((s >>> 24) & 255) / 255f, tr = ((s >> 16) & 255) / 255f, tg = ((s >> 8) & 255) / 255f, tb = (s & 255) / 255f;
                        if (sdf) {
                            float edge = .5f - ow * .25f;
                            float fa = smooth(.45f, .55f, ta), oa = smooth(edge - .05f, edge + .05f, ta);
                            r *= face[0] * fa + outline[0] * (1 - fa); g *= face[1] * fa + outline[1] * (1 - fa); bl *= face[2] * fa + outline[2] * (1 - fa);
                            al *= Math.max(fa, oa) * face[3];
                        } else if (morph) {
                            float l = .299f * tr + .587f * tg + .114f * tb, kk = smooth(thr - sm, thr + sm, l);
                            r *= lo[0] + (hi[0] - lo[0]) * kk; g *= lo[1] + (hi[1] - lo[1]) * kk; bl *= lo[2] + (hi[2] - lo[2]) * kk;
                            al *= ta;
                        } else {
                            r *= tr; g *= tg; bl *= tb; al *= ta;
                        }
                    }
                    if (stencilTest) stencil[fi] = (byte) stencilOp(it, stencil[fi] & 0xff);
                    if (it.colorMask == 0 || al <= 0) continue;
                    int d = fb[fi];
                    float dr = ((d >> 16) & 255) / 255f, dg = ((d >> 8) & 255) / 255f, db = (d & 255) / 255f;
                    al = Math.min(1, al);
                    fb[fi] = 0xFF000000 | (cl(r * al + dr * (1 - al)) << 16) | (cl(g * al + dg * (1 - al)) << 8) | cl(bl * al + db * (1 - al));
                }
            }
        }
    }

    private static float lerp3(int[] c, int a, int b, int cc, float wa, float wb, float wc, int sh) {
        return (((c[a] >>> sh) & 255) * wa + ((c[b] >>> sh) & 255) * wb + ((c[cc] >>> sh) & 255) * wc) / 255f;
    }
    private static float smooth(float e0, float e1, float x) { float t = Math.max(0, Math.min(1, (x - e0) / (e1 - e0))); return t * t * (3 - 2 * t); }

    private static boolean stencilPass(Item it, int s) {
        int ref = it.stencilRef & it.stencilRead, v = s & it.stencilRead;
        switch (it.stencilComp) {
            case 1: return false;
            case 2: return ref < v;
            case 3: return ref == v;
            case 4: return ref <= v;
            case 5: return ref > v;
            case 6: return ref != v;
            case 7: return ref >= v;
            default: return true;
        }
    }
    private static int stencilOp(Item it, int s) {
        int r;
        switch (it.stencilOp) {
            case 1: r = 0; break;
            case 2: r = it.stencilRef; break;
            case 3: r = Math.min(255, s + 1); break;
            case 4: r = Math.max(0, s - 1); break;
            case 5: r = ~s & 255; break;
            default: r = s;
        }
        return (s & ~it.stencilWrite) | (r & it.stencilWrite);
    }

    void save(File f) throws java.io.IOException {
        java.awt.image.BufferedImage im = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) im.setRGB(x, h - 1 - y, fb[y * w + x]);
        javax.imageio.ImageIO.write(im, "png", f);
    }
}
