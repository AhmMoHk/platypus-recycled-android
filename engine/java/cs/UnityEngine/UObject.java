package cs.UnityEngine;

/** UnityEngine.Object */
public class UObject {
    private static int nextId = 2;
    public String m_Name = "";
    public int m_HideFlags;
    final int $id;
    boolean $destroyed;
    /** an asset (prefab template, sprite, material...): never part of a scene, no lifecycle */
    boolean $asset;
    /** "file#pathID" of objects loaded from the game data */
    public String $ref;

    public UObject() {
        synchronized (UObject.class) { $id = nextId += 2; }
    }

    public String get_name() { return m_Name; }
    public void set_name(String n) { m_Name = n; }
    public int get_hideFlags() { return m_HideFlags; }
    public void set_hideFlags(int f) { m_HideFlags = f; }
    public int GetInstanceID() { return $id; }
    public boolean $alive() { return !$destroyed; }
    /** fill an asset (non-component) from exported data */
    public void $loadAsset(java.util.HashMap<String, Object> d, Loader.Ctx ctx) {}
    /** Instantiate() of a non-GameObject asset (Material, Mesh, Texture...) */
    public UObject $cloneAsset() { return null; }
    /** destroying a non-scene object (asset, ScriptableObject) */
    public void $onDestroyAsset() {}

    static boolean alive(UObject o) { return o != null && !o.$destroyed; }

    public static boolean op_Implicit$boolean_(UObject o) { return alive(o); }
    public static boolean op_Equality(UObject a, UObject b) {
        boolean x = alive(a), y = alive(b);
        if (!x || !y) return x == y;
        return a == b;
    }
    public static boolean op_Inequality(UObject a, UObject b) { return !op_Equality(a, b); }

    public static void Destroy(UObject o) { Eng.destroy(o, 0f); }
    public static void Destroy(UObject o, float t) { Eng.destroy(o, t); }
    public static void DestroyImmediate(UObject o) { Eng.destroyNow(o); }
    public static void DestroyImmediate(UObject o, boolean allowAssets) { Eng.destroyNow(o); }
    public static void DontDestroyOnLoad(UObject o) { Eng.dontDestroyOnLoad(o); }

    public static Object FindObjectOfType(cs.System.Type t) { return Eng.findObjectOfType(t, false); }
    public static Object FindObjectOfType(cs.System.Type t, boolean includeInactive) { return Eng.findObjectOfType(t, includeInactive); }
    public static Object FindObjectOfType(cs.System.Type g, cs.System.Type t) { return Eng.findObjectOfType(t, false); }
    public static Object FindAnyObjectByType(cs.System.Type t) { return Eng.findObjectOfType(t, false); }
    public static Object FindAnyObjectByType(cs.System.Type t, int inactive) { return Eng.findObjectOfType(t, inactive == 1); }
    public static Object FindFirstObjectByType(cs.System.Type t) { return Eng.findObjectOfType(t, false); }
    public static Object FindObjectsOfType(cs.System.Type t) { return Eng.findObjectsOfType(t, false); }
    public static Object FindObjectsOfType(cs.System.Type t, boolean includeInactive) { return Eng.findObjectsOfType(t, includeInactive); }
    public static Object FindObjectsByType(cs.System.Type t, int sortMode) { return Eng.findObjectsOfType(t, false); }
    public static Object FindObjectsByType(cs.System.Type t, int inactive, int sortMode) { return Eng.findObjectsOfType(t, inactive == 1); }

    // generic Instantiate<T>(T original, ...)
    public static Object Instantiate(cs.System.Type t, Object original) { return Eng.instantiate((UObject) original, null, false, null, null); }
    public static Object Instantiate(cs.System.Type t, Object original, Transform parent) { return Eng.instantiate((UObject) original, parent, false, null, null); }
    public static Object Instantiate(cs.System.Type t, Object original, Transform parent, boolean worldStays) { return Eng.instantiate((UObject) original, parent, worldStays, null, null); }
    public static Object Instantiate(cs.System.Type t, Object original, Vector3 pos, Quaternion rot) { return Eng.instantiate((UObject) original, null, true, pos, rot); }
    public static Object Instantiate(cs.System.Type t, Object original, Vector3 pos, Quaternion rot, Transform parent) { return Eng.instantiate((UObject) original, parent, true, pos, rot); }
    // non-generic
    public static UObject Instantiate(UObject original) { return Eng.instantiate(original, null, false, null, null); }
    public static UObject Instantiate(UObject original, Transform parent) { return Eng.instantiate(original, parent, false, null, null); }
    public static UObject Instantiate(UObject original, Transform parent, boolean worldStays) { return Eng.instantiate(original, parent, worldStays, null, null); }
    public static UObject Instantiate(UObject original, Vector3 pos, Quaternion rot) { return Eng.instantiate(original, null, true, pos, rot); }
    public static UObject Instantiate(UObject original, Vector3 pos, Quaternion rot, Transform parent) { return Eng.instantiate(original, parent, true, pos, rot); }

    @Override public String toString() {
        return get_name() + " (" + cs.System.Type.csNameOf(getClass()) + ")";
    }
    public String ToString() { return toString(); }
}
