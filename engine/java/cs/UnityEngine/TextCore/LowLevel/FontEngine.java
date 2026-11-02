package cs.UnityEngine.TextCore.LowLevel;

import cs.System.Collections.Generic.List_1;
import cs.UnityEngine.TextCore.FaceInfo;
import cs.UnityEngine.TextCore.Glyph;
import cs.UnityEngine.TextCore.GlyphMetrics;
import cs.UnityEngine.TextCore.GlyphRect;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * TextCore FontEngine: loads TrueType faces, rasterizes glyphs (coverage or signed distance field) into TMP atlases
 * with MaxRects packing. FontEngineError: Success 0, Invalid_File 2 ... ; GlyphRenderMode: SMOOTH 4117/4121, SDF* 4134+
 */
public final class FontEngine {
    private FontEngine() {}
    private static final HashMap<String, TrueType> cache = new HashMap<>();
    private static TrueType face;
    private static int pointSize = 90;
    private static float scale = 1;
    private static FaceInfo faceInfo = new FaceInfo();

    public static int InitializeFontEngine() { return 0; }
    public static int DestroyFontEngine() { return 0; }

    public static int LoadFontFace(cs.UnityEngine.Font font, int size) { return LoadFontFace(font, size, 0); }
    public static int LoadFontFace(cs.UnityEngine.Font font, int size, int faceIndex) {
        if (font == null || font.$ttf == null) return 2;
        TrueType t = load(font.$ttf, faceIndex);
        if (t == null) return 2;
        return setFace(t, size);
    }
    public static int LoadFontFace(cs.UnityEngine.Font font) { return LoadFontFace(font, font == null ? 90 : font.get_fontSize(), 0); }
    public static int LoadFontFace(String path, int size) {
        TrueType t = loadFile(new java.io.File(path), 0);
        return t == null ? 2 : setFace(t, size);
    }
    public static int LoadFontFace(String path, int size, int faceIndex) {
        TrueType t = loadFile(new java.io.File(path), faceIndex);
        return t == null ? 2 : setFace(t, size);
    }
    public static int LoadFontFace(byte[] data, int size) {
        try { return setFace(new TrueType(data, 0), size); } catch (RuntimeException e) { return 2; }
    }

    private static synchronized TrueType load(String rel, int faceIndex) {
        String k = rel + "#" + faceIndex;
        if (cache.containsKey(k)) return cache.get(k);
        byte[] b = cs.UnityEngine.Host.get().readData(rel);
        TrueType t = null;
        try { if (b != null) t = new TrueType(b, faceIndex); } catch (RuntimeException e) { cs.UnityEngine.Debug.LogWarning("font parse failed " + rel + ": " + e); }
        cache.put(k, t);
        return t;
    }
    private static TrueType loadFile(java.io.File f, int faceIndex) {
        try {
            byte[] b = new byte[(int) f.length()];
            java.io.FileInputStream in = new java.io.FileInputStream(f);
            try { int off = 0, n; while (off < b.length && (n = in.read(b, off, b.length - off)) > 0) off += n; } finally { in.close(); }
            return new TrueType(b, faceIndex);
        } catch (Exception e) {
            return null;
        }
    }

    private static int setFace(TrueType t, int size) {
        face = t;
        pointSize = size;
        scale = (float) size / t.unitsPerEm;
        FaceInfo fi = new FaceInfo();
        fi.m_FamilyName = t.family;
        fi.m_StyleName = t.style;
        fi.m_PointSize = size;
        fi.m_Scale = 1;
        fi.m_UnitsPerEM = t.unitsPerEm;
        fi.m_LineHeight = (t.ascender - t.descender + t.lineGap) * scale;
        fi.m_AscentLine = t.ascender * scale;
        fi.m_CapLine = t.capHeight * scale;
        fi.m_MeanLine = t.xHeight * scale;
        fi.m_Baseline = 0;
        fi.m_DescentLine = t.descender * scale;
        fi.m_SuperscriptOffset = fi.m_AscentLine;
        fi.m_SuperscriptSize = .5f;
        fi.m_SubscriptOffset = fi.m_DescentLine;
        fi.m_SubscriptSize = .5f;
        fi.m_UnderlineOffset = t.underlinePos * scale;
        fi.m_UnderlineThickness = t.underlineThick * scale;
        fi.m_StrikethroughOffset = fi.m_MeanLine / 2.5f;
        fi.m_StrikethroughThickness = fi.m_UnderlineThickness;
        int sp = t.glyphIndex(32);
        fi.m_TabWidth = t.advance(sp) * scale;
        faceInfo = fi;
        return 0;
    }

