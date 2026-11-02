package cs.System;

public class AppDomain {
    private static final AppDomain current = new AppDomain();
    public static AppDomain get_CurrentDomain() { return current; }
    public cs.System.Reflection.Assembly[] GetAssemblies() {
        return new cs.System.Reflection.Assembly[]{cs.System.Reflection.Assembly.game()};
    }
}
