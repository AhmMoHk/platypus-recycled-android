package cs.Rewired;

public class ControllerStatusChangedEventArgs {
    final Controller c;
    ControllerStatusChangedEventArgs(Controller c) { this.c = c; }
    public Controller get_controller() { return c; }
    public int get_controllerType() { return c.type; }
    public String get_name() { return c.name; }
}
