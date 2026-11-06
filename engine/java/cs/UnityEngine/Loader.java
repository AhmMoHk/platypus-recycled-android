package cs.UnityEngine;

import cs.rt.Json;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;

/**
 * Loads the exported Unity data (data/unity/files/*.json): scenes, prefabs and assets.
 * Scene objects are created per load; assets (incl. prefab hierarchies) are created once per file and cached.
 * MonoBehaviour / serializable-class fields are filled from the translator's $fmeta tables (flag 8 = Unity-serialized).
 */
public final class Loader {
    private Loader() {}

    // ------------------------------------------------------------------ files
    static final class FileData {
        final String name;
        final HashMap<String, Object> objects;
        final ArrayList<Object> externals;
        final HashMap<Long, UObject> assets = new HashMap<>();
        FileData(String name, HashMap<String, Object> root) {
            this.name = name;
            this.objects = Json.map(root.get("objects"));
            this.externals = root.containsKey("externals") ? Json.list(root.get("externals")) : new ArrayList<Object>();
        }
        HashMap<String, Object> obj(long pid) { return Json.map(objects.get(Long.toString(pid))); }
        String type(long pid) {
            Object o = objects.get(Long.toString(pid));
            return o == null ? null : (String) Json.map(o).get("t");
        }
        HashMap<String, Object> data(long pid) {
            HashMap<String, Object> o = obj(pid);
            return o == null ? null : Json.map(o.get("d"));
        }
    }

    private static final HashMap<String, FileData> files = new HashMap<>();
    private static HashMap<String, Object> meta;

    static String unityDir() { return Host.get().dataPath() + "/unity"; }

    public static InputStream open(String rel) throws IOException {
        return new FileInputStream(new File(Host.get().dataPath(), rel));
    }

    static String readText(File f) {
        if (!f.isFile() || f.length() > 512L * 1024 * 1024) return null;
        try {
            InputStream in = new FileInputStream(f);
            try {
                java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream((int) Math.max(16, f.length()));
                byte[] buf = new byte[65536];
                int n;
                while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
                return new String(bo.toByteArray(), "UTF-8");
            } finally {
                in.close();
            }
        } catch (IOException e) {
            return null;
        }
    }

    /** extra serialized-file locations (converted asset bundles): file name -> json path */
    private static final HashMap<String, File> fileLocations = new HashMap<>();
    static synchronized void registerFile(String name, File json) { fileLocations.put(name, json); }
    static synchronized void unloadFile(String name) {
        FileData fd = files.remove(name);
        fileLocations.remove(name);
    }

    static synchronized FileData file(String name) {
        if (files.containsKey(name)) return files.get(name);
        File f = fileLocations.containsKey(name) ? fileLocations.get(name) : new File(unityDir() + "/files/" + name + ".json");
        FileData fd = null;
        if (f.exists()) {
            String s = readText(f);
            if (s != null) fd = new FileData(name, Json.map(Json.parse(s)));
        }
        files.put(name, fd);
        return fd;
    }

    static synchronized HashMap<String, Object> meta() {
        if (meta == null) {
            String s = readText(new File(unityDir() + "/meta.json"));
            meta = s == null ? new HashMap<String, Object>() : Json.map(Json.parse(s));
            // script execution orders
            HashMap<String, Integer> orders = new HashMap<>();
            HashMap<String, Object> scripts = meta.containsKey("scripts") ? Json.map(meta.get("scripts")) : new HashMap<String, Object>();
            for (Object v : scripts.values()) {
                ArrayList<Object> e = Json.list(v);
                int o = Json.i(e.get(2));
                if (o != 0) orders.put(cs.System.Reflection.Assembly.javaName((String) e.get(1)), o);
            }
            Eng.setExecutionOrders(orders);
            applySettings();
        }
        return meta;
    }

    private static void applySettings() {
        HashMap<String, Object> tm = (HashMap<String, Object>) meta.get("TimeManager");
        if (tm != null) {
            Time.fixedDeltaTime = Json.f(tm.get("Fixed Timestep"));
            Time.maximumDeltaTime = Json.f(tm.get("Maximum Allowed Timestep"));
            Time.timeScale = Json.f(tm.get("m_TimeScale"));
        }
        HashMap<String, Object> tags = (HashMap<String, Object>) meta.get("TagManager");
        if (tags != null) {
            ArrayList<Object> t = Json.list(tags.get("tags"));
            customTags = new String[t.size()];
            for (int i = 0; i < customTags.length; i++) customTags[i] = (String) t.get(i);
            ArrayList<Object> l = Json.list(tags.get("layers"));
            for (int i = 0; i < 32 && i < l.size(); i++) LayerMask.names[i] = (String) l.get(i);
            ArrayList<Object> sl = Json.list(tags.get("m_SortingLayers"));
            SortingLayer.init(sl);
        }
        HashMap<String, Object> ph = (HashMap<String, Object>) meta.get("Physics2DSettings");
        if (ph != null) Physics2D.init(ph);
    }

