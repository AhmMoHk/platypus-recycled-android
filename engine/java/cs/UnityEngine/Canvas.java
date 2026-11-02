package cs.UnityEngine;

import java.util.ArrayList;

/** RenderMode: ScreenSpaceOverlay 0, ScreenSpaceCamera 1, WorldSpace 2 */
public class Canvas extends Behaviour {
    static final ArrayList<Canvas> all = new ArrayList<>();
    static Canvas$WillRenderCanvases willRender, preWillRender;
    static Material defaultMaterial;
    int renderMode, sortingLayerID, sortingOrder, targetDisplay, additionalShaderChannels;
    Camera worldCamera;
    float planeDistance = 100, scaleFactor = 1, referencePixelsPerUnit = 100, normalizedSortingGridSize = .1f;
    boolean pixelPerfect, overrideSorting, overridePixelPerfect, vertexColorAlwaysGammaSpace;
    public int $renderOrder;

    public static void add_willRenderCanvases(Canvas$WillRenderCanvases d) { willRender = (Canvas$WillRenderCanvases) cs.System.Delegate.Combine(willRender, d); }
    public static void remove_willRenderCanvases(Canvas$WillRenderCanvases d) { willRender = (Canvas$WillRenderCanvases) cs.System.Delegate.Remove(willRender, d); }
    public static void add_preWillRenderCanvases(Canvas$WillRenderCanvases d) { preWillRender = (Canvas$WillRenderCanvases) cs.System.Delegate.Combine(preWillRender, d); }
    public static void remove_preWillRenderCanvases(Canvas$WillRenderCanvases d) { preWillRender = (Canvas$WillRenderCanvases) cs.System.Delegate.Remove(preWillRender, d); }
    public static void ForceUpdateCanvases() { sendWillRender(); }
    static void sendWillRender() {
        if (preWillRender != null) preWillRender.Invoke();
        if (willRender != null) willRender.Invoke();
    }
    public static Material GetDefaultCanvasMaterial() {
        if (defaultMaterial == null) {
            defaultMaterial = new Material(Shader.Find("UI/Default"));
            defaultMaterial.m_Name = "Default UI Material";
            defaultMaterial.$asset = true;
        }
        return defaultMaterial;
    }
    public static Material GetETC1SupportedCanvasMaterial() { return GetDefaultCanvasMaterial(); }

    public int get_renderMode() {
        Canvas r = get_rootCanvas();
        return r == this ? renderMode : r.get_renderMode();
    }
    public void set_renderMode(int m) { renderMode = m; Render.canvasChanged(this); }
    public Camera get_worldCamera() { return worldCamera; }
    public void set_worldCamera(Camera c) { worldCamera = c; Render.canvasChanged(this); }
    public float get_planeDistance() { return planeDistance; }
    public void set_planeDistance(float d) { planeDistance = d; Render.canvasChanged(this); }
    public float get_scaleFactor() {
        Canvas r = get_rootCanvas();
        return r == this ? scaleFactor : r.scaleFactor;
    }
    public void set_scaleFactor(float f) {
        if (!get_isRootCanvas()) return;
        if (f != scaleFactor) { scaleFactor = Math.max(f, 1e-4f); Render.canvasChanged(this); }
    }
    public float get_referencePixelsPerUnit() { return referencePixelsPerUnit; }
    public void set_referencePixelsPerUnit(float v) { referencePixelsPerUnit = v; }
    public boolean get_pixelPerfect() { return pixelPerfect; }
    public void set_pixelPerfect(boolean v) { pixelPerfect = v; }
    public boolean get_overrideSorting() { return overrideSorting; }
    public void set_overrideSorting(boolean v) { overrideSorting = v; }
    public boolean get_overridePixelPerfect() { return overridePixelPerfect; }
    public void set_overridePixelPerfect(boolean v) { overridePixelPerfect = v; }
    public int get_sortingLayerID() { return sortingLayerID; }
    public void set_sortingLayerID(int id) { sortingLayerID = id; }
    public String get_sortingLayerName() { String n = SortingLayer.IDToName(sortingLayerID); return n == null ? "Default" : n; }
    public void set_sortingLayerName(String n) { sortingLayerID = SortingLayer.NameToID(n); }
    public int get_sortingOrder() { return sortingOrder; }
    public void set_sortingOrder(int o) { sortingOrder = o; }
    public int get_targetDisplay() { return targetDisplay; }
    public void set_targetDisplay(int d) { targetDisplay = d; }
    public int get_additionalShaderChannels() { return additionalShaderChannels; }
    public void set_additionalShaderChannels(int c) { additionalShaderChannels = c; }
    public int get_renderOrder() { return $renderOrder; }
    public float get_normalizedSortingGridSize() { return normalizedSortingGridSize; }
    public void set_normalizedSortingGridSize(float v) { normalizedSortingGridSize = v; }
    public boolean get_vertexColorAlwaysGammaSpace() { return vertexColorAlwaysGammaSpace; }
    public void set_vertexColorAlwaysGammaSpace(boolean v) { vertexColorAlwaysGammaSpace = v; }
    public Vector2 get_renderingDisplaySize() { return new Vector2(Screen.width, Screen.height); }
    public int get_cachedSortingLayerValue() { return SortingLayer.GetLayerValueFromID(sortingLayerID); }

