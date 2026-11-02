package cs.UnityEngine;

import cs.rt.Json;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

/** Contact-only 2D physics (the game uses kinematic bodies and reads contacts). SimulationMode2D: FixedUpdate 0, Update 1, Script 2 */
public final class Physics2D {
    private Physics2D() {}
    static final ArrayList<Collider2D> colliders = new ArrayList<>();
    static final int[] matrix = new int[32];
    static int simulationMode;
    static boolean queriesHitTriggers = true, queriesStartInColliders = true;
    static float contactOffset = 0.01f;
    static final Vector2 gravity = new Vector2(0, -9.81f);
    public static final int DefaultRaycastLayers = -5, AllLayers = -1, IgnoreRaycastLayer = 4;

    static {
        for (int i = 0; i < 32; i++) matrix[i] = -1;
    }

    static void init(HashMap<String, Object> d) {
        ArrayList<Object> m = Loader.l(d, "m_LayerCollisionMatrix");
        for (int i = 0; i < 32 && i < m.size(); i++) matrix[i] = (int) Json.lng(m.get(i));
        simulationMode = Loader.i(d, "m_SimulationMode", 0);
        queriesHitTriggers = Loader.b(d, "m_QueriesHitTriggers", true);
        queriesStartInColliders = Loader.b(d, "m_QueriesStartInColliders", true);
        contactOffset = Loader.f(d, "m_DefaultContactOffset", .01f);
        Loader.vec2(d.get("m_Gravity"), gravity);
    }

    static void register(Collider2D c) { if (!c.registered) { c.registered = true; colliders.add(c); } }
    static void unregister(Collider2D c) {
        if (!c.registered) return;
        c.registered = false;
        colliders.remove(c);
        for (Collider2D o : c.contacts) o.contacts.remove(c);
        c.contacts.clear();
    }

    public static int get_simulationMode() { return simulationMode; }
    public static void set_simulationMode(int m) { simulationMode = m; }
    public static boolean get_autoSimulation() { return simulationMode != 2; }
    public static void set_autoSimulation(boolean v) { simulationMode = v ? 0 : 2; }
    public static Vector2 get_gravity() { return gravity.$copy(); }
    public static void set_gravity(Vector2 g) { gravity.$set(g); }
    public static boolean get_queriesHitTriggers() { return queriesHitTriggers; }
    public static void set_queriesHitTriggers(boolean v) { queriesHitTriggers = v; }
    public static boolean get_queriesStartInColliders() { return queriesStartInColliders; }
    public static void set_queriesStartInColliders(boolean v) { queriesStartInColliders = v; }
    public static void SyncTransforms() {}
    public static boolean GetIgnoreLayerCollision(int a, int b) { return (matrix[a] & (1 << b)) == 0; }
    public static void IgnoreLayerCollision(int a, int b) { IgnoreLayerCollision(a, b, true); }
    public static void IgnoreLayerCollision(int a, int b, boolean ignore) {
        if (ignore) { matrix[a] &= ~(1 << b); matrix[b] &= ~(1 << a); }
        else { matrix[a] |= 1 << b; matrix[b] |= 1 << a; }
    }
    public static int GetLayerCollisionMask(int l) { return matrix[l]; }
    public static void SetLayerCollisionMask(int l, int m) { matrix[l] = m; }

    static void autoSimulate() { if (simulationMode == 0) Simulate(Time.fixedDeltaTime); }

    private static final Comparator<Collider2D> BY_MINX = new Comparator<Collider2D>() {
        public int compare(Collider2D a, Collider2D b) { return Float.compare(a.minX, b.minX); }
    };