    static String[] customTags = new String[0];
    private static final String[] BUILTIN_TAGS = {"Untagged", "Respawn", "Finish", "EditorOnly", "", "MainCamera", "Player", "GameController"};

    static String tagName(int t) {
        if (t >= 20000 && t - 20000 < customTags.length) return customTags[t - 20000];
        if (t >= 0 && t < BUILTIN_TAGS.length) return BUILTIN_TAGS[t];
        return "Untagged";
    }

    public static int buildSceneCount() { return Json.list(meta().get("scenes")).size(); }

    public static int sceneIndexOf(String nameOrPath) {
        ArrayList<Object> sc = Json.list(meta().get("scenes"));
        for (int i = 0; i < sc.size(); i++) {
            String p = (String) sc.get(i);
            String n = p.substring(p.lastIndexOf('/') + 1).replace(".unity", "");
            if (p.equals(nameOrPath) || n.equals(nameOrPath)) return i;
        }
        return -1;
    }

    // ------------------------------------------------------------------ load context
    public static final class Ctx {
        final FileData file;
        final HashMap<Long, UObject> local = new HashMap<>();
        final boolean asset;
        Ctx(FileData f, boolean asset) { file = f; this.asset = asset; }

        /** resolve a PPtr ([file, pathID] or null) */
        public UObject ref(Object pp) {
            String fn;
            long pid;
            if (pp instanceof HashMap) {
                HashMap<String, Object> m = Json.map(pp);
                if (!m.containsKey("m_PathID")) return null;
                pid = Json.lng(m.get("m_PathID"));
                if (pid == 0) return null;
                int fid = Json.i(m.get("m_FileID"));
                if (fid == 0) fn = file.name;
                else if (fid - 1 < file.externals.size()) fn = (String) file.externals.get(fid - 1);
                else return null;
            } else if (pp instanceof ArrayList) {
                ArrayList<Object> l = Json.list(pp);
                fn = (String) l.get(0);
                pid = Json.lng(l.get(1));
            } else return null;
            if (fn.equals(file.name)) {
                UObject o = local.get(pid);
                if (o != null) return o;
                if (!asset) {
                    // scene file object not created as part of the hierarchy (e.g. standalone ScriptableObject)
                    return Loader.asset(file, pid);
                }
            }
            FileData f = file(fn);
            return f == null ? missing(fn, pid) : Loader.asset(f, pid);
        }
    }

    private static final HashMap<String, UObject> missingCache = new HashMap<>();

    /** references into files we do not have (unity default resources, unity_builtin_extra) */
    static UObject missing(String fn, long pid) {
        return Builtins.get(fn, pid);
    }

    // ------------------------------------------------------------------ assets
    static synchronized UObject asset(FileData f, long pid) {
        UObject o = f.assets.get(pid);
        if (o != null || f.assets.containsKey(pid)) return o;
        String t = f.type(pid);
        if (t == null) { f.assets.put(pid, null); return null; }
        HashMap<String, Object> d = f.data(pid);
        if (isSceneObjectType(t, d)) {
            loadPrefab(f, pid);
            return f.assets.get(pid);
        }
        Ctx ctx = new Ctx(f, true);
        o = create(t, d, ctx);
        if (o == null) { f.assets.put(pid, null); return null; }
        o.$asset = true;
        o.$ref = f.name + "#" + pid;
        f.assets.put(pid, o);
        ctx.local.put(pid, o);
        fill(o, t, d, ctx);
        if (o instanceof ScriptableObject) awakeScriptable((ScriptableObject) o);
        return o;
    }

    private static boolean isSceneObjectType(String t, HashMap<String, Object> d) {
        if (t.equals("GameObject")) return true;
        return d != null && d.get("m_GameObject") instanceof ArrayList;
    }

    static void awakeScriptable(ScriptableObject o) {
        try {
            o.$Awake();
            o.$OnEnable();
        } catch (RuntimeException e) {
            Debug.logException(e, o);
        }
    }

