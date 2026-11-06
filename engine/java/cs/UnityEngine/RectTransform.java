package cs.UnityEngine;

/** UnityEngine.RectTransform: anchors/pivot/sizeDelta layout; localPosition is derived from the parent rect. */
public class RectTransform extends Transform {
    final Vector2 anchorMin = new Vector2(.5f, .5f), anchorMax = new Vector2(.5f, .5f);
    final Vector2 anchoredPosition = new Vector2(), sizeDelta = new Vector2(100, 100), pivot = new Vector2(.5f, .5f);
    private float lastW = Float.NaN, lastH = Float.NaN;

    public static void add_reapplyDrivenProperties(RectTransform$ReapplyDrivenProperties d) {}
    public static void remove_reapplyDrivenProperties(RectTransform$ReapplyDrivenProperties d) {}

    // ------------------------------------------------------------------ rect math
    private float parentW() { return parent instanceof RectTransform ? ((RectTransform) parent).width() : 0f; }
    private float parentH() { return parent instanceof RectTransform ? ((RectTransform) parent).height() : 0f; }
    float width() { return parentW() * (anchorMax.x - anchorMin.x) + sizeDelta.x; }
    float height() { return parentH() * (anchorMax.y - anchorMin.y) + sizeDelta.y; }

    public Rect get_rect() {
        float w = width(), h = height();
        return new Rect(-pivot.x * w, -pivot.y * h, w, h);
    }

    /** localPosition from anchors (parent rect) + anchoredPosition */
    private void refPoint(float[] out) {
        if (parent instanceof RectTransform) {
            RectTransform p = (RectTransform) parent;
            float pw = p.width(), ph = p.height();
            float px = -p.pivot.x * pw, py = -p.pivot.y * ph;
            out[0] = px + pw * (anchorMin.x + (anchorMax.x - anchorMin.x) * pivot.x);
            out[1] = py + ph * (anchorMin.y + (anchorMax.y - anchorMin.y) * pivot.y);
        } else {
            out[0] = 0;
            out[1] = 0;
        }
    }

    private final float[] tmp = new float[2];

    /** recompute localPosition (and children) after any layout input changed */
    void relayout() {
        refPoint(tmp);
        lp.x = tmp[0] + anchoredPosition.x;
        lp.y = tmp[1] + anchoredPosition.y;
        dirty();
        float w = width(), h = height();
        boolean sizeChanged = w != lastW || h != lastH;
        lastW = w;
        lastH = h;
        for (int i = 0, n = children.size(); i < n; i++) {
            Transform c = children.get(i);
            if (c instanceof RectTransform) ((RectTransform) c).relayout();
        }
        if (sizeChanged && gameObject != null) Eng.rectDimensionsChanged(gameObject);
    }

    @Override public void $parentChanged() { relayout(); }

    // ------------------------------------------------------------------ properties
    public Vector2 get_anchorMin() { return anchorMin.$copy(); }
    public void set_anchorMin(Vector2 v) { anchorMin.$set(v); relayout(); }
    public Vector2 get_anchorMax() { return anchorMax.$copy(); }
    public void set_anchorMax(Vector2 v) { anchorMax.$set(v); relayout(); }
    public Vector2 get_anchoredPosition() { return anchoredPosition.$copy(); }
    public void set_anchoredPosition(Vector2 v) { anchoredPosition.$set(v); relayout(); }
    public Vector3 get_anchoredPosition3D() { return new Vector3(anchoredPosition.x, anchoredPosition.y, lp.z); }
    public void set_anchoredPosition3D(Vector3 v) { anchoredPosition.x = v.x; anchoredPosition.y = v.y; lp.z = v.z; relayout(); }
    public Vector2 get_sizeDelta() { return sizeDelta.$copy(); }
    public void set_sizeDelta(Vector2 v) { sizeDelta.$set(v); relayout(); }
    public Vector2 get_pivot() { return pivot.$copy(); }
    public void set_pivot(Vector2 v) { pivot.$set(v); relayout(); }

