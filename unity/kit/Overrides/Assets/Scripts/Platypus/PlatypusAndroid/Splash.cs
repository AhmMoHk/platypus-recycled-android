// Platypus Reclayed Android port: the intro films of the loading screen (Claymatic logo, then the Platypus intro).
// On PC they start paused (playback speed 0) while the game loads and are un-paused later. Android's video decoder
// does not hold a film at speed 0, so the intro ran on, unseen, during loading and appeared late, half or not at
// all. Here both films are only prepared while loading and are started from their first frame when their turn
// comes, and they fill the whole screen (cropping the edges) instead of sitting inside black bars.
using System.Collections;
using UnityEngine;
using UnityEngine.Video;

namespace PlatypusAndroid
{
    public static class Splash
    {
        public static void Setup(params VideoPlayer[] players)
        {
            foreach (VideoPlayer v in players)
            {
                if (v == null) continue;
                v.playOnAwake = false;
                v.aspectRatio = VideoAspectRatio.FitOutside;
                v.skipOnDrop = true;
                v.waitForFirstFrame = true;
            }
        }

        /// <summary>Starts a prepared film from its first frame (waits up to 6 s for the decoder).</summary>
        public static IEnumerator PlayFromStart(VideoPlayer v)
        {
            if (v == null) yield break;
            if (!v.isPrepared) v.Prepare();
            float until = Time.realtimeSinceStartup + 6f;
            while (!v.isPrepared && Time.realtimeSinceStartup < until) yield return null;
            v.playbackSpeed = 1f;
            v.frame = 0;
            v.Play();
            until = Time.realtimeSinceStartup + 3f;
            while (v.frame < 0 && Time.realtimeSinceStartup < until) yield return null;   // the first frame is on screen
        }
    }
}
