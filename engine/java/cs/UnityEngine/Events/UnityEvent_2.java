package cs.UnityEngine.Events;

public class UnityEvent_2 extends UnityEventBase {
    public UnityEvent_2() {}
    public void $ctor_UnityEvent_2() {}
    public void AddListener(UnityAction_2 a) { $add(a); }
    public void RemoveListener(UnityAction_2 a) { $remove(a); }
    public void Invoke(Object a0, Object a1) { $invoke(new Object[]{a0, a1}); }
}
