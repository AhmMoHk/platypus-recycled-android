package cs.UnityEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;

/**
 * The engine core: scenes, object lifecycle (Awake/OnEnable/Start/Update/FixedUpdate/LateUpdate/OnDisable/OnDestroy),
 * destruction, Instantiate, coroutines, Invoke, and the frame loop with Unity's timing rules.
 */
public final class Eng {
    private Eng() {}

    // ------------------------------------------------------------------ scenes
    static final class SceneData {
        int handle, buildIndex;
        String name = "", path = "";
        final ArrayList<GameObject> roots = new ArrayList<>();
        boolean loaded;
    }

    static final ArrayList<SceneData> scenes = new ArrayList<>();
    static final SceneData dontDestroy = new SceneData();
    static int nextSceneHandle = 2;
    static SceneData active;

    static {
        dontDestroy.handle = -1;
        dontDestroy.name = "DontDestroyOnLoad";
        dontDestroy.buildIndex = -1;
        dontDestroy.loaded = true;
    }

    static SceneData sceneOf(int handle) {
        if (handle == -1) return dontDestroy;
        for (SceneData s : scenes) if (s.handle == handle) return s;
        return null;
    }

    static int activeScene() {
        if (active == null) {
            active = new SceneData();
            active.handle = nextSceneHandle++;
            active.name = "Untitled";
            active.loaded = true;
            scenes.add(active);
        }
        return active.handle;
    }

    // ---- scene queries (SceneManagement)
    public static boolean sceneExists(int h) { return sceneOf(h) != null && h != 0; }
    public static String sceneName(int h) { SceneData s = sceneOf(h); return s == null ? "" : s.name; }
    public static String scenePath(int h) { SceneData s = sceneOf(h); return s == null ? "" : s.path; }
    public static int sceneBuildIndex(int h) { SceneData s = sceneOf(h); return s == null ? -1 : s.buildIndex; }
    public static boolean sceneLoaded(int h) { SceneData s = sceneOf(h); return s != null && s.loaded; }
    public static GameObject[] sceneRoots(int h) {
        SceneData s = sceneOf(h);
        if (s == null) return new GameObject[0];
        ArrayList<GameObject> l = new ArrayList<>();
        for (GameObject g : s.roots) if (!g.$destroyed) l.add(g);
        return l.toArray(new GameObject[0]);
    }
    public static int[] loadedScenes() {
        int n = 0;
        for (SceneData s : scenes) if (s.loaded) n++;
        int[] r = new int[n];
        n = 0;
        for (SceneData s : scenes) if (s.loaded) r[n++] = s.handle;
        return r;
    }
    public static int activeSceneHandle() { return activeScene(); }
    public static boolean setActiveScene(int h) {
        SceneData s = sceneOf(h);
        if (s == null || !s.loaded || h == -1) return false;
        int old = active == null ? 0 : active.handle;
        active = s;
        if (old != h) cs.UnityEngine.SceneManagement.SceneManager.$fireActiveChanged(old, h);
        return true;
    }
    public static void moveRootToScene(GameObject g, int h) {
        if (g.transform.parent != null) throw new cs.System.ArgumentException("Gameobject is not a root in a scene");
        removeRoot(g);
        setSceneRec(g.transform, h);
        addRoot(g);
    }
    static SceneData newScene(int buildIndex, String name, String path) {
        SceneData s = new SceneData();
        s.handle = nextSceneHandle++;
        s.buildIndex = buildIndex;
        s.name = name;
        s.path = path;
        scenes.add(s);
        return s;
    }
    /** destroy all objects of a scene and remove it */
    static void unloadScene(SceneData s) {
        for (GameObject g : new ArrayList<>(s.roots)) destroyNow(g);
        s.roots.clear();
        s.loaded = false;
        scenes.remove(s);
        if (active == s) active = null;
    }

    static void registerNewRoot(GameObject go) {
        SceneData s = sceneOf(go.scene);
        if (s != null) s.roots.add(go);
        go.$activeInHierarchy = go.m_IsActive;
    }

    static void addRoot(GameObject go) {
        if (go == null) return;
        SceneData s = sceneOf(go.scene);
        if (s != null && !s.roots.contains(go)) s.roots.add(go);
    }

    static void removeRoot(GameObject go) {
        if (go == null) return;
        SceneData s = sceneOf(go.scene);
        if (s != null) s.roots.remove(go);
    }

    static int rootIndex(GameObject go) {
        SceneData s = sceneOf(go.scene);
        return s == null ? 0 : s.roots.indexOf(go);
    }

    static void setRootIndex(GameObject go, int i) {
        SceneData s = sceneOf(go.scene);
        if (s == null) return;
        s.roots.remove(go);
        s.roots.add(Math.max(0, Math.min(i, s.roots.size())), go);
    }

    static void moveToScene(GameObject go, int handle) {
        setSceneRec(go.transform, handle);
    }

    private static void setSceneRec(Transform t, int handle) {
        t.gameObject.scene = handle;
        for (Transform c : t.children) setSceneRec(c, handle);
    }

