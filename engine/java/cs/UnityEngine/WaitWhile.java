package cs.UnityEngine;

public final class WaitWhile extends CustomYieldInstruction {
    private final cs.System.Func_1 p;
    public WaitWhile(cs.System.Func_1 p) { this.p = p; }
    @Override public boolean get_keepWaiting() { return cs.rt.Rt.unboxZ(p.Invoke()); }
}
