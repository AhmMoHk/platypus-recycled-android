package cs.Rewired;

import java.util.HashMap;

/**
 * Single player input: actions read from the touch controls (and keyboard on desktop) through the controllers
 * assigned to this player. Actions: MoveHorizontal, MoveVertical, Fire, Pause, UIHorizontal, UIVertical,
 * UISubmit, UICancel, UISkinChange.
 */
public class Player {
    public final int id;
    public final String name, descriptiveName;
    public Player$ControllerHelper controllers;
    final HashMap<String, float[]> axes = new HashMap<>();       // action -> {value, prev}
    final java.util.ArrayList<Object[]> eventDelegates = new java.util.ArrayList<>();

    Player(int id, String name) {
        this.id = id;
        this.name = name;
        descriptiveName = name;
        controllers = new Player$ControllerHelper(this);
    }
    public int get_id() { return id; }
    public String get_name() { return name; }
    public String get_descriptiveName() { return descriptiveName; }
    public boolean get_isPlaying() { return true; }

    private float[] slot(String a) {
        float[] s = axes.get(a);
        if (s == null) { s = new float[2]; axes.put(a, s); }
        return s;
    }
    public float GetAxis(String a) { return slot(a)[0]; }
    public float GetAxisRaw(String a) { return slot(a)[0]; }
    public float GetAxisPrev(String a) { return slot(a)[1]; }
    public float GetAxisDelta(String a) { float[] s = slot(a); return s[0] - s[1]; }
    public boolean GetButton(String a) { return slot(a)[0] != 0; }
    public boolean GetButtonDown(String a) { float[] s = slot(a); return s[0] != 0 && s[1] == 0; }
    public boolean GetButtonUp(String a) { float[] s = slot(a); return s[0] == 0 && s[1] != 0; }
    public boolean GetButtonPrev(String a) { return slot(a)[1] != 0; }
    public boolean GetNegativeButton(String a) { return slot(a)[0] < 0; }
    public boolean GetNegativeButtonDown(String a) { float[] s = slot(a); return s[0] < 0 && s[1] >= 0; }
    public boolean GetAnyButton() { for (float[] s : axes.values()) if (s[0] != 0) return true; return false; }
    public boolean GetAnyButtonDown() { for (float[] s : axes.values()) if (s[0] != 0 && s[1] == 0) return true; return false; }
    public void SetVibration(int motor, float level, float duration) {}
    public void StopVibration() {}
    public void AddInputEventDelegate(cs.System.Action_1 cb, int updateLoop, int eventType) { eventDelegates.add(new Object[]{cb, eventType, null}); }
    public void AddInputEventDelegate(cs.System.Action_1 cb, int updateLoop, int eventType, String action) { eventDelegates.add(new Object[]{cb, eventType, action}); }
    public void RemoveInputEventDelegate(cs.System.Action_1 cb, int updateLoop, int eventType) {
        for (int i = eventDelegates.size() - 1; i >= 0; i--) if (eventDelegates.get(i)[0].equals(cb)) eventDelegates.remove(i);
    }
    public void RemoveInputEventDelegate(cs.System.Action_1 cb) { RemoveInputEventDelegate(cb, 0, 0); }

    /** per frame: copy the sampled action values for the controllers this player owns */
    void update(HashMap<String, Float> touch, HashMap<String, Float> keys) {
        boolean hasTouch = controllers.controllers.Contains(ReInput.touch), hasKeys = controllers.controllers.Contains(ReInput.keyboard);
        String[] names = ReInput.ACTIONS;
        for (String a : names) {
            float[] s = slot(a);
            s[1] = s[0];
            float v = 0;
            if (hasTouch && touch.containsKey(a)) v = touch.get(a);
            if (hasKeys && keys.containsKey(a) && Math.abs(keys.get(a)) > Math.abs(v)) v = keys.get(a);
            s[0] = v;
        }
        if (hasTouch && ReInput.touch.anyDown) controllers.setLastActive(ReInput.touch);
        else if (hasKeys && ReInput.keyboard.anyDown) controllers.setLastActive(ReInput.keyboard);
        // event delegates (InputActionEventType: ButtonJustPressed 3)
        if (!eventDelegates.isEmpty()) {
            for (Object[] e : new java.util.ArrayList<>(eventDelegates)) {
                for (String a : names) {
                    if (e[2] != null && !a.equals(e[2])) continue;
                    if ((Integer) e[1] == 3 && GetButtonDown(a) || (Integer) e[1] == 0 && GetButton(a)) {
                        InputActionEventData d = new InputActionEventData();
                        d.player = this;
                        d.action = a;
                        ((cs.System.Action_1) e[0]).Invoke(d);
                    }
                }
            }
        }
    }
}
