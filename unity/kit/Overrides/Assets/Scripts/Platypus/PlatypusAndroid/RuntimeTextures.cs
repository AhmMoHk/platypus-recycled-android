// Platypus Reclayed Android port: pictures the game loads from loose PNG files while it runs (the Spooky and Winter
// mods, Workshop add-ons; the main game's pictures come ready-made from its asset bundle). The PC game keeps them as
// they are: uncompressed, at full size and padded to powers of two, so one hill of Spooky Platypus became an
// 8192x4096 picture of 170 MB, and turning Spooky or Winter on needed several GB: the phone stopped the game.
// On a phone they are kept to at most 4096 pixels on a side (2048 on Low) and compressed the way the phone's GPU reads
// them directly (ETC2), a quarter of the memory. The log gets an [Import] line for the large ones.
using System;
using System.Diagnostics;
using UnityEngine;
using Debug = UnityEngine.Debug;

namespace PlatypusAndroid
{
    public static class RuntimeTextures
    {
        static Texture2D last;
        static int logged;
        static long totalBytes;

        /// <summary>The largest side a loaded picture may keep (0 = no limit asked by the game).</summary>
        public static int MaxDim(int asked)
        {
            if (!Application.isMobilePlatform) return asked;
            int cap = Quality.LevelCached <= 0 ? 2048 : 4096;
            return asked <= 0 ? cap : Math.Min(asked, cap);
        }

        /// <summary>Use instead of <c>texture.Apply(true, !cpuReadable)</c> at the end of a texture import.</summary>
        public static void Finish(Texture2D tex, bool compressed, bool cpuReadable)
        {
            if (tex == null || ReferenceEquals(tex, last)) return;
            last = tex;
            if (!tex.isReadable) return;                       // done already (uploaded and freed)
            TextureFormat f = tex.format;
            bool squeeze = Application.isMobilePlatform && compressed && !cpuReadable &&
                           tex.width >= 64 && tex.height >= 64 && tex.width % 4 == 0 && tex.height % 4 == 0 &&
                           (f == TextureFormat.RGBA32 || f == TextureFormat.ARGB32 || f == TextureFormat.RGB24);
            if (!squeeze)
            {
                tex.Apply(updateMipmaps: true, makeNoLongerReadable: !cpuReadable);
                return;
            }
            var sw = Stopwatch.StartNew();
            try
            {
                tex.Apply(updateMipmaps: true, makeNoLongerReadable: false);   // the mipmaps, still on the CPU side
                tex.Compress(false);                                           // ETC2 / EAC on Android
                tex.Apply(updateMipmaps: false, makeNoLongerReadable: true);   // to the GPU, CPU copy freed
            }
            catch (Exception e)
            {
                Debug.LogWarning($"[Import] {tex.name}: compressing failed ({e.Message}), kept uncompressed");
                if (tex.isReadable) tex.Apply(updateMipmaps: false, makeNoLongerReadable: true);
            }
            long bytes = Bytes(tex);
            totalBytes += bytes;
            if (tex.width * tex.height >= 1024 * 1024 && logged < 60)
            {
                logged++;
                Debug.Log($"[Import] {tex.name} {tex.width}x{tex.height} {tex.format} {bytes / 1048576f:0.0} MB in {sw.ElapsedMilliseconds} ms " +
                          $"(loaded pictures so far {totalBytes / 1048576f:0} MB)");
            }
        }

        /// <summary>Waits for a sound file the game reads while it runs (the PC game waited without end). Gives up after
        /// a few seconds, so a file that never finishes loading costs that sound, not the game.</summary>
        public static void Wait(UnityEngine.Networking.UnityWebRequest r, float seconds = 8f)
        {
            var sw = Stopwatch.StartNew();
            while (!r.isDone)
            {
                if (sw.Elapsed.TotalSeconds > seconds) { Debug.LogWarning("[Import] gave up waiting for " + r.url); r.Abort(); break; }
                System.Threading.Thread.Sleep(1);
            }
        }

        /// <summary>GPU memory of the picture with its mipmaps.</summary>
        static long Bytes(Texture2D t)
        {
            int bits;
            switch (t.format)
            {
                case TextureFormat.ETC2_RGBA8: case TextureFormat.ETC2_RGBA8Crunched: bits = 8; break;
                case TextureFormat.ETC2_RGB: case TextureFormat.ETC_RGB4: case TextureFormat.ETC2_RGBA1: bits = 4; break;
                case TextureFormat.RGB24: bits = 24; break;
                default: bits = 32; break;
            }
            long b = (long)t.width * t.height * bits / 8;
            return t.mipmapCount > 1 ? b * 4 / 3 : b;
        }
    }
}
