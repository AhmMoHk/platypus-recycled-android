package cs.System.Threading;

public class AutoResetEvent extends EventWaitHandle {
    public AutoResetEvent(boolean initial) { super(initial, true); }
}
