package cs.System.Reflection;

public class EventInfo extends MemberInfo {
    public EventInfo() {}
    public boolean get_IsSpecialName() { return false; }
    public MethodInfo GetAddMethod() { return null; }
    public MethodInfo GetRemoveMethod() { return null; }
    public MethodInfo GetAddMethod(boolean b) { return null; }
    public MethodInfo GetRemoveMethod(boolean b) { return null; }
    public cs.System.Type get_EventHandlerType() { return cs.System.Type.OBJECT; }
}
