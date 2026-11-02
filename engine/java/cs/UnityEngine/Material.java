package cs.UnityEngine;

import cs.rt.Json;
import java.util.ArrayList;
import java.util.HashMap;

public class Material extends UObject {
    Shader shader;
    final MaterialPropertyBlock props = new MaterialPropertyBlock();
    final java.util.LinkedHashSet<String> keywords = new java.util.LinkedHashSet<>();
    int renderQueue = -1;
    public boolean enableInstancing;

    public Material() {}
    public Material(Shader s) { shader = s; m_Name = ""; }
    public Material(Material m) { shader = m.shader; m_Name = m.m_Name; CopyPropertiesFromMaterial(m); }
    public Material(String contents) { shader = Shader.Find("Unlit/Texture"); }

    public Shader get_shader() { return shader; }
    public void set_shader(Shader s) { shader = s; props.$version++; }
    public int get_renderQueue() { return renderQueue >= 0 ? renderQueue : (shader == null ? 3000 : shader.renderQueue); }
    public void set_renderQueue(int q) { renderQueue = q; }
    public int get_passCount() { return 1; }
    public String[] get_shaderKeywords() { return keywords.toArray(new String[0]); }
    public void set_shaderKeywords(String[] k) { keywords.clear(); if (k != null) for (String s : k) keywords.add(s); props.$version++; }
    public void EnableKeyword(String k) { keywords.add(k); props.$version++; }
    public void DisableKeyword(String k) { keywords.remove(k); props.$version++; }
    public boolean IsKeywordEnabled(String k) { return keywords.contains(k); }
    public int ComputeCRC() {
        int h = shader == null ? 0 : shader.m_Name.hashCode();
        h = h * 31 + props.floats.hashCode();
        for (java.util.Map.Entry<Integer, float[]> e : props.vectors.entrySet()) h = h * 31 + e.getKey() + java.util.Arrays.hashCode(e.getValue());
        for (java.util.Map.Entry<Integer, Texture> e : props.textures.entrySet()) h = h * 31 + e.getKey() + System.identityHashCode(e.getValue());
        return h * 31 + keywords.hashCode();
    }
    public void CopyPropertiesFromMaterial(Material m) {
        props.copyFrom(m.props);
        keywords.clear();
        keywords.addAll(m.keywords);
        renderQueue = m.renderQueue;
    }