    /** load a prefab hierarchy (all GameObjects/components under the root of pid) as assets */
    private static void loadPrefab(FileData f, long pid) {
        // find the GameObject
        long go = f.type(pid).equals("GameObject") ? pid : Json.lng(Json.list(f.data(pid).get("m_GameObject")).get(1));
        // walk up to the root
        long root = go;
        for (int guard = 0; guard < 1000; guard++) {
            Long tr = transformOf(f, root);
            if (tr == null) break;
            Object father = f.data(tr).get("m_Father");
            if (!(father instanceof ArrayList)) break;
            long fp = Json.lng(Json.list(father).get(1));
            Object g = f.data(fp).get("m_GameObject");
            root = Json.lng(Json.list(g).get(1));
        }
        Ctx ctx = new Ctx(f, true);
        ArrayList<GameObject> roots = new ArrayList<>();
        ArrayList<Object[]> fills = new ArrayList<>();
        GameObject r = buildTree(f, root, null, ctx, fills);
        if (r != null) roots.add(r);
        for (java.util.Map.Entry<Long, UObject> e : ctx.local.entrySet()) {
            e.getValue().$asset = true;
            e.getValue().$ref = f.name + "#" + e.getKey();
            f.assets.put(e.getKey(), e.getValue());
        }
        runFills(fills, ctx);
        if (r != null) markAsset(r.transform);
        if (!f.assets.containsKey(pid)) f.assets.put(pid, null);
    }

    private static void markAsset(Transform t) {
        t.gameObject.$asset = true;
        t.gameObject.$activeInHierarchy = false;
        for (Component c : t.gameObject.components) c.$asset = true;
        for (Transform c : t.children) markAsset(c);
    }

    private static Long transformOf(FileData f, long go) {
        HashMap<String, Object> d = f.data(go);
        if (d == null) return null;
        for (Object c : Json.list(d.get("m_Component"))) {
            Object pp = Json.map(c).get("component");
            if (!(pp instanceof ArrayList)) continue;
            long cp = Json.lng(Json.list(pp).get(1));
            String t = f.type(cp);
            if ("Transform".equals(t) || "RectTransform".equals(t)) return cp;
        }
        return null;
    }

    /** create a GameObject with its components and children (data filled later) */
    private static GameObject buildTree(FileData f, long goPid, Transform parent, Ctx ctx, ArrayList<Object[]> fills) {
        HashMap<String, Object> gd = f.data(goPid);
        if (gd == null) return null;
        GameObject go = new GameObject(true);
        go.m_Name = (String) gd.get("m_Name");
        go.m_Layer = Json.i(gd.get("m_Layer"));
        go.m_TagString = tagName(Json.i(gd.get("m_Tag")));
        go.m_IsActive = Json.b(gd.get("m_IsActive"));
        go.$asset = ctx.asset;
        ctx.local.put(goPid, go);
        Transform tr = null;
        long trPid = 0;
        for (Object c : Json.list(gd.get("m_Component"))) {
            Object pp = Json.map(c).get("component");
            if (!(pp instanceof ArrayList)) continue;
            long cp = Json.lng(Json.list(pp).get(1));
            String t = f.type(cp);
            if (t == null) continue;
            HashMap<String, Object> cd = f.data(cp);
            UObject o = create(t, cd, ctx);
            if (!(o instanceof Component)) continue;
            Component comp = (Component) o;
            comp.$asset = ctx.asset;
            if (comp instanceof Behaviour && cd != null && cd.containsKey("m_Enabled")) ((Behaviour) comp).m_Enabled = Json.b(cd.get("m_Enabled"));
            go.attach(comp);
            if (comp instanceof MonoBehaviour) Eng.prepareBehaviour((MonoBehaviour) comp);
            ctx.local.put(cp, comp);
            if (comp instanceof Transform) { tr = (Transform) comp; trPid = cp; }
            fills.add(new Object[]{comp, t, cd});
        }
        if (tr == null) {
            tr = new Transform();
            go.attach(tr);
        }
        if (parent != null) {
            tr.parent = parent;
            parent.children.add(tr);
        }
        for (Component c : go.components) c.$created();
        if (trPid != 0) {
            for (Object ch : Json.list(f.data(trPid).get("m_Children"))) {
                if (!(ch instanceof ArrayList)) continue;
                long chPid = Json.lng(Json.list(ch).get(1));
                HashMap<String, Object> chd = f.data(chPid);
                if (chd == null) continue;
                long chGo = Json.lng(Json.list(chd.get("m_GameObject")).get(1));
                buildTree(f, chGo, tr, ctx, fills);
            }
        }
        return go;
    }

