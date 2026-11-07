// Platypus Reclayed Android port: the intro films of the loading screen (Claymatic logo, then the Platypus intro).
// On PC they start paused (playback speed 0) while the game loads and are un-paused later. Android's video decoder
// does not hold a film at speed 0, so the intro ran on, unseen, during loading and appeared late, half or not at
// all. Here both films are only prepared while loading and are started from their first frame when their turn
// comes. They keep the game's own framing (the whole film, black around it on a wider screen).
// The game ends a film after its length or at its last frame. Unity 6 on Android can report a length and frame count
// of 0 for these films, which cut them after a second; then the clip's own figures are used, and else the film runs
// until it stops playing (at most 20 s). The log gets a [Splash] line per film.
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
                v.skipOnDrop = true;
                v.waitForFirstFrame = true;
            }
        }

        /// <summary>Starts a prepared film from its first frame (waits up to 6 s for the decoder).</summary>
        public static IEnumerator PlayFromStart(VideoPlayer v)
        {
            if (v == null) yield break;
            if (!v.isPrepared) v.Prepare();
            float start = Time.realtimeSinceStartup, until = start + 6f;
            while (!v.isPrepared && Time.realtimeSinceStartup < until) yield return null;
            v.playbackSpeed = 1f;
            v.frame = 0;
            v.Play();
            until = Time.realtimeSinceStartup + 3f;
            while (v.frame < 0 && Time.realtimeSinceStartup < until) yield return null;   // the first frame is on screen
            VideoClip c = v.clip;
            Debug.Log($"[Splash] {v.name}: {(c != null ? c.name : "no clip")} prepared {v.isPrepared} after {Time.realtimeSinceStartup - start:0.0}s, " +
                      $"player {v.length:0.00}s / {v.frameCount} frames, clip {(c != null ? c.length : 0):0.00}s / {(c != null ? c.frameCount : 0)} frames, " +
                      $"{v.width}x{v.height}, playing {v.isPlaying}");
        }

        /// <summary>The film's length in seconds (the game waits this long plus a second at most).</summary>
        public static double Length(VideoPlayer v)
        {
            if (v == null) return 0;
            if (v.length > 0.1) return v.length;
            if (v.clip != null && v.clip.length > 0.1) return v.clip.length;
            return 20.0;
        }

        /// <summary>The film's last frame; unknown = never reached (the film then ends by time or when it stops).</summary>
        public static long LastFrame(VideoPlayer v)
        {
            if (v == null) return long.MaxValue;
            ulong n = v.frameCount;
            if (n <= 1 && v.clip != null) n = v.clip.frameCount;
            return n > 1 ? (long)n - 1 : long.MaxValue;
        }

        /// <summary>True while the film still runs: before its last frame and not stopped by itself.</summary>
        public static bool Running(VideoPlayer v, long lastFrame) => v != null && v.frame < lastFrame && v.isPlaying;
    }
}