    public Vector2 get_offsetMin() { return new Vector2(anchoredPosition.x - sizeDelta.x * pivot.x, anchoredPosition.y - sizeDelta.y * pivot.y); }
    public void set_offsetMin(Vector2 v) {
        Vector2 cur = get_offsetMin();
        float ox = v.x - cur.x, oy = v.y - cur.y;
        sizeDelta.x -= ox;
        sizeDelta.y -= oy;
        anchoredPosition.x += ox * (1 - pivot.x);
        anchoredPosition.y += oy * (1 - pivot.y);
        relayout();
    }
    public Vector2 get_offsetMax() { return new Vector2(anchoredPosition.x + sizeDelta.x * (1 - pivot.x), anchoredPosition.y + sizeDelta.y * (1 - pivot.y)); }
    public void set_offsetMax(Vector2 v) {
        Vector2 cur = get_offsetMax();
        float ox = v.x - cur.x, oy = v.y - cur.y;
        sizeDelta.x += ox;
        sizeDelta.y += oy;
        anchoredPosition.x += ox * pivot.x;
        anchoredPosition.y += oy * pivot.y;
        relayout();
    }

    @Override public void set_localPosition(Vector3 v) {
        refPoint(tmp);
        anchoredPosition.x = v.x - tmp[0];
        anchoredPosition.y = v.y - tmp[1];
        lp.z = v.z;
        relayout();
    }

    /** Axis: 0 Horizontal, 1 Vertical */
    public void SetSizeWithCurrentAnchors(int axis, float size) {
        if (axis == 0) sizeDelta.x = size - parentW() * (anchorMax.x - anchorMin.x);
        else sizeDelta.y = size - parentH() * (anchorMax.y - anchorMin.y);
        relayout();
    }

    /** Edge: 0 Left, 1 Right, 2 Top, 3 Bottom */
    public void SetInsetAndSizeFromParentEdge(int edge, float inset, float size) {
        int axis = (edge == 2 || edge == 3) ? 1 : 0;
        boolean end = edge == 2 || edge == 1;
        float anchorValue = end ? 1 : 0;
        if (axis == 0) { anchorMin.x = anchorValue; anchorMax.x = anchorValue; }
        else { anchorMin.y = anchorValue; anchorMax.y = anchorValue; }
        if (axis == 0) sizeDelta.x = size; else sizeDelta.y = size;
        float p = axis == 0 ? pivot.x : pivot.y;
        float pos = end ? -inset - size * (1 - p) : inset + size * p;
        if (axis == 0) anchoredPosition.x = pos; else anchoredPosition.y = pos;
        relayout();
    }

    public void GetLocalCorners(Vector3[] c) {
        Rect r = get_rect();
        float x0 = r.get_xMin(), y0 = r.get_yMin(), x1 = r.get_xMax(), y1 = r.get_yMax();
        c[0].Set(x0, y0, 0);
        c[1].Set(x0, y1, 0);
        c[2].Set(x1, y1, 0);
        c[3].Set(x1, y0, 0);
    }

    public void GetWorldCorners(Vector3[] c) {
        GetLocalCorners(c);
        for (int i = 0; i < 4; i++) c[i].$set(TransformPoint(c[i]));
    }

    public void ForceUpdateRectTransforms() { relayout(); }
    public Object get_drivenByObject() { return null; }
    public int get_drivenProperties() { return 0; }

    @Override public void $load(java.util.HashMap<String, Object> d, Loader.Ctx ctx) {
        super.$load(d, ctx);
        Loader.vec2(d.get("m_AnchorMin"), anchorMin);
        Loader.vec2(d.get("m_AnchorMax"), anchorMax);
        Loader.vec2(d.get("m_AnchoredPosition"), anchoredPosition);
        Loader.vec2(d.get("m_SizeDelta"), sizeDelta);
        Loader.vec2(d.get("m_Pivot"), pivot);
    }

    @Override public void $cloneFrom(Component src, Eng.CloneCtx ctx) {
        super.$cloneFrom(src, ctx);
        if (!(src instanceof RectTransform)) return;   // Transform upgraded to RectTransform
        RectTransform s = (RectTransform) src;
        anchorMin.$set(s.anchorMin);
        anchorMax.$set(s.anchorMax);
        anchoredPosition.$set(s.anchoredPosition);
        sizeDelta.$set(s.sizeDelta);
        pivot.$set(s.pivot);
    }
}
