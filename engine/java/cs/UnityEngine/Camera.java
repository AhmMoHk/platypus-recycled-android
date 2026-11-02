package cs.UnityEngine;

import java.util.ArrayList;

/** CameraClearFlags: Skybox 1, SolidColor 2, Depth 3, Nothing 4 */
public class Camera extends Behaviour {
    static final ArrayList<Camera> all = new ArrayList<>();
    static Camera current;
    boolean orthographic = true;
    float orthographicSize = 5, fieldOfView = 60, near = .3f, far = 1000, depth;
    final Rect rect = new Rect(0, 0, 1, 1);
    int cullingMask = -1, clearFlags = 1, transparencySortMode, targetDisplay;
    final Color backgroundColor = new Color(.19f, .3f, .47f, 0);
    RenderTexture targetTexture;
    float aspectOverride = -1;
    boolean allowHDR, allowMSAA;

    public static Camera get_main() {
        Camera best = null;
        for (Camera c : all) if (c.get_isActiveAndEnabled() && "MainCamera".equals(c.gameObject.m_TagString) && (best == null || c.depth < best.depth)) best = c;
        return best;
    }
    public static Camera get_current() { return current; }
    public static Camera[] get_allCameras() {
        ArrayList<Camera> l = new ArrayList<>();
        for (Camera c : all) if (c.get_isActiveAndEnabled()) l.add(c);
        return l.toArray(new Camera[0]);
    }
    public static int get_allCamerasCount() { return get_allCameras().length; }
    public static int GetAllCameras(Camera[] out) { Camera[] a = get_allCameras(); int n = Math.min(a.length, out.length); System.arraycopy(a, 0, out, 0, n); return n; }

    public boolean get_orthographic() { return orthographic; }
    public void set_orthographic(boolean v) { orthographic = v; }
    public float get_orthographicSize() { return orthographicSize; }
    public void set_orthographicSize(float v) { orthographicSize = v; }
    public float get_fieldOfView() { return fieldOfView; }
    public void set_fieldOfView(float v) { fieldOfView = v; }
    public float get_nearClipPlane() { return near; }
    public void set_nearClipPlane(float v) { near = v; }
    public float get_farClipPlane() { return far; }
    public void set_farClipPlane(float v) { far = v; }
    public float get_depth() { return depth; }
    public void set_depth(float v) { depth = v; }
    public Rect get_rect() { return rect.$copy(); }
    public void set_rect(Rect r) { rect.$set(r); }
    public int get_cullingMask() { return cullingMask; }
    public void set_cullingMask(int m) { cullingMask = m; }
    public int get_clearFlags() { return clearFlags; }
    public void set_clearFlags(int f) { clearFlags = f; }
    public Color get_backgroundColor() { return backgroundColor.$copy(); }
    public void set_backgroundColor(Color c) { backgroundColor.$set(c); }
    public int get_transparencySortMode() { return transparencySortMode; }
    public void set_transparencySortMode(int m) { transparencySortMode = m; }
    public int get_targetDisplay() { return targetDisplay; }
    public void set_targetDisplay(int d) { targetDisplay = d; }
    public RenderTexture get_targetTexture() { return targetTexture; }
    public void set_targetTexture(RenderTexture t) { targetTexture = t; }
    public boolean get_allowHDR() { return allowHDR; }
    public void set_allowHDR(boolean v) { allowHDR = v; }
    public boolean get_allowMSAA() { return allowMSAA; }
    public void set_allowMSAA(boolean v) { allowMSAA = v; }
    public void set_useOcclusionCulling(boolean v) {}
    public int get_eventMask() { return -1; }

    int targetW() { return targetTexture != null ? targetTexture.width : Screen.width; }
    int targetH() { return targetTexture != null ? targetTexture.height : Screen.height; }
    public Rect get_pixelRect() {
        int w = targetW(), h = targetH();
        float x0 = Mathf.Clamp01(rect.m_XMin), y0 = Mathf.Clamp01(rect.m_YMin);
        float x1 = Mathf.Clamp01(rect.m_XMin + rect.m_Width), y1 = Mathf.Clamp01(rect.m_YMin + rect.m_Height);
        return new Rect(x0 * w, y0 * h, (x1 - x0) * w, (y1 - y0) * h);
    }
    public void set_pixelRect(Rect r) { int w = targetW(), h = targetH(); rect.$set(new Rect(r.m_XMin / w, r.m_YMin / h, r.m_Width / w, r.m_Height / h)); }
    public int get_pixelWidth() { return (int) get_pixelRect().m_Width; }
    public int get_pixelHeight() { return (int) get_pixelRect().m_Height; }
    public int get_scaledPixelWidth() { return get_pixelWidth(); }
    public int get_scaledPixelHeight() { return get_pixelHeight(); }
    public float get_aspect() {
        if (aspectOverride > 0) return aspectOverride;
        Rect p = get_pixelRect();
        return p.m_Height == 0 ? 1 : p.m_Width / p.m_Height;
    }
    public void set_aspect(float a) { aspectOverride = a; }
    public void ResetAspect() { aspectOverride = -1; }

