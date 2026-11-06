// Platypus Reclayed Android port: the game reads its mods (StreamingAssets/<mod>/Data, GFX, SFX, Behaviours,
// modmeta.json, messages*.csv) with System.IO. Inside an APK StreamingAssets are zip entries, not files, so the
// first launch copies the loose files to persistentDataPath/StreamingAssets (shown on the game's own loading bar).
// Asset bundles are not copied: an empty marker keeps File.Exists working and the bundle loads straight from the APK.
using System;
using System.Collections;
using System.Collections.Generic;
using System.IO;
using System.IO.Compression;
using System.Threading;
using UnityEngine;

namespace PlatypusAndroid
{
    public static class ModFiles
    {
        public const string IndexName = "platypus_android_files.txt";   // written by the build script: "<size>\t<path>"
        const string StampName = ".installed";

        static bool OnDevice => Application.platform == RuntimePlatform.Android;

        /// <summary>StreamingAssets as a real folder (on Android the extracted copy).</summary>
        public static string Root => OnDevice ? Path.Combine(Application.persistentDataPath, "StreamingAssets") : Application.streamingAssetsPath;

        /// <summary>Where AssetBundle.LoadFromFile finds a bundle: inside the APK on Android.</summary>
        public static string BundlePath(string path)
        {
            if (!OnDevice) return path;
            string root = Root;
            if (!path.StartsWith(root, StringComparison.Ordinal)) return path;
            return Application.streamingAssetsPath + path.Substring(root.Length).Replace('\\', '/');
        }

        // ZipArchive (System.IO.Compression.dll) rather than ZipFile, which lives in an assembly Unity does not reference
        static ZipArchive OpenApk(string apk) => new ZipArchive(new FileStream(apk, FileMode.Open, FileAccess.Read, FileShare.Read, 1 << 16), ZipArchiveMode.Read, false);

        static bool IsBundle(string rel) => rel.Replace('\\', '/').Contains("/Bundle/");

        public static bool Installed { get; private set; }

        /// <summary>Copies the loose mod files out of the APK once per app version.</summary>
        public static IEnumerator Install(ProgressBar bar)
        {
            if (Installed || !OnDevice) { Installed = true; yield break; }
            string apk = Application.dataPath, root = Root;
            string stampPath = Path.Combine(root, StampName);
            string error = null, index = null;
            var entries = new List<KeyValuePair<string, long>>();
            try
            {
                using (var zip = OpenApk(apk))
                {
                    var e = zip.GetEntry("assets/" + IndexName);
                    if (e != null) using (var r = new StreamReader(e.Open())) index = r.ReadToEnd();
                }
            }
            catch (Exception ex) { error = ex.Message; }
            if (index == null) { Debug.LogError("[ModFiles] no file index in the APK: " + error); Installed = true; yield break; }
            uint h = 2166136261; foreach (char c in index) h = (h ^ c) * 16777619;   // FNV-1a of the index
            string stamp = Application.version + "|" + index.Length + "|" + h.ToString("x8");
            if (File.Exists(stampPath) && File.ReadAllText(stampPath) == stamp) { Installed = true; yield break; }

            long total = 0;
            foreach (string line in index.Split('\n'))
            {
                int tab = line.IndexOf('\t');
                if (tab <= 0) continue;
                long size = long.Parse(line.Substring(0, tab));
                string rel = line.Substring(tab + 1).Trim('\r');
                entries.Add(new KeyValuePair<string, long>(rel, size));
                if (!IsBundle(rel)) total += size;
            }
            long done = 0;
            var worker = new Thread(() =>
            {
                try
                {
                    var buf = new byte[1 << 20];
                    using (var zip = OpenApk(apk))
                    {
                        foreach (var kv in entries)
                        {
                            string dest = Path.Combine(root, kv.Key);
                            Directory.CreateDirectory(Path.GetDirectoryName(dest));
                            if (IsBundle(kv.Key)) { File.WriteAllBytes(dest, new byte[0]); continue; }   // marker only
                            var entry = zip.GetEntry("assets/" + kv.Key);
                            if (entry == null) { Debug.LogWarning("[ModFiles] missing in APK: " + kv.Key); continue; }
                            using (var src = entry.Open())
                            using (var dst = new FileStream(dest, FileMode.Create, FileAccess.Write, FileShare.None, 1 << 16))
                            {
                                int n;
                                while ((n = src.Read(buf, 0, buf.Length)) > 0) { dst.Write(buf, 0, n); Interlocked.Add(ref done, n); }
                            }
                        }
                    }
                    File.WriteAllText(stampPath, stamp);
                }
                catch (Exception ex) { error = ex.ToString(); }
            }) { IsBackground = true, Name = "ModFiles" };
            worker.Start();
            while (worker.IsAlive)
            {
                float p = total > 0 ? (float)Interlocked.Read(ref done) / total : 1f;
                if (bar != null) { bar.SetProgress(p); bar.SetText("Installing game data " + Mathf.FloorToInt(p * 100f) + "%"); }
                yield return null;
            }
            if (error != null) Debug.LogError("[ModFiles] install failed: " + error);
            if (bar != null) { bar.SetProgress(0f); bar.SetText(""); }
            Installed = true;
        }
    }
}
