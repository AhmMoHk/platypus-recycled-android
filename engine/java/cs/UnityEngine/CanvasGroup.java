package cs.UnityEngine;

public class CanvasGroup extends Behaviour implements ICanvasRaycastFilter {
    float alpha = 1;
    boolean interactable = true, blocksRaycasts = true, ignoreParentGroups;
    public float get_alpha() { return alpha; }
    public void set_alpha(float a) { if (alpha != a) { alpha = a; changed(); } }
    public boolean get_interactable() { return interactable; }
    public void set_interactable(boolean v) { if (interactable != v) { interactable = v; changed(); } }
    public boolean get_blocksRaycasts() { return blocksRaycasts; }
    public void set_blocksRaycasts(boolean v) { if (blocksRaycasts != v) { blocksRaycasts = v; changed(); } }
    public boolean get_ignoreParentGroups() { return ignoreParentGroups; }
    public void set_ignoreParentGroups(boolean v) { if (ignoreParentGroups != v) { ignoreParentGroups = v; changed(); } }
    public boolean IsRaycastLocationValid(Vector2 sp, Camera cam) { return blocksRaycasts; }
    private void changed() { if (gameObject != null && !gameObject.$asset) Eng.canvasGroupChanged(gameObject); }
    @Override public void $nativeEnable() { changed(); }
    @Override public void $nativeDisable() { changed(); }
    @Override public void $load(java.util.HashMap<String, Object> d, Loader.Ctx ctx) {
        alpha = Loader.f(d, "m_Alpha", 1);
        interactable = Loader.b(d, "m_Interactable", true);
        blocksRaycasts = Loader.b(d, "m_BlocksRaycasts", true);
        ignoreParentGroups = Loader.b(d, "m_IgnoreParentGroups", false);
    }
    @Override public void $cloneFrom(Component s, Eng.CloneCtx ctx) {
        CanvasGroup g = (CanvasGroup) s;
        alpha = g.alpha; interactable = g.interactable; blocksRaycasts = g.blocksRaycasts; ignoreParentGroups = g.ignoreParentGroups;
    }
}
