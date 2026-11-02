package cs.UnityEngine;

public class Vector2Int implements cs.rt.Struct {
    public int m_X, m_Y;
    public Vector2Int() {}
    public Vector2Int(int x, int y) { m_X = x; m_Y = y; }
    public Vector2Int $copy() { return new Vector2Int(m_X, m_Y); }
    public void $set(Object o) { Vector2Int v = (Vector2Int) o; m_X = v.m_X; m_Y = v.m_Y; }
    public void $clear() { m_X = m_Y = 0; }
    public static Vector2Int[] $newArray(int n) { Vector2Int[] a = new Vector2Int[n]; for (int i = 0; i < n; i++) a[i] = new Vector2Int(); return a; }
    public int get_x() { return m_X; }
    public int get_y() { return m_Y; }
    public void set_x(int v) { m_X = v; }
    public void set_y(int v) { m_Y = v; }
    public static Vector2Int get_zero() { return new Vector2Int(); }
    public static Vector2Int get_one() { return new Vector2Int(1, 1); }
    public static boolean op_Equality(Vector2Int a, Vector2Int b) { return a.m_X == b.m_X && a.m_Y == b.m_Y; }
    public static boolean op_Inequality(Vector2Int a, Vector2Int b) { return !op_Equality(a, b); }
    public static Vector2Int op_Addition(Vector2Int a, Vector2Int b) { return new Vector2Int(a.m_X + b.m_X, a.m_Y + b.m_Y); }
    public static Vector2Int op_Subtraction(Vector2Int a, Vector2Int b) { return new Vector2Int(a.m_X - b.m_X, a.m_Y - b.m_Y); }
    public static Vector2 op_Implicit$Vector2(Vector2Int v) { return new Vector2(v.m_X, v.m_Y); }
    public boolean Equals(Vector2Int o) { return op_Equality(this, o); }
    @Override public boolean equals(Object o) { return o instanceof Vector2Int && Equals((Vector2Int) o); }
    @Override public int hashCode() { return m_X ^ (m_Y << 2); }
    @Override public String toString() { return "(" + m_X + ", " + m_Y + ")"; }
}
