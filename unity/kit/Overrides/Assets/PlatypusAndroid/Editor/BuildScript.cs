// Platypus Reclayed Android port: one-click Android build (build_android.bat runs BuildAndroid in batch mode).
//  1. player settings: IL2CPP, ARM64, landscape, Android 8.0+ (API 26), target API 34, OpenGL ES 3, ASTC
//  2. the mod asset bundles (Platypus Reclayed, Platypus Original) rebuilt for Android from the exported art,
//     into StreamingAssets/<Mod>/Bundle/HighEnd/<mod> exactly where the game looks for them
//  3. an index of StreamingAssets, so the first launch can copy the loose mod files out of the APK
//  4. the APK, next to the project: Build/PlatypusReclayed.apk
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using UnityEditor;
using UnityEditor.Build;
using UnityEditor.Build.Reporting;
using UnityEngine;
using UnityEngine.Rendering;

namespace PlatypusAndroid.EditorTools
{
    public static class BuildScript
    {
        const string ModArtRoot = "Assets/generatedmods/highend";
        const string Streaming = "Assets/StreamingAssets";
        const string IconPath = "Assets/PlatypusAndroid/Icon/ic_launcher.png";

        static string Arg(string name, string def)
        {
            string[] a = Environment.GetCommandLineArgs();
            for (int i = 0; i < a.Length - 1; i++) if (a[i] == name) return a[i + 1];
            return def;
        }

        [MenuItem("Platypus Android/Build APK")]
        public static void BuildAndroidMenu() => Run(false);

        /// <summary>entry point for -executeMethod</summary>
        public static void BuildAndroid() => Run(true);

        static void Run(bool batch)
        {
            try
            {
                string astc = Arg("-astc", "6x6");
                Environment.SetEnvironmentVariable("PLATYPUS_ASTC", astc);
                TextureImportRules.Remember(astc);
                string output = Path.GetFullPath(Arg("-output", "Build/PlatypusReclayed.apk"));
                Step("Player settings", ConfigurePlayer);
                Step("Texture settings (ASTC " + astc + ")", FixImportSettings);
                Step("Mod bundles for Android", BuildModBundles);
                Step("StreamingAssets index", WriteIndex);
                Step("APK", () => BuildApk(output));
                Debug.Log("[PlatypusAndroid] done: " + output);
                if (batch) EditorApplication.Exit(0);
            }
            catch (Exception e)
            {
                Debug.LogError("[PlatypusAndroid] BUILD FAILED: " + e);
                if (batch) EditorApplication.Exit(1);
                else EditorUtility.DisplayDialog("Platypus Android", "Build failed:\n" + e.Message, "OK");
            }
            finally { EditorUtility.ClearProgressBar(); }
        }

        static void Step(string name, Action a)
        {
            Debug.Log("[PlatypusAndroid] " + name + " ...");
            EditorUtility.DisplayProgressBar("Platypus Android", name, 0.5f);
            var t = DateTime.Now;
            a();
            Debug.Log($"[PlatypusAndroid] {name}: ok ({(DateTime.Now - t).TotalSeconds:0} s)");
        }

