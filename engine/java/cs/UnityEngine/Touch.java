package cs.UnityEngine;

/** TouchPhase: Began 0, Moved 1, Stationary 2, Ended 3, Canceled 4. TouchType: Direct 0 */
public class Touch implements cs.rt.Struct {
    public int m_FingerId, m_Phase, m_Type, m_TapCount = 1;
    public final Vector2 m_Position = new Vector2(), m_RawPosition = new Vector2(), m_PositionDelta = new Vector2();
    public float m_TimeDelta, m_Pressure = 1, m_maximumPossiblePressure = 1, m_Radius, m_RadiusVariance, m_AltitudeAngle, m_AzimuthAngle;
    public Touch() {}
    public int get_fingerId() { return m_FingerId; }
    public void set_fingerId(int v) { m_FingerId = v; }
    public Vector2 get_position() { return m_Position.$copy(); }
    public void set_position(Vector2 v) { m_Position.$set(v); }
    public Vector2 get_rawPosition() { return m_RawPosition.$copy(); }
    public Vector2 get_deltaPosition() { return m_PositionDelta.$copy(); }
    public void set_deltaPosition(Vector2 v) { m_PositionDelta.$set(v); }
    public float get_deltaTime() { return m_TimeDelta; }
    public int get_tapCount() { return m_TapCount; }
    public int get_phase() { return m_Phase; }
    public void set_phase(int p) { m_Phase = p; }
    public int get_type() { return m_Type; }
    public float get_pressure() { return m_Pressure; }
    public float get_maximumPossiblePressure() { return m_maximumPossiblePressure; }
    public float get_radius() { return m_Radius; }
    public float get_radiusVariance() { return m_RadiusVariance; }
    public float get_altitudeAngle() { return m_AltitudeAngle; }
    public float get_azimuthAngle() { return m_AzimuthAngle; }
    public Touch $copy() { Touch t = new Touch(); t.$set(this); return t; }
    public void $set(Object o) {
        Touch t = (Touch) o;
        m_FingerId = t.m_FingerId; m_Phase = t.m_Phase; m_Type = t.m_Type; m_TapCount = t.m_TapCount;
        m_Position.$set(t.m_Position); m_RawPosition.$set(t.m_RawPosition); m_PositionDelta.$set(t.m_PositionDelta);
        m_TimeDelta = t.m_TimeDelta; m_Pressure = t.m_Pressure; m_Radius = t.m_Radius;
    }
    public void $clear() { $set(new Touch()); }
    public static Touch[] $newArray(int n) { Touch[] a = new Touch[n]; for (int i = 0; i < n; i++) a[i] = new Touch(); return a; }
}
