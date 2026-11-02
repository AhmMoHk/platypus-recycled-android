package cs.Blackdrop.Platforms;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;

/** Persistent key/value store (JSON file in the save folder). Values: Long (int), Double (float), String. */
public class PlatformStore {
    final File file;
    final LinkedHashMap<String, Object> data = new LinkedHashMap<>();
    public ActionWrapper SaveToDiskPerformed = new ActionWrapper();
    boolean dirty;

    public PlatformStore() { this(new File(cs.UnityEngine.Host.get().persistentDataPath(), "store.json")); }
    PlatformStore(File f) {
        file = f;
        if (f.exists()) {
            try {
                byte[] b = new byte[(int) f.length()];
                java.io.FileInputStream in = new java.io.FileInputStream(f);
                try { int off = 0, n; while (off < b.length && (n = in.read(b, off, b.length - off)) > 0) off += n; } finally { in.close(); }
                Object o = cs.rt.Json.parse(new String(b, "UTF-8"));
                if (o instanceof HashMap) data.putAll(cs.rt.Json.map(o));
            } catch (Exception e) {
                cs.UnityEngine.Debug.LogWarning("save store unreadable: " + e);
            }
        }
    }
    public synchronized boolean HasKey(String k) { return data.containsKey(k); }
    public synchronized void DeleteKey(String k) { data.remove(k); dirty = true; }
    public synchronized void DeleteAll() { data.clear(); dirty = true; }
    public synchronized int GetInt(String k, int def) { Object v = data.get(k); return v instanceof Number ? ((Number) v).intValue() : def; }
    public int GetInt(String k) { return GetInt(k, 0); }
    public synchronized float GetFloat(String k, float def) { Object v = data.get(k); return v instanceof Number ? ((Number) v).floatValue() : def; }
    public float GetFloat(String k) { return GetFloat(k, 0); }
    public synchronized String GetString(String k, String def) { Object v = data.get(k); return v instanceof String ? (String) v : def; }
    public String GetString(String k) { return GetString(k, ""); }
    public synchronized void SetInt(String k, int v) { data.put(k, (long) v); dirty = true; }
    public synchronized void SetFloat(String k, float v) { data.put(k, (double) v); dirty = true; }
    public synchronized void SetString(String k, String v) { data.put(k, v); dirty = true; }
    public void Save() {
        String s;
        synchronized (this) { s = cs.rt.Json.write(data); dirty = false; }
        try {
            file.getParentFile().mkdirs();
            File tmp = new File(file.getPath() + ".tmp");
            java.io.FileOutputStream out = new java.io.FileOutputStream(tmp);
            try { out.write(s.getBytes("UTF-8")); out.getFD().sync(); } finally { out.close(); }
            if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file); }
        } catch (java.io.IOException e) {
            cs.UnityEngine.Debug.LogError("save failed: " + e);
        }
        if (SaveToDiskPerformed != null) SaveToDiskPerformed.Invoke();
    }
}
