package cs.UnityEngine.Events;

public class UnityAction_4 extends cs.System.Delegate {
    public void Invoke(Object a0, Object a1, Object a2, Object a3) { if ($list != null) { for (cs.System.Delegate d : $list) ((UnityAction_4) d).$call(a0, a1, a2, a3); return; } $call(a0, a1, a2, a3); }
    protected void $call(Object a0, Object a1, Object a2, Object a3) { throw new IllegalStateException("unbound delegate"); }
    protected cs.System.Delegate $empty() { return new UnityAction_4(); }
}
