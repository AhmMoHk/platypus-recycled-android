package cs.Rewired;

public class ControllerAssignmentChangedEventArgs {
    final Player p;
    final Controller c;
    ControllerAssignmentChangedEventArgs(Player p, Controller c) { this.p = p; this.c = c; }
    public Controller get_controller() { return c; }
    public Player get_player() { return p; }
    public boolean get_state() { return true; }
}
