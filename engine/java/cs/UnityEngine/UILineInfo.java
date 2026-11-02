package cs.UnityEngine;

public class UILineInfo implements cs.rt.Struct {
    public int startCharIdx, height;
    public float topY, leading;
    public UILineInfo() {}
    public UILineInfo $copy() { UILineInfo c = new UILineInfo(); c.$set(this); return c; }
    public void $set(Object o) { UILineInfo c = (UILineInfo) o; startCharIdx = c.startCharIdx; height = c.height; topY = c.topY; leading = c.leading; }
    public void $clear() { startCharIdx = height = 0; topY = leading = 0; }
    public static UILineInfo[] $newArray(int n) { UILineInfo[] a = new UILineInfo[n]; for (int i = 0; i < n; i++) a[i] = new UILineInfo(); return a; }
}
