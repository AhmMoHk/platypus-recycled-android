package cs.UnityEngine;

/** RuntimeInitializeLoadType: AfterSceneLoad 0, BeforeSceneLoad 1, AfterAssembliesLoaded 2, BeforeSplashScreen 3, SubsystemRegistration 4 */
public class RuntimeInitializeOnLoadMethodAttribute extends cs.UnityEngine.Scripting.PreserveAttribute {
    public final int loadType;
    public RuntimeInitializeOnLoadMethodAttribute() { loadType = 0; }
    public RuntimeInitializeOnLoadMethodAttribute(int t) { loadType = t; }
    public int get_loadType() { return loadType; }
}
