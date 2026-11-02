package cs.System.Threading;

public abstract class WaitHandle implements cs.System.IDisposable {
    protected boolean signaled;
    protected final boolean autoReset;
    protected WaitHandle(boolean initial, boolean autoReset) { signaled = initial; this.autoReset = autoReset; }
    public synchronized boolean WaitOne() { return WaitOne(-1); }
    public synchronized boolean WaitOne(int ms) {
        long end = ms < 0 ? Long.MAX_VALUE : System.currentTimeMillis() + ms;
        while (!signaled) {
            long left = end - System.currentTimeMillis();
            if (left <= 0) return false;
            try { wait(ms < 0 ? 0 : left); } catch (InterruptedException e) { return false; }
        }
        if (autoReset) signaled = false;
        return true;
    }
    public void Close() {}
    public void Dispose() {}
}
