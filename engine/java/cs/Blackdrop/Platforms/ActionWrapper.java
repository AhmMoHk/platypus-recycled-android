package cs.Blackdrop.Platforms;

/** event wrapper: += / -= of System.Action */
public class ActionWrapper {
    cs.System.Delegate d;
    public ActionWrapper() {}
    public static ActionWrapper op_Addition(ActionWrapper w, cs.System.Action a) {
        if (w == null) w = new ActionWrapper();
        w.d = cs.System.Delegate.Combine(w.d, a);
        return w;
    }
    public static ActionWrapper op_Subtraction(ActionWrapper w, cs.System.Action a) {
        if (w == null) return null;
        w.d = cs.System.Delegate.Remove(w.d, a);
        return w;
    }
    public void Invoke() { if (d != null) ((cs.System.Action) d).Invoke(); }
}