    public static FaceInfo GetFaceInfo() { return faceInfo.$copy(); }
    public static int GetGlyphIndex(int unicode) { return face == null ? 0 : face.glyphIndex(unicode); }

    private static Glyph metrics(int index) {
        Glyph g = new Glyph();
        g.m_Index = index;
        int[] bb = face.bbox(index);
        float adv = face.advance(index) * scale;
        if (bb == null) g.m_Metrics = new GlyphMetrics(0, 0, 0, 0, adv);
        else g.m_Metrics = new GlyphMetrics((bb[2] - bb[0]) * scale, (bb[3] - bb[1]) * scale, bb[0] * scale, bb[3] * scale, adv);
        g.m_Scale = 1;
        return g;
    }

    public static boolean TryGetGlyphWithUnicodeValue(int unicode, int flags, cs.rt.RefO out) {
        if (face == null) { out.v = null; return false; }
        int gi = face.glyphIndex(unicode);
        if (gi == 0 && unicode != 0) { out.v = null; return false; }
        out.v = metrics(gi);
        return true;
    }
    public static boolean TryGetGlyphWithIndexValue(int index, int flags, cs.rt.RefO out) {
        if (face == null || index < 0 || index >= face.numGlyphs) { out.v = null; return false; }
        out.v = metrics(index);
        return true;
    }

    public static void ResetAtlasTexture(cs.UnityEngine.Texture2D t) {
        int[] p = t.$pixels();
        java.util.Arrays.fill(p, 0x00FFFFFF);
        t.$version++;
    }

    public static boolean TryAddGlyphToTexture(int index, int padding, int packing, List_1 free, List_1 used, int renderMode, cs.UnityEngine.Texture2D tex, cs.rt.RefO out) {
        if (face == null) { out.v = null; return false; }
        Glyph g = metrics(index);
        int w = (int) Math.ceil(g.m_Metrics.m_Width), h = (int) Math.ceil(g.m_Metrics.m_Height);
        if (w == 0 || h == 0) {
            g.m_GlyphRect = new GlyphRect();
            out.v = g;
            return true;
        }
        int pw = w + padding * 2, ph = h + padding * 2;
        GlyphRect slot = MaxRects.insert(free, used, pw, ph);
        if (slot == null) { out.v = null; return false; }
        g.m_GlyphRect = new GlyphRect(slot.m_X + padding, slot.m_Y + padding, w, h);
        rasterize(index, g, padding, renderMode, tex, slot);
        out.v = g;
        return true;
    }

    public static boolean TryAddGlyphsToTexture(List_1 indices, int padding, int packing, List_1 free, List_1 used, int renderMode, cs.UnityEngine.Texture2D tex, cs.rt.RefO out) {
        ArrayList<Glyph> r = new ArrayList<>();
        boolean all = true;
        for (int i = 0, n = indices.get_Count(); i < n; i++) {
            cs.rt.RefO o = new cs.rt.RefO();
            int gi = cs.rt.Rt.unboxI(indices.get_Item(i));
            if (TryAddGlyphToTexture(gi, padding, packing, free, used, renderMode, tex, o)) r.add((Glyph) o.v);
            else all = false;
        }
        out.v = r.toArray(new Glyph[0]);
        return all;
    }

    /** draw glyph `index` into the atlas at slot (including padding); SDF modes produce a distance field */
    private static void rasterize(int index, Glyph g, int padding, int mode, cs.UnityEngine.Texture2D tex, GlyphRect slot) {
        ArrayList<float[]> contours = new ArrayList<>();
        face.outline(index, contours, new float[]{1, 0, 0, 1, 0, 0}, 0);
        int[] bb = face.bbox(index);
        // to atlas pixel space: x' = (x - xMin)*scale + slot.x + padding
        float ox = slot.m_X + padding - bb[0] * scale, oy = slot.m_Y + padding - bb[1] * scale;
        for (float[] c : contours) for (int i = 0; i < c.length; i += 2) { c[i] = c[i] * scale + ox; c[i + 1] = c[i + 1] * scale + oy; }
        boolean sdf = (mode & 0x1000) != 0 && mode != 4117 && mode != 4121 && mode != 4118 && mode != 4122;
        int[] px = tex.$pixels();
        int tw = tex.get_width(), th = tex.get_height();
        int x0 = slot.m_X, y0 = slot.m_Y, x1 = Math.min(tw, slot.m_X + slot.m_Width), y1 = Math.min(th, slot.m_Y + slot.m_Height);
        float spread = Math.max(1, padding);
        for (int y = y0; y < y1; y++) {
            float cy = y + .5f;
            for (int x = x0; x < x1; x++) {
                float cx = x + .5f;
                float a;
                if (sdf) {
                    float dist = (float) Math.sqrt(minDistSq(contours, cx, cy));
                    boolean in = inside(contours, cx, cy);
                    float sd = in ? dist : -dist;
                    a = .5f + sd / (2 * spread);
                } else {
                    a = coverage(contours, cx, cy);
                }
                int av = Math.max(0, Math.min(255, Math.round(a * 255)));
                px[y * tw + x] = (av << 24) | 0xFFFFFF;
            }
        }
        tex.$version++;
    }

