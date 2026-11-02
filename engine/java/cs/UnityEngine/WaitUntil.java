package cs.UnityEngine;

public final class WaitUntil extends CustomYieldInstruction {
    private final cs.System.Func_1 p;
    public WaitUntil(cs.System.Func_1 p) { this.p = p; }
    @Override public boolean get_keepWaiting() { return !cs.rt.Rt.unboxZ(p.Invoke()); }
}
