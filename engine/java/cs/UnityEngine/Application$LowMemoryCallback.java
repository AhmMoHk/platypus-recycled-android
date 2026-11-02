package cs.UnityEngine;

public class Application$LowMemoryCallback extends cs.System.Delegate {
    public void Invoke() {
        if ($list != null) { for (cs.System.Delegate d : $list) ((Application$LowMemoryCallback) d).$call(); return; }
        $call();
    }
    protected void $call() { throw new IllegalStateException("unbound delegate"); }
    protected cs.System.Delegate $empty() { return new Application$LowMemoryCallback(); }
}
