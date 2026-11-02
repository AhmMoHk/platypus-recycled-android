package cs.UnityEngine;

public class Canvas$WillRenderCanvases extends cs.System.Delegate {
    public void Invoke() {
        if ($list != null) { for (cs.System.Delegate d : $list) ((Canvas$WillRenderCanvases) d).$call(); return; }
        $call();
    }
    protected void $call() { throw new IllegalStateException("unbound delegate"); }
    protected cs.System.Delegate $empty() { return new Canvas$WillRenderCanvases(); }
}
