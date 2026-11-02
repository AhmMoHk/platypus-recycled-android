package cs.UnityEngine.TextCore.LowLevel;

import cs.System.Collections.Generic.List_1;
import cs.UnityEngine.TextCore.GlyphRect;
import java.util.ArrayList;

/** MaxRects bin packing (best short side fit) over TMP's free/used GlyphRect lists. */
final class MaxRects {
    private MaxRects() {}

    static GlyphRect insert(List_1 free, List_1 used, int w, int h) {
        int best = -1, bestShort = Integer.MAX_VALUE, bestLong = Integer.MAX_VALUE;
        for (int i = 0, n = free.get_Count(); i < n; i++) {
            GlyphRect f = (GlyphRect) free.get_Item(i);
            if (f.m_Width >= w && f.m_Height >= h) {
                int lw = f.m_Width - w, lh = f.m_Height - h;
                int s = Math.min(lw, lh), l = Math.max(lw, lh);
                if (s < bestShort || s == bestShort && l < bestLong) { best = i; bestShort = s; bestLong = l; }
            }
        }
        if (best < 0) return null;
        GlyphRect f = (GlyphRect) free.get_Item(best);
        GlyphRect node = new GlyphRect(f.m_X, f.m_Y, w, h);
        // split all free rects intersecting node
        ArrayList<GlyphRect> keep = new ArrayList<>();
        for (int i = 0, n = free.get_Count(); i < n; i++) {
            GlyphRect r = (GlyphRect) free.get_Item(i);
            if (!intersects(r, node)) { keep.add(r); continue; }
            if (node.m_X > r.m_X) keep.add(new GlyphRect(r.m_X, r.m_Y, node.m_X - r.m_X, r.m_Height));
            if (node.m_X + node.m_Width < r.m_X + r.m_Width) keep.add(new GlyphRect(node.m_X + node.m_Width, r.m_Y, r.m_X + r.m_Width - node.m_X - node.m_Width, r.m_Height));
            if (node.m_Y > r.m_Y) keep.add(new GlyphRect(r.m_X, r.m_Y, r.m_Width, node.m_Y - r.m_Y));
            if (node.m_Y + node.m_Height < r.m_Y + r.m_Height) keep.add(new GlyphRect(r.m_X, node.m_Y + node.m_Height, r.m_Width, r.m_Y + r.m_Height - node.m_Y - node.m_Height));
        }
        // prune contained rects
        ArrayList<GlyphRect> pruned = new ArrayList<>();
        for (int i = 0; i < keep.size(); i++) {
            GlyphRect a = keep.get(i);
            boolean contained = false;
            for (int j = 0; j < keep.size() && !contained; j++) {
                if (i == j) continue;
                GlyphRect b = keep.get(j);
                if (contains(b, a) && !(a.equals(b) && j > i)) contained = true;
            }
            if (!contained) pruned.add(a);
        }
        free.Clear();
        for (GlyphRect r : pruned) free.Add(r);
        used.Add(node.$copy());
        return node;
    }

    private static boolean intersects(GlyphRect a, GlyphRect b) {
        return !(b.m_X >= a.m_X + a.m_Width || b.m_X + b.m_Width <= a.m_X || b.m_Y >= a.m_Y + a.m_Height || b.m_Y + b.m_Height <= a.m_Y);
    }
    private static boolean contains(GlyphRect outer, GlyphRect inner) {
        return inner.m_X >= outer.m_X && inner.m_Y >= outer.m_Y && inner.m_X + inner.m_Width <= outer.m_X + outer.m_Width && inner.m_Y + inner.m_Height <= outer.m_Y + outer.m_Height;
    }
}
