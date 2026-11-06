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

    /** all PPtrs registered under an asset name (full path or file name without extension) */
    private ArrayList<Object> find(String name) {
        String n = name.toLowerCase();
        ArrayList<Object> r = new ArrayList<>();
        for (java.util.Map.Entry<String, Object> e : container.entrySet()) {
            String k = e.getKey();
            String base = k.substring(k.lastIndexOf('/') + 1);
            int dot = base.lastIndexOf('.');
            if (dot > 0) base = base.substring(0, dot);
            if (!k.equals(n) && !base.equals(n)) continue;
            Object v = e.getValue();
            ArrayList<Object> l = Json.list(v);
            if (!l.isEmpty() && l.get(0) instanceof ArrayList) r.addAll(l);
            else r.add(v);
        }
        return r;
    }

    public UObject LoadAsset(String name) { return LoadAsset(name, cs.System.Type.of(UObject.class)); }
    public UObject LoadAsset(String name, cs.System.Type t) {
        UObject first = null;
        for (Object pp : find(name)) {
            ArrayList<Object> l = Json.list(pp);
            Loader.FileData fd = Loader.file((String) l.get(0));
            if (fd == null) continue;
            UObject o = Loader.asset(fd, Json.lng(l.get(1)));
            if (o == null) continue;
            if (t == null || t.cls.isInstance(o)) return o;
            if (o instanceof GameObject && Component.class.isAssignableFrom(t.cls)) {
                UObject c = ((GameObject) o).GetComponent(t);
                if (c != null) return c;
            }
            if (first == null) first = o;
        }
        return t == null || t.cls == UObject.class ? first : null;
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
    public boolean Contains(String name) { return !find(name).isEmpty(); }
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
