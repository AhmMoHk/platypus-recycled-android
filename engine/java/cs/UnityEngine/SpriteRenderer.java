package cs.UnityEngine;

/** SpriteDrawMode: Simple 0, Sliced 1, Tiled 2. SpriteMaskInteraction: None 0, VisibleInside 1, VisibleOutside 2 */
public class SpriteRenderer extends Renderer {
    Sprite sprite;
    final Color color = new Color(1, 1, 1, 1);
    boolean flipX, flipY;
    int drawMode, maskInteraction, tileMode;
    final Vector2 size = new Vector2(1, 1);
    float adaptiveModeThreshold = .5f;

    public Sprite get_sprite() { return sprite; }
    public void set_sprite(Sprite s) { sprite = s; }
    public Color get_color() { return color.$copy(); }
    public void set_color(Color c) { color.$set(c); }
    public boolean get_flipX() { return flipX; }
    public void set_flipX(boolean v) { flipX = v; }
    public boolean get_flipY() { return flipY; }
    public void set_flipY(boolean v) { flipY = v; }
    public int get_drawMode() { return drawMode; }
    public void set_drawMode(int m) { drawMode = m; }
    public Vector2 get_size() {
        if (drawMode == 0 && sprite != null) return new Vector2(sprite.rect.m_Width / sprite.pixelsPerUnit, sprite.rect.m_Height / sprite.pixelsPerUnit);
        return size.$copy();
    }
    public void set_size(Vector2 v) { size.$set(v); }
    public int get_maskInteraction() { return maskInteraction; }
    public void set_maskInteraction(int m) { maskInteraction = m; }
    public int get_tileMode() { return tileMode; }
    public void set_tileMode(int m) { tileMode = m; }

    @Override public Bounds get_bounds() {
        Bounds b = new Bounds();
        if (sprite == null || gameObject == null) return b;
        Bounds lb = sprite.get_bounds();
        float[] m = gameObject.transform.world();
        float x0 = Float.POSITIVE_INFINITY, y0 = x0, x1 = Float.NEGATIVE_INFINITY, y1 = x1;
        for (int i = 0; i < 4; i++) {
            float lx = lb.m_Center.x + ((i & 1) == 0 ? -1 : 1) * lb.m_Extent.x, ly = lb.m_Center.y + ((i & 2) == 0 ? -1 : 1) * lb.m_Extent.y;
            float wx = m[0] * lx + m[4] * ly + m[12], wy = m[1] * lx + m[5] * ly + m[13];
            x0 = Math.min(x0, wx); x1 = Math.max(x1, wx); y0 = Math.min(y0, wy); y1 = Math.max(y1, wy);
        }
        b.m_Center.Set((x0 + x1) / 2, (y0 + y1) / 2, m[14]);
        b.m_Extent.Set((x1 - x0) / 2, (y1 - y0) / 2, 0);
        return b;
    }

    @Override public void $load(java.util.HashMap<String, Object> d, Loader.Ctx ctx) {
        super.$load(d, ctx);
        UObject s = ctx.ref(d.get("m_Sprite"));
        sprite = s instanceof Sprite ? (Sprite) s : null;
        Loader.color(d.get("m_Color"), color);
        flipX = Loader.b(d, "m_FlipX", false);
        flipY = Loader.b(d, "m_FlipY", false);
        drawMode = Loader.i(d, "m_DrawMode", 0);
        Loader.vec2(d.get("m_Size"), size);
        maskInteraction = Loader.i(d, "m_MaskInteraction", 0);
        tileMode = Loader.i(d, "m_SpriteTileMode", 0);
    }
    @Override public void $cloneFrom(Component src, Eng.CloneCtx ctx) {
        super.$cloneFrom(src, ctx);
        SpriteRenderer s = (SpriteRenderer) src;
        sprite = s.sprite; color.$set(s.color); flipX = s.flipX; flipY = s.flipY; drawMode = s.drawMode; size.$set(s.size);
        maskInteraction = s.maskInteraction; tileMode = s.tileMode;
    }
}
