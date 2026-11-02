package cs.System.Threading;

public class EventWaitHandle extends WaitHandle {
    public EventWaitHandle(boolean initial, int mode) { super(initial, mode == 0); }
    protected EventWaitHandle(boolean initial, boolean autoReset) { super(initial, autoReset); }
    public synchronized boolean Set() { signaled = true; notifyAll(); return true; }
    public synchronized boolean Reset() { signaled = false; return true; }
}
