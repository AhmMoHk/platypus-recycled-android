package cs.System;

public final class Console {
    private Console() {}
    private static final cs.System.IO.TextWriter OUT = new cs.System.IO.TextWriter() {
        public void Write(String s) { cs.UnityEngine.Debug.log(0, s); }
    };
    public static void WriteLine() { cs.UnityEngine.Debug.log(0, ""); }
    public static void WriteLine(String s) { cs.UnityEngine.Debug.log(0, s); }
    public static void WriteLine(Object o) { cs.UnityEngine.Debug.log(0, ObjectX.str(o)); }
    public static void WriteLine(String f, Object a) { cs.UnityEngine.Debug.log(0, cs.rt.Fmt.compose(f, new Object[]{a})); }
    public static void Write(String s) { cs.UnityEngine.Debug.log(0, s); }
    public static cs.System.IO.TextWriter get_Error() { return OUT; }
    public static cs.System.IO.TextWriter get_Out() { return OUT; }
    public static cs.System.IO.Stream OpenStandardError() { return new cs.System.IO.MemoryStream(); }
    public static cs.System.IO.Stream OpenStandardInput() { return new cs.System.IO.MemoryStream(new byte[0]); }
    public static cs.System.IO.Stream OpenStandardOutput() { return new cs.System.IO.MemoryStream(); }
    public static String ReadLine() { return null; }
}
