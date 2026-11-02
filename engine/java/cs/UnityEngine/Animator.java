package cs.UnityEngine;

/** Minimal Animator: tracks state/trigger requests (the game only uses it for a couple of decorative animations). */
public class Animator extends Behaviour {
    static final java.util.ArrayList<Animator> active = new java.util.ArrayList<>();
    String state = "";
    float stateTime, speed = 1;
    final java.util.HashSet<String> triggers = new java.util.HashSet<>();
    final java.util.HashMap<String, Float> floats = new java.util.HashMap<>();
    final java.util.HashMap<String, Boolean> bools = new java.util.HashMap<>();
    public void Play(String s) { state = s; stateTime = 0; }
    public void Play(String s, int layer) { Play(s); }
    public void Play(String s, int layer, float t) { Play(s); }
    public void Play(int hash) {}
    public void SetTrigger(String t) { triggers.add(t); }
    public void SetTrigger(int id) {}
    public void ResetTrigger(String t) { triggers.remove(t); }
    public void ResetTrigger(int id) {}
    public void SetBool(String n, boolean v) { bools.put(n, v); }
    public boolean GetBool(String n) { Boolean b = bools.get(n); return b != null && b; }
    public void SetFloat(String n, float v) { floats.put(n, v); }
    public float GetFloat(String n) { Float f = floats.get(n); return f == null ? 0 : f; }
    public void SetInteger(String n, int v) { floats.put(n, (float) v); }
    public float get_speed() { return speed; }
    public void set_speed(float s) { speed = s; }
    public boolean get_hasBoundPlayables() { return false; }
    public void Update(float dt) { stateTime += dt * speed; }
    public void Rebind() {}
    public AnimatorStateInfo GetCurrentAnimatorStateInfo(int layer) {
        AnimatorStateInfo i = new AnimatorStateInfo();
        i.m_Length = 1;
        i.m_NormalizedTime = stateTime;
        i.m_Name = Animator.StringToHash(state);
        return i;
    }
    public static int StringToHash(String s) { return s.hashCode(); }
    static void $updateAll() { for (Animator a : active) if (a.get_isActiveAndEnabled()) a.stateTime += Time.get_deltaTime() * a.speed; }
    @Override public void $nativeEnable() { if (!active.contains(this)) active.add(this); }
    @Override public void $nativeDisable() { active.remove(this); }
    @Override public void $onDestroyNative() { active.remove(this); }
}
