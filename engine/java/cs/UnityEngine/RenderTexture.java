package cs.UnityEngine;

public class RenderTexture extends Texture {
    static RenderTexture active;
    int depthBits, format;
    public RenderTexture() {}
    public RenderTexture(int w, int h, int depth) { width = w; height = h; depthBits = depth; }
    public RenderTexture(int w, int h, int depth, int fmt) { this(w, h, depth); format = fmt; }
    public RenderTexture(int w, int h, int depth, int fmt, int rw) { this(w, h, depth); format = fmt; }
    public static RenderTexture get_active() { return active; }
    public static void set_active(RenderTexture t) { active = t; }
    public static RenderTexture GetTemporary(int w, int h) { return new RenderTexture(w, h, 0); }
    public static RenderTexture GetTemporary(int w, int h, int d) { return new RenderTexture(w, h, d); }
    public static void ReleaseTemporary(RenderTexture t) {}
    public boolean Create() { return true; }
    public void Release() {}
    public boolean IsCreated() { return true; }
}
