package cs.UnityEngine.Sprites;

import cs.UnityEngine.*;

/** outer/inner UVs, padding and min size of sprites (UI Image) */
public final class DataUtility {
    private DataUtility() {}
    public static Vector4 GetOuterUV(Sprite s) {
        Texture2D t = s.get_texture();
        if (t == null) return new Vector4(0, 0, 1, 1);
        Rect r = s.get_textureRect();
        float w = t.get_width(), h = t.get_height();
        return new Vector4(r.get_xMin() / w, r.get_yMin() / h, r.get_xMax() / w, r.get_yMax() / h);
    }
    public static Vector4 GetInnerUV(Sprite s) {
        Texture2D t = s.get_texture();
        if (t == null) return new Vector4(0, 0, 1, 1);
        Rect r = s.get_textureRect();
        Vector4 b = s.get_border();
        Vector2 off = s.get_textureRectOffset();
        float w = t.get_width(), h = t.get_height();
        float l = Math.max(0, b.x - off.x), bt = Math.max(0, b.y - off.y);
        Rect full = s.get_rect();
        float rr = Math.max(0, b.z - (full.get_width() - off.x - r.get_width())), tp = Math.max(0, b.w - (full.get_height() - off.y - r.get_height()));
        return new Vector4((r.get_xMin() + l) / w, (r.get_yMin() + bt) / h, (r.get_xMax() - rr) / w, (r.get_yMax() - tp) / h);
    }
    public static Vector4 GetPadding(Sprite s) {
        Rect full = s.get_rect(), r = s.get_textureRect();
        Vector2 off = s.get_textureRectOffset();
        return new Vector4(off.x, off.y, full.get_width() - r.get_width() - off.x, full.get_height() - r.get_height() - off.y);
    }
    public static Vector2 GetMinSize(Sprite s) {
        Vector4 b = s.get_border();
        return new Vector2(b.x + b.z, b.y + b.w);
    }
}