    private static void runFills(ArrayList<Object[]> fills, Ctx ctx) {
        // transforms first so RectTransform layout is in place when behaviours read it
        for (Object[] x : fills) if (x[0] instanceof Transform) fill((UObject) x[0], (String) x[1], (HashMap<String, Object>) x[2], ctx);
        for (Object[] x : fills) if (!(x[0] instanceof Transform)) fill((UObject) x[0], (String) x[1], (HashMap<String, Object>) x[2], ctx);
        for (Object[] x : fills) if (x[0] instanceof RectTransform && ((RectTransform) x[0]).parent == null) ((RectTransform) x[0]).relayout();
    }

    /** create the object for an exported entry (no data yet) */
    static UObject create(String t, HashMap<String, Object> d, Ctx ctx) {
        if (t.equals("MonoBehaviour")) {
            String script = d == null ? null : (String) d.get("_script");
            if (script == null) return null;
            Class<?> c = scriptClass(script);
            if (c == null) return null;
            UObject o = (UObject) Eng.newComponent(c);
            if (d.get("m_Name") instanceof String) o.m_Name = (String) d.get("m_Name");
            return o;
        }
        if (t.equals("GameObject")) return null;
        Class<?> c = builtinClass(t);
        if (c == null) return null;
        try {
            UObject o = (UObject) c.getDeclaredConstructor().newInstance();
            if (d != null && d.get("m_Name") instanceof String) o.m_Name = (String) d.get("m_Name");
            return o;
        } catch (ReflectiveOperationException e) {
            Debug.LogWarning("cannot create builtin " + t + ": " + e);
            return null;
        }
    }

    private static final HashMap<String, Class<?>> scriptClasses = new HashMap<>();
    private static final HashMap<String, Class<?>> builtinClasses = new HashMap<>();
    private static final HashMap<String, Boolean> warned = new HashMap<>();

    static Class<?> scriptClass(String fullname) {
        if (scriptClasses.containsKey(fullname)) return scriptClasses.get(fullname);
        Class<?> c = null;
        String jn = Facades.scriptClass(fullname);
        if (jn.equals("-")) { scriptClasses.put(fullname, null); return null; }
        try {
            c = cs.rt.Rt.cls(jn);
        } catch (ClassNotFoundException e) {
            if (!warned.containsKey(fullname)) {
                warned.put(fullname, true);
                Debug.LogWarning("script class not found: " + fullname);
            }
        }
        scriptClasses.put(fullname, c);
        return c;
    }

    static Class<?> builtinClass(String t) {
        if (builtinClasses.containsKey(t)) return builtinClasses.get(t);
        if (t.equals("Unknown (893571522)")) return builtinClass("CustomCollider2D");   // class id missing in UnityPy
        Class<?> c = null;
        String[] pk = {"cs.UnityEngine.", "cs.UnityEngine.Rendering.", "cs.UnityEngine.Animations.", "cs.UnityEngine.Video.", "cs.UnityEngine.U2D."};
        for (String p : pk) {
            try {
                Class<?> k = cs.rt.Rt.cls(p + (t.equals("Object") ? "UObject" : t));
                if (UObject.class.isAssignableFrom(k) && !java.lang.reflect.Modifier.isAbstract(k.getModifiers())) { c = k; break; }
            } catch (ClassNotFoundException ignored) {}
        }
        if (c == null && !warned.containsKey("#" + t)) {
            warned.put("#" + t, true);
            Debug.LogWarning("unsupported builtin type: " + t);
        }
        builtinClasses.put(t, c);
        return c;
    }

    /** fill object data: built-ins via $load, scripts via $fmeta */
    static void fill(UObject o, String t, HashMap<String, Object> d, Ctx ctx) {
        if (d == null) return;
        try {
            if (o instanceof Component) ((Component) o).$load(d, ctx);
            else o.$loadAsset(d, ctx);
            if (t.equals("MonoBehaviour")) deserialize(o, o.getClass(), d, ctx);
        } catch (RuntimeException e) {
            Debug.LogError("load failed for " + t + " " + o.get_name() + ": " + e);
            Debug.logException(e, o);
        }
    }

