package cs.Rewired;

public class Controller$Button extends Controller$Element {
    boolean value, prev;
    public boolean get_value() { return value; }
    public boolean get_justPressed() { return value && !prev; }
    public boolean get_justReleased() { return !value && prev; }
    public boolean get_justChangedState() { return value != prev; }
}
