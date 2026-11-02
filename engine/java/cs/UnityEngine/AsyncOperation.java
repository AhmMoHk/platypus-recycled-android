package cs.UnityEngine;

public class AsyncOperation extends YieldInstruction {
    boolean isDone;
    float progress;
    boolean allowSceneActivation = true;
    int priority;
    private cs.System.Action_1 completed;
    public AsyncOperation() {}
    public boolean get_isDone() { return isDone; }
    public float get_progress() { return progress; }
    public boolean get_allowSceneActivation() { return allowSceneActivation; }
    public void set_allowSceneActivation(boolean v) { allowSceneActivation = v; }
    public int get_priority() { return priority; }
    public void set_priority(int p) { priority = p; }
    public void add_completed(cs.System.Action_1 a) {
        if (isDone) { a.Invoke(this); return; }
        completed = (cs.System.Action_1) cs.System.Delegate.Combine(completed, a);
    }
    public void remove_completed(cs.System.Action_1 a) { completed = (cs.System.Action_1) cs.System.Delegate.Remove(completed, a); }
    public void $finish() { finish(); }
    void finish() {
        isDone = true;
        progress = 1f;
        if (completed != null) completed.Invoke(this);
    }
}
