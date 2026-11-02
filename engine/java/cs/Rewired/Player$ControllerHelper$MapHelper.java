package cs.Rewired;

public class Player$ControllerHelper$MapHelper extends cs.Rewired.Utils.Classes.CodeHelper {
    final Player$ControllerHelper owner;
    Player$ControllerHelper$MapHelper(Player$ControllerHelper o) { owner = o; }
    public cs.System.Collections.Generic.IList_1 GetMaps(Controller c) {
        cs.System.Collections.Generic.List_1 l = new cs.System.Collections.Generic.List_1();
        if (c != null) l.Add(new ControllerMap(c));
        return l;
    }
    public int SetMapsEnabled(boolean enabled, String category) { return 1; }
    public int SetMapsEnabled(boolean enabled, String category, String layout) { return 1; }
    public void SetAllMapsEnabled(boolean enabled) {}
}
