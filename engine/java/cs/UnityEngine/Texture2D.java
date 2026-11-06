package cs.UnityEngine;

/**
 * Texture2D. CPU pixels are ARGB ints, row 0 = bottom (Unity order). Pixels come from a PNG in the data folder
 * (loaded lazily), from a converted GPU-compressed blob ($compressed, uploaded as-is), or are set by script.
 * TextureFormat: Alpha8 1, RGB24 3, RGBA32 4, ARGB32 5, DXT5 12, BGRA32 14, ETC2_RGBA8 47, ASTC_4x4 48, ASTC_6x6 50
 */
public class Texture2D extends Texture {
    int format = 4;
    boolean readable = true, linear;
    int[] px;
    /** lazily loaded image source (path relative to the data root) */
    public String $src;
    /** GPU-compressed data per mip (converter output) and its format */
    public byte[][] $compressed;
    public int $compressedFormat;
    /** compressed blob on disk (path relative to the data root), loaded when first drawn */
    public String $compressedSrc;
    /** optional alpha mask (8 bit, row 0 = bottom) for pixel queries on compressed textures */
    public byte[] $alpha;
    public String $alphaSrc;
    /** encoded image (LoadImage) decoded only when pixels are needed; lets the GPU path drop CPU pixels */
    public byte[] $encoded;
    /** CPU pixels changed by script since they were last uploaded */
    public boolean $cpuDirty;

    private static Texture2D white, black, gray, red, normal;

    public Texture2D() {}
    public Texture2D(int w, int h) { this(w, h, 4, true); }
    public Texture2D(int w, int h, int fmt, boolean mipChain) { this(w, h, fmt, mipChain, false); }
    public Texture2D(int w, int h, int fmt, boolean mipChain, boolean linear) { this(w, h, fmt, mipChain, linear, false); }
    public Texture2D(int w, int h, int fmt, boolean mipChain, boolean linear, boolean uninit) {
        width = w;
        height = h;
        format = fmt;
        this.linear = linear;
        mipCount = mipChain ? 1 + (int) Math.floor(Math.log(Math.max(1, Math.max(w, h))) / Math.log(2)) : 1;
        px = new int[Math.max(0, w * h)];
        if (!uninit) java.util.Arrays.fill(px, 0xCDCDCDCD == 0 ? 0 : 0x00000000);
    }
    public Texture2D(int w, int h, int graphicsFormat, int flags) { this(w, h, 4, (flags & 1) != 0); }

    public static Texture2D get_whiteTexture() { if (white == null) white = solid(0xFFFFFFFF, "UnityWhite"); return white; }
    public static Texture2D get_blackTexture() { if (black == null) black = solid(0xFF000000, "UnityBlack"); return black; }
    public static Texture2D get_grayTexture() { if (gray == null) gray = solid(0xFF808080, "UnityGrey"); return gray; }
    public static Texture2D get_redTexture() { if (red == null) red = solid(0xFFFF0000, "UnityRed"); return red; }
    public static Texture2D get_normalTexture() { if (normal == null) normal = solid(0xFF8080FF, "UnityNormalMap"); return normal; }
    private static Texture2D solid(int c, String name) {
        Texture2D t = new Texture2D(4, 4, 4, false);
        java.util.Arrays.fill(t.px, c);
        t.m_Name = name;
        t.$asset = true;
        return t;
    }

    public int get_format() { return format; }
    @Override public boolean get_isReadable() { return readable; }

