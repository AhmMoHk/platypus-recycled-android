package cs.UnityEngine;

import cs.rt.Json;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Converted asset bundles: the original bundle path holds a JSON marker
 * {"prrBundle":1, "dir":"<name>.d", "files":[...], "container":{"name":[file,pid],...}}
 * and <dir>/files/<file>.json hold the serialized objects (same format as the scene export).
 */
public class AssetBundle extends UObject {
    static final ArrayList<AssetBundle> loaded = new ArrayList<>();
    final HashMap<String, Object> container = new HashMap<>();
    final ArrayList<String> fileNames = new ArrayList<>();
    String path;

    public static AssetBundle LoadFromFile(String path) { return load(path); }
    public static AssetBundle LoadFromFile(String path, int crc) { return load(path); }
    public static AssetBundleCreateRequest LoadFromFileAsync(String path) {
        AssetBundleCreateRequest r = new AssetBundleCreateRequest();
        r.bundle = load(path);
        r.finish();
        return r;
    }
    public static AssetBundleCreateRequest LoadFromFileAsync(String path, int crc) { return LoadFromFileAsync(path); }
    public static AssetBundle[] GetAllLoadedAssetBundles() { return loaded.toArray(new AssetBundle[0]); }
    public static void UnloadAllAssetBundles(boolean unloadObjects) { for (AssetBundle b : new ArrayList<>(loaded)) b.Unload(unloadObjects); }

    static AssetBundle load(String path) {
        File f = new File(path);
        String txt = Loader.readText(f);
        if (txt == null || !txt.startsWith("{")) {
            Debug.LogError("Unable to open archive file: " + path);
            return null;
        }
        HashMap<String, Object> m = Json.map(Json.parse(txt));
        AssetBundle b = new AssetBundle();
        b.path = path;
        b.m_Name = f.getName();
        File dir = new File(f.getParentFile(), (String) m.get("dir"));
        for (Object fn : Loader.l(m, "files")) {
            String n = (String) fn;
            b.fileNames.add(n);
            Loader.registerFile(n, new File(dir, "files/" + n + ".json"));
        }
        b.container.putAll(Loader.m(m, "container"));
        loaded.add(b);
        return b;
    }

    private Object find(String name) {
        String n = name.toLowerCase();
        Object pp = container.get(n);
        if (pp != null) return pp;
        for (java.util.Map.Entry<String, Object> e : container.entrySet()) {
            String k = e.getKey();
            String base = k.substring(k.lastIndexOf('/') + 1);
            int dot = base.lastIndexOf('.');
            if (dot > 0) base = base.substring(0, dot);
            if (base.equals(n)) return e.getValue();
        }
        return null;
    }

    public UObject LoadAsset(String name) { return LoadAsset(name, cs.System.Type.of(UObject.class)); }
    public UObject LoadAsset(String name, cs.System.Type t) {
        Object pp = find(name);
        if (pp == null) return null;
        ArrayList<Object> l = Json.list(pp);
        Loader.FileData fd = Loader.file((String) l.get(0));
        if (fd == null) return null;
        UObject o = Loader.asset(fd, Json.lng(l.get(1)));
        if (o == null) return null;
        if (t != null && !t.cls.isInstance(o)) {
            if (o instanceof GameObject && Component.class.isAssignableFrom(t.cls)) return ((GameObject) o).GetComponent(t);
            return null;
        }
        return o;
    }
    public Object LoadAsset(cs.System.Type g, String name) { return LoadAsset(name, g); }
    public AssetBundleRequest LoadAssetAsync(String name, cs.System.Type t) {
        AssetBundleRequest r = new AssetBundleRequest();
        r.asset = LoadAsset(name, t);
        r.finish();
        return r;
    }
    public AssetBundleRequest LoadAssetAsync(cs.System.Type g, String name) { return LoadAssetAsync(name, g); }
    public AssetBundleRequest LoadAssetAsync(String name) { return LoadAssetAsync(name, cs.System.Type.of(UObject.class)); }
    public boolean Contains(String name) { return find(name) != null; }
    public String[] GetAllAssetNames() { return container.keySet().toArray(new String[0]); }
    public boolean get_isStreamedSceneAssetBundle() { return false; }

    public void Unload(boolean unloadAllLoadedObjects) {
        loaded.remove(this);
        for (String n : fileNames) {
            Loader.FileData fd = Loader.file(n);
            if (fd != null && unloadAllLoadedObjects) {
                for (UObject o : fd.assets.values()) if (o != null) Eng.destroyNow(o);
            }
            Loader.unloadFile(n);
        }
    }
    public AssetBundleUnloadOperation UnloadAsync(boolean unloadAllLoadedObjects) {
        Unload(unloadAllLoadedObjects);
        AssetBundleUnloadOperation op = new AssetBundleUnloadOperation();
        op.finish();
        return op;
    }
}
