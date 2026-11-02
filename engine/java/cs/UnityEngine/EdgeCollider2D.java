package cs.UnityEngine;

import java.util.ArrayList;

public class EdgeCollider2D extends Collider2D {
    Vector2[] points = {new Vector2(-.5f, 0), new Vector2(.5f, 0)};
    float edgeRadius;
    public Vector2[] get_points() { Vector2[] r = new Vector2[points.length]; for (int i = 0; i < r.length; i++) r[i] = points[i].$copy(); return r; }
    public void set_points(Vector2[] p) { points = new Vector2[p.length]; for (int i = 0; i < p.length; i++) points[i] = p[i].$copy(); shapeChanged(); }
    public int get_pointCount() { return points.length; }
    public int get_edgeCount() { return Math.max(0, points.length - 1); }
    public float get_edgeRadius() { return edgeRadius; }
    public void set_edgeRadius(float r) { edgeRadius = r; shapeChanged(); }
    public boolean SetPoints(cs.System.Collections.Generic.List_1 l) {
        Vector2[] a = new Vector2[l.get_Count()];
        for (int i = 0; i < a.length; i++) a[i] = ((Vector2) l.get_Item(i)).$copy();
        set_points(a);
        return true;
    }
    @Override void localShape(ArrayList<float[]> p, ArrayList<float[]> c, ArrayList<float[]> k) {
        for (int i = 0; i + 1 < points.length; i++) k.add(new float[]{points[i].x, points[i].y, points[i + 1].x, points[i + 1].y, edgeRadius});
    }
    @Override public void $load(java.util.HashMap<String, Object> d, Loader.Ctx ctx) {
        super.$load(d, ctx);
        ArrayList<Object> l = Loader.l(d, "m_Points");
        points = new Vector2[l.size()];
        for (int i = 0; i < points.length; i++) { points[i] = new Vector2(); Loader.vec2(l.get(i), points[i]); }
        edgeRadius = Loader.f(d, "m_EdgeRadius", 0);
    }
    @Override public void $cloneFrom(Component s, Eng.CloneCtx ctx) { super.$cloneFrom(s, ctx); set_points(((EdgeCollider2D) s).points); edgeRadius = ((EdgeCollider2D) s).edgeRadius; }
}
