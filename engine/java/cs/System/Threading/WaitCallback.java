package cs.System.Threading;

public class WaitCallback extends cs.System.Delegate {
    public void Invoke(Object s) { if ($list != null) { for (cs.System.Delegate d : $list) ((WaitCallback) d).$call(s); return; } $call(s); }
    protected void $call(Object s) { throw new IllegalStateException("unbound delegate"); }
    protected cs.System.Delegate $empty() { return new WaitCallback(); }
}
