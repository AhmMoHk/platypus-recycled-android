// Platypus Reclayed Android port: the game writes its save files only when a loading screen starts. Progress made
// inside a level (an area completed, the checkpoint to continue from) stayed in memory until then, and Android ends
// a game in the background without warning (swiped away, memory needed): that progress was lost, so the next area
// was still locked on return. Now the save files are also written when an area is completed and whenever the game
// goes to the background.
using System;
using Blackdrop.Platforms;
using UnityEngine;

namespace PlatypusAndroid
{
    public static class SaveGuard
    {
        static float last = -10f;

        public static void Flush(string why)
        {
            try
            {
                if (Platform.Instance == null) return;
                if (Time.unscaledTime - last < 0.5f) return;              // twice in a row (pause and focus) is once
                last = Time.unscaledTime;
                Platform.Instance.CloudStore?.Save();
                Platform.Instance.LocalStore?.Save();
                Debug.Log("[Save] written: " + why);
            }
            catch (Exception e) { Debug.LogWarning("[Save] " + why + ": " + e.Message); }
        }
    }
}
