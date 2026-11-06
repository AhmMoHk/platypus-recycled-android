package cs.UnityEngine;

import java.util.HashMap;

/** Shaders are identified by name; the renderer maps names to its own programs. Global properties live here. */
public class Shader extends UObject {
    private static final HashMap<String, Integer> ids = new HashMap<>();
    private static final java.util.ArrayList<String> names = new java.util.ArrayList<>();
    private static final HashMap<String, Shader> byName = new HashMap<>();
    static final MaterialPropertyBlock globals = new MaterialPropertyBlock();
    static int maximumLOD = -1;
    String[] props = new String[0];
    int renderQueue = 3000;

    public Shader() {}

    public static synchronized int PropertyToID(String n) {
        Integer i = ids.get(n);
        if (i == null) {
            i = names.size() + 1;
            ids.put(n, i);
            names.add(n);
        }
        return i;
    }
    public static synchronized String $nameOf(int id) { return id >= 1 && id <= names.size() ? names.get(id - 1) : null; }

    public static Shader Find(String n) {
        Shader s = byName.get(n);
        if (s == null) {
            s = new Shader();
            s.m_Name = n;
            s.$asset = true;
            byName.put(n, s);
        }
        return s;
    }
    static Shader named(String n, String[] props) {
        Shader s = Find(n);
        if (props != null && props.length > 0) s.props = props;
        return s;
    }

    public boolean get_isSupported() { return true; }
    public int get_renderQueue() { return renderQueue; }
    public int get_maximumLOD() { return maximumLOD; }
    public void set_maximumLOD(int v) { maximumLOD = v; }
    public int FindPropertyIndex(String n) { for (int i = 0; i < props.length; i++) if (props[i].equals(n)) return i; return -1; }
    public int GetPropertyCount() { return props.length; }
    boolean hasProp(String n) { for (String p : props) if (p.equals(n)) return true; return false; }

    public static void SetGlobalFloat(String n, float v) { globals.SetFloat(n, v); }
    public static void SetGlobalFloat(int id, float v) { globals.SetFloat(id, v); }
    public static void SetGlobalInt(String n, int v) { globals.SetFloat(n, v); }
    public static void SetGlobalColor(String n, Color c) { globals.SetColor(n, c); }
    public static void SetGlobalColor(int id, Color c) { globals.SetColor(id, c); }
    public static void SetGlobalVector(String n, Vector4 v) { globals.SetVector(n, v); }
    public static void SetGlobalVector(int id, Vector4 v) { globals.SetVector(id, v); }
    public static void SetGlobalTexture(String n, Texture t) { globals.SetTexture(n, t); }
    public static void SetGlobalFloatArray(String n, float[] a) { globals.SetFloatArray(n, a); }
    public static void SetGlobalFloatArray(int id, float[] a) { globals.SetFloatArray(id, a); }
    public static void SetGlobalMatrix(String n, Matrix4x4 m) { globals.SetMatrix(n, m); }
    public static float GetGlobalFloat(String n) { return globals.GetFloat(n); }
    public static Color GetGlobalColor(String n) { return globals.GetColor(n); }
    public static void EnableKeyword(String k) { globalKeywords.add(k); }
    public static void DisableKeyword(String k) { globalKeywords.remove(k); }
    public static boolean IsKeywordEnabled(String k) { return globalKeywords.contains(k); }
    static final java.util.HashSet<String> globalKeywords = new java.util.HashSet<>();
    public static void WarmupAllShaders() {}
    public static MaterialPropertyBlock $globals() { return globals; }

    @Override public void $loadAsset(HashMap<String, Object> d, Loader.Ctx ctx) {
        Object n = d.get("m_Name");
        if (n instanceof String) m_Name = (String) n;
        java.util.ArrayList<Object> p = Loader.l(d, "props");
        props = new String[p.size()];
        for (int i = 0; i < props.length; i++) props[i] = (String) p.get(i);
    }
}
