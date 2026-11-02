package cs.UnityEngine;

public class Application$LogCallback extends cs.System.Delegate {
    public void Invoke(String condition, String stackTrace, int type) {
        if ($list != null) { for (cs.System.Delegate d : $list) ((Application$LogCallback) d).$call(condition, stackTrace, type); return; }
        $call(condition, stackTrace, type);
    }
    protected void $call(String condition, String stackTrace, int type) { throw new IllegalStateException("unbound delegate"); }
    protected cs.System.Delegate $empty() { return new Application$LogCallback(); }
}
