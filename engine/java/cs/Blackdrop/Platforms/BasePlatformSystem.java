package cs.Blackdrop.Platforms;

import java.io.File;
import java.util.ArrayList;

/**
 * Android platform system: local saves, local achievements, locale from the device, and additional content
 * (Workshop mods shipped in <data>/Workshop/<id>) reported as plain local content (no Steam badge).
 * PlatformModel: Unknown 0 ...  Locale: en_gb 0, fr_fr 1, id_id 2, it_it 3, pt_pt 4, pt_br 5, es_es 6, es_mx 7, de_de 8,
 * pl_pl 9, ru_ru 10, ja_jp 11, zh_cn 12, zh_tw 13, ko_kr 14, th_th 15
 */
public class BasePlatformSystem {
    PlatformStore local, cloud;
    boolean achievementsInit;

    public BasePlatformSystem() {}
    public boolean IsPlatformInitialized() { return true; }
    public boolean IsGameInstallValid() { return true; }
    public boolean IsSteamDeck() { return false; }
    public int GetPlatformModel() { return 0; }
    public int GetCurrentSaveProfileId() { return 0; }
    public void LoadSaveProfile(int id) {}
    public PlatformStore get_LocalStore() { if (local == null) local = new PlatformStore(new File(cs.UnityEngine.Host.get().persistentDataPath(), "local.json")); return local; }
    public PlatformStore get_CloudStore() { if (cloud == null) cloud = new PlatformStore(new File(cs.UnityEngine.Host.get().persistentDataPath(), "cloud.json")); return cloud; }
    public boolean AchievementsInitialized() { return achievementsInit; }
    public boolean GetAchievementUnlocked(PlatformAchievement a) { return get_LocalStore().GetInt(a.key(), 0) != 0; }
    public void SetAchievementUnlocked(PlatformAchievement a) {
        if (GetAchievementUnlocked(a)) return;
        get_LocalStore().SetInt(a.key(), 1);
        get_LocalStore().Save();
    }
    public void ResetAchievement(PlatformAchievement a) { get_LocalStore().DeleteKey(a.key()); get_LocalStore().Save(); }
    public void StartActivity(int id) {}
    public void EndActivity(int id, int outcome, int a, int b) {}

    public int GetPlatformLocale() {
        java.util.Locale l = java.util.Locale.getDefault();
        String lang = l.getLanguage(), c = l.getCountry();
        switch (lang) {
            case "fr": return 1;
            case "id": case "in": return 2;
            case "it": return 3;
            case "pt": return "BR".equals(c) ? 5 : 4;
            case "es": return "ES".equals(c) ? 6 : 7;
            case "de": return 8;
            case "pl": return 9;
            case "ru": return 10;
            case "ja": return 11;
            case "zh": return "TW".equals(c) || "HK".equals(c) || "Hant".equals(l.getScript()) ? 13 : 12;
            case "ko": return 14;
            case "th": return 15;
            default: return 0;
        }
    }

    public BasePlatformSystem$ContentInfo[] GetAdditionalContent() {
        ArrayList<BasePlatformSystem$ContentInfo> r = new ArrayList<>();
        File ws = new File(cs.UnityEngine.Host.get().dataPath(), "Workshop");
        File[] items = ws.listFiles();
        if (items != null) {
            java.util.Arrays.sort(items);
            for (File f : items) {
                if (!f.isDirectory()) continue;
                BasePlatformSystem$ContentInfo ci = new BasePlatformSystem$ContentInfo();
                ci.contentPath = f.getAbsolutePath();
                ci.flags = 0;
                r.add(ci);
            }
        }
        return r.toArray(new BasePlatformSystem$ContentInfo[0]);
    }

    /** host boot: run [RuntimeInitializeAfterPlatformLoad] methods and fire achievement init */
    public void $afterLoad() {
        cs.UnityEngine.Boot.invokeAttributed(RuntimeInitializeAfterPlatformLoadAttribute.class);
        achievementsInit = true;
        cs.UnityEngine.Boot.invokeAttributed(PlatformAchievementsInitializedAttribute.class);
    }
}
