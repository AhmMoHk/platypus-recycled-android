package cs.Rewired;

public class ReInput$ControllerHelper extends cs.Rewired.Utils.Classes.CodeHelper {
    public int get_controllerCount() { return ReInput.controllers.get_Count(); }
    public cs.System.Collections.Generic.IList_1 get_Controllers() { return ReInput.controllers; }
    public cs.System.Collections.Generic.IList_1 get_Joysticks() { return new cs.System.Collections.Generic.List_1(); }
    public int get_joystickCount() { return 0; }
    public Controller GetLastActiveController() { return ReInput.lastActive; }
    public Controller GetLastActiveController(int type) { return ReInput.lastActive; }
    public Mouse get_Mouse() { return ReInput.touch; }
    public Controller get_Keyboard() { return ReInput.keyboard; }
    public Controller GetController(int type, int id) { return type == 0 ? ReInput.keyboard : ReInput.touch; }
}
