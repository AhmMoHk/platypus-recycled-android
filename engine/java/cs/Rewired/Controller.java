package cs.Rewired;

import cs.System.Collections.Generic.List_1;

/** ControllerType: Keyboard 0, Mouse 1, Joystick 2, Custom 20 */
public class Controller {
    public String name;
    public int type;
    final List_1 buttons = new List_1();
    final cs.System.Guid guid = cs.System.Guid.NewGuid();
    String hardwareIdentifier;
    boolean anyDown;

    public Controller() {}
    Controller(String name, int type, String hw) { this.name = name; this.type = type; hardwareIdentifier = hw; buttons.Add(new Controller$Button()); }
    public String get_name() { return name; }
    public int get_type() { return type; }
    public int get_id() { return type; }
    public boolean get_isConnected() { return true; }
    public boolean get_enabled() { return true; }
    public String get_hardwareName() { return name; }
    public String get_hardwareIdentifier() { return hardwareIdentifier; }
    public cs.System.Guid get_hardwareTypeGuid() { return cs.System.Guid.Empty; }
    public cs.System.Guid get_deviceInstanceGuid() { return guid; }
    public cs.System.Collections.Generic.IList_1 get_Buttons() { return buttons; }
    public int get_buttonCount() { return buttons.get_Count(); }
    public boolean GetAnyButtonDown() { return anyDown; }
    public boolean GetAnyButton() { return ((Controller$Button) buttons.get_Item(0)).value; }
    public boolean GetAnyButtonUp() { return ((Controller$Button) buttons.get_Item(0)).get_justReleased(); }
    public boolean GetButtonDown(int i) { return i == 0 && anyDown; }

    void sample(boolean any, boolean down) {
        Controller$Button b = (Controller$Button) buttons.get_Item(0);
        b.prev = b.value;
        b.value = any;
        anyDown = down;
    }
}
