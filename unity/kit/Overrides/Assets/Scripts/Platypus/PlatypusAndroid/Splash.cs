// Platypus Reclayed Android port: the intro films of the loading screen (Claymatic logo, then the Platypus intro).
// On PC they start paused (playback speed 0) while the game loads and are un-paused later. Android's video decoder
// does not hold a film at speed 0, so the intro ran on, unseen, during loading and appeared late, half or not at
// all. Here both films are only prepared while loading and are started from their first frame when their turn
// comes. They keep the game's own framing (the whole film, black around it on a wider screen).
// The game ends a film after its length or at its last frame. Unity 6 on Android can report a length and frame count
// of 0 for these films, which cut them after a second; then the clip's own figures are used, and else the film runs
// until it stops playing (at most 20 s). The log gets a [Splash] line per film.
// When the Claymatic film ends or is tapped away, the intro's first picture stays behind the loading bar until the
// game has loaded, and the intro then plays from that picture (as on PC).
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
            DisplayRunner.ReleaseFrame();                                                   // the film covers the held picture
            VideoClip c = v.clip;
            Debug.Log($"[Splash] {v.name}: {(c != null ? c.name : "no clip")} prepared {v.isPrepared} after {Time.realtimeSinceStartup - start:0.0}s, " +
                      $"player {v.length:0.00}s / {v.frameCount} frames, clip {(c != null ? c.length : 0):0.00}s / {(c != null ? c.frameCount : 0)} frames, " +
                      $"{v.width}x{v.height}, playing {v.isPlaying}");
        }

        /// <summary>When the Claymatic film ends or is tapped away: the intro's first picture stays on screen behind the
        /// loading bar until loading is done, then the intro plays from there (<see cref="PlayFromStart"/>). The intro
        /// is started silently for a moment to get that picture, then stopped; if it is not ready in a few seconds,
        /// the picture held before (the Claymatic one) stays.</summary>
        public static IEnumerator HoldFirstFrame(VideoPlayer intro)
        {
            if (intro == null) yield break;
            float start = Time.realtimeSinceStartup;
            if (!intro.isPrepared) intro.Prepare();
            while (!intro.isPrepared && Time.realtimeSinceStartup < start + 4f) yield return null;
            if (!intro.isPrepared) { Debug.Log("[Splash] intro not ready, the Claymatic picture stays"); yield break; }
            float alpha = intro.targetCameraAlpha;
            intro.targetCameraAlpha = 0f;                  // the held copy is shown, not the film
            Mute(intro, true);
            intro.playbackSpeed = 1f;
            intro.frame = 0;
            intro.Play();
            float until = Time.realtimeSinceStartup + 3f;
            while (intro.frame < 0 && Time.realtimeSinceStartup < until) yield return null;
            yield return null;                             // the picture is in the film's texture
            intro.Pause();
            bool got = intro.frame >= 0;
            if (got) DisplayRunner.HoldFrame(intro);
            Mute(intro, false);
            intro.Stop();                                  // prepared again when its turn comes
            intro.targetCameraAlpha = alpha;
            Debug.Log($"[Splash] intro first picture {(got ? "held" : "not decoded")} after {Time.realtimeSinceStartup - start:0.0}s");
        }

        static void Mute(VideoPlayer v, bool on)
        {
            try
            {
                if (v.audioOutputMode == VideoAudioOutputMode.Direct)
                    for (ushort i = 0; i < v.audioTrackCount; i++) v.SetDirectAudioMute(i, on);
                else if (v.audioOutputMode == VideoAudioOutputMode.AudioSource)
                    for (ushort i = 0; i < v.audioTrackCount; i++) { AudioSource s = v.GetTargetAudioSource(i); if (s != null) s.mute = on; }
            }
            catch (System.Exception e) { Debug.LogWarning("[Splash] mute: " + e.Message); }
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
