package cs.UnityEngine;

public class LineRenderer extends Renderer {
    Vector3[] positions = new Vector3[0];
    final Color startColor = new Color(1, 1, 1, 1), endColor = new Color(1, 1, 1, 1);
    float startWidth = 1, endWidth = 1;
    boolean useWorldSpace = true, loop;
    int numCapVertices, numCornerVertices, textureMode;
    public int get_positionCount() { return positions.length; }
    public void set_positionCount(int n) {
        Vector3[] p = new Vector3[n];
        for (int i = 0; i < n; i++) p[i] = i < positions.length ? positions[i] : new Vector3();
        positions = p;
    }
    public void SetPosition(int i, Vector3 v) { positions[i] = v.$copy(); }
    public Vector3 GetPosition(int i) { return positions[i].$copy(); }
    public void SetPositions(Vector3[] p) { set_positionCount(p.length); for (int i = 0; i < p.length; i++) positions[i] = p[i].$copy(); }
    public Color get_startColor() { return startColor.$copy(); }
    public void set_startColor(Color c) { startColor.$set(c); }
    public Color get_endColor() { return endColor.$copy(); }
    public void set_endColor(Color c) { endColor.$set(c); }
    public float get_startWidth() { return startWidth; }
    public void set_startWidth(float w) { startWidth = w; }
    public float get_endWidth() { return endWidth; }
    public void set_endWidth(float w) { endWidth = w; }
    public void set_widthMultiplier(float w) { startWidth = endWidth = w; }
    public boolean get_useWorldSpace() { return useWorldSpace; }
    public void set_useWorldSpace(boolean v) { useWorldSpace = v; }
    public boolean get_loop() { return loop; }
    public void set_loop(boolean v) { loop = v; }
    public void set_numCapVertices(int n) { numCapVertices = n; }
    public void set_numCornerVertices(int n) { numCornerVertices = n; }
    public void set_textureMode(int m) { textureMode = m; }
    public Vector3[] $positions() { return positions; }
    @Override public void $load(java.util.HashMap<String, Object> d, Loader.Ctx ctx) {
        super.$load(d, ctx);
        java.util.ArrayList<Object> p = Loader.l(d, "m_Positions");
        positions = new Vector3[p.size()];
        for (int i = 0; i < positions.length; i++) { positions[i] = new Vector3(); Loader.vec3(p.get(i), positions[i]); }
        useWorldSpace = Loader.b(d, "m_UseWorldSpace", true);
        loop = Loader.b(d, "m_Loop", false);
        java.util.HashMap<String, Object> lp = Loader.m(d, "m_Parameters");
        startWidth = endWidth = Loader.f(lp, "widthMultiplier", 1);
    }
    @Override public void $cloneFrom(Component src, Eng.CloneCtx ctx) {
        super.$cloneFrom(src, ctx);
        LineRenderer s = (LineRenderer) src;
        SetPositions(s.positions); startColor.$set(s.startColor); endColor.$set(s.endColor); startWidth = s.startWidth; endWidth = s.endWidth;
        useWorldSpace = s.useWorldSpace; loop = s.loop;
    }
}