    static void dontDestroyOnLoad(UObject o) {
        GameObject go = o instanceof GameObject ? (GameObject) o : o instanceof Component ? ((Component) o).gameObject : null;
        if (go == null || go.$asset) return;
        Transform root = go.transform.get_root();
        removeRoot(root.gameObject);
        setSceneRec(root, -1);
        if (!dontDestroy.roots.contains(root.gameObject)) dontDestroy.roots.add(root.gameObject);
    }

    // ------------------------------------------------------------------ behaviour registry
    static final int H_AWAKE = 1, H_START = 2, H_UPDATE = 4, H_FIXED = 8, H_LATE = 16, H_ENABLE = 32, H_DISABLE = 64, H_DESTROY = 128,
        H_RECT = 256, H_PARENT = 512, H_CHILDREN = 1024, H_TRIGGER = 2048, H_COLLISION = 4096, H_CANVASGROUP = 8192, H_GUI = 16384;
    private static final HashMap<Class<?>, Integer> hookCache = new HashMap<>();
    private static final String[][] HOOKS = {
        {"$Awake"}, {"$Start"}, {"$Update"}, {"$FixedUpdate"}, {"$LateUpdate"}, {"$OnEnable"}, {"$OnDisable"}, {"$OnDestroy"},
        {"$OnRectTransformDimensionsChange"}, {"$OnTransformParentChanged"}, {"$OnTransformChildrenChanged"},
        {"$OnTriggerEnter2D", "$OnTriggerExit2D", "$OnTriggerStay2D"}, {"$OnCollisionEnter2D", "$OnCollisionExit2D", "$OnCollisionStay2D"},
        {"$OnCanvasGroupChanged"}, {"$OnGUI"}};

    static int hooks(Class<?> c) {
        Integer h = hookCache.get(c);
        if (h != null) return h;
        int m = 0;
        for (int i = 0; i < HOOKS.length; i++) {
            for (String n : HOOKS[i]) {
                for (java.lang.reflect.Method x : c.getMethods()) {
                    if (x.getName().equals(n) && x.getDeclaringClass() != MonoBehaviour.class) { m |= 1 << i; break; }
                }
            }
        }
        hookCache.put(c, m);
        return m;
    }

    /** enabled + active MonoBehaviours, sorted by execution order then creation */
    static final ArrayList<MonoBehaviour> running = new ArrayList<>();
    static boolean runningDirty;
    static final ArrayList<MonoBehaviour> pendingStart = new ArrayList<>();
    static long seq;
    static final HashMap<String, Integer> execOrder = new HashMap<>();

    static int orderOf(Class<?> c) {
        Integer o = execOrder.get(c.getName());
        return o == null ? 0 : o;
    }

    private static final Comparator<MonoBehaviour> ORDER = new Comparator<MonoBehaviour>() {
        public int compare(MonoBehaviour a, MonoBehaviour b) {
            if (a.$order != b.$order) return a.$order < b.$order ? -1 : 1;
            return Long.compare(a.$seq, b.$seq);
        }
    };

    static void prepareBehaviour(MonoBehaviour mb) {
        mb.$order = orderOf(mb.getClass());
        mb.$seq = seq++;
    }

    // ------------------------------------------------------------------ lifecycle
    static boolean inDestroy;

    static void callAwake(MonoBehaviour mb) {
        if (mb.$awoken) return;
        mb.$awoken = true;
        if ((hooks(mb.getClass()) & H_AWAKE) != 0) {
            try { mb.$Awake(); } catch (RuntimeException e) { Debug.logException(e, mb); }
        }
    }

    static void callEnable(Behaviour b) {
        if (b instanceof MonoBehaviour) {
            MonoBehaviour mb = (MonoBehaviour) b;
            if (mb.$enabledCalled) return;
            mb.$enabledCalled = true;
            running.add(mb);
            runningDirty = true;
            if (!mb.$started) pendingStart.add(mb);
            if ((hooks(mb.getClass()) & H_ENABLE) != 0) {
                try { mb.$OnEnable(); } catch (RuntimeException e) { Debug.logException(e, mb); }
            }
        } else b.$nativeEnable();
    }

    static void callDisable(Behaviour b) {
        if (b instanceof MonoBehaviour) {
            MonoBehaviour mb = (MonoBehaviour) b;
            if (!mb.$enabledCalled) return;
            mb.$enabledCalled = false;
            running.remove(mb);
            if ((hooks(mb.getClass()) & H_DISABLE) != 0) {
                try { mb.$OnDisable(); } catch (RuntimeException e) { Debug.logException(e, mb); }
            }
        } else b.$nativeDisable();
    }

    /** activate freshly loaded scene roots: Awake/OnEnable across all roots in execution order */
    static void activateRoots(ArrayList<GameObject> roots) {
        ArrayList<Component> comps = new ArrayList<>();
        for (GameObject g : roots) collectActivation(g.transform, comps);
        runActivation(comps);
    }

    /** a GameObject subtree became active in hierarchy */
    static void activateTree(GameObject go) {
        ArrayList<Component> comps = new ArrayList<>();
        collectActivation(go.transform, comps);
        runActivation(comps);
    }

