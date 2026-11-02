package cs.System.Threading;

public class Thread {
    private static final ThreadLocal<Thread> current = new ThreadLocal<>();
    final java.lang.Thread t;
    String name;
    public Thread(final ThreadStart start) {
        t = new java.lang.Thread(new Runnable() {
            public void run() {
                current.set(Thread.this);
                try { start.Invoke(); } catch (Throwable e) { cs.UnityEngine.Debug.logExceptionAnyThread(e); }
            }
        });
    }
    private Thread(java.lang.Thread t) { this.t = t; }
    public static Thread get_CurrentThread() {
        Thread c = current.get();
        if (c == null) { c = new Thread(java.lang.Thread.currentThread()); current.set(c); }
        return c;
    }
    public void Start() { t.start(); }
    public void Join() { try { t.join(); } catch (InterruptedException ignored) {} }
    public boolean Join(int ms) { try { t.join(ms); } catch (InterruptedException ignored) {} return !t.isAlive(); }
    public boolean get_IsAlive() { return t.isAlive(); }
    public boolean get_IsBackground() { return t.isDaemon(); }
    public void set_IsBackground(boolean v) { if (!t.isAlive()) t.setDaemon(v); }
    public String get_Name() { return name; }
    public void set_Name(String n) { name = n; t.setName(n == null ? "thread" : n); }
    public int get_ManagedThreadId() { return (int) t.getId(); }
    public void Abort() { t.interrupt(); }
    public void Interrupt() { t.interrupt(); }
    public cs.System.Globalization.CultureInfo get_CurrentCulture() { return cs.System.Globalization.CultureInfo.get_CurrentCulture(); }
    public void set_CurrentCulture(cs.System.Globalization.CultureInfo c) {}
    public cs.System.Globalization.CultureInfo get_CurrentUICulture() { return cs.System.Globalization.CultureInfo.get_CurrentCulture(); }
    public static void Sleep(int ms) { try { java.lang.Thread.sleep(Math.max(0, ms)); } catch (InterruptedException ignored) {} }
    public static void Sleep(cs.System.TimeSpan t) { Sleep((int) t.get_TotalMilliseconds()); }
    public static void MemoryBarrier() {}
    public static boolean Yield() { java.lang.Thread.yield(); return true; }
}
