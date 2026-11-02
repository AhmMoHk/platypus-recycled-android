package cs.UnityEngine.Events;

public class UnityAction_3 extends cs.System.Delegate {
    public void Invoke(Object a0, Object a1, Object a2) { if ($list != null) { for (cs.System.Delegate d : $list) ((UnityAction_3) d).$call(a0, a1, a2); return; } $call(a0, a1, a2); }
    protected void $call(Object a0, Object a1, Object a2) { throw new IllegalStateException("unbound delegate"); }
    protected cs.System.Delegate $empty() { return new UnityAction_3(); }
}
