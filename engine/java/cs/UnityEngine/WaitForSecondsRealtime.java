package cs.UnityEngine;

public class WaitForSecondsRealtime extends CustomYieldInstruction {
    private float waitTime;
    private double until = -1;
    public WaitForSecondsRealtime(float t) { waitTime = t; }
    public float get_waitTime() { return waitTime; }
    public void set_waitTime(float t) { waitTime = t; }
    @Override public boolean get_keepWaiting() {
        double now = Time.get_realtimeSinceStartupAsDouble();
        if (until < 0) until = now + waitTime;
        boolean w = now < until;
        if (!w) until = -1;
        return w;
    }
    @Override public void Reset() { until = -1; }
}