    /** make sure CPU pixels exist (decoding the source image if needed) */
    int[] pixels() {
        if (px != null) return px;
        if ($encoded != null) {
            Object[] im = Host.get().decodeImage($encoded);
            if (im != null) {
                setFromTopDown((Integer) im[0], (Integer) im[1], (int[]) im[2]);
                return px;
            }
        }
        if ($src != null) {
            byte[] b = Host.get().readData($src);
            Object[] im = b == null ? null : Host.get().decodeImage(b);
            if (im != null) {
                setFromTopDown((Integer) im[0], (Integer) im[1], (int[]) im[2]);
                return px;
            }
            Debug.LogWarning("texture source missing: " + $src);
        }
        if ($alpha == null && $alphaSrc != null) loadAlpha();
        px = new int[width * height];
        if ($alpha != null) for (int i = 0; i < px.length && i < $alpha.length; i++) px[i] = (($alpha[i] & 0xff) << 24) | 0xFFFFFF;
        return px;
    }

    private void loadAlpha() {
        byte[] b = Host.get().readData($alphaSrc);
        if (b == null) return;
        $alpha = inflate(b, width * height);
        if ($alpha == null) Debug.LogWarning("bad alpha mask " + $alphaSrc);
    }

    static byte[] inflate(byte[] b, int size) {
        try {
            java.util.zip.Inflater inf = new java.util.zip.Inflater();
            inf.setInput(b);
            byte[] out = new byte[size];
            int off = 0;
            while (off < out.length && !inf.finished()) {
                int n = inf.inflate(out, off, out.length - off);
                if (n == 0 && (inf.needsInput() || inf.needsDictionary())) break;
                off += n;
            }
            inf.end();
            return out;
        } catch (java.util.zip.DataFormatException e) {
            return null;
        }
    }

    /** raw GPU blocks of the converted texture (.astcz = deflated ASTC 4x4 blocks, lossless) */
    public byte[] $readCompressed() {
        if ($compressedSrc == null) return null;
        byte[] b = Host.get().readData($compressedSrc);
        if (b == null || !$compressedSrc.endsWith("z")) return b;
        byte[] r = inflate(b, ((width + 3) / 4) * ((height + 3) / 4) * 16);
        if (r == null) Debug.LogWarning("bad compressed texture " + $compressedSrc);
        return r;
    }

    /** after a GPU upload: free the CPU copy when it can be re-created from its source */
    public void $dropPixels() {
        if (!$cpuDirty && ($src != null || $encoded != null) && px != null) px = null;
    }
    public boolean $reloadable() { return $src != null || $encoded != null || $compressedSrc != null; }

    /** true when CPU pixels are not needed for rendering (compressed upload) */
    public boolean $hasPixels() { return px != null; }
    public int[] $pixels() { return pixels(); }

    void setFromTopDown(int w, int h, int[] argb) {
        width = w;
        height = h;
        px = new int[w * h];
        for (int y = 0; y < h; y++) System.arraycopy(argb, (h - 1 - y) * w, px, y * w, w);
        $version++;
    }

    public int[] $topDown() {
        int[] p = pixels();
        int[] r = new int[p.length];
        for (int y = 0; y < height; y++) System.arraycopy(p, (height - 1 - y) * width, r, y * width, width);
        return r;
    }

    static Color toColor(int c) { return new Color(((c >> 16) & 0xff) / 255f, ((c >> 8) & 0xff) / 255f, (c & 0xff) / 255f, ((c >>> 24) & 0xff) / 255f); }
    static int fromColor(Color c) {
        return (b(c.a) << 24) | (b(c.r) << 16) | (b(c.g) << 8) | b(c.b);
    }
    private static int b(float v) { return Math.round(Math.max(0, Math.min(1, v)) * 255f); }
    static Color32 toColor32(int c) { return new Color32((byte) (c >> 16), (byte) (c >> 8), (byte) c, (byte) (c >>> 24)); }
    static int fromColor32(Color32 c) { return ((c.a & 0xff) << 24) | ((c.r & 0xff) << 16) | ((c.g & 0xff) << 8) | (c.b & 0xff); }

    private int wrap(int v, int n, int mode) {
        if (mode == 0) return ((v % n) + n) % n;
        return Math.max(0, Math.min(n - 1, v));
    }

