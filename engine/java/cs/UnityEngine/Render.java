package cs.UnityEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Frame rendering: cameras (sorted by depth) draw world renderers and camera/world-space canvases sorted Unity-style
 * (sorting layer, order, render queue, distance), then screen-space overlay canvases. Output goes to the Gfx backend.
 */
public final class Render {
    private Render() {}
    static final ArrayList<Renderer> renderers = new ArrayList<>();
    static final ArrayList<cs.UnityEngine.Rendering.SortingGroup> groups = new ArrayList<>();
    static boolean canvasesDirty = true;
    static long frameSeq;

    static void register(Renderer r) { if (!r.registered) { r.registered = true; renderers.add(r); } }
    static void unregister(Renderer r) { if (r.registered) { r.registered = false; renderers.remove(r); } }
    static void canvasChanged(Canvas c) { canvasesDirty = true; }
    static void hierarchyChanged(Transform t) { canvasesDirty = true; }
    static void screenResized() { canvasesDirty = true; }
    static void releaseTexture(Texture t) { Gfx.current.release(t); }
    static int[] readPixels(int x, int y, int w, int h) { return Gfx.current.readPixels(x, y, w, h); }
    public static cs.UnityEngine.Rendering.SortingGroup sortingGroupByIndex(int i) { return i >= 0 && i < groups.size() ? groups.get(i) : null; }
    static void renderCamera(Camera c) {}

    /** after LateUpdate: drive root canvases, let the UI rebuild (layout/graphics), compute UI depths */
    static void beforeRender() {
        for (Canvas c : new ArrayList<>(Canvas.all)) if (c.get_isActiveAndEnabled()) c.updateRootRect();
        try {
            Canvas.sendWillRender();
        } catch (RuntimeException e) {
            Debug.logException(e, null);
        }
        for (Renderer r : renderers) if (r instanceof TrailRenderer && r.get_isVisible()) ((TrailRenderer) r).$sample();
        computeDepths();
    }

    // ------------------------------------------------------------------ UI traversal
    private static void computeDepths() {
        int[] d = {0};
        ArrayList<Canvas> roots = rootCanvases();
        for (Canvas c : roots) depthWalk(c.gameObject.transform, d);
    }

    private static void depthWalk(Transform t, int[] d) {
        GameObject g = t.gameObject;
        if (!g.get_activeInHierarchy()) return;
        for (Component c : g.components) {
            if (c instanceof CanvasRenderer) {
                CanvasRenderer cr = (CanvasRenderer) c;
                cr.$depth = cr.$mesh != null && !cr.$cull ? d[0]++ : -1;
            }
        }
        for (int i = 0; i < t.children.size(); i++) depthWalk(t.children.get(i), d);
    }

    static ArrayList<Canvas> rootCanvases() {
        ArrayList<Canvas> l = new ArrayList<>();
        for (Canvas c : Canvas.all) if (c.get_isActiveAndEnabled() && c.get_isRootCanvas()) l.add(c);
        return l;
    }

    // ------------------------------------------------------------------ sorting entries
    static final class Entry {
        int layer, order, queue;
        float dist;
        long seq;
        int sub;
        final ArrayList<Gfx.Item> items = new ArrayList<>(1);
    }

    private static final Comparator<Entry> ENTRY_ORDER = new Comparator<Entry>() {
        public int compare(Entry a, Entry b) {
            if (a.queue != b.queue) return a.queue < b.queue ? -1 : 1;
            if (a.layer != b.layer) return a.layer < b.layer ? -1 : 1;
            if (a.order != b.order) return a.order < b.order ? -1 : 1;
            if (a.dist != b.dist) return a.dist > b.dist ? -1 : 1;
            if (a.sub != b.sub) return a.sub < b.sub ? -1 : 1;
            return Long.compare(a.seq, b.seq);
        }
    };

