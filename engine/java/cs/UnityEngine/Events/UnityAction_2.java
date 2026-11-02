package cs.UnityEngine.Events;

public class UnityAction_2 extends cs.System.Delegate {
    public void Invoke(Object a0, Object a1) { if ($list != null) { for (cs.System.Delegate d : $list) ((UnityAction_2) d).$call(a0, a1); return; } $call(a0, a1); }
    protected void $call(Object a0, Object a1) { throw new IllegalStateException("unbound delegate"); }
    protected cs.System.Delegate $empty() { return new UnityAction_2(); }
}