    public Color GetPixel(int x, int y) {
        int[] p = pixels();
        if (width == 0 || height == 0) return new Color();
        return toColor(p[wrap(y, height, wrapModeV) * width + wrap(x, width, wrapModeU)]);
    }
    public Color GetPixel(int x, int y, int mip) { return GetPixel(x, y); }
    public Color GetPixelBilinear(float u, float v) {
        float x = u * width - .5f, y = v * height - .5f;
        int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
        float fx = x - x0, fy = y - y0;
        Color a = GetPixel(x0, y0), b = GetPixel(x0 + 1, y0), c = GetPixel(x0, y0 + 1), d = GetPixel(x0 + 1, y0 + 1);
        Color r = new Color();
        r.r = lerp2(a.r, b.r, c.r, d.r, fx, fy); r.g = lerp2(a.g, b.g, c.g, d.g, fx, fy);
        r.b = lerp2(a.b, b.b, c.b, d.b, fx, fy); r.a = lerp2(a.a, b.a, c.a, d.a, fx, fy);
        return r;
    }
    public Color GetPixelBilinear(float u, float v, int mip) { return GetPixelBilinear(u, v); }
    private static float lerp2(float a, float b, float c, float d, float fx, float fy) {
        return (a + (b - a) * fx) * (1 - fy) + (c + (d - c) * fx) * fy;
    }
    public void SetPixel(int x, int y, Color c) {
        int[] p = pixels();
        $cpuDirty = true;
        if (x < 0 || y < 0 || x >= width || y >= height) return;
        p[y * width + x] = fromColor(c);
    }
    public void SetPixel(int x, int y, Color c, int mip) { SetPixel(x, y, c); }
    public Color[] GetPixels() { return GetPixels(0, 0, width, height, 0); }
    public Color[] GetPixels(int mip) { return GetPixels(); }
    public Color[] GetPixels(int x, int y, int w, int h) { return GetPixels(x, y, w, h, 0); }
    public Color[] GetPixels(int x, int y, int w, int h, int mip) {
        int[] p = pixels();
        Color[] r = new Color[w * h];
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) r[j * w + i] = toColor(p[(y + j) * width + x + i]);
        return r;
    }
    public Color32[] GetPixels32() { return GetPixels32(0); }
    public Color32[] GetPixels32(int mip) {
        int[] p = pixels();
        Color32[] r = new Color32[p.length];
        for (int i = 0; i < p.length; i++) r[i] = toColor32(p[i]);
        return r;
    }
    public void SetPixels(Color[] c) { SetPixels(0, 0, width, height, c, 0); }
    public void SetPixels(Color[] c, int mip) { SetPixels(c); }
    public void SetPixels(int x, int y, int w, int h, Color[] c) { SetPixels(x, y, w, h, c, 0); }
    public void SetPixels(int x, int y, int w, int h, Color[] c, int mip) {
        int[] p = pixels();
        $cpuDirty = true;
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) p[(y + j) * width + x + i] = fromColor(c[j * w + i]);
    }
    public void SetPixels32(Color32[] c) { SetPixels32(c, 0); }
    public void SetPixels32(Color32[] c, int mip) {
        int[] p = pixels();
        $cpuDirty = true;
        for (int i = 0; i < p.length && i < c.length; i++) p[i] = fromColor32(c[i]);
    }
    public void SetPixels32(int x, int y, int w, int h, Color32[] c) {
        int[] p = pixels();
        $cpuDirty = true;
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) p[(y + j) * width + x + i] = fromColor32(c[j * w + i]);
    }
    public void Apply() { Apply(true, false); }
    public void Apply(boolean mips) { Apply(mips, false); }
    public void Apply(boolean mips, boolean makeNoLongerReadable) {
        $version++;
        if (makeNoLongerReadable) readable = false;
    }
    public boolean Resize(int w, int h) { return Reinitialize(w, h); }
    public boolean Resize(int w, int h, int fmt, boolean mips) { format = fmt; return Reinitialize(w, h); }
    public boolean Reinitialize(int w, int h) {
        width = w;
        height = h;
        px = new int[w * h];
        $compressed = null;
        $src = null;
        $version++;
        return true;
    }
    public boolean Reinitialize(int w, int h, int fmt, boolean mips) { format = fmt; return Reinitialize(w, h); }
    public void ReadPixels(Rect r, int x, int y) { ReadPixels(r, x, y, true); }
    public void ReadPixels(Rect r, int x, int y, boolean mips) {
        int[] p = pixels();
        int[] src = Render.readPixels((int) r.m_XMin, (int) r.m_YMin, (int) r.m_Width, (int) r.m_Height);
        if (src == null) return;
        int w = (int) r.m_Width, h = (int) r.m_Height;
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) {
            int tx = x + i, ty = y + j;
            if (tx < width && ty < height) p[ty * width + tx] = src[j * w + i];
        }
    }
    public byte[] GetRawTextureData() {
        int[] p = pixels();
        byte[] b = new byte[p.length * 4];
        for (int i = 0; i < p.length; i++) {
            int c = p[i];
            b[i * 4] = (byte) (c >> 16); b[i * 4 + 1] = (byte) (c >> 8); b[i * 4 + 2] = (byte) c; b[i * 4 + 3] = (byte) (c >>> 24);
        }
        return b;
    }
    public void LoadRawTextureData(byte[] b) {
        int[] p = pixels();
        $cpuDirty = true;
        for (int i = 0; i < p.length && i * 4 + 3 < b.length; i++)
            p[i] = ((b[i * 4 + 3] & 0xff) << 24) | ((b[i * 4] & 0xff) << 16) | ((b[i * 4 + 1] & 0xff) << 8) | (b[i * 4 + 2] & 0xff);
    }
    public void Compress(boolean hq) {}

    @Override public UObject $cloneAsset() {
        Texture2D t = new Texture2D();
        t.width = width; t.height = height; t.format = format; t.filterMode = filterMode; t.wrapModeU = wrapModeU; t.wrapModeV = wrapModeV;
        t.mipCount = mipCount; t.readable = readable; t.$src = $src; t.$compressed = $compressed; t.$compressedFormat = $compressedFormat;
        t.$compressedSrc = $compressedSrc;
        t.$alpha = $alpha; t.$alphaSrc = $alphaSrc; t.$encoded = $encoded;
        if (px != null) t.px = px.clone();
        return t;
    }

    @Override public void $loadAsset(java.util.HashMap<String, Object> d, Loader.Ctx ctx) {
        width = Loader.i(d, "m_Width", 0);
        height = Loader.i(d, "m_Height", 0);
        format = Loader.i(d, "m_TextureFormat", 4);
        mipCount = Loader.i(d, "m_MipCount", 1);
        readable = Loader.b(d, "m_IsReadable", false);
        java.util.HashMap<String, Object> ts = Loader.m(d, "m_TextureSettings");
        filterMode = Loader.i(ts, "m_FilterMode", 1);
        wrapModeU = Loader.i(ts, "m_WrapU", 1);
        wrapModeV = Loader.i(ts, "m_WrapV", 1);
        Object png = d.get("_png"), astc = d.get("_astc"), alpha = d.get("_alpha"), src = d.get("_src");
        if (alpha instanceof String) $alphaSrc = (String) alpha;
        if (astc instanceof String) {
            $compressedSrc = (String) astc;
            $compressedFormat = d.containsKey("_astcFormat") ? (int) cs.rt.Json.lng(d.get("_astcFormat")) : 0x93B0;
        } else if (src instanceof String) $src = (String) src;
        else if (png instanceof String && width > 0 && height > 0) $src = "unity/" + png;
        else if (width > 0 && height > 0) px = new int[width * height];
    }
}
