package cs.UnityEngine;

/** UnityEngine.Debug: logs go to the host platform and Application.logMessageReceived. */
public final class Debug {
    private Debug() {}
    /** LogType: Error 0, Assert 1, Warning 2, Log 3, Exception 4 */
    public static void log(int type, String msg) {
        Host.get().log(type, msg);
        Application.dispatchLog(msg, "", type);
    }

    static String str(Object o) { return o == null ? "Null" : cs.System.ObjectX.ToString(o); }

    public static void Log(Object m) { log(3, str(m)); }
    public static void Log(Object m, UObject ctx) { log(3, str(m)); }
    public static void LogFormat(String f, Object[] a) { log(3, cs.System.StringX.Format(f, a)); }
    public static void LogWarning(Object m) { log(2, str(m)); }
    public static void LogWarning(Object m, UObject ctx) { log(2, str(m)); }
    public static void LogWarningFormat(String f, Object[] a) { log(2, cs.System.StringX.Format(f, a)); }
    public static void LogError(Object m) { log(0, str(m)); }
    public static void LogError(Object m, UObject ctx) { log(0, str(m)); }
    public static void LogErrorFormat(String f, Object[] a) { log(0, cs.System.StringX.Format(f, a)); }
    public static void LogException(Throwable e) { logException(e, null); }
    public static void LogException(Throwable e, UObject ctx) { logException(e, ctx); }
    public static void LogAssertion(Object m) { log(1, str(m)); }
    public static void Assert(boolean c) { if (!c) log(1, "Assertion failed"); }
    public static void Assert(boolean c, Object m) { if (!c) log(1, str(m)); }
    public static void Assert(boolean c, String m) { if (!c) log(1, m); }
    public static void Break() {}
    public static boolean get_isDebugBuild() { return false; }
    public static void DrawLine(Vector3 a, Vector3 b) {}
    public static void DrawLine(Vector3 a, Vector3 b, Color c) {}
    public static void DrawLine(Vector3 a, Vector3 b, Color c, float d) {}
    public static void DrawLine(Vector3 a, Vector3 b, Color c, float d, boolean z) {}
    public static void DrawRay(Vector3 a, Vector3 b) {}
    public static void DrawRay(Vector3 a, Vector3 b, Color c) {}
    public static void DrawRay(Vector3 a, Vector3 b, Color c, float d) {}
    public static void DrawRay(Vector3 a, Vector3 b, Color c, float d, boolean z) {}

    /** from background threads: log only */
    public static void logExceptionAnyThread(Throwable e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        e.printStackTrace(new java.io.PrintWriter(sw));
        Host.get().log(4, sw.toString());
    }

    static void logException(Throwable e, UObject ctx) {
        Throwable t = e;
        String msg;
        if (t instanceof cs.System.Exception) msg = cs.System.ObjectX.ToString(t);
        else msg = t.toString();
        java.io.StringWriter sw = new java.io.StringWriter();
        t.printStackTrace(new java.io.PrintWriter(sw));
        String st = sw.toString();
        Host.get().log(4, msg + (ctx != null ? " [" + ctx.get_name() + "]" : "") + "\n" + st);
        Application.dispatchLog(msg, st, 4);
    }
}
