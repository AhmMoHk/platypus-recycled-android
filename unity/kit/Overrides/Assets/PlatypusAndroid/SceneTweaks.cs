// Platypus Reclayed Android port: scene adjustments made as each scene loads.
//  - PC-only bits with no meaning on a phone are switched off (the Steam Workshop link of the mods menu, the Xbox
//    gamer tag).
//  - One player: everything belonging to player 2 is hidden (score, timer and lives in the HUD with its
//    "Press Fire To Join", and the second panel of the ship screen with its sleeping ship).
//  - The ship screen's player 1 panel gets the ship row (ShipCarousel).
using System.Collections.Generic;
using System.Reflection;
using UnityEngine;
using UnityEngine.SceneManagement;

namespace PlatypusAndroid
{
    public static class SceneTweaks
    {
        static readonly HashSet<string> Hidden = new HashSet<string> { "Steam Workshop Link", "XboxGamerTag" };
        static readonly FieldInfo livesIndex = typeof(DisplayPlayerLives).GetField("m_playerIndex", BindingFlags.NonPublic | BindingFlags.Instance);

        [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.AfterSceneLoad)]
        static void Boot()
        {
            SceneManager.sceneLoaded += (scene, mode) => Apply(scene);
            for (int i = 0; i < SceneManager.sceneCount; i++) Apply(SceneManager.GetSceneAt(i));
        }

        static void Apply(Scene scene)
        {
            if (!scene.isLoaded) return;
            foreach (GameObject root in scene.GetRootGameObjects())
            {
                foreach (Transform t in root.GetComponentsInChildren<Transform>(true))
                    if (Hidden.Contains(t.name)) t.gameObject.SetActive(false);

                foreach (DisplayPlayerLives d in root.GetComponentsInChildren<DisplayPlayerLives>(true))
                    if (livesIndex != null && (int)livesIndex.GetValue(d) != 0) d.gameObject.SetActive(false);
                foreach (PlayerSpecificHudElement h in root.GetComponentsInChildren<PlayerSpecificHudElement>(true))
                    if (h.playerIndex != 0) h.gameObject.SetActive(false);
                foreach (ShipPanelScript p in root.GetComponentsInChildren<ShipPanelScript>(true))
                {
                    if (p.playerIndex != 0) p.gameObject.SetActive(false);
                    else if (p.GetComponent<ShipCarousel>() == null) p.gameObject.AddComponent<ShipCarousel>();
                }
            }
        }
    }
}
