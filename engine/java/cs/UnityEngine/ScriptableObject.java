package cs.UnityEngine;

public class ScriptableObject extends UObject {
    public ScriptableObject() {}
    public void $ctor_ScriptableObject() {}
    public Object $Awake() { return null; }
    public Object $OnEnable() { return null; }
    public Object $OnDisable() { return null; }
    public Object $OnDestroy() { return null; }
    public Object $OnValidate() { return null; }

    public static ScriptableObject CreateInstance(cs.System.Type t) {
        ScriptableObject o = (ScriptableObject) Eng.newComponent(t.cls);
        o.m_Name = "";
        Loader.awakeScriptable(o);
        return o;
    }
    public static Object CreateInstance(cs.System.Type g, cs.System.Type t) { return CreateInstance(t); }
    public static ScriptableObject CreateInstance(String className) {
        cs.System.Type t = cs.System.Type.GetType(className);
        return t == null ? null : CreateInstance(t);
    }
    @Override public void $onDestroyAsset() {
        try { $OnDisable(); $OnDestroy(); } catch (RuntimeException e) { Debug.logException(e, this); }
    }
}
