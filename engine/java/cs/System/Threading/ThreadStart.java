package cs.System.Threading;

public class ThreadStart extends cs.System.Delegate {
    public void Invoke() { if ($list != null) { for (cs.System.Delegate d : $list) ((ThreadStart) d).$call(); return; } $call(); }
    protected void $call() { throw new IllegalStateException("unbound delegate"); }
    protected cs.System.Delegate $empty() { return new ThreadStart(); }
}
