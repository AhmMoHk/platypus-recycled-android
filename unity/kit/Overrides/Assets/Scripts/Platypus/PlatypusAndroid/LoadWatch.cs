// Platypus Reclayed Android port: the loading screen's steps in the log ([Load] lines), so a stall shows where it
// happens: films done, the next scene's loading progress every few seconds with the scenes that are loaded, and
// the scene being ready.
using System.Collections;
using System.Text;
using UnityEngine;
using UnityEngine.SceneManagement;

namespace PlatypusAndroid
{
    public static class LoadWatch
    {
        public static void Phase(string what) => Debug.Log($"[Load] {what} ({Scenes()})");

        /// <summary>Waits for a scene load like the game did, logging its progress.</summary>
        public static IEnumerator Scene(AsyncOperation op, int scene, ProgressBar bar)
        {
            if (op == null) { Debug.LogError($"[Load] scene {scene} could not be started ({Scenes()})"); yield break; }
            float start = Time.realtimeSinceStartup, next = start + 3f;
            Debug.Log($"[Load] loading scene {scene} (activation {op.allowSceneActivation}, priority {op.priority}, background {Application.backgroundLoadingPriority})");
            while (!op.isDone)
            {
                if (bar != null) bar.SetProgress(op.progress);
                if (Time.realtimeSinceStartup >= next)
                {
                    next += 3f;
                    Debug.Log($"[Load] scene {scene}: {op.progress * 100f:0}% after {Time.realtimeSinceStartup - start:0}s ({Scenes()})");
                }
                yield return null;
            }
            Debug.Log($"[Load] scene {scene} loaded in {Time.realtimeSinceStartup - start:0.0}s ({Scenes()})");
        }

        /// <summary>"MainMenuScene#1 loaded 12 roots, ..." for every scene the scene manager knows.</summary>
        public static string Scenes()
        {
            var sb = new StringBuilder();
            for (int i = 0; i < SceneManager.sceneCount; i++)
            {
                Scene s = SceneManager.GetSceneAt(i);
                if (sb.Length > 0) sb.Append(", ");
                sb.Append(s.name).Append('#').Append(s.buildIndex).Append(s.isLoaded ? $" loaded {s.rootCount} roots" : " not loaded");
                if (s == SceneManager.GetActiveScene()) sb.Append(" (active)");
            }
            return sb.ToString();
        }
    }
}
