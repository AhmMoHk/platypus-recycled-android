package cs.UnityEngine;

/** IEnumerator that keeps the coroutine waiting while keepWaiting is true. */
public abstract class CustomYieldInstruction implements cs.System.Collections.IEnumerator {
    public CustomYieldInstruction() {}
    public void $ctor_CustomYieldInstruction() {}
    public abstract boolean get_keepWaiting();
    public Object get_Current() { return null; }
    public boolean MoveNext() { return get_keepWaiting(); }
    public void Reset() {}
}
