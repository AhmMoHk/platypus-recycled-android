package cs.UnityEngine.Animations;

import cs.UnityEngine.*;
import java.util.ArrayList;

/** Translation/rotation follow of weighted sources with offsets; evaluated between Update and LateUpdate. */
public class ParentConstraint extends Behaviour {
    static final ArrayList<ParentConstraint> active = new ArrayList<>();
    float weight = 1;
    boolean constraintActive = true, locked = true;
    final ArrayList<Object[]> sources = new ArrayList<>();      // {Transform, Float weight}
    Vector3[] tOffsets = new Vector3[0], rOffsets = new Vector3[0];
    final Vector3 translationAtRest = new Vector3(), rotationAtRest = new Vector3();
    boolean ax = true, ay = true, az = true;

    public float get_weight() { return weight; }
    public void set_weight(float w) { weight = w; }
    public boolean get_constraintActive() { return constraintActive; }
    public void set_constraintActive(boolean v) { constraintActive = v; }
    public int get_sourceCount() { return sources.size(); }
    public void SetTranslationOffset(int i, Vector3 v) { ensure(i); tOffsets[i] = v.$copy(); }
    public Vector3 GetTranslationOffset(int i) { ensure(i); return tOffsets[i].$copy(); }
    public void SetRotationOffset(int i, Vector3 v) { ensure(i); rOffsets[i] = v.$copy(); }
    private void ensure(int i) {
        if (i < tOffsets.length) return;
        Vector3[] t = new Vector3[i + 1], r = new Vector3[i + 1];
        for (int k = 0; k <= i; k++) { t[k] = k < tOffsets.length ? tOffsets[k] : new Vector3(); r[k] = k < rOffsets.length ? rOffsets[k] : new Vector3(); }
        tOffsets = t; rOffsets = r;
    }

    public static void $evaluateAll() { for (ParentConstraint c : new ArrayList<>(active)) c.evaluate(); }

    void evaluate() {
        if (!constraintActive || sources.isEmpty() || $destroyedCheck()) return;
        float tw = 0, x = 0, y = 0, z = 0;
        for (int i = 0; i < sources.size(); i++) {
            Transform s = (Transform) sources.get(i)[0];
            float w = (Float) sources.get(i)[1];
            if (!UObject.op_Implicit$boolean_(s) || w <= 0) continue;
            Vector3 off = i < tOffsets.length ? tOffsets[i] : new Vector3();
            Vector3 p = s.TransformPoint(off);
            x += p.x * w; y += p.y * w; z += p.z * w;
            tw += w;
        }
        if (tw <= 0) return;
        Transform t = get_transform();
        Vector3 cur = t.get_position();
        float k = weight;
        Vector3 target = new Vector3(x / tw, y / tw, z / tw);
        t.set_position(new Vector3(ax ? cur.x + (target.x - cur.x) * k : cur.x, ay ? cur.y + (target.y - cur.y) * k : cur.y, az ? cur.z + (target.z - cur.z) * k : cur.z));
    }
    private boolean $destroyedCheck() { return !UObject.op_Implicit$boolean_(this); }

    @Override public void $nativeEnable() { if (!active.contains(this)) active.add(this); }
    @Override public void $nativeDisable() { active.remove(this); }
    @Override public void $onDestroyNative() { active.remove(this); }

    @Override public void $load(java.util.HashMap<String, Object> d, Loader.Ctx ctx) {
        weight = Loader.f(d, "m_Weight", 1);
        constraintActive = Loader.b(d, "m_Active", true);
        ax = Loader.b(d, "m_AffectTranslationX", true); ay = Loader.b(d, "m_AffectTranslationY", true); az = Loader.b(d, "m_AffectTranslationZ", true);
        ArrayList<Object> to = Loader.l(d, "m_TranslationOffsets");
        tOffsets = new Vector3[to.size()];
        for (int i = 0; i < tOffsets.length; i++) { tOffsets[i] = new Vector3(); Loader.vec3(to.get(i), tOffsets[i]); }
        ArrayList<Object> ro = Loader.l(d, "m_RotationOffsets");
        rOffsets = new Vector3[ro.size()];
        for (int i = 0; i < rOffsets.length; i++) { rOffsets[i] = new Vector3(); Loader.vec3(ro.get(i), rOffsets[i]); }
        sources.clear();
        for (Object s : Loader.l(d, "m_Sources")) {
            java.util.HashMap<String, Object> m = cs.rt.Json.map(s);
            sources.add(new Object[]{ctx.ref(m.get("sourceTransform")), Loader.f(m, "weight", 1)});
        }
    }
    @Override public void $cloneFrom(Component src, Eng.CloneCtx ctx) {
        ParentConstraint s = (ParentConstraint) src;
        weight = s.weight; constraintActive = s.constraintActive; ax = s.ax; ay = s.ay; az = s.az;
        tOffsets = s.tOffsets.clone(); rOffsets = s.rOffsets.clone();
        sources.clear();
        for (Object[] o : s.sources) sources.add(new Object[]{ctx.remap(o[0]), o[1]});
    }
}
