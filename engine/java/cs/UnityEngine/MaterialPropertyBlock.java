package cs.UnityEngine;

import java.util.HashMap;

/** Property storage shared by Material, MaterialPropertyBlock and shader globals (keyed by property id). */
public class MaterialPropertyBlock {
    final HashMap<Integer, Float> floats = new HashMap<>();
    final HashMap<Integer, float[]> vectors = new HashMap<>();
    final HashMap<Integer, Texture> textures = new HashMap<>();
    final HashMap<Integer, float[]> texST = new HashMap<>();
    final HashMap<Integer, float[]> arrays = new HashMap<>();
    final HashMap<Integer, float[]> matrices = new HashMap<>();
    public int $version;

    public MaterialPropertyBlock() {}
    public boolean get_isEmpty() { return floats.isEmpty() && vectors.isEmpty() && textures.isEmpty() && arrays.isEmpty() && matrices.isEmpty(); }
    public void Clear() { floats.clear(); vectors.clear(); textures.clear(); texST.clear(); arrays.clear(); matrices.clear(); $version++; }

    static int id(String n) { return Shader.PropertyToID(n); }
    public void SetFloat(String n, float v) { SetFloat(id(n), v); }
    public void SetFloat(int i, float v) { floats.put(i, v); $version++; }
    public void SetInt(String n, int v) { SetFloat(id(n), v); }
    public void SetInt(int i, int v) { SetFloat(i, v); }
    public void SetInteger(String n, int v) { SetFloat(id(n), v); }
    public void SetColor(String n, Color c) { SetColor(id(n), c); }
    public void SetColor(int i, Color c) { vectors.put(i, new float[]{c.r, c.g, c.b, c.a}); $version++; }
    public void SetVector(String n, Vector4 v) { SetVector(id(n), v); }
    public void SetVector(int i, Vector4 v) { vectors.put(i, new float[]{v.x, v.y, v.z, v.w}); $version++; }
    public void SetTexture(String n, Texture t) { SetTexture(id(n), t); }
    public void SetTexture(int i, Texture t) { textures.put(i, t); $version++; }
    public void SetFloatArray(String n, float[] a) { SetFloatArray(id(n), a); }
    public void SetFloatArray(int i, float[] a) { arrays.put(i, a.clone()); $version++; }
    public void SetMatrix(String n, Matrix4x4 m) { SetMatrix(id(n), m); }
    public void SetMatrix(int i, Matrix4x4 m) { matrices.put(i, m.m.clone()); $version++; }
    public float GetFloat(String n) { return GetFloat(id(n)); }
    public float GetFloat(int i) { Float f = floats.get(i); return f == null ? 0 : f; }
    public int GetInt(String n) { return (int) GetFloat(n); }
    public Color GetColor(String n) { return GetColor(id(n)); }
    public Color GetColor(int i) { float[] v = vectors.get(i); return v == null ? new Color() : new Color(v[0], v[1], v[2], v[3]); }
    public Vector4 GetVector(String n) { return GetVector(id(n)); }
    public Vector4 GetVector(int i) { float[] v = vectors.get(i); return v == null ? new Vector4() : new Vector4(v[0], v[1], v[2], v[3]); }
    public Texture GetTexture(String n) { return textures.get(id(n)); }
    public Texture GetTexture(int i) { return textures.get(i); }
    public boolean HasFloat(String n) { return floats.containsKey(id(n)); }
    public boolean HasProperty(String n) { int i = id(n); return floats.containsKey(i) || vectors.containsKey(i) || textures.containsKey(i); }

    void copyFrom(MaterialPropertyBlock o) {
        Clear();
        floats.putAll(o.floats);
        for (java.util.Map.Entry<Integer, float[]> e : o.vectors.entrySet()) vectors.put(e.getKey(), e.getValue().clone());
        textures.putAll(o.textures);
        for (java.util.Map.Entry<Integer, float[]> e : o.texST.entrySet()) texST.put(e.getKey(), e.getValue().clone());
        for (java.util.Map.Entry<Integer, float[]> e : o.arrays.entrySet()) arrays.put(e.getKey(), e.getValue().clone());
        for (java.util.Map.Entry<Integer, float[]> e : o.matrices.entrySet()) matrices.put(e.getKey(), e.getValue().clone());
    }
    /** read a float through: this block, then fallback */
    public float $float(int i, float def) { Float f = floats.get(i); return f == null ? def : f; }
    public float[] $vector(int i) { return vectors.get(i); }
    public Texture $texture(int i) { return textures.get(i); }
}