    public boolean get_isRootCanvas() { return parentCanvas() == null; }
    public Canvas get_rootCanvas() {
        Canvas c = this, p;
        while ((p = c.parentCanvas()) != null) c = p;
        return c;
    }
    Canvas parentCanvas() {
        if (gameObject == null) return null;
        for (Transform t = gameObject.transform.parent; t != null; t = t.parent) {
            for (Component c : t.gameObject.components) if (c instanceof Canvas && !c.$destroyed && ((Canvas) c).m_Enabled) return (Canvas) c;
        }
        return null;
    }

    /** drive the root canvas RectTransform from the screen / camera (screen-space modes) */
    void updateRootRect() {
        if (!get_isRootCanvas() || gameObject == null || !(gameObject.transform instanceof RectTransform)) return;
        int mode = renderMode;
        if (mode == 2) return;
        RectTransform rt = (RectTransform) gameObject.transform;
        float sw = Screen.width, sh = Screen.height, sf = scaleFactor;
        Camera cam = mode == 1 ? worldCamera : null;
        if (cam != null && cam.$destroyed) cam = null;
        float w = sw / sf, h = sh / sf;
        Vector3 pos, scale;
        Quaternion rot;
        if (cam == null) {
            pos = new Vector3(sw / 2, sh / 2, 0);
            rot = Quaternion.get_identity();
            scale = new Vector3(sf, sf, sf);
        } else {
            Transform ct = cam.gameObject.transform;
            Vector3 cp = ct.get_position();
            rot = ct.get_rotation();
            Vector3 fwd = Quaternion.op_Multiply(rot, new Vector3(0, 0, 1));
            pos = new Vector3(cp.x + fwd.x * planeDistance, cp.y + fwd.y * planeDistance, cp.z + fwd.z * planeDistance);
            float worldH = cam.orthographic ? cam.orthographicSize * 2 : 2 * planeDistance * (float) Math.tan(Math.toRadians(cam.fieldOfView) / 2);
            Rect pr = cam.get_pixelRect();
            if (pr.m_Height > 0) { w = pr.m_Width / sf; h = pr.m_Height / sf; }
            float s = worldH / h;
            scale = new Vector3(s, s, s);
        }
        boolean changed = rt.sizeDelta.x != w || rt.sizeDelta.y != h;
        rt.anchorMin.Set(0, 0);
        rt.anchorMax.Set(0, 0);
        rt.pivot.Set(.5f, .5f);
        rt.sizeDelta.Set(w, h);
        rt.anchoredPosition.Set(pos.x, pos.y);
        rt.lp.Set(pos.x, pos.y, pos.z);
        rt.lr.$set(rot);
        rt.ls.$set(scale);
        rt.dirty();
        if (changed) rt.relayout();
    }

    @Override public void $nativeEnable() { if (!all.contains(this)) all.add(this); Render.canvasChanged(this); }
    @Override public void $nativeDisable() { all.remove(this); Render.canvasChanged(this); }
    @Override public void $onDestroyNative() { all.remove(this); }

    @Override public void $load(java.util.HashMap<String, Object> d, Loader.Ctx ctx) {
        renderMode = Loader.i(d, "m_RenderMode", 0);
        UObject c = ctx.ref(d.get("m_Camera"));
        worldCamera = c instanceof Camera ? (Camera) c : null;
        planeDistance = Loader.f(d, "m_PlaneDistance", 100);
        pixelPerfect = Loader.b(d, "m_PixelPerfect", false);
        overrideSorting = Loader.b(d, "m_OverrideSorting", false);
        overridePixelPerfect = Loader.b(d, "m_OverridePixelPerfect", false);
        normalizedSortingGridSize = Loader.f(d, "m_SortingBucketNormalizedSize", .1f);
        vertexColorAlwaysGammaSpace = Loader.b(d, "m_VertexColorAlwaysGammaSpace", false);
        additionalShaderChannels = Loader.i(d, "m_AdditionalShaderChannelsFlag", 0);
        sortingLayerID = (int) cs.rt.Json.lng(d.containsKey("m_SortingLayerID") ? d.get("m_SortingLayerID") : 0L);
        sortingOrder = Loader.i(d, "m_SortingOrder", 0);
        targetDisplay = Loader.i(d, "m_TargetDisplay", 0);
    }
    @Override public void $cloneFrom(Component src, Eng.CloneCtx ctx) {
        Canvas s = (Canvas) src;
        renderMode = s.renderMode; worldCamera = (Camera) ctx.remap(s.worldCamera); planeDistance = s.planeDistance; pixelPerfect = s.pixelPerfect;
        overrideSorting = s.overrideSorting; sortingLayerID = s.sortingLayerID; sortingOrder = s.sortingOrder; additionalShaderChannels = s.additionalShaderChannels;
        scaleFactor = s.scaleFactor; referencePixelsPerUnit = s.referencePixelsPerUnit;
    }
}