        // ------------------------------------------------------------------------------------------- 1. player
        static void ConfigurePlayer()
        {
            if (!BuildPipeline.IsBuildTargetSupported(BuildTargetGroup.Android, BuildTarget.Android))
                throw new Exception("Android Build Support is not installed for this Unity (Unity Hub > Installs > Add modules)");
            if (EditorUserBuildSettings.activeBuildTarget != BuildTarget.Android)
                EditorUserBuildSettings.SwitchActiveBuildTarget(BuildTargetGroup.Android, BuildTarget.Android);
            var android = NamedBuildTarget.Android;
            EditorUserBuildSettings.androidBuildSubtarget = MobileTextureSubtarget.ASTC;
            EditorUserBuildSettings.buildAppBundle = false;
            EditorUserBuildSettings.exportAsGoogleAndroidProject = false;
            EditorUserBuildSettings.development = false;

            PlayerSettings.productName = "Platypus Reclayed";
            if (string.IsNullOrEmpty(PlayerSettings.companyName) || PlayerSettings.companyName == "DefaultCompany") PlayerSettings.companyName = "Platypus Port";
            PlayerSettings.SetApplicationIdentifier(android, "com.platypusport.reclayed");
            PlayerSettings.bundleVersion = "1.0";
            PlayerSettings.Android.bundleVersionCode = 1;

            PlayerSettings.SetScriptingBackend(android, ScriptingImplementation.IL2CPP);
            PlayerSettings.SetIl2CppCompilerConfiguration(android, Il2CppCompilerConfiguration.Release);
            PlayerSettings.SetManagedStrippingLevel(android, ManagedStrippingLevel.Minimal);
            PlayerSettings.stripEngineCode = false;          // components used only inside the mod bundles must stay
            PlayerSettings.Android.targetArchitectures = AndroidArchitecture.ARM64;
            PlayerSettings.Android.minSdkVersion = AndroidSdkVersions.AndroidApiLevel26;
            PlayerSettings.Android.targetSdkVersion = SdkHas(34) ? (AndroidSdkVersions)34 : AndroidSdkVersions.AndroidApiLevelAuto;
            if (!SdkHas(34)) Debug.LogWarning("[PlatypusAndroid] Android SDK platform 34 not found next to Unity, target API = highest installed");
            PlayerSettings.Android.preferredInstallLocation = AndroidPreferredInstallLocation.Auto;
            PlayerSettings.Android.forceInternetPermission = false;
            PlayerSettings.Android.startInFullscreen = true;
            PlayerSettings.Android.renderOutsideSafeArea = true;
            PlayerSettings.Android.maxAspectRatio = 3f;                // up to 3:1, so Android adds no bars of its own
            PlayerSettings.Android.useCustomKeystore = false;      // Unity's debug key (v1 + v2 signature), fine to sideload

            PlayerSettings.defaultInterfaceOrientation = UIOrientation.AutoRotation;
            PlayerSettings.allowedAutorotateToLandscapeLeft = true;
            PlayerSettings.allowedAutorotateToLandscapeRight = true;
            PlayerSettings.allowedAutorotateToPortrait = false;
            PlayerSettings.allowedAutorotateToPortraitUpsideDown = false;
            PlayerSettings.SetUseDefaultGraphicsAPIs(BuildTarget.Android, false);
            PlayerSettings.SetGraphicsAPIs(BuildTarget.Android, new[] { GraphicsDeviceType.OpenGLES3 });
            PlayerSettings.runInBackground = false;
            // No "Made with Unity": Unity 6 lets a Personal licence switch the splash off. Unity 2022 Personal always
            // shows it, whatever this says (the build then logs a warning).
            PlayerSettings.SplashScreen.show = false;
            PlayerSettings.SplashScreen.showUnityLogo = false;
            if (Application.unityVersion.StartsWith("2022", StringComparison.Ordinal))
                Debug.LogWarning("[PlatypusAndroid] Unity 2022 Personal always shows the Made with Unity splash; build with Unity 6 to remove it");
            // Unity 6 defaults to GameActivity; the port is made and tested for the classic Activity entry point
            var entry = typeof(PlayerSettings.Android).GetProperty("applicationEntry");
            if (entry != null) entry.SetValue(null, Enum.ToObject(entry.PropertyType, 1));        // AndroidApplicationEntry.Activity

            var icon = AssetDatabase.LoadAssetAtPath<Texture2D>(IconPath);
            if (icon != null) PlayerSettings.SetIconsForTargetGroup(BuildTargetGroup.Unknown, new[] { icon });
            AssetDatabase.SaveAssets();
        }

        static bool SdkHas(int api)
        {
            try
            {
                // UnityEditor.Android.AndroidExternalToolsSettings.sdkRootPath, looked up by reflection so this
                // script still compiles if the Android module is missing (the build then stops with a clear error)
                var t = Type.GetType("UnityEditor.Android.AndroidExternalToolsSettings, UnityEditor.Android.Extensions");
                string sdk = t?.GetProperty("sdkRootPath")?.GetValue(null) as string;
                return string.IsNullOrEmpty(sdk) || Directory.Exists(Path.Combine(sdk, "platforms", "android-" + api));
            }
            catch { return true; }
        }

        // ------------------------------------------------------------------------------------------- 2. bundles
        static void FixImportSettings()
        {
            var changed = new List<string>();
            foreach (string guid in AssetDatabase.FindAssets("t:Texture2D", new[] { ModArtRoot, "Assets/PlatypusAndroid" }))
            {
                string p = AssetDatabase.GUIDToAssetPath(guid);
                if (UnityEditor.AssetImporter.GetAtPath(p) is UnityEditor.TextureImporter ti && !TextureImportRules.Apply(ti, p.StartsWith(TextureImportRules.ModArt, StringComparison.Ordinal)))
                    changed.Add(p);
            }
            if (changed.Count == 0) return;
            Debug.Log($"[PlatypusAndroid] re-encoding {changed.Count} textures");
            AssetDatabase.StartAssetEditing();
            try { foreach (string p in changed) AssetDatabase.ImportAsset(p, ImportAssetOptions.ForceUpdate); }
            finally { AssetDatabase.StopAssetEditing(); }
        }

