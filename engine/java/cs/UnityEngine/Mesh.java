package cs.UnityEngine;

import cs.System.Collections.Generic.List_1;

/** Mesh with flat arrays: positions xyz, colors RGBA32 (packed ARGB int), up to 4 uv channels (4 floats each), submesh indices. */
public class Mesh extends UObject {
    public int $count;
    public float[] $pos = new float[0];
    public int[] $col;
    public float[][] $uv = new float[4][];
    public int[] $uvDim = {2, 2, 2, 2};
    public float[] $normals, $tangents;
    public int[][] $sub = {new int[0]};
    final Bounds bounds = new Bounds();
    public int $version;
    public int indexFormat;

    public Mesh() {}
    public void MarkDynamic() {}
    public void UploadMeshData(boolean b) {}
    public boolean get_isReadable() { return true; }
    public int get_indexFormat() { return indexFormat; }
    public void set_indexFormat(int f) { indexFormat = f; }

    public void Clear() { Clear(true); }
    public void Clear(boolean keepLayout) {
        $count = 0;
        $pos = new float[0];
        $col = null;
        $uv = new float[4][];
        $normals = $tangents = null;
        $sub = new int[][]{new int[0]};
        $version++;
    }

    public int get_vertexCount() { return $count; }
    public int get_subMeshCount() { return $sub.length; }
    public void set_subMeshCount(int n) {
        int[][] s = new int[n][];
        for (int i = 0; i < n; i++) s[i] = i < $sub.length ? $sub[i] : new int[0];
        $sub = s;
        $version++;
    }

    // ------------------------------------------------------------------ vertices
    public Vector3[] get_vertices() {
        Vector3[] r = new Vector3[$count];
        for (int i = 0; i < $count; i++) r[i] = new Vector3($pos[i * 3], $pos[i * 3 + 1], $pos[i * 3 + 2]);
        return r;
    }
    public void set_vertices(Vector3[] v) {
        $count = v.length;
        $pos = new float[$count * 3];
        for (int i = 0; i < $count; i++) { $pos[i * 3] = v[i].x; $pos[i * 3 + 1] = v[i].y; $pos[i * 3 + 2] = v[i].z; }
        $version++;
    }
    public void SetVertices(List_1 l) { SetVertices(l, 0, l.get_Count()); }
    public void SetVertices(List_1 l, int start, int n) {
        $count = n;
        $pos = new float[n * 3];
        for (int i = 0; i < n; i++) { Vector3 v = (Vector3) l.get_Item(start + i); $pos[i * 3] = v.x; $pos[i * 3 + 1] = v.y; $pos[i * 3 + 2] = v.z; }
        $version++;
    }
    public void SetVertices(Vector3[] v) { set_vertices(v); }
    public void SetVertices(Vector3[] v, int start, int n) {
        $count = n;
        $pos = new float[n * 3];
        for (int i = 0; i < n; i++) { Vector3 p = v[start + i]; $pos[i * 3] = p.x; $pos[i * 3 + 1] = p.y; $pos[i * 3 + 2] = p.z; }
        $version++;
    }
    public void GetVertices(List_1 out) { out.Clear(); for (Vector3 v : get_vertices()) out.Add(v); }

    // ------------------------------------------------------------------ colors
    public Color32[] get_colors32() {
        Color32[] r = new Color32[$col == null ? 0 : $count];
        for (int i = 0; i < r.length; i++) r[i] = Texture2D.toColor32($col[i]);
        return r;
    }
    public void set_colors32(Color32[] c) {
        $col = new int[c.length];
        for (int i = 0; i < c.length; i++) $col[i] = Texture2D.fromColor32(c[i]);
        $version++;
    }
    public Color[] get_colors() {
        Color[] r = new Color[$col == null ? 0 : $count];
        for (int i = 0; i < r.length; i++) r[i] = Texture2D.toColor($col[i]);
        return r;
    }
    public void set_colors(Color[] c) {
        $col = new int[c.length];
        for (int i = 0; i < c.length; i++) $col[i] = Texture2D.fromColor(c[i]);
        $version++;
    }
    public void SetColors(List_1 l) {
        int n = l.get_Count();
        $col = new int[n];
        for (int i = 0; i < n; i++) {
            Object o = l.get_Item(i);
            $col[i] = o instanceof Color32 ? Texture2D.fromColor32((Color32) o) : Texture2D.fromColor((Color) o);
        }
        $version++;
    }
    public void SetColors(Color32[] c) { set_colors32(c); }
    public void SetColors(Color[] c) { set_colors(c); }
    public void GetColors(List_1 out) { out.Clear(); for (Color32 c : get_colors32()) out.Add(c); }

