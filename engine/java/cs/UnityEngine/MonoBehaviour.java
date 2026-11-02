package cs.UnityEngine;

public class MonoBehaviour extends Behaviour {
    // lifecycle state (see Eng)
    boolean $awoken, $started, $enabledCalled;
    int $order;          // script execution order
    long $seq;           // creation sequence
    final java.util.ArrayList<Coroutine> $coroutines = new java.util.ArrayList<>(0);

    // message hooks (overridden by translated classes that declare the message)
    public Object $Awake() { return null; }
    public Object $Start() { return null; }
    public Object $Update() { return null; }
    public Object $FixedUpdate() { return null; }
    public Object $LateUpdate() { return null; }
    public Object $OnEnable() { return null; }
    public Object $OnDisable() { return null; }
    public Object $OnDestroy() { return null; }
    public Object $OnGUI() { return null; }
    public Object $OnValidate() { return null; }
    public Object $Reset() { return null; }
    public Object $OnApplicationQuit() { return null; }
    public Object $OnApplicationFocus(boolean f) { return null; }
    public Object $OnApplicationPause(boolean p) { return null; }
    public Object $OnTriggerEnter2D(Collider2D c) { return null; }
    public Object $OnTriggerExit2D(Collider2D c) { return null; }
    public Object $OnTriggerStay2D(Collider2D c) { return null; }
    public Object $OnCollisionEnter2D(Collision2D c) { return null; }
    public Object $OnCollisionExit2D(Collision2D c) { return null; }
    public Object $OnCollisionStay2D(Collision2D c) { return null; }
    public Object $OnBecameVisible() { return null; }
    public Object $OnBecameInvisible() { return null; }
    public Object $OnRectTransformDimensionsChange() { return null; }
    public Object $OnTransformParentChanged() { return null; }
    public Object $OnTransformChildrenChanged() { return null; }
    public Object $OnCanvasGroupChanged() { return null; }
    public Object $OnDidApplyAnimationProperties() { return null; }
    public Object $OnRenderObject() { return null; }
    public Object $OnWillRenderObject() { return null; }
    public Object $OnPreRender() { return null; }
    public Object $OnPostRender() { return null; }

    public Coroutine StartCoroutine(cs.System.Collections.IEnumerator e) { return Eng.startCoroutine(this, e); }
    public Coroutine StartCoroutine(String method) { return Eng.startCoroutine(this, (cs.System.Collections.IEnumerator) Eng.invokeByName(this, method, null)); }
    public Coroutine StartCoroutine(String method, Object arg) { return Eng.startCoroutine(this, (cs.System.Collections.IEnumerator) Eng.invokeByName(this, method, arg)); }
    public void StopCoroutine(cs.System.Collections.IEnumerator e) { Eng.stopCoroutine(this, e); }
    public void StopCoroutine(Coroutine c) { Eng.stopCoroutine(this, c); }
    public void StopCoroutine(String method) { Eng.stopCoroutineByName(this, method); }
    public void StopAllCoroutines() { Eng.stopAllCoroutines(this); }

    public void Invoke(String method, float time) { Eng.invoke(this, method, time, -1); }
    public void InvokeRepeating(String method, float time, float rate) { Eng.invoke(this, method, time, rate); }
    public void CancelInvoke() { Eng.cancelInvoke(this, null); }
    public void CancelInvoke(String method) { Eng.cancelInvoke(this, method); }
    public boolean IsInvoking(String method) { return Eng.isInvoking(this, method); }
    public boolean IsInvoking() { return Eng.isInvoking(this, null); }
    public boolean get_useGUILayout() { return false; }
    public void set_useGUILayout(boolean v) {}
    public static void print(Object o) { Debug.Log(o); }
}
