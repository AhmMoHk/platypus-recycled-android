package cs.UnityEngine;

import java.util.ArrayList;

/** Scene load/unload requests, applied at the start of the next frame (Unity semantics for LoadScene). */
public final class SceneLoads {
    private SceneLoads() {}
    static final class Req {
        int buildIndex, unloadHandle;
        boolean additive, unload;
        AsyncOperation op;
    }
    private static final ArrayList<Req> queue = new ArrayList<>();

    public static AsyncOperation request(int bi, boolean additive, boolean sync) {
        Req r = new Req();
        r.buildIndex = bi;
        r.additive = additive;
        r.op = new AsyncOperation();
        queue.add(r);
        return r.op;
    }

    public static AsyncOperation unload(int handle) {
        Req r = new Req();
        r.unload = true;
        r.unloadHandle = handle;
        r.op = new AsyncOperation();
        queue.add(r);
        return r.op;
    }

    static void process() {
        while (!queue.isEmpty()) {
            Req r = queue.get(0);
            if (!r.unload && !r.op.allowSceneActivation) {
                r.op.progress = .9f;
                return;
            }
            queue.remove(0);
            if (r.unload) {
                Eng.SceneData s = Eng.sceneOf(r.unloadHandle);
                if (s != null) {
                    Eng.unloadScene(s);
                    cs.UnityEngine.SceneManagement.SceneManager.$fireUnloaded(r.unloadHandle);
                }
                r.op.finish();
                continue;
            }
            load(r);
        }
    }

    /** load immediately (also used at boot) */
    static void load(Req r) {
        if (!r.additive) {
            for (Eng.SceneData s : new ArrayList<>(Eng.scenes)) {
                int h = s.handle;
                Eng.unloadScene(s);
                cs.UnityEngine.SceneManagement.SceneManager.$fireUnloaded(h);
            }
            Eng.processDestroys();
        }
        String path = "";
        try { path = (String) cs.rt.Json.list(Loader.meta().get("scenes")).get(r.buildIndex); } catch (RuntimeException ignored) {}
        String name = path.substring(path.lastIndexOf('/') + 1).replace(".unity", "");
        Eng.SceneData sd = Eng.newScene(r.buildIndex, name, path);
        int prevActive = Eng.active == null ? 0 : Eng.active.handle;
        if (!r.additive) Eng.active = sd;
        ArrayList<GameObject> roots = Loader.loadScene(r.buildIndex, sd);
        sd.loaded = true;
        Time.levelLoaded();
        Eng.activateRoots(roots);
        if (!r.additive && prevActive != sd.handle) cs.UnityEngine.SceneManagement.SceneManager.$fireActiveChanged(prevActive, sd.handle);
        cs.UnityEngine.SceneManagement.SceneManager.$fireLoaded(sd.handle, r.additive ? 1 : 0);
        r.op.finish();
    }

    /** boot: load the first scene synchronously */
    public static void boot(int buildIndex) {
        Req r = new Req();
        r.buildIndex = buildIndex;
        r.op = new AsyncOperation();
        load(r);
    }
}
