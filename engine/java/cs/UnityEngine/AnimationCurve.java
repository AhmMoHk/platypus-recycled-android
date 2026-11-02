package cs.UnityEngine;

import cs.rt.Json;
import java.util.ArrayList;
import java.util.HashMap;

/** Hermite keyframe curve (WrapMode: Default 0 -> clamp, Loop 2, PingPong 4, ClampForever 8). */
public class AnimationCurve {
    Keyframe[] keys = new Keyframe[0];
    int preWrap = 8, postWrap = 8;
    public AnimationCurve() {}
    public AnimationCurve(Keyframe[] k) { set_keys(k); }
    public void $ctor_AnimationCurve() {}

    public Keyframe[] get_keys() { Keyframe[] r = new Keyframe[keys.length]; for (int i = 0; i < r.length; i++) r[i] = keys[i].$copy(); return r; }
    public void set_keys(Keyframe[] k) {
        keys = new Keyframe[k == null ? 0 : k.length];
        for (int i = 0; i < keys.length; i++) keys[i] = k[i].$copy();
        java.util.Arrays.sort(keys, new java.util.Comparator<Keyframe>() {
            public int compare(Keyframe a, Keyframe b) { return Float.compare(a.m_Time, b.m_Time); }
        });
    }
    public int get_length() { return keys.length; }
    public Keyframe get_Item(int i) { return keys[i].$copy(); }
    public int AddKey(float t, float v) { return AddKey(new Keyframe(t, v)); }
    public int AddKey(Keyframe k) {
        Keyframe[] n = java.util.Arrays.copyOf(keys, keys.length + 1);
        n[keys.length] = k.$copy();
        set_keys(n);
        for (int i = 0; i < keys.length; i++) if (keys[i].m_Time == k.m_Time) return i;
        return -1;
    }
    public int get_preWrapMode() { return preWrap; }
    public void set_preWrapMode(int m) { preWrap = m; }
    public int get_postWrapMode() { return postWrap; }
    public void set_postWrapMode(int m) { postWrap = m; }

    public float Evaluate(float t) {
        int n = keys.length;
        if (n == 0) return 0;
        if (n == 1) return keys[0].m_Value;
        float t0 = keys[0].m_Time, t1 = keys[n - 1].m_Time, len = t1 - t0;
        if (t < t0) t = wrap(t, t0, len, preWrap);
        else if (t > t1) t = wrap(t, t0, len, postWrap);
        if (t <= t0) return keys[0].m_Value;
        if (t >= t1) return keys[n - 1].m_Value;
        int i = 0;
        while (i < n - 2 && t >= keys[i + 1].m_Time) i++;
        Keyframe a = keys[i], b = keys[i + 1];
        float dt = b.m_Time - a.m_Time;
        if (dt <= 0) return b.m_Value;
        if (Float.isInfinite(a.m_OutTangent) || Float.isInfinite(b.m_InTangent)) return a.m_Value;
        float s = (t - a.m_Time) / dt;
        float m0 = a.m_OutTangent * dt, m1 = b.m_InTangent * dt;
        float s2 = s * s, s3 = s2 * s;
        return (2 * s3 - 3 * s2 + 1) * a.m_Value + (s3 - 2 * s2 + s) * m0 + (-2 * s3 + 3 * s2) * b.m_Value + (s3 - s2) * m1;
    }

    private static float wrap(float t, float t0, float len, int mode) {
        if (len <= 0) return t0;
        if (mode == 2) return t0 + Mathf.Repeat(t - t0, len);
        if (mode == 4) return t0 + Mathf.PingPong(t - t0, len);
        return t;
    }

    public static AnimationCurve Linear(float ts, float vs, float te, float ve) {
        float tan = (ve - vs) / (te - ts);
        return new AnimationCurve(new Keyframe[]{new Keyframe(ts, vs, 0, tan), new Keyframe(te, ve, tan, 0)});
    }
    public static AnimationCurve EaseInOut(float ts, float vs, float te, float ve) {
        return new AnimationCurve(new Keyframe[]{new Keyframe(ts, vs, 0, 0), new Keyframe(te, ve, 0, 0)});
    }
    public static AnimationCurve Constant(float ts, float te, float v) {
        return new AnimationCurve(new Keyframe[]{new Keyframe(ts, v, 0, 0), new Keyframe(te, v, 0, 0)});
    }

    AnimationCurve $clone() {
        AnimationCurve c = new AnimationCurve(keys);
        c.preWrap = preWrap;
        c.postWrap = postWrap;
        return c;
    }

    static AnimationCurve $fromJson(Object v) {
        AnimationCurve c = new AnimationCurve();
        if (!(v instanceof HashMap)) return c;
        HashMap<String, Object> m = Json.map(v);
        ArrayList<Keyframe> ks = new ArrayList<>();
        if (m.get("m_Curve") instanceof ArrayList) {
            for (Object o : Json.list(m.get("m_Curve"))) {
                HashMap<String, Object> k = Json.map(o);
                Keyframe f = new Keyframe(Json.f(k.get("time")), Json.f(k.get("value")), Json.f(k.get("inSlope")), Json.f(k.get("outSlope")));
                if (k.containsKey("inWeight")) { f.m_InWeight = Json.f(k.get("inWeight")); f.m_OutWeight = Json.f(k.get("outWeight")); }
                ks.add(f);
            }
        }
        c.set_keys(ks.toArray(new Keyframe[0]));
        c.preWrap = m.containsKey("m_PreInfinity") ? Json.i(m.get("m_PreInfinity")) : 8;
        c.postWrap = m.containsKey("m_PostInfinity") ? Json.i(m.get("m_PostInfinity")) : 8;
        return c;
    }
}