    private static void collectActivation(Transform t, ArrayList<Component> out) {
        GameObject go = t.gameObject;
        if (!go.m_IsActive || go.$destroyed) return;
        if (go.$activeInHierarchy) return;
        go.$activeInHierarchy = true;
        out.addAll(go.components);
        for (int i = 0; i < t.children.size(); i++) collectActivation(t.children.get(i), out);
    }

    /** Awake+OnEnable per component (MonoBehaviours in execution order), built-ins first */
    static void runActivation(ArrayList<Component> comps) {
        ArrayList<MonoBehaviour> mbs = new ArrayList<>();
        for (Component c : comps) {
            if (c instanceof MonoBehaviour) mbs.add((MonoBehaviour) c);
            else if (c instanceof Behaviour && ((Behaviour) c).m_Enabled && !c.$destroyed) callEnable((Behaviour) c);
            else if (!(c instanceof Behaviour)) c.$onActivated();
        }
        Collections.sort(mbs, ORDER);
        for (MonoBehaviour mb : mbs) {
            if (mb.$destroyed || mb.gameObject == null || !mb.gameObject.get_activeInHierarchy()) continue;
            callAwake(mb);
            if (mb.m_Enabled && !mb.$destroyed && mb.gameObject.get_activeInHierarchy()) callEnable(mb);
        }
    }

    static void deactivateTree(GameObject go) {
        deactivateRec(go.transform);
    }

    private static void deactivateRec(Transform t) {
        GameObject go = t.gameObject;
        if (!go.$activeInHierarchy) return;
        for (int i = 0; i < t.children.size(); i++) deactivateRec(t.children.get(i));
        go.$activeInHierarchy = false;
        for (Component c : new ArrayList<>(go.components)) {
            if (c instanceof Behaviour) {
                Behaviour b = (Behaviour) c;
                if (b.m_Enabled) callDisable(b);
            } else c.$onDeactivated();
            if (c instanceof MonoBehaviour) stopAllCoroutines((MonoBehaviour) c);
        }
    }

    static void setActive(GameObject go, boolean v) {
        if (go.m_IsActive == v) return;
        go.m_IsActive = v;
        if (go.$asset) return;
        boolean parentActive = go.transform.parent == null || go.transform.parent.gameObject.get_activeInHierarchy();
        if (v && parentActive) activateTree(go);
        else if (!v && go.$activeInHierarchy) deactivateTree(go);
    }

    static void setEnabled(Behaviour b, boolean v) {
        if (b.m_Enabled == v) return;
        b.m_Enabled = v;
        if (b.gameObject == null || b.gameObject.$asset || !b.gameObject.get_activeInHierarchy()) return;
        if (v) {
            if (b instanceof MonoBehaviour) callAwake((MonoBehaviour) b);
            callEnable(b);
        } else callDisable(b);
    }

    static void hierarchyChanged(Transform t, Transform oldParent) {
        GameObject go = t.gameObject;
        if (go == null || go.$asset) return;
        boolean shouldBeActive = go.m_IsActive && (t.parent == null || t.parent.gameObject.get_activeInHierarchy());
        if (shouldBeActive && !go.$activeInHierarchy) activateTree(go);
        else if (!shouldBeActive && go.$activeInHierarchy) deactivateTree(go);
        sendHook(go, H_PARENT);
        if (oldParent != null && oldParent.gameObject != null) sendHook(oldParent.gameObject, H_CHILDREN);
        if (t.parent != null && t.parent.gameObject != null) sendHook(t.parent.gameObject, H_CHILDREN);
        Render.hierarchyChanged(t);
    }

    static void siblingOrderChanged(Transform parent) {
        if (parent.gameObject != null) sendHook(parent.gameObject, H_CHILDREN);
        Render.hierarchyChanged(parent);
    }

    static void rectDimensionsChanged(GameObject go) {
        if (go.$asset || !go.get_activeInHierarchy()) return;
        sendHook(go, H_RECT);
    }

    static void sendHook(GameObject go, int hook) {
        ArrayList<Component> cs = go.components;
        for (int i = 0; i < cs.size(); i++) {
            Component c = cs.get(i);
            if (!(c instanceof MonoBehaviour) || c.$destroyed) continue;
            MonoBehaviour mb = (MonoBehaviour) c;
            if ((hooks(mb.getClass()) & hook) == 0) continue;
            if (hook != H_RECT && hook != H_PARENT && hook != H_CHILDREN && !mb.get_isActiveAndEnabled()) continue;
            if (!mb.$awoken) continue;
            try {
                switch (hook) {
                    case H_RECT: mb.$OnRectTransformDimensionsChange(); break;
                    case H_PARENT: mb.$OnTransformParentChanged(); break;
                    case H_CHILDREN: mb.$OnTransformChildrenChanged(); break;
                    case H_CANVASGROUP: mb.$OnCanvasGroupChanged(); break;
                }
            } catch (RuntimeException e) { Debug.logException(e, mb); }
        }
    }

    static void canvasGroupChanged(GameObject go) {
        sendHookTree(go.transform, H_CANVASGROUP);
    }

