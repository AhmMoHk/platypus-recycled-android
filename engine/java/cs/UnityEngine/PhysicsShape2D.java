package cs.UnityEngine;

public class PhysicsShape2D implements cs.rt.Struct {
    public int m_ShapeType, m_VertexStartIndex, m_VertexCount, m_UseAdjacentStart, m_UseAdjacentEnd;
    public float m_Radius;
    public final Vector2 m_AdjacentStart = new Vector2(), m_AdjacentEnd = new Vector2();
    public PhysicsShape2D() {}
    public int get_shapeType() { return m_ShapeType; }
    public float get_radius() { return m_Radius; }
    public void set_radius(float r) { m_Radius = r; }
    public int get_vertexStartIndex() { return m_VertexStartIndex; }
    public int get_vertexCount() { return m_VertexCount; }
    public PhysicsShape2D $copy() { PhysicsShape2D s = new PhysicsShape2D(); s.$set(this); return s; }
    public void $set(Object o) {
        PhysicsShape2D s = (PhysicsShape2D) o;
        m_ShapeType = s.m_ShapeType; m_VertexStartIndex = s.m_VertexStartIndex; m_VertexCount = s.m_VertexCount; m_Radius = s.m_Radius;
        m_UseAdjacentStart = s.m_UseAdjacentStart; m_UseAdjacentEnd = s.m_UseAdjacentEnd;
        m_AdjacentStart.$set(s.m_AdjacentStart); m_AdjacentEnd.$set(s.m_AdjacentEnd);
    }
    public void $clear() { $set(new PhysicsShape2D()); }
    public static PhysicsShape2D[] $newArray(int n) { PhysicsShape2D[] a = new PhysicsShape2D[n]; for (int i = 0; i < n; i++) a[i] = new PhysicsShape2D(); return a; }
}
