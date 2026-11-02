package cs.Rewired;

public class PlayerActiveControllerChangedDelegate extends cs.System.Delegate {
    public void Invoke(Player p, Controller c) { if ($list != null) { for (cs.System.Delegate d : $list) ((PlayerActiveControllerChangedDelegate) d).$call(p, c); return; } $call(p, c); }
    protected void $call(Player p, Controller c) { throw new IllegalStateException("unbound delegate"); }
    protected cs.System.Delegate $empty() { return new PlayerActiveControllerChangedDelegate(); }
}
