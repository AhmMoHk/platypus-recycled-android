package cs.UnityEngine;

import java.util.ArrayList;

/** 2D geometry: ear-clipping triangulation and overlap/distance tests between convex parts. */
final class Geom2D {
    private Geom2D() {}

    /** triangulate a simple polygon (any winding); appends triangles {x0,y0,x1,y1,x2,y2} */
    static void triangulate(Vector2[] path, ArrayList<float[]> out) {
        int n = path.length;
        if (n < 3) return;
        float[] xs = new float[n], ys = new float[n];
        for (int i = 0; i < n; i++) { xs[i] = path[i].x; ys[i] = path[i].y; }
        float area = 0;
        for (int i = 0; i < n; i++) { int j = (i + 1) % n; area += xs[i] * ys[j] - xs[j] * ys[i]; }
        int[] idx = new int[n];
        for (int i = 0; i < n; i++) idx[i] = area >= 0 ? i : n - 1 - i;
        if (isConvex(xs, ys, idx, n)) {
            // convex: keep as one polygon (fewer parts)
            float[] p = new float[n * 2];
            for (int i = 0; i < n; i++) { p[i * 2] = xs[idx[i]]; p[i * 2 + 1] = ys[idx[i]]; }
            out.add(p);
            return;
        }
        int m = n, guard = 0;
        while (m > 3 && guard++ < n * n) {
            boolean clipped = false;
            for (int i = 0; i < m; i++) {
                int a = idx[(i + m - 1) % m], b = idx[i], c = idx[(i + 1) % m];
                float cr = (xs[b] - xs[a]) * (ys[c] - ys[a]) - (ys[b] - ys[a]) * (xs[c] - xs[a]);
                if (cr <= 1e-9f) continue;
                boolean inside = false;
                for (int k = 0; k < m; k++) {
                    int p = idx[k];
                    if (p == a || p == b || p == c) continue;
                    if (pointInTri(xs[p], ys[p], xs[a], ys[a], xs[b], ys[b], xs[c], ys[c])) { inside = true; break; }
                }
                if (inside) continue;
                out.add(new float[]{xs[a], ys[a], xs[b], ys[b], xs[c], ys[c]});
                System.arraycopy(idx, i + 1, idx, i, m - i - 1);
                m--;
                clipped = true;
                break;
            }
            if (!clipped) {
                // degenerate / self-intersecting: fan the rest
                for (int i = 1; i + 1 < m; i++) out.add(new float[]{xs[idx[0]], ys[idx[0]], xs[idx[i]], ys[idx[i]], xs[idx[i + 1]], ys[idx[i + 1]]});
                return;
            }
        }
        if (m == 3) out.add(new float[]{xs[idx[0]], ys[idx[0]], xs[idx[1]], ys[idx[1]], xs[idx[2]], ys[idx[2]]});
    }

    private static boolean isConvex(float[] xs, float[] ys, int[] idx, int n) {
        if (n > 8) return false;
        for (int i = 0; i < n; i++) {
            int a = idx[i], b = idx[(i + 1) % n], c = idx[(i + 2) % n];
            float cr = (xs[b] - xs[a]) * (ys[c] - ys[a]) - (ys[b] - ys[a]) * (xs[c] - xs[a]);
            if (cr < 0) return false;
        }
        return true;
    }

    static boolean pointInTri(float px, float py, float ax, float ay, float bx, float by, float cx, float cy) {
        float d1 = (px - bx) * (ay - by) - (ax - bx) * (py - by);
        float d2 = (px - cx) * (by - cy) - (bx - cx) * (py - cy);
        float d3 = (px - ax) * (cy - ay) - (cx - ax) * (py - ay);
        boolean neg = d1 < 0 || d2 < 0 || d3 < 0, pos = d1 > 0 || d2 > 0 || d3 > 0;
        return !(neg && pos);
    }

    /** convex polygons (CCW) overlap by SAT, with tolerance eps */
    static boolean polyPoly(float[] a, float[] b, float eps) {
        return !separated(a, b, eps) && !separated(b, a, eps);
    }

    private static boolean separated(float[] a, float[] b, float eps) {
        int n = a.length;
        for (int i = 0; i < n; i += 2) {
            int j = (i + 2) % n;
            float nx = a[j + 1] - a[i + 1], ny = -(a[j] - a[i]);
            float len = (float) Math.sqrt(nx * nx + ny * ny);
            if (len == 0) continue;
            nx /= len; ny /= len;
            float amax = Float.NEGATIVE_INFINITY, bmin = Float.POSITIVE_INFINITY;
            for (int k = 0; k < n; k += 2) amax = Math.max(amax, a[k] * nx + a[k + 1] * ny);
            for (int k = 0; k < b.length; k += 2) bmin = Math.min(bmin, b[k] * nx + b[k + 1] * ny);
            if (bmin > amax + eps) return true;
        }
        return false;
    }

    static boolean pointInPoly(float[] p, float x, float y) {
        int n = p.length;
        for (int i = 0; i < n; i += 2) {
            int j = (i + 2) % n;
            if ((p[j] - p[i]) * (y - p[i + 1]) - (p[j + 1] - p[i + 1]) * (x - p[i]) < 0) return false;
        }
        return true;
    }

    /** squared distance from point to segment */
    static float distSegSq(float px, float py, float ax, float ay, float bx, float by) {
        float dx = bx - ax, dy = by - ay, l = dx * dx + dy * dy;
        float t = l == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / l));
        float qx = ax + t * dx - px, qy = ay + t * dy - py;
        return qx * qx + qy * qy;
    }

    /** distance from point to convex polygon (0 if inside) */
    static float distPoly(float[] p, float x, float y) {
        if (pointInPoly(p, x, y)) return 0;
        float best = Float.POSITIVE_INFINITY;
        for (int i = 0, n = p.length; i < n; i += 2) {
            int j = (i + 2) % n;
            best = Math.min(best, distSegSq(x, y, p[i], p[i + 1], p[j], p[j + 1]));
        }
        return (float) Math.sqrt(best);
    }

    static boolean segSeg(float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy) {
        float d1 = cross(cx, cy, dx, dy, ax, ay), d2 = cross(cx, cy, dx, dy, bx, by);
        float d3 = cross(ax, ay, bx, by, cx, cy), d4 = cross(ax, ay, bx, by, dx, dy);
        return ((d1 > 0) != (d2 > 0)) && ((d3 > 0) != (d4 > 0));
    }

    private static float cross(float ax, float ay, float bx, float by, float px, float py) {
        return (bx - ax) * (py - ay) - (by - ay) * (px - ax);
    }

    static float segSegDist(float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy) {
        if (segSeg(ax, ay, bx, by, cx, cy, dx, dy)) return 0;
        float d = Math.min(Math.min(distSegSq(ax, ay, cx, cy, dx, dy), distSegSq(bx, by, cx, cy, dx, dy)),
            Math.min(distSegSq(cx, cy, ax, ay, bx, by), distSegSq(dx, dy, ax, ay, bx, by)));
        return (float) Math.sqrt(d);
    }

    /** distance between segment and convex polygon (0 if touching/inside) */
    static float segPolyDist(float[] p, float ax, float ay, float bx, float by) {
        if (pointInPoly(p, ax, ay) || pointInPoly(p, bx, by)) return 0;
        float best = Float.POSITIVE_INFINITY;
        for (int i = 0, n = p.length; i < n; i += 2) {
            int j = (i + 2) % n;
            best = Math.min(best, segSegDist(ax, ay, bx, by, p[i], p[i + 1], p[j], p[j + 1]));
            if (best == 0) return 0;
        }
        return best;
    }
}
