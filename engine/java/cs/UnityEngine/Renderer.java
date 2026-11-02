package cs.UnityEngine;

/** Base renderer: enabled flag, materials, sorting. Registered with Render while active. */
public class Renderer extends Component {
    boolean enabled = true;
    Material[] materials = new Material[0];
    int sortingLayerID, sortingOrder, rendererPriority;
    int renderingLayerMask = 1;
    MaterialPropertyBlock block;
    boolean registered;
    /** set by Render for SortingGroup sorting */
    public int $sortingGroupID = -1, $sortingGroupOrder;
    private boolean instanced;

    public boolean get_enabled() { return enabled; }
    public void set_enabled(boolean v) { enabled = v; }
    public boolean get_isVisible() { return enabled && gameObject != null && gameObject.get_activeInHierarchy(); }
    public Material get_sharedMaterial() { return materials.length == 0 ? null : materials[0]; }
    public void set_sharedMaterial(Material m) { if (materials.length == 0) materials = new Material[1]; materials[0] = m; }
    public Material[] get_sharedMaterials() { return materials.clone(); }
    public void set_sharedMaterials(Material[] m) { materials = m == null ? new Material[0] : m.clone(); }
    /** Unity instantiates the material on first access of .material */
    public Material get_material() {
        if (materials.length == 0 || materials[0] == null) return null;
        if (!instanced) {
            Material m = new Material(materials[0]);
            m.m_Name = materials[0].m_Name + " (Instance)";
            materials[0] = m;
            instanced = true;
        }
        return materials[0];
    }
    public void set_material(Material m) { set_sharedMaterial(m); instanced = false; }
    public Material[] get_materials() { get_material(); return materials.clone(); }
    public void set_materials(Material[] m) { set_sharedMaterials(m); }
    public int get_sortingLayerID() { return sortingLayerID; }
    public void set_sortingLayerID(int id) { sortingLayerID = id; }
    public String get_sortingLayerName() { String n = SortingLayer.IDToName(sortingLayerID); return n == null ? "Default" : n; }
    public void set_sortingLayerName(String n) { sortingLayerID = SortingLayer.NameToID(n); }
    public int get_sortingOrder() { return sortingOrder; }
    public void set_sortingOrder(int o) { sortingOrder = Math.max(-32768, Math.min(32767, o)); }
    public int get_rendererPriority() { return rendererPriority; }
    public void set_rendererPriority(int p) { rendererPriority = p; }
    public int get_sortingGroupID() { return $sortingGroupID; }
    public int get_sortingGroupOrder() { return $sortingGroupOrder; }
    public int get_renderingLayerMask() { return renderingLayerMask; }
    public void set_renderingLayerMask(int m) { renderingLayerMask = m; }
    public void set_receiveShadows(boolean v) {}
    public void set_shadowCastingMode(int m) {}
    public void set_allowOcclusionWhenDynamic(boolean v) {}
    public boolean HasPropertyBlock() { return block != null && !block.get_isEmpty(); }
    public void GetPropertyBlock(MaterialPropertyBlock b) { if (block == null) b.Clear(); else b.copyFrom(block); }
    public void SetPropertyBlock(MaterialPropertyBlock b) {
        if (b == null) { block = null; return; }
        if (block == null) block = new MaterialPropertyBlock();
        block.copyFrom(b);
    }
    public MaterialPropertyBlock $block() { return block; }
    public Material[] $materials() { return materials; }

    /** world-space bounds (z ignored for sorting) */
    public Bounds get_bounds() {
        Bounds b = new Bounds();
        Transform t = gameObject == null ? null : gameObject.transform;
        if (t != null) b.m_Center.$set(t.get_position());
        return b;
    }
    public Bounds get_localBounds() { return new Bounds(); }

    @Override public void $onActivated() { Render.register(this); }
    @Override public void $onDeactivated() { Render.unregister(this); }
    @Override public void $onDestroyNative() { Render.unregister(this); }

    @Override public void $load(java.util.HashMap<String, Object> d, Loader.Ctx ctx) {
        enabled = Loader.b(d, "m_Enabled", true);
        java.util.ArrayList<Object> ms = Loader.l(d, "m_Materials");
        materials = new Material[ms.size()];
        for (int i = 0; i < materials.length; i++) {
            UObject m = ctx.ref(ms.get(i));
            materials[i] = m instanceof Material ? (Material) m : null;
        }
        sortingLayerID = (int) cs.rt.Json.lng(d.containsKey("m_SortingLayerID") ? d.get("m_SortingLayerID") : 0L);
        sortingOrder = Loader.i(d, "m_SortingOrder", 0);
        rendererPriority = Loader.i(d, "m_RendererPriority", 0);
    }
    @Override public void $cloneFrom(Component src, Eng.CloneCtx ctx) {
        Renderer s = (Renderer) src;
        enabled = s.enabled;
        materials = s.materials.clone();
        sortingLayerID = s.sortingLayerID;
        sortingOrder = s.sortingOrder;
        rendererPriority = s.rendererPriority;
        if (s.block != null) { block = new MaterialPropertyBlock(); block.copyFrom(s.block); }
    }
}
