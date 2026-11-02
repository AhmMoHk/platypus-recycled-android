package cs.System;

public class Activator {
    public static Object CreateInstance(Type t) { return t.createInstance(); }
    public static Object CreateInstance(Type t, Object[] args) {
        if (args == null || args.length == 0) return t.createInstance();
        throw cs.rt.Rt.todo("Activator.CreateInstance with arguments");
    }
}
