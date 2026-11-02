package cs.UnityEngine.SceneManagement;

/** UnityEngine.SceneManagement.Scene (a handle) */
public class Scene implements cs.rt.Struct {
    public int m_Handle;
    public Scene() {}
    public static Scene ofHandle(int h) { Scene s = new Scene(); s.m_Handle = h; return s; }
    public int get_handle() { return m_Handle; }
    public boolean IsValid() { return cs.UnityEngine.Eng.sceneExists(m_Handle); }
    public String get_name() { return cs.UnityEngine.Eng.sceneName(m_Handle); }
    public String get_path() { return cs.UnityEngine.Eng.scenePath(m_Handle); }
    public int get_buildIndex() { return cs.UnityEngine.Eng.sceneBuildIndex(m_Handle); }
    public boolean get_isLoaded() { return cs.UnityEngine.Eng.sceneLoaded(m_Handle); }
    public int get_rootCount() { return cs.UnityEngine.Eng.sceneRoots(m_Handle).length; }
    public cs.UnityEngine.GameObject[] GetRootGameObjects() { return cs.UnityEngine.Eng.sceneRoots(m_Handle); }
    public void GetRootGameObjects(cs.System.Collections.Generic.List_1 l) {
        l.Clear();
        for (cs.UnityEngine.GameObject g : GetRootGameObjects()) l.Add(g);
    }
    public static boolean op_Equality(Scene a, Scene b) { return a.m_Handle == b.m_Handle; }
    public static boolean op_Inequality(Scene a, Scene b) { return a.m_Handle != b.m_Handle; }
    @Override public boolean equals(Object o) { return o instanceof Scene && ((Scene) o).m_Handle == m_Handle; }
    @Override public int hashCode() { return m_Handle; }
    public Scene $copy() { return ofHandle(m_Handle); }
    public void $set(Object o) { m_Handle = ((Scene) o).m_Handle; }
    public void $clear() { m_Handle = 0; }
    public static Scene[] $newArray(int n) { Scene[] a = new Scene[n]; for (int i = 0; i < n; i++) a[i] = new Scene(); return a; }
}