    private static float minDistSq(ArrayList<float[]> contours, float x, float y) {
        float best = Float.POSITIVE_INFINITY;
        for (float[] c : contours) {
            for (int i = 0; i + 3 < c.length; i += 2) {
                float ax = c[i], ay = c[i + 1], bx = c[i + 2], by = c[i + 3];
                float dx = bx - ax, dy = by - ay, l = dx * dx + dy * dy;
                float t = l == 0 ? 0 : Math.max(0, Math.min(1, ((x - ax) * dx + (y - ay) * dy) / l));
                float qx = ax + t * dx - x, qy = ay + t * dy - y;
                float d = qx * qx + qy * qy;
                if (d < best) best = d;
            }
        }
        return best;
    }

    /** nonzero winding */
    private static boolean inside(ArrayList<float[]> contours, float x, float y) {
        int w = 0;
        for (float[] c : contours) {
            for (int i = 0; i + 3 < c.length; i += 2) {
                float ay = c[i + 1], by = c[i + 3];
                if (ay <= y) {
                    if (by > y && (c[i + 2] - c[i]) * (y - ay) - (x - c[i]) * (by - ay) > 0) w++;
                } else if (by <= y && (c[i + 2] - c[i]) * (y - ay) - (x - c[i]) * (by - ay) < 0) w--;
            }
        }
        return w != 0;
    }

    private static float coverage(ArrayList<float[]> contours, float x, float y) {
        int hit = 0;
        for (int sy = 0; sy < 4; sy++) for (int sx = 0; sx < 4; sx++) if (inside(contours, x - .375f + sx * .25f, y - .375f + sy * .25f)) hit++;
        return hit / 16f;
    }

    // ------------------------------------------------------------------ kerning
    public static GlyphPairAdjustmentRecord[] GetGlyphPairAdjustmentTable(int[] glyphIndexes) {
        if (face == null) return new GlyphPairAdjustmentRecord[0];
        java.util.HashSet<Integer> set = new java.util.HashSet<>();
        for (int g : glyphIndexes) set.add(g);
        ArrayList<GlyphPairAdjustmentRecord> r = new ArrayList<>();
        for (int k : face.kernPairs()) {
            int a = k >>> 16, b = k & 0xFFFF;
            if (!set.contains(a) || !set.contains(b)) continue;
            r.add(pair(a, b, face.kerning(a, b) * scale));
        }
        return r.toArray(new GlyphPairAdjustmentRecord[0]);
    }
    public static GlyphPairAdjustmentRecord[] GetGlyphPairAdjustmentRecords(List_1 glyphIndexes, cs.rt.RefI count) {
        int[] a = new int[glyphIndexes.get_Count()];
        for (int i = 0; i < a.length; i++) a[i] = cs.rt.Rt.unboxI(glyphIndexes.get_Item(i));
        GlyphPairAdjustmentRecord[] r = GetGlyphPairAdjustmentTable(a);
        count.v = r.length;
        return r;
    }
    public static GlyphPairAdjustmentRecord[] GetGlyphPairAdjustmentRecords(int glyphIndex, cs.rt.RefI count) {
        count.v = 0;
        return new GlyphPairAdjustmentRecord[0];
    }
    private static GlyphPairAdjustmentRecord pair(int a, int b, float xAdv) {
        GlyphAdjustmentRecord first = new GlyphAdjustmentRecord(a, new GlyphValueRecord(0, 0, xAdv, 0));
        GlyphAdjustmentRecord second = new GlyphAdjustmentRecord(b, new GlyphValueRecord());
        return new GlyphPairAdjustmentRecord(first, second);
    }
    public static String[] GetFontFaces() { return new String[]{face == null ? "" : face.style}; }
    public static String[] GetSystemFontNames() { return new String[0]; }
}
