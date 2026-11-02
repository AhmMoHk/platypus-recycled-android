package cs.UnityEngine.SceneManagement;

/** UnityEngine.SceneManagement.SceneManager. LoadSceneMode: Single 0, Additive 1. */
public final class SceneManager {
    private SceneManager() {}
    static cs.System.Delegate sceneLoaded, sceneUnloaded, activeSceneChanged;

    public static int get_sceneCount() { return cs.UnityEngine.Eng.loadedScenes().length; }
    public static int get_sceneCountInBuildSettings() { return cs.UnityEngine.Loader.buildSceneCount(); }
    public static Scene GetActiveScene() { return Scene.ofHandle(cs.UnityEngine.Eng.activeSceneHandle()); }
    public static boolean SetActiveScene(Scene s) { return cs.UnityEngine.Eng.setActiveScene(s.m_Handle); }
    public static Scene GetSceneAt(int i) { return Scene.ofHandle(cs.UnityEngine.Eng.loadedScenes()[i]); }
    public static Scene GetSceneByBuildIndex(int bi) {
        for (int h : cs.UnityEngine.Eng.loadedScenes()) if (cs.UnityEngine.Eng.sceneBuildIndex(h) == bi) return Scene.ofHandle(h);
        return new Scene();
    }
    public static Scene GetSceneByName(String n) {
        for (int h : cs.UnityEngine.Eng.loadedScenes()) if (cs.UnityEngine.Eng.sceneName(h).equals(n)) return Scene.ofHandle(h);
        return new Scene();
    }
    public static void LoadScene(int bi) { LoadScene(bi, 0); }
    public static void LoadScene(int bi, int mode) { cs.UnityEngine.SceneLoads.request(bi, mode == 1, true); }
    public static void LoadScene(String n) { LoadScene(n, 0); }
    public static void LoadScene(String n, int mode) { LoadScene(cs.UnityEngine.Loader.sceneIndexOf(n), mode); }
    public static cs.UnityEngine.AsyncOperation LoadSceneAsync(int bi) { return LoadSceneAsync(bi, 0); }
    public static cs.UnityEngine.AsyncOperation LoadSceneAsync(int bi, int mode) { return cs.UnityEngine.SceneLoads.request(bi, mode == 1, false); }
    public static cs.UnityEngine.AsyncOperation LoadSceneAsync(String n) { return LoadSceneAsync(n, 0); }
    public static cs.UnityEngine.AsyncOperation LoadSceneAsync(String n, int mode) { return LoadSceneAsync(cs.UnityEngine.Loader.sceneIndexOf(n), mode); }
    public static cs.UnityEngine.AsyncOperation UnloadSceneAsync(Scene s) { return cs.UnityEngine.SceneLoads.unload(s.m_Handle); }
    public static cs.UnityEngine.AsyncOperation UnloadSceneAsync(int bi) { return UnloadSceneAsync(GetSceneByBuildIndex(bi)); }
    public static cs.UnityEngine.AsyncOperation UnloadSceneAsync(String n) { return UnloadSceneAsync(GetSceneByName(n)); }
    public static void MoveGameObjectToScene(cs.UnityEngine.GameObject g, Scene s) { cs.UnityEngine.Eng.moveRootToScene(g, s.m_Handle); }

    // events (UnityAction`2<Scene, LoadSceneMode>, UnityAction`1<Scene>, UnityAction`2<Scene, Scene>)
    public static void add_sceneLoaded(cs.System.Delegate d) { sceneLoaded = cs.System.Delegate.Combine(sceneLoaded, d); }
    public static void remove_sceneLoaded(cs.System.Delegate d) { sceneLoaded = cs.System.Delegate.Remove(sceneLoaded, d); }
    public static void add_sceneUnloaded(cs.System.Delegate d) { sceneUnloaded = cs.System.Delegate.Combine(sceneUnloaded, d); }
    public static void remove_sceneUnloaded(cs.System.Delegate d) { sceneUnloaded = cs.System.Delegate.Remove(sceneUnloaded, d); }
    public static void add_activeSceneChanged(cs.System.Delegate d) { activeSceneChanged = cs.System.Delegate.Combine(activeSceneChanged, d); }
    public static void remove_activeSceneChanged(cs.System.Delegate d) { activeSceneChanged = cs.System.Delegate.Remove(activeSceneChanged, d); }

    public static void $fireLoaded(int handle, int mode) {
        if (sceneLoaded != null) sceneLoaded.DynamicInvoke(new Object[]{Scene.ofHandle(handle), LoadSceneMode.$info.box(mode)});
    }
    public static void $fireUnloaded(int handle) {
        if (sceneUnloaded != null) sceneUnloaded.DynamicInvoke(new Object[]{Scene.ofHandle(handle)});
    }
    public static void $fireActiveChanged(int from, int to) {
        if (activeSceneChanged != null) activeSceneChanged.DynamicInvoke(new Object[]{Scene.ofHandle(from), Scene.ofHandle(to)});
    }
}