    // ------------------------------------------------------------------ matrices
    public Matrix4x4 get_worldToCameraMatrix() {
        Transform t = gameObject.transform;
        Matrix4x4 trs = Matrix4x4.TRS(t.get_position(), t.get_rotation(), new Vector3(1, 1, 1));
        return Matrix4x4.op_Multiply(Matrix4x4.Scale(new Vector3(1, 1, -1)), Matrix4x4.Inverse(trs));
    }
    public Matrix4x4 get_cameraToWorldMatrix() { return Matrix4x4.Inverse(get_worldToCameraMatrix()); }
    public Matrix4x4 get_projectionMatrix() {
        float a = get_aspect();
        if (orthographic) return Matrix4x4.Ortho(-orthographicSize * a, orthographicSize * a, -orthographicSize, orthographicSize, near, far);
        return Matrix4x4.Perspective(fieldOfView, a, near, far);
    }
    public Matrix4x4 $viewProj() { return Matrix4x4.op_Multiply(get_projectionMatrix(), get_worldToCameraMatrix()); }

    public Vector3 WorldToScreenPoint(Vector3 p) {
        Vector3 v = WorldToViewportPoint(p);
        Rect r = get_pixelRect();
        return new Vector3(r.m_XMin + v.x * r.m_Width, r.m_YMin + v.y * r.m_Height, v.z);
    }
    public Vector3 WorldToScreenPoint(Vector3 p, int eye) { return WorldToScreenPoint(p); }
    public Vector3 WorldToViewportPoint(Vector3 p) {
        Matrix4x4 vm = get_worldToCameraMatrix();
        Vector3 cp = vm.MultiplyPoint(p);
        Vector4 c = Matrix4x4.op_Multiply(get_projectionMatrix(), new Vector4(cp.x, cp.y, cp.z, 1));
        float w = c.w == 0 ? 1 : c.w;
        return new Vector3((c.x / w + 1) * .5f, (c.y / w + 1) * .5f, -cp.z);
    }
    public Vector3 ViewportToWorldPoint(Vector3 v) {
        float nx = v.x * 2 - 1, ny = v.y * 2 - 1;
        Matrix4x4 inv = Matrix4x4.Inverse(get_projectionMatrix());
        if (orthographic) {
            Vector4 q = Matrix4x4.op_Multiply(inv, new Vector4(nx, ny, 0, 1));
            Vector3 camPt = new Vector3(q.x, q.y, -v.z);
            return get_cameraToWorldMatrix().MultiplyPoint(camPt);
        }
        Vector4 q = Matrix4x4.op_Multiply(inv, new Vector4(nx, ny, 1, 1));
        Vector3 dir = new Vector3(q.x / q.w, q.y / q.w, q.z / q.w);
        float s = v.z / -dir.z;
        return get_cameraToWorldMatrix().MultiplyPoint(new Vector3(dir.x * s, dir.y * s, dir.z * s));
    }
    public Vector3 ScreenToWorldPoint(Vector3 s) { return ViewportToWorldPoint(ScreenToViewportPoint(s)); }
    public Vector3 ScreenToViewportPoint(Vector3 s) {
        Rect r = get_pixelRect();
        return new Vector3(r.m_Width == 0 ? 0 : (s.x - r.m_XMin) / r.m_Width, r.m_Height == 0 ? 0 : (s.y - r.m_YMin) / r.m_Height, s.z);
    }
    public Vector3 ViewportToScreenPoint(Vector3 v) {
        Rect r = get_pixelRect();
        return new Vector3(r.m_XMin + v.x * r.m_Width, r.m_YMin + v.y * r.m_Height, v.z);
    }
    public Ray ScreenPointToRay(Vector3 s) {
        Vector3 o = ScreenToWorldPoint(new Vector3(s.x, s.y, near));
        Vector3 f = ScreenToWorldPoint(new Vector3(s.x, s.y, far));
        return new Ray(o, Vector3.op_Subtraction(f, o).get_normalized());
    }
    public Ray ViewportPointToRay(Vector3 v) { return ScreenPointToRay(ViewportToScreenPoint(v)); }
    public void Render() { Render.renderCamera(this); }

    @Override public void $nativeEnable() { if (!all.contains(this)) all.add(this); }
    @Override public void $nativeDisable() { all.remove(this); }
    @Override public void $onDestroyNative() { all.remove(this); }

    @Override public void $load(java.util.HashMap<String, Object> d, Loader.Ctx ctx) {
        clearFlags = Loader.i(d, "m_ClearFlags", 1);
        Loader.color(d.get("m_BackGroundColor"), backgroundColor);
        Loader.rect(d.get("m_NormalizedViewPortRect"), rect);
        near = Loader.f(d, "near clip plane", .3f);
        far = Loader.f(d, "far clip plane", 1000);
        fieldOfView = Loader.f(d, "field of view", 60);
        orthographic = Loader.b(d, "orthographic", false);
        orthographicSize = Loader.f(d, "orthographic size", 5);
        depth = Loader.f(d, "m_Depth", 0);
        cullingMask = (int) cs.rt.Json.lng(Loader.m(d, "m_CullingMask").get("m_Bits"));
        UObject t = ctx.ref(d.get("m_TargetTexture"));
        targetTexture = t instanceof RenderTexture ? (RenderTexture) t : null;
        targetDisplay = Loader.i(d, "m_TargetDisplay", 0);
        allowHDR = Loader.b(d, "m_HDR", false);
        allowMSAA = Loader.b(d, "m_AllowMSAA", false);
    }
    @Override public void $cloneFrom(Component src, Eng.CloneCtx ctx) {
        Camera s = (Camera) src;
        clearFlags = s.clearFlags; backgroundColor.$set(s.backgroundColor); rect.$set(s.rect); near = s.near; far = s.far;
        fieldOfView = s.fieldOfView; orthographic = s.orthographic; orthographicSize = s.orthographicSize; depth = s.depth;
        cullingMask = s.cullingMask; targetTexture = s.targetTexture; targetDisplay = s.targetDisplay;
    }
}
