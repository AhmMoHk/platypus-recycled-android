package cs.UnityEngine;

import java.util.ArrayList;

public class BoxCollider2D extends Collider2D {
    final Vector2 size = new Vector2(1, 1);
    float edgeRadius;
    public Vector2 get_size() { return size.$copy(); }
    public void set_size(Vector2 v) { size.$set(v); shapeChanged(); }
    public float get_edgeRadius() { return edgeRadius; }
    public void set_edgeRadius(float r) { edgeRadius = r; shapeChanged(); }
    public boolean get_autoTiling() { return false; }
    public void set_autoTiling(boolean v) {}
    @Override void localShape(ArrayList<float[]> p, ArrayList<float[]> c, ArrayList<float[]> k) {
        float hx = size.x / 2 + edgeRadius, hy = size.y / 2 + edgeRadius;
        p.add(new float[]{-hx, -hy, hx, -hy, hx, hy, -hx, hy});
    }
    @Override public void $load(java.util.HashMap<String, Object> d, Loader.Ctx ctx) {
        super.$load(d, ctx);
        Loader.vec2(d.get("m_Size"), size);
        edgeRadius = Loader.f(d, "m_EdgeRadius", 0);
    }
    @Override public void $cloneFrom(Component s, Eng.CloneCtx ctx) {
        super.$cloneFrom(s, ctx);
        size.$set(((BoxCollider2D) s).size);
        edgeRadius = ((BoxCollider2D) s).edgeRadius;
    }
}
