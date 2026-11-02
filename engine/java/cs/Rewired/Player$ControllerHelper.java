package cs.Rewired;

import cs.System.Collections.Generic.List_1;

public class Player$ControllerHelper extends cs.Rewired.Utils.Classes.CodeHelper {
    final Player player;
    final List_1 controllers = new List_1();
    public Player$ControllerHelper$MapHelper maps;
    boolean hasMouse, hasKeyboard;
    Controller lastActive;
    cs.System.Delegate added, removed;
    final java.util.ArrayList<PlayerActiveControllerChangedDelegate> lastActiveChanged = new java.util.ArrayList<>();

    Player$ControllerHelper(Player p) { player = p; maps = new Player$ControllerHelper$MapHelper(this); }

    public cs.System.Collections.Generic.IList_1 get_Controllers() { return controllers; }
    public int get_controllerCount() { return controllers.get_Count(); }
    public boolean get_hasMouse() { return hasMouse; }
    public void set_hasMouse(boolean v) { hasMouse = v; }
    public boolean get_hasKeyboard() { return hasKeyboard; }
    public void set_hasKeyboard(boolean v) { hasKeyboard = v; }
    public Controller GetLastActiveController() { return lastActive; }
    public boolean ContainsController(Controller c) { return controllers.Contains(c); }
    public void AddController(Controller c, boolean removeFromOthers) {
        if (c == null || controllers.Contains(c)) return;
        if (removeFromOthers) for (Player p : ReInput.allPlayers) if (p != player) p.controllers.RemoveController(c);
        controllers.Add(c);
        if (lastActive == null) setLastActive(c);
        if (added != null) added.DynamicInvoke(new Object[]{new ControllerAssignmentChangedEventArgs(player, c)});
    }
    public void RemoveController(Controller c) {
        if (!controllers.Remove(c)) return;
        if (lastActive == c) setLastActive(controllers.get_Count() > 0 ? (Controller) controllers.get_Item(0) : null);
        if (removed != null) removed.DynamicInvoke(new Object[]{new ControllerAssignmentChangedEventArgs(player, c)});
    }
    public void ClearAllControllers() {
        for (Object c : controllers.ToArray()) RemoveController((Controller) c);
    }
    public void add_ControllerAddedEvent(cs.System.Action_1 a) { added = cs.System.Delegate.Combine(added, a); }
    public void remove_ControllerAddedEvent(cs.System.Action_1 a) { added = cs.System.Delegate.Remove(added, a); }
    public void add_ControllerRemovedEvent(cs.System.Action_1 a) { removed = cs.System.Delegate.Combine(removed, a); }
    public void remove_ControllerRemovedEvent(cs.System.Action_1 a) { removed = cs.System.Delegate.Remove(removed, a); }
    public void AddLastActiveControllerChangedDelegate(PlayerActiveControllerChangedDelegate d) { if (d != null) lastActiveChanged.add(d); }
    public void RemoveLastActiveControllerChangedDelegate(PlayerActiveControllerChangedDelegate d) { lastActiveChanged.remove(d); }

    void setLastActive(Controller c) {
        if (c == lastActive) return;
        lastActive = c;
        for (PlayerActiveControllerChangedDelegate d : new java.util.ArrayList<>(lastActiveChanged)) d.Invoke(player, c);
    }
}
