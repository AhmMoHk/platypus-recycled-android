package cs.UnityEngine.TextCore.LowLevel;

import java.util.ArrayList;
import java.util.HashMap;

/** Minimal TrueType (glyf) reader: metrics, cmap (4/12), outlines (simple + composite), kern pairs. */
final class TrueType {
    final byte[] d;
    final int base;
    final HashMap<String, int[]> tables = new HashMap<>();
    int unitsPerEm = 1000, numGlyphs, numHMetrics, locFormat;
    int ascender, descender, lineGap, capHeight, xHeight, underlinePos, underlineThick, strikeSize, strikePos;
    int subY, superY, subSize = -1, superSize = -1;
    String family = "", style = "";
    private int[] cmapSub4, cmapSub12;
    private final HashMap<Integer, Integer> kern = new HashMap<>();

    TrueType(byte[] data, int faceIndex) {
        d = data;
        int off = 0;
        if (tag(0).equals("ttcf")) {
            int n = u32(8);
            off = u32(12 + 4 * Math.min(faceIndex, n - 1));
        }
        base = off;
        int nt = u16(off + 4);
        for (int i = 0; i < nt; i++) {
            int r = off + 12 + i * 16;
            tables.put(tag(r), new int[]{u32(r + 8), u32(r + 12)});
        }
        int[] head = tables.get("head");
        unitsPerEm = u16(head[0] + 18);
        locFormat = s16(head[0] + 50);
        numGlyphs = u16(tables.get("maxp")[0] + 4);
        int[] hhea = tables.get("hhea");
        ascender = s16(hhea[0] + 4);
        descender = s16(hhea[0] + 6);
        lineGap = s16(hhea[0] + 8);
        numHMetrics = u16(hhea[0] + 34);
        int[] os2 = tables.get("OS/2");
        if (os2 != null) {
            int o = os2[0], ver = u16(o);
            subSize = s16(o + 12); subY = s16(o + 16);
            superSize = s16(o + 20); superY = s16(o + 24);
            strikeSize = s16(o + 26); strikePos = s16(o + 28);
            if (ver >= 2 && os2[1] >= 90) { xHeight = s16(o + 86); capHeight = s16(o + 88); }
        }
        if (capHeight == 0) capHeight = (int) (ascender * .7f);
        if (xHeight == 0) xHeight = (int) (ascender * .5f);
        int[] post = tables.get("post");
        if (post != null) { underlinePos = s16(post[0] + 8); underlineThick = s16(post[0] + 10); }
        readCmap();
        readKern();
        readNames();
    }

    private String tag(int o) { return new String(new char[]{(char) (d[o] & 0xff), (char) (d[o + 1] & 0xff), (char) (d[o + 2] & 0xff), (char) (d[o + 3] & 0xff)}); }
    int u8(int o) { return d[o] & 0xff; }
    int u16(int o) { return ((d[o] & 0xff) << 8) | (d[o + 1] & 0xff); }
    int s16(int o) { return (short) u16(o); }
    int u32(int o) { return ((d[o] & 0xff) << 24) | ((d[o + 1] & 0xff) << 16) | ((d[o + 2] & 0xff) << 8) | (d[o + 3] & 0xff); }

    private void readCmap() {
        int[] t = tables.get("cmap");
        if (t == null) return;
        int o = t[0], n = u16(o + 2);
        int best4 = -1, best12 = -1;
        for (int i = 0; i < n; i++) {
            int pid = u16(o + 4 + i * 8), eid = u16(o + 6 + i * 8), so = o + u32(o + 8 + i * 8);
            int fmt = u16(so);
            boolean uni = pid == 0 || (pid == 3 && (eid == 1 || eid == 10));
            if (!uni) continue;
            if (fmt == 12) best12 = so;
            else if (fmt == 4 && best4 < 0) best4 = so;
        }
        if (best12 >= 0) {
            int ng = u32(best12 + 12);
            cmapSub12 = new int[ng * 3];
            for (int i = 0; i < ng; i++) {
                int g = best12 + 16 + i * 12;
                cmapSub12[i * 3] = u32(g); cmapSub12[i * 3 + 1] = u32(g + 4); cmapSub12[i * 3 + 2] = u32(g + 8);
            }
        }
        if (best4 >= 0) cmapSub4 = new int[]{best4};
    }

