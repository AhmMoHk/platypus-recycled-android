package cs.System;

public final class Environment {
    private Environment() {}
    public static final String NewLine = "\n";
    public static String get_NewLine() { return "\n"; }
    public static void Exit(int code) { cs.UnityEngine.Application.Quit(); }
    public static String[] GetCommandLineArgs() { return new String[]{"Platypus"}; }
    public static String GetEnvironmentVariable(String n) { return null; }
    public static int get_CurrentManagedThreadId() { return (int) Thread.currentThread().getId(); }
    public static int get_TickCount() { return (int) (System.nanoTime() / 1000000L); }
    public static int get_ProcessorCount() { return Runtime.getRuntime().availableProcessors(); }
    public static String get_StackTrace() { return ""; }
    public static String GetFolderPath(int f) { return cs.UnityEngine.Application.get_persistentDataPath(); }
}
