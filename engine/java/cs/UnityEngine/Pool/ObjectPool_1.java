package cs.UnityEngine.Pool;

import java.util.ArrayList;

public class ObjectPool_1 implements IPool, IObjectPool_1, cs.System.IDisposable {
    final ArrayList<Object> stack = new ArrayList<>();
    cs.System.Func_1 create;
    cs.System.Action_1 onGet, onRelease, onDestroy;
    boolean check;
    int max = 10000, countAll;
    public ObjectPool_1() {}
    public ObjectPool_1(cs.System.Func_1 create, cs.System.Action_1 get, cs.System.Action_1 rel, cs.System.Action_1 destroy, boolean check, int cap, int max) {
        if (create == null) throw new cs.System.ArgumentNullException("createFunc");
        this.create = create; onGet = get; onRelease = rel; onDestroy = destroy; this.check = check; this.max = max;
    }
    public int get_CountAll() { return countAll; }
    public int get_CountActive() { return countAll - stack.size(); }
    public int get_CountInactive() { return stack.size(); }
    public Object Get() {
        Object o;
        if (stack.isEmpty()) { o = create.Invoke(); countAll++; }
        else o = stack.remove(stack.size() - 1);
        if (onGet != null) onGet.Invoke(o);
        return o;
    }
    public void Release(Object o) {
        if (check && stack.contains(o)) throw new cs.System.InvalidOperationException("Trying to release an object that has already been released to the pool.");
        if (onRelease != null) onRelease.Invoke(o);
        if (stack.size() < max) stack.add(o);
        else { countAll--; if (onDestroy != null) onDestroy.Invoke(o); }
    }
    public void Clear() {
        if (onDestroy != null) for (Object o : stack) onDestroy.Invoke(o);
        stack.clear();
        countAll = 0;
    }
    public void Dispose() { Clear(); }
}
