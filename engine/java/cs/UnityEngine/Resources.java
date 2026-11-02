package cs.UnityEngine;

public final class Resources {
    private Resources() {}
    public static UObject Load(String path) { return Loader.resource(path, null); }
    public static UObject Load(String path, cs.System.Type t) { return Loader.resource(path, t); }
    public static Object Load(cs.System.Type g, String path) { return Loader.resource(path, g); }
    public static ResourceRequest LoadAsync(String path, cs.System.Type t) {
        ResourceRequest r = new ResourceRequest();
        r.asset = Loader.resource(path, t);
        r.finish();
        return r;
    }
    public static Object LoadAll(cs.System.Type g, String path) { return java.lang.reflect.Array.newInstance(g.cls, 0); }
    public static Object GetBuiltinResource(cs.System.Type t, String path) { return Loader.builtinResource(t, path); }
    public static AsyncOperation UnloadUnusedAssets() { AsyncOperation a = new AsyncOperation(); a.finish(); return a; }
    public static void UnloadAsset(UObject o) {}
    public static Object FindObjectsOfTypeAll(cs.System.Type t) { return Eng.findObjectsOfType(t, true); }
}
