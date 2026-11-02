package cs.UnityEngine;

/**
 * Rendering backend (GLES on Android, software rasterizer on desktop).
 * Coordinates: draw items carry a full model-view-projection matrix (column-major) mapping vertex positions to clip space.
 */
public abstract class Gfx {
    /** one draw: triangles with per-vertex position (xyz), color (ARGB packed), uv0 (4 floats) */
    public static final class Item {
        public float[] mvp = new float[16];
        public float[] pos;         // 3 per vertex
        public int[] col;           // ARGB per vertex (null = white)
        public float[] uv0, uv1;    // 4 per vertex (null = 0)
        public int[] idx;           // triangle list
        public int count, idxCount;
        public Material mat;
        public Texture tex;         // main texture (null = white)
        public MaterialPropertyBlock block;
        public final float[] tint = {1, 1, 1, 1};   // multiplied with vertex color (renderer color, canvas alpha)
        public boolean scissor;
        public int sx, sy, sw, sh;  // scissor rect in target pixels (origin bottom-left)
        public String shader;       // resolved shader name
        public int stencilRef, stencilComp = 8, stencilOp, stencilRead = 255, stencilWrite = 255, colorMask = 15;
        public boolean world;       // world (sprite) vs UI
        public float[] model = new float[16];   // object-to-world (for shaders needing world positions)
    }

    /** start a frame for a target of w x h pixels */
    public abstract void begin(int w, int h);
    public abstract void viewport(int x, int y, int w, int h);
    public abstract void clear(boolean color, boolean depth, float r, float g, float b, float a);
    public abstract void draw(Item it);
    public abstract void end();
    /** release backend resources of a texture */
    public void release(Texture t) {}
    /** read back pixels (ARGB, row 0 = bottom) */
    public int[] readPixels(int x, int y, int w, int h) { return null; }

    static Gfx current = new Gfx() {
        public void begin(int w, int h) {}
        public void viewport(int x, int y, int w, int h) {}
        public void clear(boolean c, boolean d, float r, float g, float b, float a) {}
        public void draw(Item it) {}
        public void end() {}
    };
    public static void set(Gfx g) { current = g; }
    public static Gfx get() { return current; }
}
