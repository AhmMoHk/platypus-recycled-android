package cs.UnityEngine;

import cs.System.Collections.Generic.List_1;

/** Holds the UI mesh/material/texture/color of a Graphic; drawn by Render in canvas hierarchy order. */
public class CanvasRenderer extends Component {
    public Mesh $mesh;
    public Material[] $materials = new Material[0];
    public Material[] $popMaterials = new Material[0];
    public Texture $texture, $alphaTexture;
    public final Color $color = new Color(1, 1, 1, 1);
    public boolean $cull, $clip, cullTransparentMesh, hasPopInstruction;
    public final Rect $clipRect = new Rect();
    public final Vector2 $clipSoftness = new Vector2();
    public int $depth = -1;

    public void SetMesh(Mesh m) { $mesh = m == null ? null : (Mesh) m.$cloneAsset(); }
    public Mesh GetMesh() { return $mesh; }
    public void Clear() { $mesh = null; $materials = new Material[0]; $texture = null; }
    public void SetMaterial(Material m, Texture t) { set_materialCount(Math.max(1, $materials.length)); $materials[0] = m; $texture = t; }
    public void SetMaterial(Material m, int i) { if (i >= $materials.length) set_materialCount(i + 1); $materials[i] = m; }
    public Material GetMaterial() { return $materials.length == 0 ? null : $materials[0]; }
    public Material GetMaterial(int i) { return i < $materials.length ? $materials[i] : null; }
    public int get_materialCount() { return $materials.length; }
    public void set_materialCount(int n) { Material[] m = new Material[n]; System.arraycopy($materials, 0, m, 0, Math.min(n, $materials.length)); $materials = m; }
    public int get_popMaterialCount() { return $popMaterials.length; }
    public void set_popMaterialCount(int n) { Material[] m = new Material[n]; System.arraycopy($popMaterials, 0, m, 0, Math.min(n, $popMaterials.length)); $popMaterials = m; }
    public void SetPopMaterial(Material m, int i) { if (i >= $popMaterials.length) set_popMaterialCount(i + 1); $popMaterials[i] = m; }
    public Material GetPopMaterial(int i) { return i < $popMaterials.length ? $popMaterials[i] : null; }
    public boolean get_hasPopInstruction() { return hasPopInstruction; }
    public void set_hasPopInstruction(boolean v) { hasPopInstruction = v; }
    public void SetTexture(Texture t) { $texture = t; }
    public void SetAlphaTexture(Texture t) { $alphaTexture = t; }
    public void SetColor(Color c) { $color.$set(c); }
    public Color GetColor() { return $color.$copy(); }
    public void SetAlpha(float a) { $color.a = a; }
    public float GetAlpha() { return $color.a; }
    public float GetInheritedAlpha() {
        float a = 1;
        for (Transform t = gameObject.transform; t != null; t = t.parent) {
            boolean stop = false;
            for (Component c : t.gameObject.components) {
                if (c instanceof CanvasGroup && ((CanvasGroup) c).m_Enabled && !c.$destroyed) {
                    a *= ((CanvasGroup) c).alpha;
                    if (((CanvasGroup) c).ignoreParentGroups) stop = true;
                }
            }
            if (stop) break;
        }
        return a;
    }
    public boolean get_cull() { return $cull; }
    public void set_cull(boolean v) { $cull = v; }
    public boolean get_cullTransparentMesh() { return cullTransparentMesh; }
    public void set_cullTransparentMesh(boolean v) { cullTransparentMesh = v; }
    public void EnableRectClipping(Rect r) { $clip = true; $clipRect.$set(r); }
    public void DisableRectClipping() { $clip = false; }
    public Vector2 get_clippingSoftness() { return $clipSoftness.$copy(); }
    public void set_clippingSoftness(Vector2 v) { $clipSoftness.$set(v); }
    public int get_absoluteDepth() { return $depth; }
    public int get_relativeDepth() { return $depth; }
    public boolean get_hasMoved() { return true; }
    public boolean get_hasRectClipping() { return $clip; }

