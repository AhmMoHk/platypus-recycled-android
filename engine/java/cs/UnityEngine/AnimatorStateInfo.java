package cs.UnityEngine;

public class AnimatorStateInfo implements cs.rt.Struct {
    public int m_Name, m_Path, m_FullPath, m_Tag, m_Loop;
    public float m_NormalizedTime, m_Length, m_Speed = 1, m_SpeedMultiplier = 1;
    public AnimatorStateInfo() {}
    public float get_length() { return m_Length; }
    public float get_normalizedTime() { return m_NormalizedTime; }
    public float get_speed() { return m_Speed; }
    public boolean get_loop() { return m_Loop != 0; }
    public boolean IsName(String n) { return m_Name == Animator.StringToHash(n); }
    public int get_shortNameHash() { return m_Name; }
    public int get_fullPathHash() { return m_FullPath; }
    public AnimatorStateInfo $copy() { AnimatorStateInfo a = new AnimatorStateInfo(); a.$set(this); return a; }
    public void $set(Object o) {
        AnimatorStateInfo a = (AnimatorStateInfo) o;
        m_Name = a.m_Name; m_Path = a.m_Path; m_FullPath = a.m_FullPath; m_Tag = a.m_Tag; m_Loop = a.m_Loop;
        m_NormalizedTime = a.m_NormalizedTime; m_Length = a.m_Length; m_Speed = a.m_Speed; m_SpeedMultiplier = a.m_SpeedMultiplier;
    }
    public void $clear() { $set(new AnimatorStateInfo()); }
    public static AnimatorStateInfo[] $newArray(int n) { AnimatorStateInfo[] a = new AnimatorStateInfo[n]; for (int i = 0; i < n; i++) a[i] = new AnimatorStateInfo(); return a; }
}
