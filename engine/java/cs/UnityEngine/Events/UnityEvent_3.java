package cs.UnityEngine.Events;

public class UnityEvent_3 extends UnityEventBase {
    public UnityEvent_3() {}
    public void $ctor_UnityEvent_3() {}
    public void AddListener(UnityAction_3 a) { $add(a); }
    public void RemoveListener(UnityAction_3 a) { $remove(a); }
    public void Invoke(Object a0, Object a1, Object a2) { $invoke(new Object[]{a0, a1, a2}); }
}
