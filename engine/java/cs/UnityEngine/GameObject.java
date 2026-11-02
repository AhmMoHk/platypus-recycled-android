package cs.UnityEngine;

import java.util.ArrayList;

public class GameObject extends UObject {
    final ArrayList<Component> components = new ArrayList<>(4);
    Transform transform;
    boolean m_IsActive = true;
    /** cached activeInHierarchy */
    boolean $activeInHierarchy;
    public int m_Layer;
    public String m_TagString = "Untagged";
    int scene;           // scene handle (0 = none / DontDestroyOnLoad = -1)

    /** used by the loader: no Transform yet */
    GameObject(boolean bare) { }

    public GameObject() { this("New Game Object"); }
    public GameObject(String name) {
        m_Name = name == null ? "" : name;
        Transform t = new Transform();
        attach(t);
        scene = Eng.activeScene();
        Eng.registerNewRoot(this);
    }
    public GameObject(String name, cs.System.Type[] comps) {
        this(name);
        if (comps != null) for (cs.System.Type t : comps) AddComponent(t);
    }

    void attach(Component c) {
        c.gameObject = this;
        if (c instanceof Transform) {
            transform = (Transform) c;
            components.add(0, c);
        } else components.add(c);
    }

    public GameObject get_gameObject() { return this; }
    public Transform get_transform() { return transform; }
    public int get_layer() { return m_Layer; }
    public void set_layer(int l) { m_Layer = l; }
    public String get_tag() { return m_TagString; }
    public void set_tag(String t) { m_TagString = t; }
    public boolean CompareTag(String t) { return m_TagString.equals(t); }
    public boolean get_activeSelf() { return m_IsActive; }
    public boolean get_activeInHierarchy() { return $activeInHierarchy && !$destroyed; }
    public void SetActive(boolean v) { Eng.setActive(this, v); }
    public cs.UnityEngine.SceneManagement.Scene get_scene() { return cs.UnityEngine.SceneManagement.Scene.ofHandle(scene); }
    public boolean get_isStatic() { return false; }

    public Component AddComponent(cs.System.Type t) { return Eng.addComponent(this, t); }
    public Object AddComponent(cs.System.Type g, cs.System.Type t) { return Eng.addComponent(this, t); }

    public Component GetComponent(cs.System.Type t) {
        for (int i = 0, n = components.size(); i < n; i++) {
            Component c = components.get(i);
            if (!c.$destroyed && t.cls.isInstance(c)) return c;
        }
        return null;
    }
    public Component GetComponent(String typeName) {
        for (Component c : components) {
            if (c.$destroyed) continue;
            for (Class<?> k = c.getClass(); k != null; k = k.getSuperclass())
                if (cs.System.Type.csNameOf(k).endsWith(typeName) && (cs.System.Type.csNameOf(k).equals(typeName) || cs.System.Type.csNameOf(k).endsWith("." + typeName))) return c;
        }
        return null;
    }
    public boolean TryGetComponent(cs.System.Type t, cs.rt.RefO out) { out.v = GetComponent(t); return out.v != null; }

    public Object GetComponents(cs.System.Type t) {
        ArrayList<Component> l = new ArrayList<>();
        for (Component c : components) if (!c.$destroyed && t.cls.isInstance(c)) l.add(c);
        return Eng.typedArray(t, l);
    }
    public void GetComponents(cs.System.Type t, cs.System.Collections.Generic.List_1 out) {
        out.Clear();
        for (Component c : components) if (!c.$destroyed && t.cls.isInstance(c)) out.Add(c);
    }

    public Object GetComponentInChildren(cs.System.Type t) { return GetComponentInChildren(t, false); }
    public Object GetComponentInChildren(cs.System.Type t, boolean includeInactive) { return Eng.componentInChildren(this, t, includeInactive); }
    public Object GetComponentsInChildren(cs.System.Type t) { return GetComponentsInChildren(t, false); }
    public Object GetComponentsInChildren(cs.System.Type t, boolean includeInactive) {
        ArrayList<Component> l = new ArrayList<>();
        Eng.componentsInChildren(this, t, includeInactive, l);
        return Eng.typedArray(t, l);
    }
    public void GetComponentsInChildren(cs.System.Type t, cs.System.Collections.Generic.List_1 out) { GetComponentsInChildren(t, false, out); }
    public void GetComponentsInChildren(cs.System.Type t, boolean includeInactive, cs.System.Collections.Generic.List_1 out) {
        ArrayList<Component> l = new ArrayList<>();
        Eng.componentsInChildren(this, t, includeInactive, l);
        out.Clear();
        for (Component c : l) out.Add(c);
    }
    public Object GetComponentInParent(cs.System.Type t) { return GetComponentInParent(t, false); }
    public Object GetComponentInParent(cs.System.Type t, boolean includeInactive) {
        for (Transform tr = transform; tr != null; tr = tr.parent) {
            if (!includeInactive && !tr.gameObject.get_activeInHierarchy()) continue;
            Component c = tr.gameObject.GetComponent(t);
            if (c != null) return c;
        }
        return null;
    }
    public Object GetComponentsInParent(cs.System.Type t) { return GetComponentsInParent(t, false); }
    public Object GetComponentsInParent(cs.System.Type t, boolean includeInactive) {
        ArrayList<Component> l = new ArrayList<>();
        for (Transform tr = transform; tr != null; tr = tr.parent) {
            if (!includeInactive && !tr.gameObject.get_activeInHierarchy()) continue;
            for (Component c : tr.gameObject.components) if (!c.$destroyed && t.cls.isInstance(c)) l.add(c);
        }
        return Eng.typedArray(t, l);
    }
    public void GetComponentsInParent(cs.System.Type t, boolean includeInactive, cs.System.Collections.Generic.List_1 out) {
        out.Clear();
        for (Transform tr = transform; tr != null; tr = tr.parent) {
            if (!includeInactive && !tr.gameObject.get_activeInHierarchy()) continue;
            for (Component c : tr.gameObject.components) if (!c.$destroyed && t.cls.isInstance(c)) out.Add(c);
        }
    }

    public void SendMessage(String method) { SendMessage(method, null, 1); }
    public void SendMessage(String method, Object arg) { SendMessage(method, arg, 1); }
    public void SendMessage(String method, Object arg, int options) { Eng.sendMessage(this, method, arg); }
    public void BroadcastMessage(String method) { Eng.broadcastMessage(this, method, null); }
    public void BroadcastMessage(String method, Object arg) { Eng.broadcastMessage(this, method, arg); }

    public static GameObject Find(String name) { return Eng.findGameObject(name); }
    public static GameObject FindWithTag(String tag) { return Eng.findWithTag(tag); }
    public static GameObject FindGameObjectWithTag(String tag) { return Eng.findWithTag(tag); }
    public static GameObject[] FindGameObjectsWithTag(String tag) { return Eng.findAllWithTag(tag); }
}
