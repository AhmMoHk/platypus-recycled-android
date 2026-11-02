package cs.UnityEngine;

public final class RectTransformUtility {
    private RectTransformUtility() {}

    public static Vector2 WorldToScreenPoint(Camera cam, Vector3 p) {
        if (cam == null) return new Vector2(p.x, p.y);
        Vector3 s = cam.WorldToScreenPoint(p);
        return new Vector2(s.x, s.y);
    }
    public static Ray ScreenPointToRay(Camera cam, Vector2 sp) {
        if (cam != null) return cam.ScreenPointToRay(new Vector3(sp.x, sp.y, 0));
        return new Ray(new Vector3(sp.x, sp.y, -100), new Vector3(0, 0, 1));
    }
    /** intersect the screen ray with the rect plane; result in rect local space */
    public static boolean ScreenPointToWorldPointInRectangle(RectTransform rect, Vector2 sp, Camera cam, Vector3 world) {
        Ray ray = ScreenPointToRay(cam, sp);
        Vector3 n = rect.get_forward(), p0 = rect.get_position();
        float denom = Vector3.Dot(ray.m_Direction, n);
        if (Math.abs(denom) < 1e-6f) { world.Set(0, 0, 0); return false; }
        float t = Vector3.Dot(Vector3.op_Subtraction(p0, ray.m_Origin), n) / denom;
        world.$set(Vector3.op_Addition(ray.m_Origin, Vector3.op_Multiply(ray.m_Direction, t)));
        return true;
    }
    public static boolean ScreenPointToLocalPointInRectangle(RectTransform rect, Vector2 sp, Camera cam, Vector2 local) {
        Vector3 w = new Vector3();
        if (!ScreenPointToWorldPointInRectangle(rect, sp, cam, w)) { local.Set(0, 0); return false; }
        Vector3 l = rect.InverseTransformPoint(w);
        local.Set(l.x, l.y);
        return true;
    }
    public static boolean RectangleContainsScreenPoint(RectTransform rect, Vector2 sp) { return RectangleContainsScreenPoint(rect, sp, null, new Vector4()); }
    public static boolean RectangleContainsScreenPoint(RectTransform rect, Vector2 sp, Camera cam) { return RectangleContainsScreenPoint(rect, sp, cam, new Vector4()); }
    public static boolean RectangleContainsScreenPoint(RectTransform rect, Vector2 sp, Camera cam, Vector4 pad) {
        Vector2 l = new Vector2();
        if (!ScreenPointToLocalPointInRectangle(rect, sp, cam, l)) return false;
        Rect r = rect.get_rect();
        return l.x >= r.m_XMin + pad.x && l.x <= r.m_XMin + r.m_Width - pad.z && l.y >= r.m_YMin + pad.y && l.y <= r.m_YMin + r.m_Height - pad.w;
    }
    public static Vector2 PixelAdjustPoint(Vector2 p, Transform t, Canvas c) {
        if (c == null || c.get_renderMode() == 2 || c.get_scaleFactor() == 0 || !c.get_pixelPerfect()) return p.$copy();
        return p.$copy();
    }
    public static Rect PixelAdjustRect(RectTransform rt, Canvas c) { return rt.get_rect(); }
    public static Bounds CalculateRelativeRectTransformBounds(Transform root, Transform child) { return new Bounds(); }
    public static Bounds CalculateRelativeRectTransformBounds(Transform t) { return new Bounds(); }
    public static void FlipLayoutOnAxis(RectTransform rt, int axis, boolean keepPositioning, boolean recursive) {
        if (rt == null) return;
        if (recursive) for (int i = 0; i < rt.get_childCount(); i++) {
            Transform c = rt.GetChild(i);
            if (c instanceof RectTransform) FlipLayoutOnAxis((RectTransform) c, axis, false, true);
        }
        Vector2 pivot = rt.get_pivot();
        if (axis == 0) pivot.x = 1 - pivot.x; else pivot.y = 1 - pivot.y;
        rt.set_pivot(pivot);
        if (keepPositioning) return;
        Vector2 ap = rt.get_anchoredPosition();
        if (axis == 0) ap.x = -ap.x; else ap.y = -ap.y;
        rt.set_anchoredPosition(ap);
        Vector2 mn = rt.get_anchorMin(), mx = rt.get_anchorMax();
        if (axis == 0) { float t = mn.x; mn.x = 1 - mx.x; mx.x = 1 - t; } else { float t = mn.y; mn.y = 1 - mx.y; mx.y = 1 - t; }
        rt.set_anchorMin(mn);
        rt.set_anchorMax(mx);
    }
    public static void FlipLayoutAxes(RectTransform rt, boolean keepPositioning, boolean recursive) {
        if (rt == null) return;
        if (recursive) for (int i = 0; i < rt.get_childCount(); i++) {
            Transform c = rt.GetChild(i);
            if (c instanceof RectTransform) FlipLayoutAxes((RectTransform) c, false, true);
        }
        rt.set_pivot(swap(rt.get_pivot()));
        rt.set_sizeDelta(swap(rt.get_sizeDelta()));
        if (keepPositioning) return;
        rt.set_anchoredPosition(swap(rt.get_anchoredPosition()));
        rt.set_anchorMin(swap(rt.get_anchorMin()));
        rt.set_anchorMax(swap(rt.get_anchorMax()));
    }
    private static Vector2 swap(Vector2 v) { return new Vector2(v.y, v.x); }
}
