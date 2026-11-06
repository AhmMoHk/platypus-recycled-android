package cs.UnityEngine;

import cs.rt.Json;
import java.util.ArrayList;
import java.util.HashMap;

/** Sprite: rect in texture pixels, pivot in pixels, mesh in local units (relative to the pivot). */
public class Sprite extends UObject {
    Texture2D texture, alphaTexture;
    final Rect rect = new Rect(), textureRect = new Rect();
    final Vector2 pivot = new Vector2(), textureRectOffset = new Vector2();
    final Vector4 border = new Vector4();
    float pixelsPerUnit = 100;
    boolean packed;
    int packingMode, packingRotation, meshType = 1;
    /** mesh: positions (x,y per vertex, units), uvs, triangle indices */
    float[] verts, uvs;
    int[] tris;

    public Sprite() {}

    public static Sprite Create(Texture2D t, Rect r, Vector2 pivot) { return Create(t, r, pivot, 100f, 0, 1, new Vector4(), false); }
    public static Sprite Create(Texture2D t, Rect r, Vector2 pivot, float ppu) { return Create(t, r, pivot, ppu, 0, 1, new Vector4(), false); }
    public static Sprite Create(Texture2D t, Rect r, Vector2 pivot, float ppu, int extrude) { return Create(t, r, pivot, ppu, extrude, 1, new Vector4(), false); }
    public static Sprite Create(Texture2D t, Rect r, Vector2 pivot, float ppu, int extrude, int meshType) { return Create(t, r, pivot, ppu, extrude, meshType, new Vector4(), false); }
    public static Sprite Create(Texture2D t, Rect r, Vector2 pivot, float ppu, int extrude, int meshType, Vector4 border) { return Create(t, r, pivot, ppu, extrude, meshType, border, false); }
    public static Sprite Create(Texture2D t, Rect r, Vector2 pivot, float ppu, int extrude, int meshType, Vector4 border, boolean physics) {
        if (t == null) throw new cs.System.ArgumentNullException("texture");
        if (r.m_XMin < 0 || r.m_YMin < 0 || r.m_XMin + r.m_Width > t.width + 0.001f || r.m_YMin + r.m_Height > t.height + 0.001f)
            throw new cs.System.ArgumentException("Could not create sprite (" + r.m_XMin + ", " + r.m_YMin + ", " + r.m_Width + ", " + r.m_Height + ") from a " + t.width + "x" + t.height + " texture.");
        Sprite s = new Sprite();
        s.texture = t;
        s.rect.$set(r);
        s.textureRect.$set(r);
        s.pivot.Set(pivot.x * r.m_Width, pivot.y * r.m_Height);
        s.pixelsPerUnit = ppu;
        s.border.$set(border);
        s.meshType = meshType;
        s.fullRectMesh();
        return s;
    }

    /** quad mesh covering textureRect */
    void fullRectMesh() {
        float ppu = pixelsPerUnit;
        float x0 = (textureRect.m_XMin - rect.m_XMin - pivot.x) / ppu, y0 = (textureRect.m_YMin - rect.m_YMin - pivot.y) / ppu;
        float x1 = x0 + textureRect.m_Width / ppu, y1 = y0 + textureRect.m_Height / ppu;
        float tw = texture == null || texture.width == 0 ? 1 : texture.width, th = texture == null || texture.height == 0 ? 1 : texture.height;
        float u0 = textureRect.m_XMin / tw, v0 = textureRect.m_YMin / th, u1 = (textureRect.m_XMin + textureRect.m_Width) / tw, v1 = (textureRect.m_YMin + textureRect.m_Height) / th;
        verts = new float[]{x0, y1, x1, y1, x0, y0, x1, y0};
        uvs = new float[]{u0, v1, u1, v1, u0, v0, u1, v0};
        tris = new int[]{0, 1, 3, 3, 2, 0};
    }