    private static void sendHookTree(Transform t, int hook) {
        sendHook(t.gameObject, hook);
        for (int i = 0; i < t.children.size(); i++) sendHookTree(t.children.get(i), hook);
    }

    // ------------------------------------------------------------------ components
    static Component addComponent(GameObject go, cs.System.Type t) {
        if (t == null) throw new cs.System.ArgumentNullException("componentType");
        // RequireComponent
        for (Object a : allTypeAttrs(t.cls)) {
            if (a instanceof RequireComponent) {
                RequireComponent rc = (RequireComponent) a;
                for (cs.System.Type r : new cs.System.Type[]{rc.m_Type0, rc.m_Type1, rc.m_Type2}) {
                    if (r != null && go.GetComponent(r) == null) addComponent(go, r);
                }
            }
        }
        Component c;
        if (t.cls == Transform.class || t.cls == RectTransform.class) {
            if (t.cls == RectTransform.class && !(go.transform instanceof RectTransform)) {
                RectTransform rt = new RectTransform();
                Transform old = go.transform;
                rt.$cloneFrom(old, null);
                rt.parent = old.parent;
                rt.children.addAll(old.children);
                for (Transform ch : old.children) ch.parent = rt;
                if (old.parent != null) old.parent.children.set(old.parent.children.indexOf(old), rt);
                go.components.remove(old);
                old.$destroyed = true;
                go.attach(rt);
                return rt;
            }
            return go.transform;
        }
        c = (Component) newComponent(t.cls);
        go.attach(c);
        if (c instanceof MonoBehaviour) prepareBehaviour((MonoBehaviour) c);
        c.$created();
        if (!go.$asset && go.get_activeInHierarchy()) {
            ArrayList<Component> l = new ArrayList<>();
            l.add(c);
            runActivation(l);
        }
        return c;
    }

