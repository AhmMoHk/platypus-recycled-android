package cs.UnityEngine;

public final class GeometryUtility {
    private GeometryUtility() {}
    /** frustum planes (left, right, bottom, top, near, far) from the camera's view-projection */
    public static Plane[] CalculateFrustumPlanes(Camera cam) { return CalculateFrustumPlanes(cam.$viewProj()); }
    public static Plane[] CalculateFrustumPlanes(Matrix4x4 m) {
        float[] a = m.m;
        float[][] rows = new float[4][4];
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) rows[r][c] = a[c * 4 + r];
        int[][] combos = {{3, 0, 1}, {3, 0, -1}, {3, 1, 1}, {3, 1, -1}, {3, 2, 1}, {3, 2, -1}};
        Plane[] p = new Plane[6];
        for (int i = 0; i < 6; i++) {
            float[] r3 = rows[combos[i][0]], rk = rows[combos[i][1]];
            float s = combos[i][2];
            float nx = r3[0] + s * rk[0], ny = r3[1] + s * rk[1], nz = r3[2] + s * rk[2], d = r3[3] + s * rk[3];
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            p[i] = new Plane(new Vector3(nx / len, ny / len, nz / len), d / len);
        }
        return p;
    }
    public static boolean TestPlanesAABB(Plane[] planes, Bounds b) {
        for (Plane p : planes) {
            Vector3 n = p.get_normal();
            float r = b.m_Extent.x * Math.abs(n.x) + b.m_Extent.y * Math.abs(n.y) + b.m_Extent.z * Math.abs(n.z);
            float s = Vector3.Dot(n, b.m_Center) + p.get_distance();
            if (s + r < 0) return false;
        }
        return true;
    }
}