    public Texture2D get_texture() { return texture; }
    public Texture2D get_associatedAlphaSplitTexture() { return alphaTexture; }
    public Rect get_rect() { return rect.$copy(); }
    public Rect get_textureRect() { return textureRect.$copy(); }
    public Vector2 get_textureRectOffset() { return textureRectOffset.$copy(); }
    public Vector2 get_pivot() { return pivot.$copy(); }
    public Vector4 get_border() { return border.$copy(); }
    public float get_pixelsPerUnit() { return pixelsPerUnit; }
    public boolean get_packed() { return packed; }
    public int get_packingMode() { return packingMode; }
    public int get_packingRotation() { return packingRotation; }
    public Bounds get_bounds() {
        float minX = Float.POSITIVE_INFINITY, minY = minX, maxX = Float.NEGATIVE_INFINITY, maxY = maxX;
        for (int i = 0; i < verts.length; i += 2) {
            minX = Math.min(minX, verts[i]); maxX = Math.max(maxX, verts[i]);
            minY = Math.min(minY, verts[i + 1]); maxY = Math.max(maxY, verts[i + 1]);
        }
        if (verts.length == 0) { minX = minY = maxX = maxY = 0; }
        // Unity reports the full rect bounds for sprites
        float rx0 = -pivot.x / pixelsPerUnit, ry0 = -pivot.y / pixelsPerUnit;
        float rx1 = rx0 + rect.m_Width / pixelsPerUnit, ry1 = ry0 + rect.m_Height / pixelsPerUnit;
        Bounds b = new Bounds();
        b.m_Center.Set((rx0 + rx1) / 2, (ry0 + ry1) / 2, 0);
        b.m_Extent.Set((rx1 - rx0) / 2, (ry1 - ry0) / 2, 0);
        return b;
    }
    public Vector2[] get_vertices() {
        Vector2[] r = new Vector2[verts.length / 2];
        for (int i = 0; i < r.length; i++) r[i] = new Vector2(verts[i * 2], verts[i * 2 + 1]);
        return r;
    }
    public Vector2[] get_uv() {
        Vector2[] r = new Vector2[uvs.length / 2];
        for (int i = 0; i < r.length; i++) r[i] = new Vector2(uvs[i * 2], uvs[i * 2 + 1]);
        return r;
    }
    public char[] get_triangles() {
        char[] r = new char[tris.length];
        for (int i = 0; i < r.length; i++) r[i] = (char) tris[i];
        return r;
    }
    public void OverrideGeometry(Vector2[] v, char[] t) {
        float tw = texture.width, th = texture.height;
        verts = new float[v.length * 2];
        uvs = new float[v.length * 2];
        for (int i = 0; i < v.length; i++) {
            verts[i * 2] = (v[i].x - pivot.x) / pixelsPerUnit;
            verts[i * 2 + 1] = (v[i].y - pivot.y) / pixelsPerUnit;
            uvs[i * 2] = (rect.m_XMin + v[i].x) / tw;
            uvs[i * 2 + 1] = (rect.m_YMin + v[i].y) / th;
        }
        tris = new int[t.length];
        for (int i = 0; i < t.length; i++) tris[i] = t[i];
    }

    @Override public void $loadAsset(HashMap<String, Object> d, Loader.Ctx ctx) {
        Loader.rect(d.get("m_Rect"), rect);
        Loader.vec4(d.get("m_Border"), border);
        pixelsPerUnit = Loader.f(d, "m_PixelsToUnits", 100);
        Vector2 pv = new Vector2();
        Loader.vec2(d.get("m_Pivot"), pv);
        pivot.Set(pv.x * rect.m_Width, pv.y * rect.m_Height);
        HashMap<String, Object> rd = Loader.m(d, "m_RD");
        UObject t = ctx.ref(rd.get("texture"));
        texture = t instanceof Texture2D ? (Texture2D) t : null;
        UObject at = ctx.ref(rd.get("alphaTexture"));
        alphaTexture = at instanceof Texture2D ? (Texture2D) at : null;
        Loader.rect(rd.get("textureRect"), textureRect);
        Loader.vec2(rd.get("textureRectOffset"), textureRectOffset);
        int settings = Loader.i(rd, "settingsRaw", 0);
        packed = (settings & 1) != 0;
        packingMode = (settings >> 1) & 1;
        packingRotation = (settings >> 2) & 15;
        meshType = (settings >> 6) & 1;
        if (!decodeMesh(rd)) fullRectMesh();
    }