    // ------------------------------------------------------------------ scenes
    /** instantiate scene `buildIndex` into the scene record; objects are created inactive-in-hierarchy */
    static ArrayList<GameObject> loadScene(int buildIndex, Eng.SceneData sd) {
        meta();
        FileData f = file("level" + buildIndex);
        ArrayList<GameObject> roots = new ArrayList<>();
        if (f == null) {
            Debug.LogError("scene data missing: level" + buildIndex);
            return roots;
        }
        Ctx ctx = new Ctx(f, false);
        ArrayList<Object[]> fills = new ArrayList<>();
        ArrayList<Long> pids = new ArrayList<>();
        for (String k : f.objects.keySet()) pids.add(Long.parseLong(k));
        java.util.Collections.sort(pids);
        for (long pid : pids) {
            if (!"GameObject".equals(f.type(pid))) continue;
            Long tr = transformOf(f, pid);
            if (tr != null && f.data(tr).get("m_Father") instanceof ArrayList) continue;
            GameObject g = buildTree(f, pid, null, ctx, fills);
            if (g != null) roots.add(g);
        }
        for (GameObject g : roots) setScene(g.transform, sd.handle);
        runFills(fills, ctx);
        // scene settings objects
        for (long pid : pids) {
            String t = f.type(pid);
            if ("RenderSettings".equals(t)) RenderSettings.load(f.data(pid), ctx);
        }
        for (GameObject g : roots) {
            sd.roots.add(g);
            clearAssetFlag(g.transform);
        }
        return roots;
    }

    private static void setScene(Transform t, int h) {
        t.gameObject.scene = h;
        for (Transform c : t.children) setScene(c, h);
    }

    private static void clearAssetFlag(Transform t) {
        t.gameObject.$asset = false;
        t.gameObject.$activeInHierarchy = false;
        for (Component c : t.gameObject.components) c.$asset = false;
        for (Transform c : t.children) clearAssetFlag(c);
    }

    // ------------------------------------------------------------------ Resources
    /** Resources path (lower case) -> all PPtrs with that path (several assets may share one path) */
    private static HashMap<String, ArrayList<Object>> resourceMap;

    private static synchronized HashMap<String, ArrayList<Object>> resources() {
        if (resourceMap != null) return resourceMap;
        resourceMap = new HashMap<>();
        FileData gg = file("globalgamemanagers");
        if (gg == null) return resourceMap;
        for (Object o : gg.objects.values()) {
            HashMap<String, Object> e = Json.map(o);
            if (!"ResourceManager".equals(e.get("t"))) continue;
            for (Object pair : l(Json.map(e.get("d")), "m_Container")) {
                ArrayList<Object> pr = Json.list(pair);
                String k = ((String) pr.get(0)).toLowerCase();
                ArrayList<Object> l = resourceMap.get(k);
                if (l == null) { l = new ArrayList<>(); resourceMap.put(k, l); }
                l.add(pr.get(1));
            }
        }
        return resourceMap;
    }

    static UObject resource(String path, cs.System.Type t) {
        ArrayList<Object> refs = resources().get(path.toLowerCase());
        if (refs == null) return null;
        Ctx ctx = new Ctx(file("globalgamemanagers"), true);
        UObject first = null;
        for (Object pp : refs) {
            UObject o = ctx.ref(pp);
            if (o == null) continue;
            if (t == null || t.cls.isInstance(o)) return o;
            if (o instanceof GameObject && Component.class.isAssignableFrom(t.cls)) {
                UObject c = ((GameObject) o).GetComponent(t);
                if (c != null) return c;
            }
            if (first == null) first = o;
        }
        return t == null ? first : null;
    }

    /** Resources.LoadAll: every asset of type t under the folder path */
    static java.util.ArrayList<UObject> resourcesUnder(String path, cs.System.Type t) {
        String p = path.toLowerCase();
        while (p.endsWith("/")) p = p.substring(0, p.length() - 1);
        java.util.ArrayList<UObject> r = new java.util.ArrayList<>();
        Ctx ctx = new Ctx(file("globalgamemanagers"), true);
        for (java.util.Map.Entry<String, ArrayList<Object>> e : resources().entrySet()) {
            String k = e.getKey();
            if (!(p.isEmpty() || k.equals(p) || k.startsWith(p + "/"))) continue;
            for (Object pp : e.getValue()) {
                UObject o = ctx.ref(pp);
                if (o == null) continue;
                if (t == null || t.cls.isInstance(o)) r.add(o);
                else if (o instanceof GameObject && Component.class.isAssignableFrom(t.cls)) {
                    UObject c = ((GameObject) o).GetComponent(t);
                    if (c != null) r.add(c);
                }
            }
        }
        return r;
    }

    /** Resources.GetBuiltinResource: built-in font / sprites */
    static Object builtinResource(cs.System.Type t, String path) {
        if (t.cls == Font.class) {
            Font f = new Font(path);
            f.$asset = true;
            return f;
        }
        if (t.cls == Sprite.class) return Builtins.uiSprite(path);
        if (t.cls == Shader.class) return Shader.Find(path);
        return null;
    }

    static UObject findAsset(cs.System.Type t) {
        for (FileData f : files.values()) {
            if (f == null) continue;
            for (UObject o : f.assets.values()) if (o != null && !o.$destroyed && t.cls.isInstance(o)) return o;
        }
        return null;
    }