    /** step: apply kinematic moves, rebuild shapes, recompute contacts (sweep and prune on x) */
    public static boolean Simulate(float dt) {
        Rigidbody2D.applyMoves();
        ArrayList<Collider2D> act = new ArrayList<>(colliders.size());
        for (Collider2D c : colliders) {
            c.contacts.clear();
            if (c.$destroyed || c.gameObject == null || !c.gameObject.get_activeInHierarchy() || !c.m_Enabled) continue;
            Rigidbody2D rb = c.get_attachedRigidbody();
            if (rb != null && !rb.simulated) continue;
            c.rebuild();
            if (c.minX > c.maxX) continue;
            c.cachedBody = rb;
            act.add(c);
        }
        Collections.sort(act, BY_MINX);
        float eps = contactOffset;
        for (int i = 0, n = act.size(); i < n; i++) {
            Collider2D a = act.get(i);
            for (int j = i + 1; j < n; j++) {
                Collider2D b = act.get(j);
                if (b.minX > a.maxX + eps) break;
                if (b.minY > a.maxY + eps || a.minY > b.maxY + eps) continue;
                if (!pairAllowed(a, b)) continue;
                if (overlap(a, b, eps)) {
                    a.contacts.add(b);
                    b.contacts.add(a);
                }
            }
        }
        return true;
    }

    static boolean pairAllowed(Collider2D a, Collider2D b) {
        int la = a.gameObject.m_Layer, lb = b.gameObject.m_Layer;
        if ((matrix[la] & (1 << lb)) == 0) return false;
        Rigidbody2D ra = a.cachedBody, rbb = b.cachedBody;
        if (ra == null && rbb == null) return false;
        if (ra == rbb) return false;
        boolean da = ra != null && ra.bodyType == 0, db = rbb != null && rbb.bodyType == 0;
        if (da || db) return true;
        if (a.isTrigger || b.isTrigger) return true;
        return (ra != null && ra.useFullKinematicContacts) || (rbb != null && rbb.useFullKinematicContacts);
    }

    static boolean overlap(Collider2D a, Collider2D b, float eps) {
        for (float[] p : a.polys) {
            for (float[] q : b.polys) if (aabb(p, q, eps) && Geom2D.polyPoly(p, q, eps)) return true;
            for (float[] c : b.circles) if (Geom2D.distPoly(p, c[0], c[1]) <= c[2] + eps) return true;
            for (float[] k : b.capsules) if (Geom2D.segPolyDist(p, k[0], k[1], k[2], k[3]) <= k[4] + eps) return true;
        }
        for (float[] c : a.circles) {
            for (float[] q : b.polys) if (Geom2D.distPoly(q, c[0], c[1]) <= c[2] + eps) return true;
            for (float[] d : b.circles) {
                float dx = c[0] - d[0], dy = c[1] - d[1], r = c[2] + d[2] + eps;
                if (dx * dx + dy * dy <= r * r) return true;
            }
            for (float[] k : b.capsules) if (Math.sqrt(Geom2D.distSegSq(c[0], c[1], k[0], k[1], k[2], k[3])) <= c[2] + k[4] + eps) return true;
        }
        for (float[] k : a.capsules) {
            for (float[] q : b.polys) if (Geom2D.segPolyDist(q, k[0], k[1], k[2], k[3]) <= k[4] + eps) return true;
            for (float[] d : b.circles) if (Math.sqrt(Geom2D.distSegSq(d[0], d[1], k[0], k[1], k[2], k[3])) <= d[2] + k[4] + eps) return true;
            for (float[] m : b.capsules) if (Geom2D.segSegDist(k[0], k[1], k[2], k[3], m[0], m[1], m[2], m[3]) <= k[4] + m[4] + eps) return true;
        }
        return false;
    }

    private static boolean aabb(float[] p, float[] q, float eps) {
        float pminx = Float.POSITIVE_INFINITY, pmaxx = Float.NEGATIVE_INFINITY, pminy = pminx, pmaxy = pmaxx;
        for (int i = 0; i < p.length; i += 2) { pminx = Math.min(pminx, p[i]); pmaxx = Math.max(pmaxx, p[i]); pminy = Math.min(pminy, p[i + 1]); pmaxy = Math.max(pmaxy, p[i + 1]); }
        for (int i = 0; i < q.length; i += 2) {
            if (q[i] >= pminx - eps && q[i] <= pmaxx + eps && q[i + 1] >= pminy - eps && q[i + 1] <= pmaxy + eps) return true;
        }
        float qminx = Float.POSITIVE_INFINITY, qmaxx = Float.NEGATIVE_INFINITY, qminy = qminx, qmaxy = qmaxx;
        for (int i = 0; i < q.length; i += 2) { qminx = Math.min(qminx, q[i]); qmaxx = Math.max(qmaxx, q[i]); qminy = Math.min(qminy, q[i + 1]); qmaxy = Math.max(qmaxy, q[i + 1]); }
        return !(qminx > pmaxx + eps || pminx > qmaxx + eps || qminy > pmaxy + eps || pminy > qmaxy + eps);
    }

