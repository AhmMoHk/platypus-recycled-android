package cs.UnityEngine.UIElements;

/** UI Toolkit runtime panels: the game uses only UGUI, so there are never any panels. */
@SuppressWarnings({"unchecked", "rawtypes", "unused"})
public class UIElementsRuntimeUtility {
    public UIElementsRuntimeUtility() {}
    public static int s_ResolvedSortingIndexMax;
    private static cs.UnityEngine.UObject active;
    public static cs.UnityEngine.UIElements.EventBase CreateEvent(cs.UnityEngine.Event a0) { return null; }
    public static cs.System.Collections.Generic.List_1 GetSortedPlayerPanels() { return new cs.System.Collections.Generic.List_1(); }
    public static void RegisterEventSystem(cs.UnityEngine.UObject a0) { active = a0; }
    public static void UnregisterEventSystem(cs.UnityEngine.UObject a0) { if (active == a0) active = null; }
    public static void add_onCreatePanel(cs.System.Action_1 a0) {}
    public static void remove_onCreatePanel(cs.System.Action_1 a0) {}
    public static cs.UnityEngine.UObject get_activeEventSystem() { return active; }
}
