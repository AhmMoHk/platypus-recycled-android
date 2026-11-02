package cs.UnityEngine;

import java.util.ArrayList;

/** PhysicsShapeType2D: Circle 0, Capsule 1, Polygon 2, Edges 3, Box 4 */
public class PhysicsShapeGroup2D {
    final ArrayList<PhysicsShape2D> shapes = new ArrayList<>();
    final ArrayList<Vector2> verts = new ArrayList<>();
    public PhysicsShapeGroup2D() {}
    public PhysicsShapeGroup2D(int shapeCap, int vertCap) {}
    public int get_shapeCount() { return shapes.size(); }
    public int get_vertexCount() { return verts.size(); }
    public void Clear() { shapes.clear(); verts.clear(); }
    public PhysicsShape2D GetShape(int i) { return shapes.get(i).$copy(); }
    public Vector2 GetShapeVertex(int s, int v) { return verts.get(shapes.get(s).m_VertexStartIndex + v).$copy(); }
    public int AddPolygon(cs.System.Collections.Generic.List_1 pts) {
        PhysicsShape2D s = new PhysicsShape2D();
        s.m_ShapeType = 2;
        s.m_VertexStartIndex = verts.size();
        s.m_VertexCount = pts.get_Count();
        for (int i = 0; i < pts.get_Count(); i++) verts.add(((Vector2) pts.get_Item(i)).$copy());
        shapes.add(s);
        return shapes.size() - 1;
    }
    public int AddCircle(Vector2 c, float r) {
        PhysicsShape2D s = new PhysicsShape2D();
        s.m_ShapeType = 0; s.m_Radius = r; s.m_VertexStartIndex = verts.size(); s.m_VertexCount = 1;
        verts.add(c.$copy());
        shapes.add(s);
        return shapes.size() - 1;
    }
    public int AddBox(Vector2 c, Vector2 size) { return AddBox(c, size, 0, 0); }
    public int AddBox(Vector2 c, Vector2 size, float angle, float edgeRadius) {
        cs.System.Collections.Generic.List_1 l = new cs.System.Collections.Generic.List_1();
        float hx = size.x / 2, hy = size.y / 2, ca = (float) Math.cos(Math.toRadians(angle)), sa = (float) Math.sin(Math.toRadians(angle));
        float[][] q = {{-hx, -hy}, {hx, -hy}, {hx, hy}, {-hx, hy}};
        for (float[] v : q) l.Add(new Vector2(c.x + v[0] * ca - v[1] * sa, c.y + v[0] * sa + v[1] * ca));
        return AddPolygon(l);
    }
    void copyFrom(PhysicsShapeGroup2D g) {
        Clear();
        for (PhysicsShape2D s : g.shapes) shapes.add(s.$copy());
        for (Vector2 v : g.verts) verts.add(v.$copy());
    }
    void toParts(ArrayList<float[]> p, ArrayList<float[]> c, ArrayList<float[]> k) {
        for (PhysicsShape2D s : shapes) {
            if (s.m_ShapeType == 0) {
                Vector2 v = verts.get(s.m_VertexStartIndex);
                c.add(new float[]{v.x, v.y, s.m_Radius});
            } else if (s.m_ShapeType == 3) {
                for (int i = 0; i + 1 < s.m_VertexCount; i++) {
                    Vector2 a = verts.get(s.m_VertexStartIndex + i), b = verts.get(s.m_VertexStartIndex + i + 1);
                    k.add(new float[]{a.x, a.y, b.x, b.y, s.m_Radius});
                }
            } else {
                Vector2[] pts = new Vector2[s.m_VertexCount];
                for (int i = 0; i < pts.length; i++) pts[i] = verts.get(s.m_VertexStartIndex + i);
                Geom2D.triangulate(pts, p);
            }
        }
    }
}
