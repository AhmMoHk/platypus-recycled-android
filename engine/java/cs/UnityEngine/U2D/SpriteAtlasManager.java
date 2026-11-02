package cs.UnityEngine.U2D;

public final class SpriteAtlasManager {
    private SpriteAtlasManager() {}
    static cs.System.Delegate registered, requested;
    public static void add_atlasRegistered(cs.System.Action_1 a) { registered = cs.System.Delegate.Combine(registered, a); }
    public static void remove_atlasRegistered(cs.System.Action_1 a) { registered = cs.System.Delegate.Remove(registered, a); }
    public static void add_atlasRequested(cs.System.Action_2 a) { requested = cs.System.Delegate.Combine(requested, a); }
    public static void remove_atlasRequested(cs.System.Action_2 a) { requested = cs.System.Delegate.Remove(requested, a); }
}