    @Override public void $load(java.util.HashMap<String, Object> d, Loader.Ctx ctx) { cullTransparentMesh = Loader.b(d, "m_CullTransparentMesh", true); }
    @Override public void $cloneFrom(Component s, Eng.CloneCtx ctx) { cullTransparentMesh = ((CanvasRenderer) s).cullTransparentMesh; }

    // ------------------------------------------------------------------ UIVertex stream helpers
    public static void SplitUIVertexStreams(List_1 verts, List_1 pos, List_1 col, List_1 uv0, List_1 uv1, List_1 uv2, List_1 uv3, List_1 normals, List_1 tangents, List_1 indices) {
        pos.Clear(); col.Clear(); uv0.Clear(); uv1.Clear(); uv2.Clear(); uv3.Clear(); normals.Clear(); tangents.Clear(); indices.Clear();
        for (int i = 0, n = verts.get_Count(); i < n; i++) {
            UIVertex v = (UIVertex) verts.get_Item(i);
            pos.Add(v.position.$copy()); col.Add(v.color.$copy()); uv0.Add(v.uv0.$copy()); uv1.Add(v.uv1.$copy());
            uv2.Add(v.uv2.$copy()); uv3.Add(v.uv3.$copy()); normals.Add(v.normal.$copy()); tangents.Add(v.tangent.$copy());
            indices.Add(i);
        }
    }
    public static void SplitUIVertexStreams(List_1 verts, List_1 pos, List_1 col, List_1 uv0, List_1 uv1, List_1 normals, List_1 tangents, List_1 indices) {
        SplitUIVertexStreams(verts, pos, col, uv0, uv1, new List_1(), new List_1(), normals, tangents, indices);
    }
    public static void CreateUIVertexStream(List_1 verts, List_1 pos, List_1 col, List_1 uv0, List_1 uv1, List_1 uv2, List_1 uv3, List_1 normals, List_1 tangents, List_1 indices) {
        verts.Clear();
        for (int k = 0, n = indices.get_Count(); k < n; k++) {
            int i = cs.rt.Rt.unboxI(indices.get_Item(k));
            verts.Add(make(i, pos, col, uv0, uv1, uv2, uv3, normals, tangents));
        }
    }
    public static void CreateUIVertexStream(List_1 verts, List_1 pos, List_1 col, List_1 uv0, List_1 uv1, List_1 normals, List_1 tangents, List_1 indices) {
        CreateUIVertexStream(verts, pos, col, uv0, uv1, new List_1(), new List_1(), normals, tangents, indices);
    }
    public static void AddUIVertexStream(List_1 verts, List_1 pos, List_1 col, List_1 uv0, List_1 uv1, List_1 uv2, List_1 uv3, List_1 normals, List_1 tangents) {
        for (int i = 0, n = pos.get_Count(); i < n; i++) verts.Add(make(i, pos, col, uv0, uv1, uv2, uv3, normals, tangents));
    }
    public static void AddUIVertexStream(List_1 verts, List_1 pos, List_1 col, List_1 uv0, List_1 uv1, List_1 normals, List_1 tangents) {
        AddUIVertexStream(verts, pos, col, uv0, uv1, new List_1(), new List_1(), normals, tangents);
    }
    private static UIVertex make(int i, List_1 pos, List_1 col, List_1 uv0, List_1 uv1, List_1 uv2, List_1 uv3, List_1 normals, List_1 tangents) {
        UIVertex v = new UIVertex();
        v.position.$set(pos.get_Item(i));
        if (i < col.get_Count()) v.color.$set(col.get_Item(i));
        if (i < uv0.get_Count()) v.uv0.$set(uv0.get_Item(i));
        if (i < uv1.get_Count()) v.uv1.$set(uv1.get_Item(i));
        if (i < uv2.get_Count()) v.uv2.$set(uv2.get_Item(i));
        if (i < uv3.get_Count()) v.uv3.$set(uv3.get_Item(i));
        if (i < normals.get_Count()) v.normal.$set(normals.get_Item(i));
        if (i < tangents.get_Count()) v.tangent.$set(tangents.get_Item(i));
        return v;
    }
}
