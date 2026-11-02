package cs.UnityEngine;

import java.util.ArrayList;

/** Polygon paths, decomposed into triangles (ear clipping) for the narrow phase. */
public class PolygonCollider2D extends Collider2D {
    ArrayList<Vector2[]> paths = new ArrayList<>();
    private ArrayList<float[]> tris;

    public PolygonCollider2D() { paths.add(new Vector2[]{new Vector2(-.5f, -.5f), new Vector2(.5f, -.5f), new Vector2(.5f, .5f), new Vector2(-.5f, .5f)}); }

    public int get_pathCount() { return paths.size(); }
    public void set_pathCount(int n) {
        while (paths.size() > n) paths.remove(paths.size() - 1);
        while (paths.size() < n) paths.add(new Vector2[0]);
        changed();
    }
    public Vector2[] GetPath(int i) { return copy(paths.get(i)); }
    public int GetPath(int i, cs.System.Collections.Generic.List_1 out) {
        out.Clear();
        for (Vector2 v : paths.get(i)) out.Add(v.$copy());
        return paths.get(i).length;
    }
    public void SetPath(int i, Vector2[] pts) {
        while (paths.size() <= i) paths.add(new Vector2[0]);
        paths.set(i, copy(pts));
        changed();
    }
    public void SetPath(int i, cs.System.Collections.Generic.List_1 pts) {
        Vector2[] a = new Vector2[pts.get_Count()];
        for (int k = 0; k < a.length; k++) a[k] = ((Vector2) pts.get_Item(k)).$copy();
        SetPath(i, a);
    }
    public Vector2[] get_points() { return paths.isEmpty() ? new Vector2[0] : copy(paths.get(0)); }
    public void set_points(Vector2[] p) { paths.clear(); paths.add(copy(p)); changed(); }
    public int GetTotalPointCount() { int n = 0; for (Vector2[] p : paths) n += p.length; return n; }
    public void CreatePrimitive(int sides) { CreatePrimitive(sides, new Vector2(1, 1), new Vector2()); }
    public void CreatePrimitive(int sides, Vector2 scale) { CreatePrimitive(sides, scale, new Vector2()); }
    public void CreatePrimitive(int sides, Vector2 scale, Vector2 off) {
        Vector2[] p = new Vector2[sides];
        for (int i = 0; i < sides; i++) {
            double a = Math.PI * 2 * i / sides;
            p[i] = new Vector2((float) Math.cos(a) * scale.x * .5f + off.x, (float) Math.sin(a) * scale.y * .5f + off.y);
        }
        set_points(p);
    }

    private static Vector2[] copy(Vector2[] a) { Vector2[] r = new Vector2[a.length]; for (int i = 0; i < a.length; i++) r[i] = a[i].$copy(); return r; }
    private void changed() { tris = null; shapeChanged(); }

    @Override void localShape(ArrayList<float[]> p, ArrayList<float[]> c, ArrayList<float[]> k) {
        if (tris == null) {
            tris = new ArrayList<>();
            for (Vector2[] path : paths) Geom2D.triangulate(path, tris);
        }
        p.addAll(tris);
    }

    @Override public void $load(java.util.HashMap<String, Object> d, Loader.Ctx ctx) {
        super.$load(d, ctx);
        java.util.HashMap<String, Object> pts = Loader.m(d, "m_Points");
        paths.clear();
        for (Object path : Loader.l(pts, "m_Paths")) {
            ArrayList<Object> l = cs.rt.Json.list(path);
            Vector2[] a = new Vector2[l.size()];
            for (int i = 0; i < a.length; i++) { a[i] = new Vector2(); Loader.vec2(l.get(i), a[i]); }
            paths.add(a);
        }
        changed();
    }
    @Override public void $cloneFrom(Component s, Eng.CloneCtx ctx) {
        super.$cloneFrom(s, ctx);
        paths.clear();
        for (Vector2[] p : ((PolygonCollider2D) s).paths) paths.add(copy(p));
        tris = ((PolygonCollider2D) s).tris;
        shapeChanged();
    }
}