    private static final HashMap<Class<?>, Object[]> attrCache = new HashMap<>();
    /** type attributes including inherited ones */
    static Object[] allTypeAttrs(Class<?> c) {
        Object[] r = attrCache.get(c);
        if (r != null) return r;
        ArrayList<Object> l = new ArrayList<>();
        for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass())
            for (Object a : cs.System.Reflection.Assembly.typeAttrs(cs.System.Type.of(k))) l.add(a);
        r = l.toArray();
        attrCache.put(c, r);
        return r;
    }

    /** Java constructor + translated parameterless .NET constructor (field initializers). */
    static Object newComponent(Class<?> c) {
        try {
            Object o = c.getDeclaredConstructor().newInstance();
            cs.System.Type.construct(o, c);
            return o;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("cannot create " + c.getName() + ": " + e, e);
        }
    }

    // ------------------------------------------------------------------ queries
    static Object typedArray(cs.System.Type t, ArrayList<? extends Object> l) {
        Object arr = java.lang.reflect.Array.newInstance(t.cls, l.size());
        for (int i = 0; i < l.size(); i++) java.lang.reflect.Array.set(arr, i, l.get(i));
        return arr;
    }

    static Object componentInChildren(GameObject go, cs.System.Type t, boolean includeInactive) {
        if (!includeInactive && !go.get_activeInHierarchy()) return null;
        Component c = go.GetComponent(t);
        if (c != null) return c;
        for (Transform ch : go.transform.children) {
            Object r = componentInChildren(ch.gameObject, t, includeInactive);
            if (r != null) return r;
        }
        return null;
    }

    static void componentsInChildren(GameObject go, cs.System.Type t, boolean includeInactive, ArrayList<Component> out) {
        if (!includeInactive && !go.get_activeInHierarchy() && !go.$asset) return;
        for (Component c : go.components) if (!c.$destroyed && t.cls.isInstance(c)) out.add(c);
        for (Transform ch : go.transform.children) componentsInChildren(ch.gameObject, t, includeInactive, out);
    }

    static void allGameObjects(ArrayList<GameObject> out) {
        for (SceneData s : scenes) for (GameObject g : s.roots) collectGOs(g.transform, out);
        for (GameObject g : dontDestroy.roots) collectGOs(g.transform, out);
    }

    private static void collectGOs(Transform t, ArrayList<GameObject> out) {
        out.add(t.gameObject);
        for (Transform c : t.children) collectGOs(c, out);
    }

    static Object findObjectOfType(cs.System.Type t, boolean includeInactive) {
        ArrayList<GameObject> all = new ArrayList<>();
        allGameObjects(all);
        if (t.cls == GameObject.class) {
            for (GameObject g : all) if (!g.$destroyed && (includeInactive || g.get_activeInHierarchy())) return g;
            return null;
        }
        for (GameObject g : all) {
            if (g.$destroyed || (!includeInactive && !g.get_activeInHierarchy())) continue;
            for (Component c : g.components) {
                if (c.$destroyed || !t.cls.isInstance(c)) continue;
                if (!includeInactive && c instanceof Behaviour && !((Behaviour) c).m_Enabled && false) continue;
                return c;
            }
        }
        if (ScriptableObject.class.isAssignableFrom(t.cls) || !Component.class.isAssignableFrom(t.cls)) return Loader.findAsset(t);
        return null;
    }

    static Object findObjectsOfType(cs.System.Type t, boolean includeInactive) {
        ArrayList<Object> out = new ArrayList<>();
        ArrayList<GameObject> all = new ArrayList<>();
        allGameObjects(all);
        for (GameObject g : all) {
            if (g.$destroyed || (!includeInactive && !g.get_activeInHierarchy())) continue;
            if (t.cls == GameObject.class) { out.add(g); continue; }
            for (Component c : g.components) if (!c.$destroyed && t.cls.isInstance(c)) out.add(c);
        }
        return typedArray(t, out);
    }

    static GameObject findGameObject(String path) {
        ArrayList<GameObject> all = new ArrayList<>();
        allGameObjects(all);
        if (path.indexOf('/') >= 0) {
            String p = path.startsWith("/") ? path.substring(1) : path;
            String first = p.split("/")[0];
            for (GameObject g : all) {
                if (g.transform.parent == null && g.m_Name.equals(first) && g.get_activeInHierarchy()) {
                    Transform t = p.equals(first) ? g.transform : g.transform.Find(p.substring(first.length() + 1));
                    if (t != null && t.gameObject.get_activeInHierarchy()) return t.gameObject;
                }
            }
            return null;
        }
        for (GameObject g : all) if (g.m_Name.equals(path) && g.get_activeInHierarchy()) return g;
        return null;
    }

    static GameObject findWithTag(String tag) {
        ArrayList<GameObject> all = new ArrayList<>();
        allGameObjects(all);
        for (GameObject g : all) if (g.m_TagString.equals(tag) && g.get_activeInHierarchy()) return g;
        return null;
    }

    static GameObject[] findAllWithTag(String tag) {
        ArrayList<GameObject> all = new ArrayList<>(), out = new ArrayList<>();
        allGameObjects(all);
        for (GameObject g : all) if (g.m_TagString.equals(tag) && g.get_activeInHierarchy()) out.add(g);
        return out.toArray(new GameObject[0]);
    }

    // ------------------------------------------------------------------ messages by name
    static Object invokeByName(Object target, String method, Object arg) {
        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
            for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
                String n = m.getName();
                if (!(n.equals(method) || n.startsWith(method + "$"))) continue;
                int np = m.getParameterTypes().length;
                try {
                    m.setAccessible(true);
                    if (np == 0) return m.invoke(target);
                    if (np == 1) return m.invoke(target, cs.System.Reflection.MethodBase.toJava(arg, m.getParameterTypes()[0]));
                } catch (java.lang.reflect.InvocationTargetException e) {
                    throw cs.rt.Rt.toThrowable(e.getCause());
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }

    static void sendMessage(GameObject go, String method, Object arg) {
        for (Component c : new ArrayList<>(go.components)) if (c instanceof MonoBehaviour) invokeByName(c, method, arg);
    }

    static void broadcastMessage(GameObject go, String method, Object arg) {
        sendMessage(go, method, arg);
        for (Transform t : new ArrayList<>(go.transform.children)) broadcastMessage(t.gameObject, method, arg);
    }

    // ------------------------------------------------------------------ destruction
    private static final ArrayList<UObject> destroyQueue = new ArrayList<>();
    private static final ArrayList<Object[]> delayedDestroy = new ArrayList<>();

    static void destroy(UObject o, float t) {
        if (o == null || o.$destroyed) return;
        if (t > 0) { delayedDestroy.add(new Object[]{o, Time.timeD + t}); return; }
        if (!destroyQueue.contains(o)) destroyQueue.add(o);
    }

    static void processDestroys() {
        if (!delayedDestroy.isEmpty()) {
            for (int i = 0; i < delayedDestroy.size(); i++) {
                Object[] d = delayedDestroy.get(i);
                if (Time.timeD >= (Double) d[1]) {
                    delayedDestroy.remove(i--);
                    destroy((UObject) d[0], 0);
                }
            }
        }
        while (!destroyQueue.isEmpty()) {
            ArrayList<UObject> q = new ArrayList<>(destroyQueue);
            destroyQueue.clear();
            for (UObject o : q) destroyNow(o);
        }
    }

    static void destroyNow(UObject o) {
        if (o == null || o.$destroyed) return;
        if (o instanceof GameObject) destroyGameObject((GameObject) o, true);
        else if (o instanceof Transform) destroyGameObject(((Transform) o).gameObject, true);
        else if (o instanceof Component) destroyComponent((Component) o);
        else {
            o.$destroyed = true;
            o.$onDestroyAsset();
        }
    }

    private static void destroyComponent(Component c) {
        GameObject go = c.gameObject;
        if (c instanceof Behaviour) {
            Behaviour b = (Behaviour) c;
            if (b.m_Enabled && go != null && go.get_activeInHierarchy()) callDisable(b);
        }
        if (c instanceof MonoBehaviour) {
            MonoBehaviour mb = (MonoBehaviour) c;
            stopAllCoroutines(mb);
            cancelInvoke(mb, null);
            pendingStart.remove(mb);
            if (mb.$awoken && (hooks(mb.getClass()) & H_DESTROY) != 0) {
                try { mb.$OnDestroy(); } catch (RuntimeException e) { Debug.logException(e, mb); }
            }
        }
        c.$onDestroyNative();
        c.$destroyed = true;
        if (go != null) go.components.remove(c);
    }

    private static void destroyGameObject(GameObject go, boolean top) {
        if (go.$destroyed) return;
        if (top && go.$activeInHierarchy) deactivateTree(go);
        for (Transform ch : new ArrayList<>(go.transform.children)) destroyGameObject(ch.gameObject, false);
        for (Component c : new ArrayList<>(go.components)) {
            if (c instanceof MonoBehaviour) {
                MonoBehaviour mb = (MonoBehaviour) c;
                stopAllCoroutines(mb);
                cancelInvoke(mb, null);
                pendingStart.remove(mb);
                running.remove(mb);
                if (mb.$awoken && (hooks(mb.getClass()) & H_DESTROY) != 0) {
                    try { mb.$OnDestroy(); } catch (RuntimeException e) { Debug.logException(e, mb); }
                }
            }
            c.$onDestroyNative();
            c.$destroyed = true;
        }
        go.$destroyed = true;
        if (top) {
            Transform p = go.transform.parent;
            if (p != null) {
                p.children.remove(go.transform);
                if (p.gameObject != null && !p.gameObject.$destroyed) {
                    sendHook(p.gameObject, H_CHILDREN);
                    Render.hierarchyChanged(p);
                }
            } else removeRoot(go);
        }
    }

    // ------------------------------------------------------------------ Instantiate
    /** reference remapping for cloned hierarchies */
    public static final class CloneCtx {
        public final IdentityHashMap<Object, Object> map = new IdentityHashMap<>();
        public Object remap(Object o) {
            Object r = map.get(o);
            return r != null ? r : o;
        }
    }

    static UObject instantiate(UObject original, Transform parent, boolean worldStays, Vector3 pos, Quaternion rot) {
        if (original == null) throw new cs.System.ArgumentException("The Object you want to instantiate is null.");
        if (original instanceof ScriptableObject) return Loader.cloneScriptable((ScriptableObject) original);
        if (!(original instanceof GameObject) && !(original instanceof Component)) return Loader.cloneAsset(original);
        GameObject srcGo = original instanceof GameObject ? (GameObject) original : ((Component) original).gameObject;
        CloneCtx ctx = new CloneCtx();
        GameObject clone = cloneTree(srcGo.transform, ctx);
        clone.m_Name = srcGo.m_Name + "(Clone)";
        // copy serialized state (after all objects exist so references can be remapped)
        for (java.util.Map.Entry<Object, Object> e : new ArrayList<>(ctx.map.entrySet())) {
            if (e.getKey() instanceof Component) {
                Component s = (Component) e.getKey(), d = (Component) e.getValue();
                if (!(d instanceof Transform)) d.$cloneFrom(s, ctx);
                if (s instanceof MonoBehaviour) Loader.copySerialized(s, d, ctx);
            }
        }
        Transform t = clone.transform;
        clone.scene = activeScene();
        if (parent != null) {
            clone.scene = parent.gameObject.scene;
            setSceneRec(t, clone.scene);
            t.parent = parent;
            parent.children.add(t);
            t.dirty();
            if (pos != null) {
                t.set_position(pos);
                t.set_rotation(rot);
            } else if (worldStays) {
                Transform s = srcGo.transform;
                t.set_position(s.get_position());
                t.set_rotation(s.get_rotation());
            }
            if (t instanceof RectTransform) ((RectTransform) t).relayout();
        } else {
            setSceneRec(t, clone.scene);
            SceneData sd = sceneOf(clone.scene);
            sd.roots.add(clone);
            if (pos != null) {
                t.lp.$set(pos);
                t.lr.$set(rot);
                t.dirty();
            }
        }
        // activation
        clearAsset(t);
        boolean parentActive = parent == null || parent.gameObject.get_activeInHierarchy();
        if (clone.m_IsActive && parentActive) activateTree(clone);
        if (parent != null) {
            sendHook(parent.gameObject, H_CHILDREN);
            Render.hierarchyChanged(parent);
        }
        Object result = ctx.map.get(original);
        return (UObject) result;
    }

    private static void clearAsset(Transform t) {
        t.gameObject.$asset = false;
        t.gameObject.$activeInHierarchy = false;
        for (Component c : t.gameObject.components) c.$asset = false;
        for (Transform c : t.children) clearAsset(c);
    }

    private static GameObject cloneTree(Transform src, CloneCtx ctx) {
        GameObject sg = src.gameObject;
        GameObject g = new GameObject(true);
        g.m_Name = sg.m_Name;
        g.m_Layer = sg.m_Layer;
        g.m_TagString = sg.m_TagString;
        g.m_IsActive = sg.m_IsActive;
        g.$asset = true;               // no lifecycle until the whole tree is ready
        ctx.map.put(sg, g);
        for (Component c : sg.components) {
            if (c.$destroyed) continue;
            Component n;
            if (c instanceof Transform) {
                n = c instanceof RectTransform ? new RectTransform() : new Transform();
                n.$cloneFrom(c, ctx);
            } else {
                n = (Component) newComponent(c.getClass());
                if (n instanceof MonoBehaviour) prepareBehaviour((MonoBehaviour) n);
            }
            if (n instanceof Behaviour) ((Behaviour) n).m_Enabled = ((Behaviour) c).m_Enabled;
            n.$asset = true;
            g.attach(n);
            ctx.map.put(c, n);
            n.$created();
        }
        for (Transform ch : src.children) {
            GameObject cg = cloneTree(ch, ctx);
            cg.transform.parent = g.transform;
            g.transform.children.add(cg.transform);
        }
        return g;
    }

    // ------------------------------------------------------------------ coroutines
    static final ArrayList<Coroutine> coroutines = new ArrayList<>();

    static Coroutine startCoroutine(MonoBehaviour owner, cs.System.Collections.IEnumerator e) {
        if (e == null) throw new cs.System.NullReferenceException("routine is null");
        Coroutine co = new Coroutine(owner, e);
        if (owner.gameObject == null || !owner.gameObject.get_activeInHierarchy()) {
            Debug.LogWarning("Coroutine couldn't be started because the the game object '" + owner.get_name() + "' is inactive!");
            co.done = true;
            return co;
        }
        owner.$coroutines.add(co);
        step(co);
        if (!co.done) coroutines.add(co);
        return co;
    }

    /** advance a coroutine until its next wait */
    static void step(Coroutine co) {
        while (!co.done) {
            cs.System.Collections.IEnumerator top = co.stack.get(co.stack.size() - 1);
            boolean more;
            try {
                more = top.MoveNext();
            } catch (RuntimeException ex) {
                Debug.logException(ex, co.owner);
                finish(co);
                return;
            }
            if (!more) {
                co.stack.remove(co.stack.size() - 1);
                if (co.stack.isEmpty()) { finish(co); return; }
                continue;
            }
            Object y = top.get_Current();
            co.waitFrame = Time.frameCount;
            if (y == null) { co.kind = Coroutine.FRAME; return; }
            if (y instanceof WaitForSeconds) { co.kind = Coroutine.TIME; co.until = Time.timeD + ((WaitForSeconds) y).m_Seconds; return; }
            if (y instanceof WaitForFixedUpdate) { co.kind = Coroutine.FIXED; return; }
            if (y instanceof WaitForEndOfFrame) { co.kind = Coroutine.ENDFRAME; return; }
            if (y instanceof Coroutine) { co.kind = Coroutine.CO; co.waitFor = y; if (((Coroutine) y).done) continue; return; }
            if (y instanceof CustomYieldInstruction) { co.kind = Coroutine.CUSTOM; co.waitFor = y; if (!((CustomYieldInstruction) y).get_keepWaiting()) continue; return; }
            if (y instanceof AsyncOperation) { co.kind = Coroutine.ASYNC; co.waitFor = y; if (((AsyncOperation) y).get_isDone()) continue; return; }
            if (y instanceof cs.System.Collections.IEnumerator) { co.stack.add((cs.System.Collections.IEnumerator) y); continue; }
            co.kind = Coroutine.FRAME;
            return;
        }
    }

    private static void finish(Coroutine co) {
        co.done = true;
        co.owner.$coroutines.remove(co);
    }

    /** resume coroutines whose wait condition (of the given phase) is met */
    static void resumeCoroutines(int phase) {
        if (coroutines.isEmpty()) return;
        ArrayList<Coroutine> snap = new ArrayList<>(coroutines);
        for (Coroutine co : snap) {
            if (co.done) continue;
            boolean ready;
            switch (co.kind) {
                case Coroutine.FRAME: ready = phase == Coroutine.FRAME && co.waitFrame < Time.frameCount; break;
                case Coroutine.TIME: ready = phase == Coroutine.FRAME && Time.timeD >= co.until && co.waitFrame < Time.frameCount; break;
                case Coroutine.FIXED: ready = phase == Coroutine.FIXED; break;
                case Coroutine.ENDFRAME: ready = phase == Coroutine.ENDFRAME; break;
                case Coroutine.CO: ready = phase == Coroutine.FRAME && ((Coroutine) co.waitFor).done; break;
                case Coroutine.CUSTOM: ready = phase == Coroutine.FRAME && !((CustomYieldInstruction) co.waitFor).get_keepWaiting(); break;
                case Coroutine.ASYNC: ready = phase == Coroutine.FRAME && ((AsyncOperation) co.waitFor).get_isDone(); break;
                default: ready = false;
            }
            if (ready) step(co);
        }
        for (int i = coroutines.size() - 1; i >= 0; i--) if (coroutines.get(i).done) coroutines.remove(i);
    }

    static void stopCoroutine(MonoBehaviour owner, cs.System.Collections.IEnumerator e) {
        for (Coroutine co : new ArrayList<>(owner.$coroutines)) if (co.stack.size() > 0 && co.stack.get(0) == e) finish(co);
    }

    static void stopCoroutine(MonoBehaviour owner, Coroutine c) {
        if (c != null && c.owner == owner) finish(c);
    }

    static void stopCoroutineByName(MonoBehaviour owner, String name) {
        for (Coroutine co : new ArrayList<>(owner.$coroutines)) {
            if (co.stack.size() > 0 && co.stack.get(0).getClass().getName().contains("$" + name + "$")) finish(co);
        }
    }

    static void stopAllCoroutines(MonoBehaviour owner) {
        for (Coroutine co : new ArrayList<>(owner.$coroutines)) finish(co);
    }

    // ------------------------------------------------------------------ Invoke
    private static final ArrayList<Object[]> invokes = new ArrayList<>();      // {owner, method, time, rate}

    static void invoke(MonoBehaviour owner, String method, float time, float rate) {
        invokes.add(new Object[]{owner, method, Time.timeD + Math.max(0, time), rate});
    }

    static void cancelInvoke(MonoBehaviour owner, String method) {
        for (int i = invokes.size() - 1; i >= 0; i--) {
            Object[] v = invokes.get(i);
            if (v[0] == owner && (method == null || method.equals(v[1]))) invokes.remove(i);
        }
    }

    static boolean isInvoking(MonoBehaviour owner, String method) {
        for (Object[] v : invokes) if (v[0] == owner && (method == null || method.equals(v[1]))) return true;
        return false;
    }

    private static void runInvokes() {
        if (invokes.isEmpty()) return;
        for (Object[] v : new ArrayList<>(invokes)) {
            MonoBehaviour mb = (MonoBehaviour) v[0];
            if (mb.$destroyed) { invokes.remove(v); continue; }
            if (Time.timeD < (Double) v[2]) continue;
            float rate = (Float) v[3];
            if (rate > 0) v[2] = (Double) v[2] + rate; else invokes.remove(v);
            if (!mb.get_isActiveAndEnabled() && rate <= 0) continue;
            try { invokeByName(mb, (String) v[1], null); } catch (RuntimeException e) { Debug.logException(e, mb); }
        }
    }

    // ------------------------------------------------------------------ frame loop
    static void runStarts() {
        while (!pendingStart.isEmpty()) {
            ArrayList<MonoBehaviour> l = new ArrayList<>(pendingStart);
            pendingStart.clear();
            Collections.sort(l, ORDER);
            for (MonoBehaviour mb : l) {
                if (mb.$started || mb.$destroyed || !mb.$enabledCalled) continue;
                mb.$started = true;
                if ((hooks(mb.getClass()) & H_START) != 0) {
                    try {
                        Object r = mb.$Start();
                        if (r instanceof cs.System.Collections.IEnumerator) startCoroutine(mb, (cs.System.Collections.IEnumerator) r);
                    } catch (RuntimeException e) { Debug.logException(e, mb); }
                }
            }
        }
    }

    private static ArrayList<MonoBehaviour> snapshot() {
        if (runningDirty) {
            Collections.sort(running, ORDER);
            runningDirty = false;
        }
        return new ArrayList<>(running);
    }

    private static void callPhase(int hook) {
        ArrayList<MonoBehaviour> l = snapshot();
        for (int i = 0, n = l.size(); i < n; i++) {
            MonoBehaviour mb = l.get(i);
            if (!mb.$enabledCalled || !mb.$started || mb.$destroyed) continue;
            if ((hooks(mb.getClass()) & hook) == 0) continue;
            try {
                if (hook == H_UPDATE) mb.$Update();
                else if (hook == H_FIXED) mb.$FixedUpdate();
                else mb.$LateUpdate();
            } catch (RuntimeException e) {
                Debug.logException(e, mb);
            }
        }
    }

    /** one frame of the player loop; realDt = real seconds since last frame */
    public static void frame(float realDt) {
        SceneLoads.process();
        Time.beginFrame(realDt);
        Input.beginFrame();
        cs.Rewired.ReInput.$beginFrame();
        runStarts();
        // FixedUpdate steps (scaled time)
        int steps = 0;
        while (Time.fixedDue() && steps < 20) {
            Time.advanceFixed();
            Time.inFixed = true;
            runStarts();
            callPhase(H_FIXED);
            Physics2D.autoSimulate();
            resumeCoroutines(Coroutine.FIXED);
            Time.inFixed = false;
            processDestroys();
            steps++;
        }
        if (steps == 20) Time.syncFixed();
        runStarts();
        callPhase(H_UPDATE);
        runInvokes();
        resumeCoroutines(Coroutine.FRAME);
        processDestroys();
        cs.UnityEngine.Animations.ParentConstraint.$evaluateAll();
        Animator.$updateAll();
        callPhase(H_LATE);
        processDestroys();
        Render.beforeRender();
    }

    /** after rendering */
    public static void endOfFrame() {
        resumeCoroutines(Coroutine.ENDFRAME);
        processDestroys();
    }

    /** behaviours currently running (for engine subsystems) */
    static ArrayList<MonoBehaviour> runningBehaviours() { return running; }

    /** apply exported script execution orders (class java name -> order) */
    public static void setExecutionOrders(HashMap<String, Integer> m) { execOrder.putAll(m); }
}