    // ------------------------------------------------------------------ serialized fields
    private static final HashMap<Class<?>, Object[]> fieldCache = new HashMap<>();

    /** [java.lang.reflect.Field, csName, typeStr] for serialized fields of exactly class c */
    private static Object[] serializedFields(Class<?> c) {
        Object[] r = fieldCache.get(c);
        if (r != null) return r;
        ArrayList<Object> l = new ArrayList<>();
        try {
            java.lang.reflect.Field mf = c.getDeclaredField("$fmeta");
            Object[] fm = (Object[]) mf.get(null);
            for (int i = 0; i + 3 < fm.length; i += 4) {
                int flags = (Integer) fm[i + 3];
                if ((flags & 8) == 0 || (flags & 1) != 0) continue;
                try {
                    java.lang.reflect.Field jf = c.getDeclaredField((String) fm[i]);
                    jf.setAccessible(true);
                    l.add(new Object[]{jf, fm[i + 1], fm[i + 2]});
                } catch (NoSuchFieldException ignored) {}
            }
        } catch (NoSuchFieldException ignored) {
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        r = l.toArray();
        fieldCache.put(c, r);
        return r;
    }

    /** Unity-deserialize d into obj for the class chain from c up */
    static void deserialize(Object obj, Class<?> c, HashMap<String, Object> d, Ctx ctx) {
        for (Class<?> k = c; k != null && k != Object.class && k != MonoBehaviour.class && k != ScriptableObject.class; k = k.getSuperclass()) {
            for (Object x : serializedFields(k)) {
                Object[] e = (Object[]) x;
                String name = (String) e[1];
                if (!d.containsKey(name)) continue;
                java.lang.reflect.Field jf = (java.lang.reflect.Field) e[0];
                try {
                    Object cur = jf.get(obj);
                    Object v = convert(d.get(name), (String) e[2], jf.getType(), cur, ctx);
                    if (v != NOVALUE) jf.set(obj, v);
                } catch (IllegalAccessException ex) {
                    throw new RuntimeException(ex);
                } catch (RuntimeException ex) {
                    Debug.LogWarning("field " + k.getName() + "." + name + ": " + ex);
                }
            }
        }
        if (obj instanceof cs.UnityEngine.ISerializationCallbackReceiver) {
            ((cs.UnityEngine.ISerializationCallbackReceiver) obj).OnAfterDeserialize();
        }
    }

    static final Object NOVALUE = new Object();

    /** convert exported JSON value v of .NET type `ts` to a Java value assignable to `jt` */
    static Object convert(Object v, String ts, Class<?> jt, Object cur, Ctx ctx) {
        if (jt.isPrimitive()) {
            if (jt == boolean.class) return Json.b(v);
            if (jt == int.class) return (int) Json.lng(v);
            if (jt == float.class) return Json.f(v);
            if (jt == long.class) return Json.lng(v);
            if (jt == double.class) return Json.num(v);
            if (jt == char.class) return (char) Json.lng(v);
            if (jt == short.class) return (short) Json.lng(v);
            if (jt == byte.class) return (byte) Json.lng(v);
        }
        if (jt == String.class) return v instanceof String ? v : (v == null ? "" : v.toString());
        if (UObject.class.isAssignableFrom(jt) || (jt == Object.class && v instanceof ArrayList && isPPtr((ArrayList<Object>) v))) {
            UObject o = ctx == null ? null : ctx.ref(v);
            if (o != null && !jt.isInstance(o)) {
                if (o instanceof GameObject && Component.class.isAssignableFrom(jt)) return ((GameObject) o).GetComponent(cs.System.Type.of(jt));
                return null;
            }
            return o;
        }
        if (jt.isArray()) {
            if (!(v instanceof ArrayList)) return java.lang.reflect.Array.newInstance(jt.getComponentType(), 0);
            ArrayList<Object> l = Json.list(v);
            String ets = ts.endsWith("[]") ? ts.substring(0, ts.length() - 2) : "System.Object";
            Class<?> et = jt.getComponentType();
            Object arr = java.lang.reflect.Array.newInstance(et, l.size());
            for (int i = 0; i < l.size(); i++) {
                Object e = convert(l.get(i), ets, et, null, ctx);
                if (e != NOVALUE) java.lang.reflect.Array.set(arr, i, e);
            }
            return arr;
        }
        if (cs.System.Collections.Generic.List_1.class.isAssignableFrom(jt)) {
            cs.System.Collections.Generic.List_1 list = new cs.System.Collections.Generic.List_1();
            if (v instanceof ArrayList) {
                String ets = genericArg(ts);
                cs.System.Type et = cs.System.Reflection.Assembly.resolveType(ets);
                Class<?> ec = elementJavaClass(et);
                for (Object e : Json.list(v)) {
                    Object x = convert(e, ets, ec, null, ctx);
                    if (x == NOVALUE) x = null;
                    if (et != null && et.prim != null && x != null) x = box(x, et);
                    list.Add(x);
                }
            }
            return list;
        }
        // Unity value types and serializable classes
        Object o = Builtins.valueType(jt, v, ctx);
        if (o != NOVALUE) return o;
        if (!(v instanceof HashMap)) return NOVALUE;
        HashMap<String, Object> m = Json.map(v);
        Object inst = cur;
        if (inst == null || inst.getClass() != jt && !jt.isInstance(inst)) {
            cs.System.Type t = cs.System.Reflection.Assembly.resolveType(ts);
            Class<?> cls = t != null && jt.isAssignableFrom(t.cls) ? t.cls : jt;
            if (java.lang.reflect.Modifier.isAbstract(cls.getModifiers()) || cls.isInterface()) return NOVALUE;
            inst = instantiate(cls);
            if (inst == null) return NOVALUE;
        }
        if (inst instanceof cs.UnityEngine.Events.UnityEventBase) {
            ((cs.UnityEngine.Events.UnityEventBase) inst).$loadPersistent(m.get("m_PersistentCalls"), ctx);
        }
        deserialize(inst, inst.getClass(), m, ctx);
        return inst;
    }

    private static boolean isPPtr(ArrayList<Object> l) {
        return l.size() == 2 && l.get(0) instanceof String && l.get(1) instanceof Long;
    }

    private static Object box(Object x, cs.System.Type et) {
        return x;
    }

    static Class<?> elementJavaClass(cs.System.Type t) {
        if (t == null) return Object.class;
        if (t.prim != null) return t.prim;
        return t.cls;
    }

    static String genericArg(String ts) {
        int lt = ts.indexOf('<');
        return lt < 0 ? "System.Object" : ts.substring(lt + 1, ts.length() - 1);
    }

    static Object instantiate(Class<?> c) {
        try {
            Object o = c.getDeclaredConstructor().newInstance();
            cs.System.Type.construct(o, c);
            return o;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ Instantiate support
    /** copy Unity-serialized fields s -> d (deep for serializable classes, refs remapped) */
    static void copySerialized(Object s, Object d, Eng.CloneCtx ctx) {
        for (Class<?> k = s.getClass(); k != null && k != Object.class && k != MonoBehaviour.class && k != ScriptableObject.class; k = k.getSuperclass()) {
            for (Object x : serializedFields(k)) {
                java.lang.reflect.Field jf = (java.lang.reflect.Field) ((Object[]) x)[0];
                try {
                    jf.set(d, cloneValue(jf.get(s), jf.get(d), ctx));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        if (d instanceof cs.UnityEngine.ISerializationCallbackReceiver) ((cs.UnityEngine.ISerializationCallbackReceiver) d).OnAfterDeserialize();
    }

    static Object cloneValue(Object v, Object cur, Eng.CloneCtx ctx) {
        if (v == null || v instanceof String || v instanceof Number || v instanceof Boolean || v instanceof Character) return v;
        if (v instanceof UObject) return ctx.remap(v);
        if (v instanceof cs.rt.Struct) {
            Object c = ((cs.rt.Struct) v).$copy();
            if (c != null && !(c.getClass().getName().startsWith("cs.UnityEngine.") && c.getClass().getName().indexOf('.', 15) < 0)) {
                copySerialized(v, c, ctx);
            }
            return c;
        }
        if (v.getClass().isArray()) {
            int n = java.lang.reflect.Array.getLength(v);
            Object a = java.lang.reflect.Array.newInstance(v.getClass().getComponentType(), n);
            for (int i = 0; i < n; i++) java.lang.reflect.Array.set(a, i, cloneValue(java.lang.reflect.Array.get(v, i), null, ctx));
            return a;
        }
        if (v instanceof cs.System.Collections.Generic.List_1) {
            cs.System.Collections.Generic.List_1 s = (cs.System.Collections.Generic.List_1) v, l = new cs.System.Collections.Generic.List_1();
            for (int i = 0; i < s.get_Count(); i++) l.Add(cloneValue(s.get_Item(i), null, ctx));
            return l;
        }
        if (v instanceof cs.UnityEngine.Events.UnityEventBase) {
            cs.UnityEngine.Events.UnityEventBase e = (cs.UnityEngine.Events.UnityEventBase) instantiate(v.getClass());
            if (e == null) return v;
            e.$clonePersistent((cs.UnityEngine.Events.UnityEventBase) v, ctx);
            copySerialized(v, e, ctx);
            return e;
        }
        Object b = Builtins.cloneValueType(v);
        if (b != null) return b;
        if (isSerializableClass(v.getClass())) {
            Object c = instantiate(v.getClass());
            if (c == null) return v;
            copySerialized(v, c, ctx);
            return c;
        }
        return v;
    }

    private static boolean isSerializableClass(Class<?> c) {
        try {
            c.getDeclaredField("$fmeta");
            return c.getName().startsWith("cs.") && !c.getName().startsWith("cs.System.");
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    static UObject cloneScriptable(ScriptableObject o) {
        ScriptableObject c = (ScriptableObject) instantiate(o.getClass());
        c.m_Name = o.m_Name + "(Clone)";
        Eng.CloneCtx ctx = new Eng.CloneCtx();
        copySerialized(o, c, ctx);
        awakeScriptable(c);
        return c;
    }

    static UObject cloneAsset(UObject o) {
        UObject c = o.$cloneAsset();
        if (c == null) throw new cs.System.NotSupportedException("Instantiate of " + o.getClass().getName());
        c.m_Name = o.m_Name + "(Clone)";
        return c;
    }

    // ------------------------------------------------------------------ value helpers for built-ins
    public static void vec2(Object o, Vector2 v) {
        if (!(o instanceof HashMap)) return;
        HashMap<String, Object> m = Json.map(o);
        v.x = Json.f(m.get("x"));
        v.y = Json.f(m.get("y"));
    }
    public static void vec3(Object o, Vector3 v) {
        if (!(o instanceof HashMap)) return;
        HashMap<String, Object> m = Json.map(o);
        v.x = Json.f(m.get("x"));
        v.y = Json.f(m.get("y"));
        v.z = Json.f(m.get("z"));
    }
    public static void vec4(Object o, Vector4 v) {
        if (!(o instanceof HashMap)) return;
        HashMap<String, Object> m = Json.map(o);
        v.x = Json.f(m.get("x"));
        v.y = Json.f(m.get("y"));
        v.z = Json.f(m.get("z"));
        v.w = Json.f(m.get("w"));
    }
    public static void quat(Object o, Quaternion q) {
        if (!(o instanceof HashMap)) return;
        HashMap<String, Object> m = Json.map(o);
        q.x = Json.f(m.get("x"));
        q.y = Json.f(m.get("y"));
        q.z = Json.f(m.get("z"));
        q.w = Json.f(m.get("w"));
    }
    public static void color(Object o, Color c) {
        if (!(o instanceof HashMap)) return;
        HashMap<String, Object> m = Json.map(o);
        if (m.containsKey("rgba")) {
            long v = Json.lng(m.get("rgba"));
            c.r = (v & 0xff) / 255f;
            c.g = ((v >> 8) & 0xff) / 255f;
            c.b = ((v >> 16) & 0xff) / 255f;
            c.a = ((v >> 24) & 0xff) / 255f;
            return;
        }
        c.r = Json.f(m.get("r"));
        c.g = Json.f(m.get("g"));
        c.b = Json.f(m.get("b"));
        c.a = Json.f(m.get("a"));
    }
    public static void rect(Object o, Rect r) {
        if (!(o instanceof HashMap)) return;
        HashMap<String, Object> m = Json.map(o);
        r.m_XMin = Json.f(m.get("x"));
        r.m_YMin = Json.f(m.get("y"));
        r.m_Width = Json.f(m.get("width"));
        r.m_Height = Json.f(m.get("height"));
    }
    public static float f(HashMap<String, Object> d, String k, float def) { return d.containsKey(k) ? Json.f(d.get(k)) : def; }
    public static int i(HashMap<String, Object> d, String k, int def) { return d.containsKey(k) ? Json.i(d.get(k)) : def; }
    public static boolean b(HashMap<String, Object> d, String k, boolean def) { return d.containsKey(k) ? Json.b(d.get(k)) : def; }
    public static String s(HashMap<String, Object> d, String k) { Object o = d.get(k); return o instanceof String ? (String) o : ""; }
    public static HashMap<String, Object> m(HashMap<String, Object> d, String k) { Object o = d.get(k); return o instanceof HashMap ? Json.map(o) : new HashMap<String, Object>(); }
    public static ArrayList<Object> l(HashMap<String, Object> d, String k) { Object o = d.get(k); return o instanceof ArrayList ? Json.list(o) : new ArrayList<Object>(); }
}