    public boolean HasProperty(String n) { return HasProperty(Shader.PropertyToID(n)); }
    public boolean HasProperty(int id) {
        if (props.floats.containsKey(id) || props.vectors.containsKey(id) || props.textures.containsKey(id)) return true;
        String n = Shader.$nameOf(id);
        return shader != null && n != null && shader.hasProp(n);
    }
    public boolean HasFloat(String n) { return HasProperty(n); }
    public boolean HasColor(String n) { return HasProperty(n); }
    public boolean HasTexture(String n) { return HasProperty(n); }
    public float GetFloat(String n) { return props.GetFloat(n); }
    public float GetFloat(int i) { return props.GetFloat(i); }
    public int GetInt(String n) { return (int) props.GetFloat(n); }
    public int GetInt(int i) { return (int) props.GetFloat(i); }
    public Color GetColor(String n) { return props.GetColor(n); }
    public Color GetColor(int i) { return props.GetColor(i); }
    public Vector4 GetVector(String n) { return props.GetVector(n); }
    public Vector4 GetVector(int i) { return props.GetVector(i); }
    public Texture GetTexture(String n) { return props.GetTexture(n); }
    public Texture GetTexture(int i) { return props.GetTexture(i); }
    public void SetFloat(String n, float v) { props.SetFloat(n, v); }
    public void SetFloat(int i, float v) { props.SetFloat(i, v); }
    public void SetInt(String n, int v) { props.SetFloat(n, v); }
    public void SetInt(int i, int v) { props.SetFloat(i, v); }
    public void SetInteger(String n, int v) { props.SetFloat(n, v); }
    public void SetColor(String n, Color c) { props.SetColor(n, c); }
    public void SetColor(int i, Color c) { props.SetColor(i, c); }
    public void SetVector(String n, Vector4 v) { props.SetVector(n, v); }
    public void SetVector(int i, Vector4 v) { props.SetVector(i, v); }
    public void SetTexture(String n, Texture t) { props.SetTexture(n, t); }
    public void SetTexture(int i, Texture t) { props.SetTexture(i, t); }
    public void SetMatrix(int i, Matrix4x4 m) { props.SetMatrix(i, m); }
    public void SetMatrix(String n, Matrix4x4 m) { props.SetMatrix(n, m); }
    public void SetFloatArray(String n, float[] a) { props.SetFloatArray(n, a); }
    public Vector2 GetTextureOffset(String n) { float[] st = props.texST.get(Shader.PropertyToID(n)); return st == null ? new Vector2() : new Vector2(st[2], st[3]); }
    public Vector2 GetTextureScale(String n) { float[] st = props.texST.get(Shader.PropertyToID(n)); return st == null ? new Vector2(1, 1) : new Vector2(st[0], st[1]); }
    public void SetTextureOffset(String n, Vector2 o) { float[] st = st(n); st[2] = o.x; st[3] = o.y; props.$version++; }
    public void SetTextureScale(String n, Vector2 s) { float[] st = st(n); st[0] = s.x; st[1] = s.y; props.$version++; }
    private float[] st(String n) {
        int id = Shader.PropertyToID(n);
        float[] st = props.texST.get(id);
        if (st == null) { st = new float[]{1, 1, 0, 0}; props.texST.put(id, st); }
        return st;
    }
    public Texture get_mainTexture() { return props.GetTexture("_MainTex"); }
    public void set_mainTexture(Texture t) { props.SetTexture("_MainTex", t); }
    public Color get_color() { return props.GetColor("_Color"); }
    public void set_color(Color c) { props.SetColor("_Color", c); }
    public Vector2 get_mainTextureOffset() { return GetTextureOffset("_MainTex"); }
    public void set_mainTextureOffset(Vector2 o) { SetTextureOffset("_MainTex", o); }
    public Vector2 get_mainTextureScale() { return GetTextureScale("_MainTex"); }
    public void set_mainTextureScale(Vector2 s) { SetTextureScale("_MainTex", s); }
    public MaterialPropertyBlock $props() { return props; }
    public java.util.Set<String> $keywords() { return keywords; }

    @Override public UObject $cloneAsset() { return new Material(this); }

    @Override public void $loadAsset(HashMap<String, Object> d, Loader.Ctx ctx) {
        UObject s = ctx.ref(d.get("m_Shader"));
        shader = s instanceof Shader ? (Shader) s : Shader.Find("Sprites/Default");
        for (Object k : Loader.l(d, "m_ValidKeywords")) keywords.add((String) k);
        Object sk = d.get("m_ShaderKeywords");
        if (sk instanceof String) for (String k : ((String) sk).split(" ")) if (!k.isEmpty()) keywords.add(k);
        renderQueue = Loader.i(d, "m_CustomRenderQueue", -1);
        HashMap<String, Object> sp = Loader.m(d, "m_SavedProperties");
        for (Object e : Loader.l(sp, "m_TexEnvs")) {
            ArrayList<Object> p = Json.list(e);
            HashMap<String, Object> v = Json.map(p.get(1));
            int id = Shader.PropertyToID((String) p.get(0));
            UObject t = ctx.ref(v.get("m_Texture"));
            if (t instanceof Texture) props.textures.put(id, (Texture) t);
            Vector2 sc = new Vector2(1, 1), of = new Vector2();
            Loader.vec2(v.get("m_Scale"), sc);
            Loader.vec2(v.get("m_Offset"), of);
            props.texST.put(id, new float[]{sc.x, sc.y, of.x, of.y});
        }
        for (Object e : Loader.l(sp, "m_Floats")) {
            ArrayList<Object> p = Json.list(e);
            props.floats.put(Shader.PropertyToID((String) p.get(0)), Json.f(p.get(1)));
        }
        for (Object e : Loader.l(sp, "m_Ints")) {
            ArrayList<Object> p = Json.list(e);
            props.floats.put(Shader.PropertyToID((String) p.get(0)), (float) Json.i(p.get(1)));
        }
        for (Object e : Loader.l(sp, "m_Colors")) {
            ArrayList<Object> p = Json.list(e);
            Color c = new Color();
            Loader.color(p.get(1), c);
            props.vectors.put(Shader.PropertyToID((String) p.get(0)), new float[]{c.r, c.g, c.b, c.a});
        }
    }
}
