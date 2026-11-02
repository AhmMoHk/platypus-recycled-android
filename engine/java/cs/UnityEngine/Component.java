package cs.UnityEngine;

public class Component extends UObject {
    GameObject gameObject;

    public GameObject get_gameObject() { return gameObject; }
    public Transform get_transform() { return gameObject == null ? null : gameObject.transform; }
    @Override public String get_name() { return gameObject == null ? m_Name : gameObject.m_Name; }
    @Override public void set_name(String n) { if (gameObject != null) gameObject.m_Name = n; else m_Name = n; }
    public String get_tag() { return gameObject.m_TagString; }
    public void set_tag(String t) { gameObject.m_TagString = t; }
    public boolean CompareTag(String t) { return gameObject.m_TagString.equals(t); }

    public Component GetComponent(cs.System.Type t) { return gameObject.GetComponent(t); }
    public Component GetComponent(String n) { return gameObject.GetComponent(n); }
    public boolean TryGetComponent(cs.System.Type t, cs.rt.RefO out) { return gameObject.TryGetComponent(t, out); }
    public Object GetComponents(cs.System.Type t) { return gameObject.GetComponents(t); }
    public void GetComponents(cs.System.Type t, cs.System.Collections.Generic.List_1 out) { gameObject.GetComponents(t, out); }
    public Object GetComponentInChildren(cs.System.Type t) { return gameObject.GetComponentInChildren(t, false); }
    public Object GetComponentInChildren(cs.System.Type t, boolean inactive) { return gameObject.GetComponentInChildren(t, inactive); }
    public Object GetComponentsInChildren(cs.System.Type t) { return gameObject.GetComponentsInChildren(t, false); }
    public Object GetComponentsInChildren(cs.System.Type t, boolean inactive) { return gameObject.GetComponentsInChildren(t, inactive); }
    public void GetComponentsInChildren(cs.System.Type t, cs.System.Collections.Generic.List_1 out) { gameObject.GetComponentsInChildren(t, false, out); }
    public void GetComponentsInChildren(cs.System.Type t, boolean inactive, cs.System.Collections.Generic.List_1 out) { gameObject.GetComponentsInChildren(t, inactive, out); }
    public Object GetComponentInParent(cs.System.Type t) { return gameObject.GetComponentInParent(t, false); }
    public Object GetComponentInParent(cs.System.Type t, boolean inactive) { return gameObject.GetComponentInParent(t, inactive); }
    public Object GetComponentsInParent(cs.System.Type t) { return gameObject.GetComponentsInParent(t, false); }
    public Object GetComponentsInParent(cs.System.Type t, boolean inactive) { return gameObject.GetComponentsInParent(t, inactive); }
    public void GetComponentsInParent(cs.System.Type t, boolean inactive, cs.System.Collections.Generic.List_1 out) { gameObject.GetComponentsInParent(t, inactive, out); }
    public void SendMessage(String m) { gameObject.SendMessage(m); }
    public void SendMessage(String m, Object a) { gameObject.SendMessage(m, a); }
    public void BroadcastMessage(String m) { gameObject.BroadcastMessage(m); }

    /** called by the loader with the exported serialized data of built-in components */
    public void $load(java.util.HashMap<String, Object> d, Loader.Ctx ctx) {}
    /** copy built-in state for Instantiate (refs mapped through ctx) */
    public void $cloneFrom(Component src, Eng.CloneCtx ctx) {}
    /** engine notifications */
    public void $onDestroyNative() {}
    /** after construction + attach (built-ins register with subsystems here) */
    public void $created() {}
    /** non-Behaviour components: the GameObject became active / inactive in hierarchy */
    public void $onActivated() {}
    public void $onDeactivated() {}
}