    /** draw the whole frame to the backend */
    public static void renderFrame() {
        Gfx g = Gfx.current;
        int sw = Screen.width, sh = Screen.height;
        g.begin(sw, sh);
        g.viewport(0, 0, sw, sh);
        g.clear(true, true, 0, 0, 0, 1);
        ArrayList<Camera> cams = new ArrayList<>();
        for (Camera c : Camera.all) if (c.get_isActiveAndEnabled() && c.targetTexture == null) cams.add(c);
        Collections.sort(cams, new Comparator<Camera>() {
            public int compare(Camera a, Camera b) { return Float.compare(a.depth, b.depth); }
        });
        ArrayList<Canvas> roots = rootCanvases();
        for (Camera cam : cams) {
            Camera.current = cam;
            Rect pr = cam.get_pixelRect();
            g.viewport((int) pr.m_XMin, (int) pr.m_YMin, (int) pr.m_Width, (int) pr.m_Height);
            if (cam.clearFlags == 2 || cam.clearFlags == 1) g.clear(true, true, cam.backgroundColor.r, cam.backgroundColor.g, cam.backgroundColor.b, cam.backgroundColor.a);
            else if (cam.clearFlags == 3) g.clear(false, true, 0, 0, 0, 0);
            float[] vp = cam.$viewProj().m;
            ArrayList<Entry> entries = new ArrayList<>();
            collectWorld(cam, vp, entries);
            for (Canvas c : roots) {
                int mode = c.renderMode;
                Camera cc = c.worldCamera;
                if (mode == 1 && cc == cam || mode == 2 && (cam.cullingMask & (1 << c.gameObject.m_Layer)) != 0) {
                    collectCanvas(c, vp, cam, entries, mode == 1 ? c.planeDistance : distance(cam, c.gameObject.transform.get_position()));
                }
            }
            Collections.sort(entries, ENTRY_ORDER);
            for (Entry e : entries) for (Gfx.Item it : e.items) g.draw(it);
        }
        Camera.current = null;
        // overlay canvases (and camera canvases without a camera)
        g.viewport(0, 0, sw, sh);
        float[] ovp = Matrix4x4.Ortho(0, sw, 0, sh, -1000, 1000).m;
        ArrayList<Entry> overlay = new ArrayList<>();
        for (Canvas c : roots) {
            if (c.renderMode == 0 || c.renderMode == 1 && (c.worldCamera == null || c.worldCamera.$destroyed || !c.worldCamera.get_isActiveAndEnabled())) {
                collectCanvas(c, ovp, null, overlay, 0);
            }
        }
        Collections.sort(overlay, ENTRY_ORDER);
        for (Entry e : overlay) for (Gfx.Item it : e.items) g.draw(it);
        g.end();
        frameSeq++;
    }