    int glyphIndex(int cp) {
        if (cmapSub12 != null) {
            int lo = 0, hi = cmapSub12.length / 3 - 1;
            while (lo <= hi) {
                int m = (lo + hi) >>> 1;
                int s = cmapSub12[m * 3], e = cmapSub12[m * 3 + 1];
                if (cp < s) hi = m - 1;
                else if (cp > e) lo = m + 1;
                else return cmapSub12[m * 3 + 2] + (cp - s);
            }
            return 0;
        }
        if (cmapSub4 == null || cp > 0xFFFF) return 0;
        int o = cmapSub4[0];
        int segX2 = u16(o + 6), ends = o + 14, starts = ends + segX2 + 2, deltas = starts + segX2, ranges = deltas + segX2;
        for (int i = 0; i < segX2 / 2; i++) {
            int end = u16(ends + i * 2);
            if (cp > end) continue;
            int start = u16(starts + i * 2);
            if (cp < start) return 0;
            int delta = s16(deltas + i * 2), ro = u16(ranges + i * 2);
            if (ro == 0) return (cp + delta) & 0xFFFF;
            int gi = u16(ranges + i * 2 + ro + (cp - start) * 2);
            return gi == 0 ? 0 : (gi + delta) & 0xFFFF;
        }
        return 0;
    }

    int advance(int g) {
        int[] t = tables.get("hmtx");
        if (t == null) return 0;
        int i = Math.min(g, numHMetrics - 1);
        return u16(t[0] + i * 4);
    }

    int leftBearing(int g) {
        int[] t = tables.get("hmtx");
        if (g < numHMetrics) return s16(t[0] + g * 4 + 2);
        return s16(t[0] + numHMetrics * 4 + (g - numHMetrics) * 2);
    }

    private void readKern() {
        int[] t = tables.get("kern");
        if (t == null) return;
        int o = t[0], n = u16(o + 2), p = o + 4;
        for (int i = 0; i < n; i++) {
            int len = u16(p + 2), cov = u16(p + 4);
            if ((cov >> 8) == 0 && (cov & 1) != 0) {
                int np = u16(p + 6);
                for (int k = 0; k < np; k++) {
                    int q = p + 14 + k * 6;
                    kern.put((u16(q) << 16) | u16(q + 2), s16(q + 4));
                }
            }
            p += len;
        }
    }

    int kerning(int a, int b) { Integer v = kern.get((a << 16) | b); return v == null ? 0 : v; }
    java.util.Set<Integer> kernPairs() { return kern.keySet(); }

    private void readNames() {
        int[] t = tables.get("name");
        if (t == null) return;
        int o = t[0], n = u16(o + 2), so = o + u16(o + 4);
        for (int i = 0; i < n; i++) {
            int r = o + 6 + i * 12;
            int pid = u16(r), nid = u16(r + 6), len = u16(r + 8), off = u16(r + 10);
            if (nid != 1 && nid != 2) continue;
            String s;
            if (pid == 3 || pid == 0) {
                char[] c = new char[len / 2];
                for (int k = 0; k < c.length; k++) c[k] = (char) u16(so + off + k * 2);
                s = new String(c);
            } else {
                char[] c = new char[len];
                for (int k = 0; k < len; k++) c[k] = (char) u8(so + off + k);
                s = new String(c);
            }
            if (nid == 1 && family.isEmpty()) family = s;
            if (nid == 2 && style.isEmpty()) style = s;
        }
    }

    /** glyph bbox {xMin,yMin,xMax,yMax} in font units, or null for empty glyphs */
    int[] bbox(int g) {
        int[] r = glyphRange(g);
        if (r == null) return null;
        int o = r[0];
        return new int[]{s16(o + 2), s16(o + 4), s16(o + 6), s16(o + 8)};
    }

    private int[] glyphRange(int g) {
        int[] loca = tables.get("loca"), glyf = tables.get("glyf");
        if (loca == null || glyf == null || g < 0 || g >= numGlyphs) return null;
        int a, b;
        if (locFormat == 0) { a = u16(loca[0] + g * 2) * 2; b = u16(loca[0] + g * 2 + 2) * 2; }
        else { a = u32(loca[0] + g * 4); b = u32(loca[0] + g * 4 + 4); }
        if (b <= a) return null;
        return new int[]{glyf[0] + a, b - a};
    }

    /** outline as contours of line segments {x0,y0,x1,y1,...} (quadratics flattened), font units */
    void outline(int g, ArrayList<float[]> contours, float[] m, int depth) {
        int[] r = glyphRange(g);
        if (r == null || depth > 8) return;
        int o = r[0];
        int nc = s16(o);
        if (nc >= 0) simple(o, nc, contours, m);
        else composite(o, contours, m, depth);
    }

