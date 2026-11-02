package cs.Unity.Profiling;

public class ProfilerMarker implements cs.rt.Struct {
    public ProfilerMarker() {}
    public ProfilerMarker(String n) {}
    public void Begin() {}
    public void End() {}
    public ProfilerMarker $copy() { return this; }
    public void $set(Object o) {}
    public void $clear() {}
    public static ProfilerMarker[] $newArray(int n) { ProfilerMarker[] a = new ProfilerMarker[n]; for (int i = 0; i < n; i++) a[i] = new ProfilerMarker(); return a; }
}
