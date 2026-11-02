package cs.Rewired;

public class ControllerMap {
    final Controller controller;
    ControllerMap(Controller c) { controller = c; }
    public ActionElementMap[] GetElementMapsWithAction(String action) {
        String e;
        if (controller.type == 0) {
            switch (action) {
                case "Fire": e = "Z"; break;
                case "Pause": e = "Escape"; break;
                case "UISubmit": e = "Return"; break;
                case "UICancel": e = "Escape"; break;
                default: e = action;
            }
        } else {
            e = "Fire".equals(action) ? "Left Mouse Button" : action;
        }
        return new ActionElementMap[]{new ActionElementMap(e)};
    }
    public boolean get_enabled() { return true; }
    public String get_categoryName() { return "Default"; }
}