    private void simple(int o, int nc, ArrayList<float[]> contours, float[] m) {
        int p = o + 10;
        int[] ends = new int[nc];
        for (int i = 0; i < nc; i++) ends[i] = u16(p + i * 2);
        p += nc * 2;
        int np = nc == 0 ? 0 : ends[nc - 1] + 1;
        p += 2 + u16(p);
        int[] flags = new int[np];
        for (int i = 0; i < np; ) {
            int f = u8(p++);
            flags[i++] = f;
            if ((f & 8) != 0) { int rep = u8(p++); for (int k = 0; k < rep && i < np; k++) flags[i++] = f; }
        }
        float[] xs = new float[np], ys = new float[np];
        int v = 0;
        for (int i = 0; i < np; i++) {
            int f = flags[i];
            if ((f & 2) != 0) { int dx = u8(p++); v += (f & 16) != 0 ? dx : -dx; }
            else if ((f & 16) == 0) { v += s16(p); p += 2; }
            xs[i] = v;
        }
        v = 0;
        for (int i = 0; i < np; i++) {
            int f = flags[i];
            if ((f & 4) != 0) { int dy = u8(p++); v += (f & 32) != 0 ? dy : -dy; }
            else if ((f & 32) == 0) { v += s16(p); p += 2; }
            ys[i] = v;
        }
        for (int i = 0; i < np; i++) {
            float x = xs[i], y = ys[i];
            xs[i] = m[0] * x + m[2] * y + m[4];
            ys[i] = m[1] * x + m[3] * y + m[5];
        }
        int start = 0;
        for (int c = 0; c < nc; c++) {
            int end = ends[c], n = end - start + 1;
            if (n < 2) { start = end + 1; continue; }
            ArrayList<Float> seg = new ArrayList<>();
            // find a starting on-curve point (or synthesize one)
            int first = -1;
            for (int k = 0; k < n; k++) if ((flags[start + k] & 1) != 0) { first = k; break; }
            float sx, sy;
            if (first < 0) { sx = (xs[start] + xs[start + 1]) / 2; sy = (ys[start] + ys[start + 1]) / 2; first = 0; }
            else { sx = xs[start + first]; sy = ys[start + first]; }
            float cx = sx, cy = sy;
            boolean hasCtrl = false;
            float qx = 0, qy = 0;
            seg.add(sx); seg.add(sy);
            for (int k = 1; k <= n; k++) {
                int i = start + (first + k) % n;
                float x = xs[i], y = ys[i];
                boolean on = (flags[i] & 1) != 0;
                if (k == n && first >= 0 && (flags[start + first] & 1) != 0) { x = sx; y = sy; on = true; }
                if (on) {
                    if (hasCtrl) quad(seg, cx, cy, qx, qy, x, y);
                    else { seg.add(x); seg.add(y); }
                    cx = x; cy = y; hasCtrl = false;
                } else {
                    if (hasCtrl) {
                        float mx = (qx + x) / 2, my = (qy + y) / 2;
                        quad(seg, cx, cy, qx, qy, mx, my);
                        cx = mx; cy = my;
                    }
                    qx = x; qy = y; hasCtrl = true;
                }
            }
            if (hasCtrl) quad(seg, cx, cy, qx, qy, sx, sy);
            float[] a = new float[seg.size()];
            for (int k = 0; k < a.length; k++) a[k] = seg.get(k);
            contours.add(a);
            start = end + 1;
        }
    }

    private static void quad(ArrayList<Float> seg, float x0, float y0, float cx, float cy, float x1, float y1) {
        float dd = Math.abs(x0 - 2 * cx + x1) + Math.abs(y0 - 2 * cy + y1);
        int n = Math.max(2, Math.min(16, (int) Math.ceil(Math.sqrt(dd) * 0.5)));
        for (int i = 1; i <= n; i++) {
            float t = (float) i / n, u = 1 - t;
            seg.add(u * u * x0 + 2 * u * t * cx + t * t * x1);
            seg.add(u * u * y0 + 2 * u * t * cy + t * t * y1);
        }
    }

    private void composite(int o, ArrayList<float[]> contours, float[] m, int depth) {
        int p = o + 10;
        while (true) {
            int flags = u16(p), gi = u16(p + 2);
            p += 4;
            float dx, dy;
            if ((flags & 1) != 0) { dx = s16(p); dy = s16(p + 2); p += 4; }
            else { dx = (byte) d[p]; dy = (byte) d[p + 1]; p += 2; }
            float a = 1, b = 0, c = 0, dd = 1;
            if ((flags & 8) != 0) { a = dd = f2(p); p += 2; }
            else if ((flags & 0x40) != 0) { a = f2(p); dd = f2(p + 2); p += 4; }
            else if ((flags & 0x80) != 0) { a = f2(p); b = f2(p + 2); c = f2(p + 4); dd = f2(p + 6); p += 8; }
            float[] cm = {a, b, c, dd, dx, dy};
            float[] r = {m[0] * cm[0] + m[2] * cm[1], m[1] * cm[0] + m[3] * cm[1], m[0] * cm[2] + m[2] * cm[3], m[1] * cm[2] + m[3] * cm[3],
                m[0] * cm[4] + m[2] * cm[5] + m[4], m[1] * cm[4] + m[3] * cm[5] + m[5]};
            outline(gi, contours, r, depth + 1);
            if ((flags & 32) == 0) break;
        }
    }

    private float f2(int o) { return s16(o) / 16384f; }
}