    static boolean pointInCollider(Collider2D c, float x, float y) {
        for (float[] p : c.polys) if (Geom2D.pointInPoly(p, x, y)) return true;
        for (float[] d : c.circles) { float dx = x - d[0], dy = y - d[1]; if (dx * dx + dy * dy <= d[2] * d[2]) return true; }
        for (float[] k : c.capsules) if (Geom2D.distSegSq(x, y, k[0], k[1], k[2], k[3]) <= k[4] * k[4]) return true;
        return false;
    }

    /** approximate contact point: midpoint of the AABB intersection */
    static void contactPoint(Collider2D a, Collider2D b, ContactPoint2D p) {
        float x0 = Math.max(a.minX, b.minX), x1 = Math.min(a.maxX, b.maxX), y0 = Math.max(a.minY, b.minY), y1 = Math.min(a.maxY, b.maxY);
        p.m_Point.x = (x0 + x1) / 2;
        p.m_Point.y = (y0 + y1) / 2;
        float nx = (a.minX + a.maxX) / 2 - (b.minX + b.maxX) / 2, ny = (a.minY + a.maxY) / 2 - (b.minY + b.maxY) / 2;
        float l = (float) Math.sqrt(nx * nx + ny * ny);
        p.m_Normal.x = l == 0 ? 0 : nx / l;
        p.m_Normal.y = l == 0 ? 1 : ny / l;
        p.m_Separation = 0;
    }

    // ------------------------------------------------------------------ queries
    private static boolean usable(Collider2D c, int mask) {
        if (c.$destroyed || c.gameObject == null || !c.gameObject.get_activeInHierarchy() || !c.m_Enabled) return false;
        if ((mask & (1 << c.gameObject.m_Layer)) == 0) return false;
        return queriesHitTriggers || !c.isTrigger;
    }

    private static Collider2D circleCollider(float x, float y, float r) {
        CircleCollider2D q = QUERY_CIRCLE;
        return q;
    }
    private static final CircleCollider2D QUERY_CIRCLE = new CircleCollider2D();

    private static boolean circleHits(Collider2D c, float x, float y, float r) {
        c.rebuild();
        if (x + r < c.minX || x - r > c.maxX || y + r < c.minY || y - r > c.maxY) return false;
        for (float[] p : c.polys) if (Geom2D.distPoly(p, x, y) <= r) return true;
        for (float[] d : c.circles) { float dx = x - d[0], dy = y - d[1], rr = r + d[2]; if (dx * dx + dy * dy <= rr * rr) return true; }
        for (float[] k : c.capsules) if (Math.sqrt(Geom2D.distSegSq(x, y, k[0], k[1], k[2], k[3])) <= r + k[4]) return true;
        return false;
    }

    public static Collider2D OverlapCircle(Vector2 p, float r) { return OverlapCircle(p, r, DefaultRaycastLayers); }
    public static Collider2D OverlapCircle(Vector2 p, float r, int mask) {
        for (Collider2D c : colliders) if (usable(c, mask) && circleHits(c, p.x, p.y, r)) return c;
        return null;
    }
    public static int OverlapCircleNonAlloc(Vector2 p, float r, Collider2D[] out) { return OverlapCircleNonAlloc(p, r, out, DefaultRaycastLayers); }
    public static int OverlapCircleNonAlloc(Vector2 p, float r, Collider2D[] out, int mask) {
        int n = 0;
        for (Collider2D c : colliders) { if (n >= out.length) break; if (usable(c, mask) && circleHits(c, p.x, p.y, r)) out[n++] = c; }
        return n;
    }
    public static Collider2D OverlapPoint(Vector2 p) { return OverlapPoint(p, DefaultRaycastLayers); }
    public static Collider2D OverlapPoint(Vector2 p, int mask) {
        for (Collider2D c : colliders) { if (!usable(c, mask)) continue; c.rebuild(); if (pointInCollider(c, p.x, p.y)) return c; }
        return null;
    }

