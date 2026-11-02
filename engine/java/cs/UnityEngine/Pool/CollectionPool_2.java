package cs.UnityEngine.Pool;

import java.util.ArrayList;

/** Only used with List<T> (ExecuteEvents): the collection type is erased, so lists are pooled. */
public class CollectionPool_2 {
    private static final ArrayList<cs.System.Collections.Generic.List_1> pool = new ArrayList<>();
    public static Object Get() { return pool.isEmpty() ? new cs.System.Collections.Generic.List_1() : pool.remove(pool.size() - 1); }
    public static void Release(Object o) {
        cs.System.Collections.Generic.List_1 l = (cs.System.Collections.Generic.List_1) o;
        l.Clear();
        if (!pool.contains(l)) pool.add(l);
    }
}
