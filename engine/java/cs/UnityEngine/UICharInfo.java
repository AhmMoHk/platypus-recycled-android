package cs.UnityEngine;

public class UICharInfo implements cs.rt.Struct {
    public Vector2 cursorPos = new Vector2();
    public float charWidth;
    public UICharInfo() {}
    public UICharInfo $copy() { UICharInfo c = new UICharInfo(); c.$set(this); return c; }
    public void $set(Object o) { UICharInfo c = (UICharInfo) o; cursorPos.$set(c.cursorPos); charWidth = c.charWidth; }
    public void $clear() { cursorPos.Set(0, 0); charWidth = 0; }
    public static UICharInfo[] $newArray(int n) { UICharInfo[] a = new UICharInfo[n]; for (int i = 0; i < n; i++) a[i] = new UICharInfo(); return a; }
}
