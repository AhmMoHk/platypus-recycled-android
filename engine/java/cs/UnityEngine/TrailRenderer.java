package cs.UnityEngine;

import java.util.ArrayList;

/** Trail: records world positions over time (sampled each LateUpdate by Render) */
public class TrailRenderer extends Renderer {
    float time = 5, minVertexDistance = .1f, widthMultiplier = 1;
    boolean emitting = true, autodestruct;
    Gradient gradient = new Gradient();
    AnimationCurve widthCurve = AnimationCurve.Constant(0, 1, 1);
    public final ArrayList<float[]> $points = new ArrayList<>();      // x, y, z, birthTime
    public float get_time() { return time; }
    public void set_time(float t) { time = t; }
    public boolean get_emitting() { return emitting; }
    public void set_emitting(boolean e) { emitting = e; }
    public void set_colorGradient(Gradient g) { gradient = g.$clone(); }
    public Gradient get_colorGradient() { return gradient.$clone(); }
    public void set_widthCurve(AnimationCurve c) { widthCurve = c.$clone(); }
    public AnimationCurve get_widthCurve() { return widthCurve.$clone(); }
    public void set_widthMultiplier(float w) { widthMultiplier = w; }
    public float get_widthMultiplier() { return widthMultiplier; }
    public void set_minVertexDistance(float d) { minVertexDistance = d; }
    public void set_startWidth(float w) { widthMultiplier = w; }
    public void set_endWidth(float w) {}
    public void set_startColor(Color c) {}
    public void set_endColor(Color c) {}
    public void set_autodestruct(boolean v) { autodestruct = v; }
    public int get_positionCount() { return $points.size(); }
    public void Clear() { $points.clear(); }
    public Gradient $gradient() { return gradient; }
    public AnimationCurve $widthCurve() { return widthCurve; }
    public float $widthMul() { return widthMultiplier; }

    /** called once per frame by the renderer */
    public void $sample() {
        float now = Time.time;
        while (!$points.isEmpty() && now - $points.get(0)[3] > time) $points.remove(0);
        if (!emitting || gameObject == null) return;
        Vector3 p = gameObject.transform.get_position();
        if (!$points.isEmpty()) {
            float[] l = $points.get($points.size() - 1);
            float dx = p.x - l[0], dy = p.y - l[1];
            if (dx * dx + dy * dy < minVertexDistance * minVertexDistance) { l[0] = p.x; l[1] = p.y; l[2] = p.z; return; }
        }
        $points.add(new float[]{p.x, p.y, p.z, now});
    }
    @Override public void $load(java.util.HashMap<String, Object> d, Loader.Ctx ctx) {
        super.$load(d, ctx);
        time = Loader.f(d, "m_Time", 5);
        minVertexDistance = Loader.f(d, "m_MinVertexDistance", .1f);
        emitting = Loader.b(d, "m_Emitting", true);
        autodestruct = Loader.b(d, "m_Autodestruct", false);
        java.util.HashMap<String, Object> lp = Loader.m(d, "m_Parameters");
        widthMultiplier = Loader.f(lp, "widthMultiplier", 1);
        if (lp.containsKey("widthCurve")) widthCurve = AnimationCurve.$fromJson(lp.get("widthCurve"));
        if (lp.containsKey("colorGradient")) gradient = Gradient.$fromJson(lp.get("colorGradient"));
    }
    @Override public void $cloneFrom(Component src, Eng.CloneCtx ctx) {
        super.$cloneFrom(src, ctx);
        TrailRenderer s = (TrailRenderer) src;
        time = s.time; minVertexDistance = s.minVertexDistance; emitting = s.emitting; autodestruct = s.autodestruct;
        widthMultiplier = s.widthMultiplier; widthCurve = s.widthCurve.$clone(); gradient = s.gradient.$clone();
    }
}
