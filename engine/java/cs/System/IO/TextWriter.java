package cs.System.IO;

public abstract class TextWriter implements cs.System.IDisposable {
    public abstract void Write(String s);
    public void Write(char c) { Write(String.valueOf(c)); }
    public void Write(Object o) { if (o != null) Write(cs.System.ObjectX.ToString(o)); }
    public void Write(String f, Object a) { Write(cs.rt.Fmt.compose(f, new Object[]{a})); }
    public void WriteLine() { Write("\n"); }
    public void WriteLine(String s) { Write(s == null ? "\n" : s + "\n"); }
    public void WriteLine(Object o) { WriteLine(o == null ? "" : cs.System.ObjectX.ToString(o)); }
    public void WriteLine(String f, Object a) { WriteLine(cs.rt.Fmt.compose(f, new Object[]{a})); }
    public void WriteLine(String f, Object a, Object b) { WriteLine(cs.rt.Fmt.compose(f, new Object[]{a, b})); }
    public void Flush() {}
    public void Close() { Dispose(); }
    public void Dispose() { Flush(); }
}
