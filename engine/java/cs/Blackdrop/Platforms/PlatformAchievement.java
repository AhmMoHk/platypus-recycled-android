package cs.Blackdrop.Platforms;

public class PlatformAchievement {
    public String SteamworksID, EpicID, XboxGameCore;
    public int PS4ID, PS5ID;
    public PlatformAchievement() {}
    String key() { return "ach_" + (SteamworksID != null ? SteamworksID : EpicID != null ? EpicID : Integer.toString(PS5ID)); }
}
