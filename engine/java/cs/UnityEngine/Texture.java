package cs.UnityEngine;

/** FilterMode: Point 0, Bilinear 1, Trilinear 2. TextureWrapMode: Repeat 0, Clamp 1, Mirror 2, MirrorOnce 3 */
public class Texture extends UObject {
    int width, height, filterMode = 1, wrapModeU = 1, wrapModeV = 1, mipCount = 1, anisoLevel = 1;
    float mipMapBias;
    /** backend handle and the version it was created for */
    public Object $gpu;
    public int $version, $gpuVersion = -1;

    public int get_width() { return width; }
    public void set_width(int w) { width = w; }
    public int get_height() { return height; }
    public void set_height(int h) { height = h; }
    public int get_filterMode() { return filterMode; }
    public void set_filterMode(int m) { filterMode = m; $version++; }
    public int get_wrapMode() { return wrapModeU; }
    public void set_wrapMode(int m) { wrapModeU = wrapModeV = m; $version++; }
    public int get_wrapModeU() { return wrapModeU; }
    public void set_wrapModeU(int m) { wrapModeU = m; $version++; }
    public int get_wrapModeV() { return wrapModeV; }
    public void set_wrapModeV(int m) { wrapModeV = m; $version++; }
    public int get_anisoLevel() { return anisoLevel; }
    public void set_anisoLevel(int a) { anisoLevel = a; }
    public float get_mipMapBias() { return mipMapBias; }
    public void set_mipMapBias(float b) { mipMapBias = b; }
    public int get_mipmapCount() { return mipCount; }
    public boolean get_isReadable() { return false; }
    public boolean get_isDataSRGB() { return true; }
    public Vector2 get_texelSize() { return new Vector2(width == 0 ? 0 : 1f / width, height == 0 ? 0 : 1f / height); }
    public int get_dimension() { return 2; }
    public long GetNativeTexturePtr() { return 0; }
    @Override public void $onDestroyAsset() { Render.releaseTexture(this); }
}