    /** circle sweep: conservative stepping along dir (radius/4 steps) then bisection to the first touching distance */
    public static int CircleCastNonAlloc(Vector2 origin, float r, Vector2 dir, RaycastHit2D[] out, float dist, int mask) {
        float len = (float) Math.sqrt(dir.x * dir.x + dir.y * dir.y);
        if (len == 0) return 0;
        float dx = dir.x / len, dy = dir.y / len;
        if (Float.isInfinite(dist) || dist > 1e6f) dist = 1e6f;
        ArrayList<Object[]> hits = new ArrayList<>();
        for (Collider2D c : colliders) {
            if (!usable(c, mask)) continue;
            c.rebuild();
            // quick reject: swept AABB
            float sx0 = Math.min(origin.x, origin.x + dx * dist) - r, sx1 = Math.max(origin.x, origin.x + dx * dist) + r;
            float sy0 = Math.min(origin.y, origin.y + dy * dist) - r, sy1 = Math.max(origin.y, origin.y + dy * dist) + r;
            if (sx1 < c.minX || sx0 > c.maxX || sy1 < c.minY || sy0 > c.maxY) continue;
            if (circleHits(c, origin.x, origin.y, r)) {
                if (!queriesStartInColliders) continue;
                hits.add(new Object[]{c, 0f});
                continue;
            }
            float step = Math.max(r * 0.25f, 0.5f), t = 0, hit = -1;
            while (t < dist) {
                float nt = Math.min(dist, t + step);
                if (circleHits(c, origin.x + dx * nt, origin.y + dy * nt, r)) {
                    float lo = t, hi = nt;
                    for (int k = 0; k < 16; k++) {
                        float mid = (lo + hi) / 2;
                        if (circleHits(c, origin.x + dx * mid, origin.y + dy * mid, r)) hi = mid; else lo = mid;
                    }
                    hit = hi;
                    break;
                }
                t = nt;
            }
            if (hit >= 0) hits.add(new Object[]{c, hit});
        }
        Collections.sort(hits, new Comparator<Object[]>() {
            public int compare(Object[] a, Object[] b) { return Float.compare((Float) a[1], (Float) b[1]); }
        });
        int n = 0;
        for (Object[] h : hits) {
            if (n >= out.length) break;
            RaycastHit2D o = out[n++];
            Collider2D c = (Collider2D) h[0];
            float d = (Float) h[1];
            o.m_Collider = c;
            o.m_Distance = d;
            o.m_Fraction = d / dist;
            o.m_Centroid.Set(origin.x + dx * d, origin.y + dy * d);
            o.m_Point.Set(o.m_Centroid.x + dx * r, o.m_Centroid.y + dy * r);
            o.m_Normal.Set(-dx, -dy);
        }
        return n;
    }
    public static int CircleCastNonAlloc(Vector2 origin, float r, Vector2 dir, RaycastHit2D[] out) { return CircleCastNonAlloc(origin, r, dir, out, Float.POSITIVE_INFINITY, DefaultRaycastLayers); }
    public static int CircleCastNonAlloc(Vector2 origin, float r, Vector2 dir, RaycastHit2D[] out, float dist) { return CircleCastNonAlloc(origin, r, dir, out, dist, DefaultRaycastLayers); }

    public static RaycastHit2D Raycast(Vector2 origin, Vector2 dir) { return Raycast(origin, dir, Float.POSITIVE_INFINITY, DefaultRaycastLayers); }
    public static RaycastHit2D Raycast(Vector2 origin, Vector2 dir, float dist) { return Raycast(origin, dir, dist, DefaultRaycastLayers); }
    public static RaycastHit2D Raycast(Vector2 origin, Vector2 dir, float dist, int mask) {
        RaycastHit2D[] a = RaycastHit2D.$newArray(1);
        int n = CircleCastNonAlloc(origin, 0.0001f, dir, a, dist, mask);
        return n > 0 ? a[0] : new RaycastHit2D();
    }
}
