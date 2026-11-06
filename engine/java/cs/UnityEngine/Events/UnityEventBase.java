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

    /** C# parameter type a persistent call's mode passes (PersistentListenerMode: 2 object, 3 int, 4 float, 5 string, 6 bool) */
    private static String modeType(int mode) {
        switch (mode) { case 3: return "i4"; case 4: return "r4"; case 5: return "string"; case 6: return "bool"; default: return null; }
    }

    private static java.lang.reflect.Method find(Object target, String name, int nparams, int mode, Object arg) {
        for (Class<?> k = target.getClass(); k != null; k = k.getSuperclass()) {
            String jn = null;
            String[] pts = null;
            int best = Integer.MIN_VALUE;
            try {
                java.lang.reflect.Field f = k.getDeclaredField("$mmeta");
                String want = modeType(mode);
                for (Object[] mm : (Object[][]) f.get(null)) {
                    String[] p = (String[]) mm[4];
                    if (!name.equals(mm[1]) || p.length != nparams || (((Integer) mm[2]) & 1) != 0) continue;
                    // overloads with the same parameter count: prefer the one taking the call's argument type
                    int score = 0;
                    if (nparams == 1 && want != null) score = want.equals(p[0]) ? 2 : -2;
                    else if (nparams == 1 && arg != null) {
                        Class<?> pc = cs.System.Reflection.Meta.csClassPublic(p[0]);
                        score = pc == null ? 0 : pc.isInstance(arg) ? 2 : -2;
                    }
                    if (score > best) { best = score; jn = (String) mm[0]; pts = p; }
                }
            } catch (NoSuchFieldException ignored) {
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            if (jn != null) {
                java.lang.reflect.Method m = cs.System.Reflection.Meta.findJavaPublic(k, jn, nparams, pts);
                if (m != null) { m.setAccessible(true); return m; }
            }
            for (java.lang.reflect.Method m : k.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterTypes().length == nparams && !m.isBridge()) {
                    if (nparams == 1 && arg != null && !cs.rt.Rt.instOf(cs.System.Type.of(m.getParameterTypes()[0]), arg)) continue;
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
                c.resolved = find(c.target, c.method, a.length, c.mode, a.length == 1 ? a[0] : null);
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
