package cs.UnityEngine;

public class GradientColorKey implements cs.rt.Struct {
    public Color color = new Color();
    public float time;
    public GradientColorKey() {}
    public GradientColorKey(Color c, float t) { color.$set(c); time = t; }
    public GradientColorKey $copy() { return new GradientColorKey(color, time); }
    public void $set(Object o) { GradientColorKey k = (GradientColorKey) o; color.$set(k.color); time = k.time; }
    public void $clear() { color.$clear(); time = 0; }
    public static GradientColorKey[] $newArray(int n) { GradientColorKey[] a = new GradientColorKey[n]; for (int i = 0; i < n; i++) a[i] = new GradientColorKey(); return a; }
}
