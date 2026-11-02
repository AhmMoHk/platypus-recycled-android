package cs.UnityEngine;

public class DrivenRectTransformTracker implements cs.rt.Struct {
    public DrivenRectTransformTracker() {}
    public void Add(UObject driver, RectTransform rt, int props) {}
    public void Clear() {}
    public static void StopRecordingUndo() {}
    public static void StartRecordingUndo() {}
    public DrivenRectTransformTracker $copy() { return new DrivenRectTransformTracker(); }
    public void $set(Object o) {}
    public void $clear() {}
    public static DrivenRectTransformTracker[] $newArray(int n) { DrivenRectTransformTracker[] a = new DrivenRectTransformTracker[n]; for (int i = 0; i < n; i++) a[i] = new DrivenRectTransformTracker(); return a; }
}
