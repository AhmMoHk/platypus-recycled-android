package cs.UnityEngine;

public class UIVertex implements cs.rt.Struct {
    public Vector3 position = new Vector3(), normal = new Vector3();
    public Vector4 tangent = new Vector4();
    public Color32 color = new Color32();
    public Vector4 uv0 = new Vector4(), uv1 = new Vector4(), uv2 = new Vector4(), uv3 = new Vector4();
    public static UIVertex simpleVert;
    static {
        simpleVert = new UIVertex();
        simpleVert.normal.Set(0, 0, -1);
        simpleVert.tangent.x = 1; simpleVert.tangent.w = -1;
        simpleVert.color = new Color32((byte) 255, (byte) 255, (byte) 255, (byte) 255);
    }
    public UIVertex() {}
    public UIVertex $copy() { UIVertex v = new UIVertex(); v.$set(this); return v; }
    public void $set(Object o) {
        UIVertex v = (UIVertex) o;
        position.$set(v.position); normal.$set(v.normal); tangent.$set(v.tangent); color.$set(v.color);
        uv0.$set(v.uv0); uv1.$set(v.uv1); uv2.$set(v.uv2); uv3.$set(v.uv3);
    }
    public void $clear() { $set(new UIVertex()); }
    public static UIVertex[] $newArray(int n) { UIVertex[] a = new UIVertex[n]; for (int i = 0; i < n; i++) a[i] = new UIVertex(); return a; }
}
