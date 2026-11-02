package cs.System.Threading;

public class ManualResetEvent extends EventWaitHandle {
    public ManualResetEvent(boolean initial) { super(initial, false); }
}
