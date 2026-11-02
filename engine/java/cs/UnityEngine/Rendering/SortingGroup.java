package cs.UnityEngine.Rendering;

public class SortingGroup extends cs.UnityEngine.Behaviour {
    public int sortingLayerID, sortingOrder;
    public boolean sortAtRoot;
    public static int get_invalidSortingGroupID() { return 1048575; }
    public static SortingGroup GetSortingGroupByIndex(int i) { return cs.UnityEngine.Render.sortingGroupByIndex(i); }
    public int get_sortingLayerID() { return sortingLayerID; }
    public void set_sortingLayerID(int v) { sortingLayerID = v; }
    public String get_sortingLayerName() { String n = cs.UnityEngine.SortingLayer.IDToName(sortingLayerID); return n == null ? "Default" : n; }
    public void set_sortingLayerName(String n) { sortingLayerID = cs.UnityEngine.SortingLayer.NameToID(n); }
    public int get_sortingOrder() { return sortingOrder; }
    public void set_sortingOrder(int v) { sortingOrder = v; }
    public boolean get_sortAtRoot() { return sortAtRoot; }
    public void set_sortAtRoot(boolean v) { sortAtRoot = v; }
    @Override public void $load(java.util.HashMap<String, Object> d, cs.UnityEngine.Loader.Ctx ctx) {
        sortingLayerID = (int) cs.rt.Json.lng(d.containsKey("m_SortingLayerID") ? d.get("m_SortingLayerID") : 0L);
        sortingOrder = cs.UnityEngine.Loader.i(d, "m_SortingOrder", 0);
        sortAtRoot = cs.UnityEngine.Loader.b(d, "m_SortAtRoot", false);
    }
    @Override public void $cloneFrom(cs.UnityEngine.Component s, cs.UnityEngine.Eng.CloneCtx ctx) {
        SortingGroup g = (SortingGroup) s;
        sortingLayerID = g.sortingLayerID; sortingOrder = g.sortingOrder; sortAtRoot = g.sortAtRoot;
    }
}