        static void BuildModBundles()
        {
            if (!Directory.Exists(ModArtRoot)) throw new Exception(ModArtRoot + " is missing: run setup_project.bat first");
            var modDirs = Directory.GetDirectories(Streaming);
            foreach (string art in Directory.GetDirectories(ModArtRoot))
            {
                string bundleName = Path.GetFileName(art).ToLowerInvariant();                 // e.g. "platypus reclayed"
                string mod = modDirs.FirstOrDefault(d => Path.GetFileName(d).ToLowerInvariant() == bundleName);
                if (mod == null) { Debug.LogWarning("[PlatypusAndroid] no StreamingAssets folder for " + bundleName); continue; }
                string[] assets = AssetDatabase.FindAssets("", new[] { art.Replace('\\', '/') })
                    .Select(AssetDatabase.GUIDToAssetPath).Where(p => !AssetDatabase.IsValidFolder(p)).Distinct().ToArray();
                if (assets.Length == 0) continue;
                string temp = Path.Combine("Library", "PlatypusBundles", bundleName, "HighEnd");
                Directory.CreateDirectory(temp);
                var build = new AssetBundleBuild { assetBundleName = bundleName, assetNames = assets };
                Debug.Log($"[PlatypusAndroid] bundle \"{bundleName}\": {assets.Length} assets");
                var manifest = BuildPipeline.BuildAssetBundles(temp, new[] { build },
                    BuildAssetBundleOptions.ChunkBasedCompression, BuildTarget.Android);
                if (manifest == null) throw new Exception("asset bundle build failed for " + bundleName);
                string dest = Path.Combine(mod, "Bundle", "HighEnd");
                string bundleRoot = Path.Combine(mod, "Bundle");
                if (Directory.Exists(bundleRoot)) Directory.Delete(bundleRoot, true);             // the Windows bundles
                if (File.Exists(bundleRoot + ".meta")) File.Delete(bundleRoot + ".meta");
                Directory.CreateDirectory(dest);
                File.Copy(Path.Combine(temp, bundleName), Path.Combine(dest, bundleName), true);
                File.Copy(Path.Combine(temp, "HighEnd"), Path.Combine(dest, "HighEnd"), true);
            }
            AssetDatabase.Refresh();
        }

        // ------------------------------------------------------------------------------------------- 3. index
        static void WriteIndex()
        {
            string root = Path.GetFullPath(Streaming);
            var lines = new List<string>();
            foreach (string f in Directory.GetFiles(root, "*", SearchOption.AllDirectories))
            {
                if (f.EndsWith(".meta", StringComparison.OrdinalIgnoreCase) || f.EndsWith(".manifest", StringComparison.OrdinalIgnoreCase)) continue;
                string rel = f.Substring(root.Length + 1).Replace('\\', '/');
                if (rel == ModFiles.IndexName) continue;
                lines.Add(new FileInfo(f).Length + "\t" + rel);
            }
            lines.Sort(StringComparer.Ordinal);
            File.WriteAllText(Path.Combine(root, ModFiles.IndexName), string.Join("\n", lines) + "\n");
            AssetDatabase.Refresh();
            Debug.Log($"[PlatypusAndroid] index: {lines.Count} files");
        }

        // ------------------------------------------------------------------------------------------- 4. apk
        static void BuildApk(string output)
        {
            Directory.CreateDirectory(Path.GetDirectoryName(output));
            var scenes = EditorBuildSettings.scenes.Where(s => s.enabled).Select(s => s.path).ToArray();
            if (scenes.Length == 0) throw new Exception("no scenes in Build Settings");
            var report = BuildPipeline.BuildPlayer(new BuildPlayerOptions
            {
                scenes = scenes,
                locationPathName = output,
                target = BuildTarget.Android,
                targetGroup = BuildTargetGroup.Android,
                options = BuildOptions.None,
            });
            if (report.summary.result != BuildResult.Succeeded)
                throw new Exception($"player build {report.summary.result}, {report.summary.totalErrors} errors (see the log)");
            Debug.Log($"[PlatypusAndroid] APK {report.summary.totalSize / (1024 * 1024)} MB");
        }
    }
}
