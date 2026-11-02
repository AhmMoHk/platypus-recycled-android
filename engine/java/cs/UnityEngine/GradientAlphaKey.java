package cs.UnityEngine;

public class GradientAlphaKey implements cs.rt.Struct {
    public float alpha, time;
    public GradientAlphaKey() {}
    public GradientAlphaKey(float a, float t) { alpha = a; time = t; }
    public GradientAlphaKey $copy() { return new GradientAlphaKey(alpha, time); }
    public void $set(Object o) { GradientAlphaKey k = (GradientAlphaKey) o; alpha = k.alpha; time = k.time; }
    public void $clear() { alpha = time = 0; }
    public static GradientAlphaKey[] $newArray(int n) { GradientAlphaKey[] a = new GradientAlphaKey[n]; for (int i = 0; i < n; i++) a[i] = new GradientAlphaKey(); return a; }
}