    // ------------------------------------------------------------------ uvs
    public Vector2[] get_uv() { return uv2(0); }
    public void set_uv(Vector2[] v) { setUv2(0, v); }
    public Vector2[] get_uv2() { return uv2(1); }
    public void set_uv2(Vector2[] v) { setUv2(1, v); }
    public Vector2[] get_uv3() { return uv2(2); }
    public void set_uv3(Vector2[] v) { setUv2(2, v); }
    public Vector2[] get_uv4() { return uv2(3); }
    public void set_uv4(Vector2[] v) { setUv2(3, v); }
    private Vector2[] uv2(int ch) {
        float[] u = $uv[ch];
        Vector2[] r = new Vector2[u == null ? 0 : u.length / 4];
        for (int i = 0; i < r.length; i++) r[i] = new Vector2(u[i * 4], u[i * 4 + 1]);
        return r;
    }
    private void setUv2(int ch, Vector2[] v) {
        float[] u = new float[v.length * 4];
        for (int i = 0; i < v.length; i++) { u[i * 4] = v[i].x; u[i * 4 + 1] = v[i].y; }
        $uv[ch] = u;
        $uvDim[ch] = 2;
        $version++;
    }
    public void SetUVs(int ch, List_1 l) {
        int n = l.get_Count();
        float[] u = new float[n * 4];
        int dim = 2;
        for (int i = 0; i < n; i++) {
            Object o = l.get_Item(i);
            if (o instanceof Vector2) { Vector2 v = (Vector2) o; u[i * 4] = v.x; u[i * 4 + 1] = v.y; }
            else if (o instanceof Vector3) { Vector3 v = (Vector3) o; u[i * 4] = v.x; u[i * 4 + 1] = v.y; u[i * 4 + 2] = v.z; dim = 3; }
            else if (o instanceof Vector4) { Vector4 v = (Vector4) o; u[i * 4] = v.x; u[i * 4 + 1] = v.y; u[i * 4 + 2] = v.z; u[i * 4 + 3] = v.w; dim = 4; }
        }
        $uv[ch] = u;
        $uvDim[ch] = dim;
        $version++;
    }
    public void SetUVs(int ch, Vector2[] v) { setUv2(ch, v); }
    public void SetUVs(int ch, Vector4[] v) {
        float[] u = new float[v.length * 4];
        for (int i = 0; i < v.length; i++) { u[i * 4] = v[i].x; u[i * 4 + 1] = v[i].y; u[i * 4 + 2] = v[i].z; u[i * 4 + 3] = v[i].w; }
        $uv[ch] = u;
        $uvDim[ch] = 4;
        $version++;
    }
    public void GetUVs(int ch, List_1 out) {
        out.Clear();
        float[] u = $uv[ch];
        if (u == null) return;
        int n = u.length / 4;
        for (int i = 0; i < n; i++) {
            if ($uvDim[ch] == 2) out.Add(new Vector2(u[i * 4], u[i * 4 + 1]));
            else out.Add(new Vector4(u[i * 4], u[i * 4 + 1], u[i * 4 + 2], u[i * 4 + 3]));
        }
    }

    // ------------------------------------------------------------------ normals / tangents
    public Vector3[] get_normals() {
        Vector3[] r = new Vector3[$normals == null ? 0 : $normals.length / 3];
        for (int i = 0; i < r.length; i++) r[i] = new Vector3($normals[i * 3], $normals[i * 3 + 1], $normals[i * 3 + 2]);
        return r;
    }
    public void set_normals(Vector3[] v) {
        $normals = new float[v.length * 3];
        for (int i = 0; i < v.length; i++) { $normals[i * 3] = v[i].x; $normals[i * 3 + 1] = v[i].y; $normals[i * 3 + 2] = v[i].z; }
    }
    public void SetNormals(List_1 l) {
        Vector3[] a = new Vector3[l.get_Count()];
        for (int i = 0; i < a.length; i++) a[i] = (Vector3) l.get_Item(i);
        set_normals(a);
    }
    public Vector4[] get_tangents() {
        Vector4[] r = new Vector4[$tangents == null ? 0 : $tangents.length / 4];
        for (int i = 0; i < r.length; i++) r[i] = new Vector4($tangents[i * 4], $tangents[i * 4 + 1], $tangents[i * 4 + 2], $tangents[i * 4 + 3]);
        return r;
    }
    public void set_tangents(Vector4[] v) {
        $tangents = new float[v.length * 4];
        for (int i = 0; i < v.length; i++) { $tangents[i * 4] = v[i].x; $tangents[i * 4 + 1] = v[i].y; $tangents[i * 4 + 2] = v[i].z; $tangents[i * 4 + 3] = v[i].w; }
    }
    public void SetTangents(List_1 l) {
        Vector4[] a = new Vector4[l.get_Count()];
        for (int i = 0; i < a.length; i++) a[i] = (Vector4) l.get_Item(i);
        set_tangents(a);
    }