    private static float distance(Camera cam, Vector3 p) {
        Transform t = cam.gameObject.transform;
        Vector3 c = t.get_position(), f = t.get_forward();
        if (cam.transparencySortMode == 1 || cam.transparencySortMode == 0 && cam.orthographic) return (p.x - c.x) * f.x + (p.y - c.y) * f.y + (p.z - c.z) * f.z;
        float dx = p.x - c.x, dy = p.y - c.y, dz = p.z - c.z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    // ------------------------------------------------------------------ world renderers
    private static long seqCounter;

    private static void collectWorld(Camera cam, float[] vp, ArrayList<Entry> out) {
        long seq = 0;
        for (int i = 0, n = renderers.size(); i < n; i++) {
            Renderer r = renderers.get(i);
            seq++;
            if (!r.enabled || r.$destroyed || r.gameObject == null || !r.gameObject.get_activeInHierarchy()) continue;
            if ((cam.cullingMask & (1 << r.gameObject.m_Layer)) == 0) continue;
            Entry e = new Entry();
            e.seq = seq;
            float[] model = r.gameObject.transform.world();
            e.dist = distance(cam, new Vector3(model[12], model[13], model[14]));
            cs.UnityEngine.Rendering.SortingGroup sg = sortingGroupOf(r);
            if (sg != null) {
                e.layer = SortingLayer.GetLayerValueFromID(sg.sortingLayerID);
                e.order = sg.sortingOrder;
                e.dist = distance(cam, sg.get_transform().get_position());
                e.sub = (SortingLayer.GetLayerValueFromID(r.sortingLayerID) << 16) + r.sortingOrder;
            } else {
                e.layer = SortingLayer.GetLayerValueFromID(r.sortingLayerID);
                e.order = r.sortingOrder;
            }
            Material m = r.materials.length > 0 ? r.materials[0] : null;
            e.queue = m == null ? 3000 : m.get_renderQueue();
            if (r instanceof SpriteRenderer) spriteItem((SpriteRenderer) r, vp, model, m, e);
            else if (r instanceof MeshRenderer) meshItems((MeshRenderer) r, vp, model, e);
            else if (r instanceof LineRenderer) lineItem((LineRenderer) r, vp, model, m, e);
            else if (r instanceof TrailRenderer) trailItem((TrailRenderer) r, vp, m, e);
            if (!e.items.isEmpty()) out.add(e);
        }
    }

    private static cs.UnityEngine.Rendering.SortingGroup sortingGroupOf(Renderer r) {
        cs.UnityEngine.Rendering.SortingGroup found = null;
        for (Transform t = r.gameObject.transform; t != null; t = t.parent) {
            for (Component c : t.gameObject.components) {
                if (c instanceof cs.UnityEngine.Rendering.SortingGroup && ((Behaviour) c).m_Enabled && !c.$destroyed) {
                    found = (cs.UnityEngine.Rendering.SortingGroup) c;
                    if (found.sortAtRoot) return found;
                }
            }
            if (found != null) return found;
        }
        return null;
    }

    static void mul(float[] a, float[] b, float[] out) {
        for (int c = 0; c < 4; c++) for (int r = 0; r < 4; r++) {
            out[c * 4 + r] = a[r] * b[c * 4] + a[4 + r] * b[c * 4 + 1] + a[8 + r] * b[c * 4 + 2] + a[12 + r] * b[c * 4 + 3];
        }
    }

    private static Gfx.Item item(float[] vp, float[] model, Material m, MaterialPropertyBlock block) {
        Gfx.Item it = new Gfx.Item();
        mul(vp, model, it.mvp);
        System.arraycopy(model, 0, it.model, 0, 16);
        it.mat = m;
        it.block = block;
        it.shader = m == null || m.shader == null ? "Sprites/Default" : m.shader.m_Name;
        applyStencil(it, m);
        return it;
    }

    private static void applyStencil(Gfx.Item it, Material m) {
        if (m == null) return;
        MaterialPropertyBlock p = m.props;
        it.stencilRef = (int) p.$float(STENCIL, 0);
        it.stencilComp = (int) p.$float(STENCIL_COMP, 8);
        it.stencilOp = (int) p.$float(STENCIL_OP, 0);
        it.stencilRead = (int) p.$float(STENCIL_READ, 255);
        it.stencilWrite = (int) p.$float(STENCIL_WRITE, 255);
        it.colorMask = (int) p.$float(COLOR_MASK, 15);
    }

    static final int STENCIL = Shader.PropertyToID("_Stencil"), STENCIL_COMP = Shader.PropertyToID("_StencilComp"),
        STENCIL_OP = Shader.PropertyToID("_StencilOp"), STENCIL_READ = Shader.PropertyToID("_StencilReadMask"),
        STENCIL_WRITE = Shader.PropertyToID("_StencilWriteMask"), COLOR_MASK = Shader.PropertyToID("_ColorMask"),
        MAIN_TEX = Shader.PropertyToID("_MainTex");

    private static void spriteItem(SpriteRenderer r, float[] vp, float[] model, Material m, Entry e) {
        Sprite s = r.sprite;
        if (s == null || s.verts == null) return;
        Gfx.Item it = item(vp, model, m, r.block);
        it.world = true;
        it.src = r;
        Texture t = r.block != null ? r.block.$texture(MAIN_TEX) : null;
        it.tex = t != null ? t : s.texture;
        it.tint[0] = r.color.r; it.tint[1] = r.color.g; it.tint[2] = r.color.b; it.tint[3] = r.color.a;
        if (r.drawMode != 0 && (s.border.x > 0 || s.border.y > 0 || s.border.z > 0 || s.border.w > 0)) {
            slicedMesh(s, r.size.x, r.size.y, it);
        } else if (r.drawMode != 0) {
            // simple sprite stretched to size
            float sx = r.size.x / (s.rect.m_Width / s.pixelsPerUnit), sy = r.size.y / (s.rect.m_Height / s.pixelsPerUnit);
            fill(it, s.verts, s.uvs, s.tris, sx, sy);
        } else fill(it, s.verts, s.uvs, s.tris, 1, 1);
        if (r.flipX || r.flipY) {
            for (int i = 0; i < it.count; i++) {
                if (r.flipX) it.pos[i * 3] = -it.pos[i * 3];
                if (r.flipY) it.pos[i * 3 + 1] = -it.pos[i * 3 + 1];
            }
        }
        e.items.add(it);
    }

    private static void fill(Gfx.Item it, float[] v, float[] uv, int[] tris, float sx, float sy) {
        int n = v.length / 2;
        it.count = n;
        it.pos = new float[n * 3];
        it.uv0 = new float[n * 4];
        for (int i = 0; i < n; i++) {
            it.pos[i * 3] = v[i * 2] * sx;
            it.pos[i * 3 + 1] = v[i * 2 + 1] * sy;
            it.uv0[i * 4] = uv[i * 2];
            it.uv0[i * 4 + 1] = uv[i * 2 + 1];
        }
        it.idx = tris;
        it.idxCount = tris.length;
    }

    /** 9-slice for SpriteRenderer drawMode Sliced/Tiled (tiled approximated by slicing) */
    private static void slicedMesh(Sprite s, float w, float h, Gfx.Item it) {
        float ppu = s.pixelsPerUnit;
        float px = s.pivot.x / s.rect.m_Width, py = s.pivot.y / s.rect.m_Height;
        float l = s.border.x / ppu, b = s.border.y / ppu, rr = s.border.z / ppu, t = s.border.w / ppu;
        float x0 = -px * w, y0 = -py * h;
        float[] xs = {x0, x0 + l, x0 + w - rr, x0 + w}, ys = {y0, y0 + b, y0 + h - t, y0 + h};
        float tw = s.texture == null ? 1 : s.texture.width, th = s.texture == null ? 1 : s.texture.height;
        float u0 = s.textureRect.m_XMin / tw, v0 = s.textureRect.m_YMin / th;
        float u3 = (s.textureRect.m_XMin + s.textureRect.m_Width) / tw, v3 = (s.textureRect.m_YMin + s.textureRect.m_Height) / th;
        float[] us = {u0, u0 + s.border.x / tw, u3 - s.border.z / tw, u3}, vs = {v0, v0 + s.border.y / th, v3 - s.border.w / th, v3};
        it.count = 16;
        it.pos = new float[48];
        it.uv0 = new float[64];
        for (int j = 0; j < 4; j++) for (int i = 0; i < 4; i++) {
            int k = j * 4 + i;
            it.pos[k * 3] = xs[i]; it.pos[k * 3 + 1] = ys[j];
            it.uv0[k * 4] = us[i]; it.uv0[k * 4 + 1] = vs[j];
        }
        it.idx = new int[54];
        int n = 0;
        for (int j = 0; j < 3; j++) for (int i = 0; i < 3; i++) {
            int a = j * 4 + i;
            it.idx[n++] = a; it.idx[n++] = a + 4; it.idx[n++] = a + 5;
            it.idx[n++] = a + 5; it.idx[n++] = a + 1; it.idx[n++] = a;
        }
        it.idxCount = 54;
    }

    private static void meshItems(MeshRenderer r, float[] vp, float[] model, Entry e) {
        Mesh mesh = r.$mesh();
        if (mesh == null || mesh.$count == 0) return;
        for (int s = 0; s < mesh.$sub.length; s++) {
            Material m = s < r.materials.length ? r.materials[s] : (r.materials.length > 0 ? r.materials[r.materials.length - 1] : null);
            Gfx.Item it = item(vp, model, m, r.block);
            it.world = true;
            it.count = mesh.$count;
            it.pos = mesh.$pos;
            it.col = mesh.$col;
            it.uv0 = mesh.$uv[0];
            it.uv1 = mesh.$uv[1];
            it.idx = mesh.$sub[s];
            it.idxCount = it.idx.length;
            it.tex = m == null ? null : m.props.$texture(MAIN_TEX);
            e.items.add(it);
        }
    }

    private static void lineItem(LineRenderer r, float[] vp, float[] model, Material m, Entry e) {
        Vector3[] p = r.positions;
        int n = p.length;
        if (n < 2) return;
        float[] ident = Matrix4x4.get_identity().m;
        Gfx.Item it = item(vp, r.useWorldSpace ? ident : model, m, r.block);
        it.world = true;
        it.count = n * 2;
        it.pos = new float[n * 6];
        it.uv0 = new float[n * 8];
        it.col = new int[n * 2];
        for (int i = 0; i < n; i++) {
            Vector3 a = p[Math.max(0, i - 1)], b = p[Math.min(n - 1, i + 1)];
            float dx = b.x - a.x, dy = b.y - a.y, len = (float) Math.sqrt(dx * dx + dy * dy);
            float nx = len == 0 ? 0 : -dy / len, ny = len == 0 ? 1 : dx / len;
            float tt = (float) i / (n - 1);
            float w = (r.startWidth + (r.endWidth - r.startWidth) * tt) / 2;
            it.pos[i * 6] = p[i].x + nx * w; it.pos[i * 6 + 1] = p[i].y + ny * w; it.pos[i * 6 + 2] = p[i].z;
            it.pos[i * 6 + 3] = p[i].x - nx * w; it.pos[i * 6 + 4] = p[i].y - ny * w; it.pos[i * 6 + 5] = p[i].z;
            it.uv0[i * 8] = tt; it.uv0[i * 8 + 1] = 1; it.uv0[i * 8 + 4] = tt; it.uv0[i * 8 + 5] = 0;
            Color c = Color.Lerp(r.startColor, r.endColor, tt);
            int ci = Texture2D.fromColor(c);
            it.col[i * 2] = ci; it.col[i * 2 + 1] = ci;
        }
        strip(it, n);
        it.tex = m == null ? null : m.props.$texture(MAIN_TEX);
        e.items.add(it);
    }

    private static void trailItem(TrailRenderer r, float[] vp, Material m, Entry e) {
        ArrayList<float[]> pts = r.$points;
        int n = pts.size();
        if (n < 2) return;
        Gfx.Item it = item(vp, Matrix4x4.get_identity().m, m, r.block);
        it.world = true;
        it.count = n * 2;
        it.pos = new float[n * 6];
        it.uv0 = new float[n * 8];
        it.col = new int[n * 2];
        float now = Time.time;
        for (int i = 0; i < n; i++) {
            float[] a = pts.get(Math.max(0, i - 1)), b = pts.get(Math.min(n - 1, i + 1)), q = pts.get(i);
            float dx = b[0] - a[0], dy = b[1] - a[1], len = (float) Math.sqrt(dx * dx + dy * dy);
            float nx = len == 0 ? 0 : -dy / len, ny = len == 0 ? 1 : dx / len;
            float age = r.time <= 0 ? 0 : Math.min(1, (now - q[3]) / r.time);
            float tt = 1 - age;
            float w = r.$widthCurve().Evaluate(age) * r.$widthMul() / 2;
            it.pos[i * 6] = q[0] + nx * w; it.pos[i * 6 + 1] = q[1] + ny * w; it.pos[i * 6 + 2] = q[2];
            it.pos[i * 6 + 3] = q[0] - nx * w; it.pos[i * 6 + 4] = q[1] - ny * w; it.pos[i * 6 + 5] = q[2];
            it.uv0[i * 8] = tt; it.uv0[i * 8 + 1] = 1; it.uv0[i * 8 + 4] = tt;
            int ci = Texture2D.fromColor(r.$gradient().Evaluate(age));
            it.col[i * 2] = ci; it.col[i * 2 + 1] = ci;
        }
        strip(it, n);
        it.tex = m == null ? null : m.props.$texture(MAIN_TEX);
        e.items.add(it);
    }

    private static void strip(Gfx.Item it, int n) {
        it.idx = new int[(n - 1) * 6];
        for (int i = 0; i < n - 1; i++) {
            int a = i * 2;
            it.idx[i * 6] = a; it.idx[i * 6 + 1] = a + 1; it.idx[i * 6 + 2] = a + 3;
            it.idx[i * 6 + 3] = a + 3; it.idx[i * 6 + 4] = a + 2; it.idx[i * 6 + 5] = a;
        }
        it.idxCount = it.idx.length;
    }

    // ------------------------------------------------------------------ canvases
    private static void collectCanvas(Canvas root, float[] vp, Camera cam, ArrayList<Entry> out, float dist) {
        Entry e = new Entry();
        e.layer = SortingLayer.GetLayerValueFromID(root.sortingLayerID);
        e.order = root.sortingOrder;
        e.queue = 3000;
        e.dist = dist;
        e.seq = root.$id;
        root.$renderOrder = out.size();
        float[] rootInv = Matrix4x4.Inverse(root.gameObject.transform.get_localToWorldMatrix()).m;
        walkCanvas(root.gameObject.transform, root, vp, cam, e, out, dist, 1f, rootInv);
        if (!e.items.isEmpty()) out.add(e);
    }

    private static void walkCanvas(Transform t, Canvas root, float[] vp, Camera cam, Entry e, ArrayList<Entry> out, float dist, float alpha, float[] rootInv) {
        GameObject g = t.gameObject;
        if (!g.get_activeInHierarchy()) return;
        CanvasRenderer pop = null;
        for (int ci = 0, n = g.components.size(); ci < n; ci++) {
            Component c = g.components.get(ci);
            if (c instanceof Canvas && c != root && ((Canvas) c).m_Enabled && ((Canvas) c).overrideSorting && !c.$destroyed) {
                // nested canvas with its own sorting: separate entry
                Canvas nc = (Canvas) c;
                Entry ne = new Entry();
                ne.layer = SortingLayer.GetLayerValueFromID(nc.sortingLayerID);
                ne.order = nc.sortingOrder;
                ne.queue = 3000;
                ne.dist = dist;
                ne.seq = nc.$id;
                walkChildrenAndSelf(t, root, vp, cam, ne, out, dist, alpha, rootInv);
                if (!ne.items.isEmpty()) out.add(ne);
                return;
            }
        }
        walkChildrenAndSelf(t, root, vp, cam, e, out, dist, alpha, rootInv);
    }

    private static void walkChildrenAndSelf(Transform t, Canvas root, float[] vp, Camera cam, Entry e, ArrayList<Entry> out, float dist, float alpha, float[] rootInv) {
        GameObject g = t.gameObject;
        CanvasRenderer pop = null;
        for (int ci = 0, n = g.components.size(); ci < n; ci++) {
            Component c = g.components.get(ci);
            if (c instanceof CanvasGroup && ((CanvasGroup) c).m_Enabled && !c.$destroyed) {
                CanvasGroup cg = (CanvasGroup) c;
                alpha = cg.ignoreParentGroups ? cg.alpha : alpha * cg.alpha;
            }
        }
        for (int ci = 0, n = g.components.size(); ci < n; ci++) {
            Component c = g.components.get(ci);
            if (!(c instanceof CanvasRenderer) || c.$destroyed) continue;
            CanvasRenderer cr = (CanvasRenderer) c;
            if (cr.$mesh == null || cr.$cull || cr.$mesh.$count == 0) continue;
            float a = cr.$color.a * alpha;
            if (a <= 0 && cr.cullTransparentMesh) { if (cr.hasPopInstruction) pop = cr; continue; }
            Material m = cr.$materials.length > 0 && cr.$materials[0] != null ? cr.$materials[0] : Canvas.GetDefaultCanvasMaterial();
            e.items.add(uiItem(cr, m, vp, cam, a, root, rootInv));
            if (cr.hasPopInstruction && cr.$popMaterials.length > 0) pop = cr;
        }
        for (int i = 0; i < t.children.size(); i++) walkCanvas(t.children.get(i), root, vp, cam, e, out, dist, alpha, rootInv);
        if (pop != null) {
            for (Material pm : pop.$popMaterials) if (pm != null) e.items.add(uiItem(pop, pm, vp, cam, pop.$color.a * alpha, root, rootInv));
        }
    }

    private static Gfx.Item uiItem(CanvasRenderer cr, Material m, float[] vp, Camera cam, float alpha, Canvas root, float[] rootInv) {
        float[] model = cr.gameObject.transform.world();
        Gfx.Item it = item(vp, model, m, null);
        Mesh mesh = cr.$mesh;
        it.count = mesh.$count;
        it.pos = mesh.$pos;
        it.col = mesh.$col;
        it.uv0 = mesh.$uv[0];
        it.uv1 = mesh.$uv[1];
        it.idx = mesh.$sub[0];
        it.idxCount = it.idx.length;
        it.tex = cr.$texture;
        it.tint[0] = cr.$color.r; it.tint[1] = cr.$color.g; it.tint[2] = cr.$color.b; it.tint[3] = alpha;
        if (cr.$clip) scissorFor(cr.$clipRect, root, vp, it);
        return it;
    }

    /** clip rect (root canvas space) -> screen scissor box */
    private static void scissorFor(Rect r, Canvas root, float[] vp, Gfx.Item it) {
        float[] rm = root.gameObject.transform.world();
        float[] full = new float[16];
        mul(vp, rm, full);
        float minX = Float.POSITIVE_INFINITY, minY = minX, maxX = Float.NEGATIVE_INFINITY, maxY = maxX;
        for (int k = 0; k < 4; k++) {
            float x = (k & 1) == 0 ? r.m_XMin : r.m_XMin + r.m_Width, y = (k & 2) == 0 ? r.m_YMin : r.m_YMin + r.m_Height;
            float cx = full[0] * x + full[4] * y + full[12], cy = full[1] * x + full[5] * y + full[13], cw = full[3] * x + full[7] * y + full[15];
            if (cw == 0) cw = 1;
            float sx = (cx / cw + 1) * .5f * Screen.width, sy = (cy / cw + 1) * .5f * Screen.height;
            minX = Math.min(minX, sx); maxX = Math.max(maxX, sx); minY = Math.min(minY, sy); maxY = Math.max(maxY, sy);
        }
        it.scissor = true;
        it.sx = Math.round(minX);
        it.sy = Math.round(minY);
        it.sw = Math.max(0, Math.round(maxX) - it.sx);
        it.sh = Math.max(0, Math.round(maxY) - it.sy);
    }
}
