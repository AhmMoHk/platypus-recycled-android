package cs.Blackdrop.Platforms;

public class Platform {
    private static final BasePlatformSystem instance = new BasePlatformSystem();
    public static BasePlatformSystem get_Instance() { return instance; }
}
