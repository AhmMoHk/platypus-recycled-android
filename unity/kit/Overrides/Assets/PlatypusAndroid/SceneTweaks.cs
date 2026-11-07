// Platypus Reclayed Android port: PC-only bits of the menus that have no meaning on a phone are switched off as each
// scene loads (the Steam Workshop link of the mods menu, the Xbox gamer tag).
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.SceneManagement;

namespace PlatypusAndroid
{
    public static class SceneTweaks
    {
        static readonly HashSet<string> Hidden = new HashSet<string> { "Steam Workshop Link", "XboxGamerTag" };

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
                foreach (Transform t in root.GetComponentsInChildren<Transform>(true))
                    if (Hidden.Contains(t.name)) t.gameObject.SetActive(false);
        }
    }
}