    /** decode m_VertexData (positions: channel 0, uv: channel 4) + m_IndexBuffer (u16) */
    boolean decodeMesh(HashMap<String, Object> rd) {
        try {
            HashMap<String, Object> vd = Loader.m(rd, "m_VertexData");
            int n = Loader.i(vd, "m_VertexCount", 0);
            Object ds = vd.get("m_DataSize");
            if (n == 0 || !(ds instanceof HashMap) || !(Json.map(ds).get("_b64") instanceof String)) return false;
            byte[] data = cs.rt.Rt.b64((String) Json.map(ds).get("_b64"));
            ArrayList<Object> ch = Loader.l(vd, "m_Channels");
            int[] streamStride = new int[4];
            int[][] chInfo = new int[ch.size()][];
            for (int i = 0; i < ch.size(); i++) {
                HashMap<String, Object> c = Json.map(ch.get(i));
                int stream = Json.i(c.get("stream")), off = Json.i(c.get("offset")), fmt = Json.i(c.get("format")), dim = Json.i(c.get("dimension")) & 15;
                chInfo[i] = new int[]{stream, off, fmt, dim};
                if (dim > 0) streamStride[stream] = Math.max(streamStride[stream], off + dim * formatSize(fmt));
            }
            int[] streamOff = new int[4];
            int o = 0;
            for (int s = 0; s < 4; s++) {
                streamOff[s] = o;
                o += streamStride[s] * n;
                o = (o + 15) & ~15;
            }
            java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(data).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            float[] pos = new float[n * 2], uv = new float[n * 2];
            int[] pc = chInfo[0], uc = chInfo.length > 4 ? chInfo[4] : null;
            if (pc[3] < 2 || pc[2] != 0) return false;
            for (int v = 0; v < n; v++) {
                int p = streamOff[pc[0]] + v * streamStride[pc[0]] + pc[1];
                pos[v * 2] = bb.getFloat(p);
                pos[v * 2 + 1] = bb.getFloat(p + 4);
                if (uc != null && uc[3] >= 2 && uc[2] == 0) {
                    int q = streamOff[uc[0]] + v * streamStride[uc[0]] + uc[1];
                    uv[v * 2] = bb.getFloat(q);
                    uv[v * 2 + 1] = bb.getFloat(q + 4);
                }
            }
            ArrayList<Object> ib = Loader.l(rd, "m_IndexBuffer");
            int[] idx = new int[ib.size() / 2];
            for (int i = 0; i < idx.length; i++) idx[i] = (Json.i(ib.get(i * 2)) & 0xff) | ((Json.i(ib.get(i * 2 + 1)) & 0xff) << 8);
            boolean noUv = uc == null || uc[3] < 2;
            if (!noUv) {
                noUv = true;
                for (float f : uv) if (f != 0) { noUv = false; break; }
            }
            if (noUv) {
                // no (or zeroed) uv channel: Unity derives the uvs from m_RD.uvTransform (ppu, x origin, ppu, y origin)
                float tw = texture == null ? 1 : texture.width, th = texture == null ? 1 : texture.height;
                HashMap<String, Object> ut = rd.get("uvTransform") instanceof HashMap ? Json.map(rd.get("uvTransform")) : null;
                float sx = pixelsPerUnit, ox = rect.m_XMin + pivot.x, sy = pixelsPerUnit, oy = rect.m_YMin + pivot.y;
                if (ut != null && Loader.f(ut, "x", 0) != 0) {
                    sx = Loader.f(ut, "x", sx); ox = Loader.f(ut, "y", ox); sy = Loader.f(ut, "z", sy); oy = Loader.f(ut, "w", oy);
                }
                for (int v = 0; v < n; v++) {
                    uv[v * 2] = (ox + pos[v * 2] * sx) / tw;
                    uv[v * 2 + 1] = (oy + pos[v * 2 + 1] * sy) / th;
                }
            }
            verts = pos;
            uvs = uv;
            tris = idx;
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static int formatSize(int fmt) {
        switch (fmt) {
            case 0: return 4;            // float32
            case 1: return 2;            // float16
            case 2: case 3: return 1;    // unorm8 / snorm8
            case 4: case 5: return 2;    // unorm16 / snorm16
            default: return 4;
        }
    }
}
