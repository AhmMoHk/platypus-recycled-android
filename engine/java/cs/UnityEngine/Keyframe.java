package cs.UnityEngine;

public class Keyframe implements cs.rt.Struct {
    public float m_Time, m_Value, m_InTangent, m_OutTangent, m_InWeight = 1f / 3f, m_OutWeight = 1f / 3f;
    public int m_WeightedMode, m_TangentMode;
    public Keyframe() {}
    public Keyframe(float t, float v) { m_Time = t; m_Value = v; }
    public Keyframe(float t, float v, float in, float out) { m_Time = t; m_Value = v; m_InTangent = in; m_OutTangent = out; }
    public Keyframe(float t, float v, float in, float out, float iw, float ow) { this(t, v, in, out); m_InWeight = iw; m_OutWeight = ow; }
    public float get_time() { return m_Time; }
    public void set_time(float v) { m_Time = v; }
    public float get_value() { return m_Value; }
    public void set_value(float v) { m_Value = v; }
    public float get_inTangent() { return m_InTangent; }
    public void set_inTangent(float v) { m_InTangent = v; }
    public float get_outTangent() { return m_OutTangent; }
    public void set_outTangent(float v) { m_OutTangent = v; }
    public Keyframe $copy() { Keyframe k = new Keyframe(); k.$set(this); return k; }
    public void $set(Object o) {
        Keyframe k = (Keyframe) o;
        m_Time = k.m_Time; m_Value = k.m_Value; m_InTangent = k.m_InTangent; m_OutTangent = k.m_OutTangent;
        m_InWeight = k.m_InWeight; m_OutWeight = k.m_OutWeight; m_WeightedMode = k.m_WeightedMode; m_TangentMode = k.m_TangentMode;
    }
    public void $clear() { $set(new Keyframe()); }
    public static Keyframe[] $newArray(int n) { Keyframe[] a = new Keyframe[n]; for (int i = 0; i < n; i++) a[i] = new Keyframe(); return a; }
}