    // ------------------------------------------------------------------ indices
    public int[] get_triangles() {
        int n = 0;
        for (int[] s : $sub) n += s.length;
        int[] r = new int[n];
        n = 0;
        for (int[] s : $sub) { System.arraycopy(s, 0, r, n, s.length); n += s.length; }
        return r;
    }
    public void set_triangles(int[] t) { $sub = new int[][]{t.clone()}; $version++; }
    public int[] GetTriangles(int sub) { return $sub[sub].clone(); }
    public int[] GetIndices(int sub) { return $sub[sub].clone(); }
    public void SetTriangles(int[] t, int sub) { SetTriangles(t, sub, true); }
    public void SetTriangles(int[] t, int sub, boolean calcBounds) {
        if (sub >= $sub.length) set_subMeshCount(sub + 1);
        $sub[sub] = t.clone();
        $version++;
    }
    public void SetTriangles(List_1 l, int sub) { SetTriangles(l, sub, true); }
    public void SetTriangles(List_1 l, int sub, boolean calcBounds) {
        int[] t = new int[l.get_Count()];
        for (int i = 0; i < t.length; i++) t[i] = cs.rt.Rt.unboxI(l.get_Item(i));
        SetTriangles(t, sub, calcBounds);
    }
    public void SetIndices(int[] t, int topology, int sub) { SetTriangles(t, sub); }
    public void SetIndices(int[] t, int topology, int sub, boolean calc) { SetTriangles(t, sub); }
    public void SetIndices(List_1 l, int topology, int sub) { SetTriangles(l, sub); }

    // ------------------------------------------------------------------ bounds
    public Bounds get_bounds() { return bounds.$copy(); }
    public void set_bounds(Bounds b) { bounds.$set(b); }
    public void RecalculateBounds() {
        if ($count == 0) { bounds.m_Center.Set(0, 0, 0); bounds.m_Extent.Set(0, 0, 0); return; }
        float x0 = Float.POSITIVE_INFINITY, y0 = x0, z0 = x0, x1 = Float.NEGATIVE_INFINITY, y1 = x1, z1 = x1;
        for (int i = 0; i < $count; i++) {
            float x = $pos[i * 3], y = $pos[i * 3 + 1], z = $pos[i * 3 + 2];
            x0 = Math.min(x0, x); x1 = Math.max(x1, x); y0 = Math.min(y0, y); y1 = Math.max(y1, y); z0 = Math.min(z0, z); z1 = Math.max(z1, z);
        }
        bounds.m_Center.Set((x0 + x1) / 2, (y0 + y1) / 2, (z0 + z1) / 2);
        bounds.m_Extent.Set((x1 - x0) / 2, (y1 - y0) / 2, (z1 - z0) / 2);
    }
    public void RecalculateNormals() {}
    public void RecalculateTangents() {}
    public void Optimize() {}

    @Override public UObject $cloneAsset() {
        Mesh m = new Mesh();
        m.$count = $count; m.$pos = $pos.clone(); m.$col = $col == null ? null : $col.clone();
        for (int i = 0; i < 4; i++) { m.$uv[i] = $uv[i] == null ? null : $uv[i].clone(); m.$uvDim[i] = $uvDim[i]; }
        m.$normals = $normals == null ? null : $normals.clone();
        m.$tangents = $tangents == null ? null : $tangents.clone();
        m.$sub = new int[$sub.length][];
        for (int i = 0; i < $sub.length; i++) m.$sub[i] = $sub[i].clone();
        m.bounds.$set(bounds);
        return m;
    }
}
