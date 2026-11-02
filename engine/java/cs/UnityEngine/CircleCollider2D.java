package cs.UnityEngine;

import java.util.ArrayList;

public class CircleCollider2D extends Collider2D {
    float radius = 0.5f;
    public float get_radius() { return radius; }
    public void set_radius(float r) { radius = r; shapeChanged(); }
    @Override void localShape(ArrayList<float[]> p, ArrayList<float[]> c, ArrayList<float[]> k) { c.add(new float[]{0, 0, radius}); }
    @Override public void $load(java.util.HashMap<String, Object> d, Loader.Ctx ctx) { super.$load(d, ctx); radius = Loader.f(d, "m_Radius", .5f); }
    @Override public void $cloneFrom(Component s, Eng.CloneCtx ctx) { super.$cloneFrom(s, ctx); radius = ((CircleCollider2D) s).radius; }
}
