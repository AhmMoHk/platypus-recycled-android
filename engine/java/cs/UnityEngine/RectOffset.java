package cs.UnityEngine;

public class RectOffset {
    int left, right, top, bottom;
    public RectOffset() {}
    public RectOffset(int l, int r, int t, int b) { left = l; right = r; top = t; bottom = b; }
    public int get_left() { return left; }
    public void set_left(int v) { left = v; }
    public int get_right() { return right; }
    public void set_right(int v) { right = v; }
    public int get_top() { return top; }
    public void set_top(int v) { top = v; }
    public int get_bottom() { return bottom; }
    public void set_bottom(int v) { bottom = v; }
    public int get_horizontal() { return left + right; }
    public int get_vertical() { return top + bottom; }
    public Rect Add(Rect r) { return new Rect(r.get_x() - left, r.get_y() - top, r.get_width() + left + right, r.get_height() + top + bottom); }
    public Rect Remove(Rect r) { return new Rect(r.get_x() + left, r.get_y() + top, r.get_width() - left - right, r.get_height() - top - bottom); }
    @Override public String toString() { return "RectOffset (l:" + left + " r:" + right + " t:" + top + " b:" + bottom + ")"; }
}
