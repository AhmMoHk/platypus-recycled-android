package cs.UnityEngine;

import java.util.ArrayList;

public class CustomCollider2D extends Collider2D {
    final PhysicsShapeGroup2D group = new PhysicsShapeGroup2D();
    public int get_customShapeCount() { return group.shapes.size(); }
    public int get_customVertexCount() { return group.verts.size(); }
    public int GetCustomShapes(PhysicsShapeGroup2D g) { g.copyFrom(group); return group.shapes.size(); }
    public void SetCustomShapes(PhysicsShapeGroup2D g) { group.copyFrom(g); shapeChanged(); }
    public void ClearCustomShapes() { group.Clear(); shapeChanged(); }
    @Override void localShape(ArrayList<float[]> p, ArrayList<float[]> c, ArrayList<float[]> k) { group.toParts(p, c, k); }
    @Override public void $cloneFrom(Component s, Eng.CloneCtx ctx) { super.$cloneFrom(s, ctx); group.copyFrom(((CustomCollider2D) s).group); }
}
