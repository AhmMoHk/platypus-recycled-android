package cs.UnityEngine.Events;

public class UnityEvent extends UnityEventBase {
    private static final Object[] NOARGS = new Object[0];
    public UnityEvent() {}
    public void $ctor_UnityEvent() {}
    public void AddListener(UnityAction a) { $add(a); }
    public void RemoveListener(UnityAction a) { $remove(a); }
    public void Invoke() { $invoke(NOARGS); }
}
