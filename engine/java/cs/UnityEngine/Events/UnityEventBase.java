package cs.UnityEngine.Events;

import cs.rt.Json;
import java.util.ArrayList;
import java.util.HashMap;

/** UnityEventBase: persistent (serialized) calls + runtime listeners. */
public abstract class UnityEventBase implements cs.UnityEngine.ISerializationCallbackReceiver {
    /** mode: 0 EventDefined, 1 Void, 2 Object, 3 Int, 4 Float, 5 String, 6 Bool; callState 0 Off */
    static final class Call {
        cs.UnityEngine.UObject target;
        String method, typeName;
        int mode, callState = 2;
        Object arg;
        java.lang.reflect.Method resolved;
        boolean failed;
    }

    final ArrayList<Call> persistent = new ArrayList<>();
    final ArrayList<cs.System.Delegate> listeners = new ArrayList<>();

    public UnityEventBase() {}
    public void $ctor_UnityEventBase() {}

    public void OnBeforeSerialize() {}
    public void OnAfterDeserialize() {}

    public void $loadPersistent(Object pc, cs.UnityEngine.Loader.Ctx ctx) {
        persistent.clear();
        if (!(pc instanceof HashMap)) return;
        Object calls = Json.map(pc).get("m_Calls");
        if (!(calls instanceof ArrayList)) return;
        for (Object o : Json.list(calls)) {
            HashMap<String, Object> m = Json.map(o);
            Call c = new Call();
            c.target = ctx.ref(m.get("m_Target"));
            c.method = (String) m.get("m_MethodName");
            c.typeName = m.get("m_TargetAssemblyTypeName") instanceof String ? (String) m.get("m_TargetAssemblyTypeName") : "";
            c.mode = Json.i(m.get("m_Mode"));
            c.callState = m.containsKey("m_CallState") ? Json.i(m.get("m_CallState")) : 2;
            HashMap<String, Object> a = m.get("m_Arguments") instanceof HashMap ? Json.map(m.get("m_Arguments")) : new HashMap<String, Object>();
            switch (c.mode) {
                case 2: c.arg = ctx.ref(a.get("m_ObjectArgument")); break;
                case 3: c.arg = Json.i(a.get("m_IntArgument")); break;
                case 4: c.arg = Json.f(a.get("m_FloatArgument")); break;
                case 5: c.arg = a.get("m_StringArgument") instanceof String ? a.get("m_StringArgument") : ""; break;
                case 6: c.arg = Json.b(a.get("m_BoolArgument")); break;
                default: c.arg = null;
            }
            persistent.add(c);
        }
    }

    public void $clonePersistent(UnityEventBase src, cs.UnityEngine.Eng.CloneCtx ctx) {
        persistent.clear();
        for (Call s : src.persistent) {
            Call c = new Call();
            c.target = (cs.UnityEngine.UObject) ctx.remap(s.target);
            c.method = s.method;
            c.typeName = s.typeName;
            c.mode = s.mode;
            c.callState = s.callState;
            c.arg = s.arg instanceof cs.UnityEngine.UObject ? ctx.remap(s.arg) : s.arg;
            persistent.add(c);
        }
    }

    private static java.lang.reflect.Method find(Object target, String name, int nparams) {
        for (Class<?> k = target.getClass(); k != null; k = k.getSuperclass()) {
            String jn = null;
            try {
                java.lang.reflect.Field f = k.getDeclaredField("$mmeta");
                for (Object[] mm : (Object[][]) f.get(null)) {
                    if (name.equals(mm[1]) && ((String[]) mm[4]).length == nparams && ((((Integer) mm[2]) & 1) == 0)) { jn = (String) mm[0]; break; }
                }
            } catch (NoSuchFieldException ignored) {
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            if (jn == null) jn = name;
            for (java.lang.reflect.Method m : k.getDeclaredMethods()) {
                if (m.getName().equals(jn) && m.getParameterTypes().length == nparams) {
                    m.setAccessible(true);
                    return m;
                }
            }
        }
        return null;
    }

    protected void $invoke(Object[] args) {
        for (int i = 0; i < persistent.size(); i++) {
            Call c = persistent.get(i);
            if (c.callState == 0 || c.failed) continue;
            if (!cs.UnityEngine.UObject.op_Implicit$boolean_(c.target)) continue;
            Object[] a = c.mode == 1 ? new Object[0] : c.mode == 0 ? args : new Object[]{c.arg};
            if (c.resolved == null) {
                c.resolved = find(c.target, c.method, a.length);
                if (c.resolved == null) {
                    c.failed = true;
                    cs.UnityEngine.Debug.LogWarning("UnityEvent: method " + c.method + " not found on " + c.target.getClass().getName());
                    continue;
                }
            }
            Class<?>[] pt = c.resolved.getParameterTypes();
            Object[] ja = new Object[a.length];
            for (int k = 0; k < a.length; k++) ja[k] = cs.System.Reflection.MethodBase.toJava(a[k], pt[k]);
            try {
                c.resolved.invoke(c.target, ja);
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw cs.rt.Rt.toThrowable(e.getCause());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        ArrayList<cs.System.Delegate> ls = new ArrayList<>(listeners);
        for (cs.System.Delegate d : ls) d.DynamicInvoke(args);
    }

    protected void $add(cs.System.Delegate d) { if (d != null) listeners.add(d); }
    protected void $remove(cs.System.Delegate d) {
        for (int i = listeners.size() - 1; i >= 0; i--) if (listeners.get(i).equals(d)) { listeners.remove(i); return; }
    }

    public void RemoveAllListeners() { listeners.clear(); }
    public int GetPersistentEventCount() { return persistent.size(); }
    public String GetPersistentMethodName(int i) { return persistent.get(i).method; }
    public cs.UnityEngine.UObject GetPersistentTarget(int i) { return persistent.get(i).target; }
    public void SetPersistentListenerState(int i, int state) { persistent.get(i).callState = state; }
    public int GetPersistentListenerState(int i) { return persistent.get(i).callState; }
}
