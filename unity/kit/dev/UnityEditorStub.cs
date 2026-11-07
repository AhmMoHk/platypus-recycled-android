// compile-check stub: only members verified against UnityCsReference 2022.3
using System; using UnityEngine; using UnityEngine.Rendering;
namespace UnityEditor.Build { public struct NamedBuildTarget { public static readonly NamedBuildTarget Android; } }
namespace UnityEditor.Build.Reporting { public enum BuildResult { Unknown, Succeeded, Failed, Cancelled } public struct BuildSummary { public BuildResult result; public int totalErrors; public ulong totalSize; } public class BuildReport { public BuildSummary summary; } }
namespace UnityEditor {
 using UnityEditor.Build; using UnityEditor.Build.Reporting;
 public enum BuildTarget { Android = 13 } public enum BuildTargetGroup { Unknown = 0, Android = 7 }
 public enum ScriptingImplementation { Mono2x, IL2CPP } public enum Il2CppCompilerConfiguration { Debug, Release, Master }
 public enum ManagedStrippingLevel { Disabled, Low, Medium, High, Minimal } public enum AndroidArchitecture { ARMv7 = 1, ARM64 = 2 }
 public enum AndroidSdkVersions { AndroidApiLevelAuto = 0, AndroidApiLevel26 = 26 } public enum AndroidPreferredInstallLocation { Auto, PreferExternal, ForceInternal }
 public enum UIOrientation { Portrait, PortraitUpsideDown, LandscapeRight, LandscapeLeft, AutoRotation }
 public enum MobileTextureSubtarget { Generic, DXT, PVRTC, ETC, ETC2, ASTC }
 [Flags] public enum BuildAssetBundleOptions { None = 0, ChunkBasedCompression = 256, StrictMode = 8192 } [Flags] public enum BuildOptions { None = 0 }
 public enum TextureImporterFormat { RGBA32 = 4, ASTC_4x4 = 48, ASTC_5x5 = 49, ASTC_6x6 = 50, ASTC_8x8 = 51 }
 public enum TextureImporterCompression { Uncompressed, Compressed, CompressedHQ, CompressedLQ }
 public enum ImportAssetOptions { Default = 0, ForceUpdate = 1 }
 public static class PlayerSettings {
  public static string productName, companyName, bundleVersion; public static bool stripEngineCode, runInBackground, allowedAutorotateToLandscapeLeft, allowedAutorotateToLandscapeRight, allowedAutorotateToPortrait, allowedAutorotateToPortraitUpsideDown;
  public static UIOrientation defaultInterfaceOrientation; public static bool gcIncremental; internal static void GetBatchingForPlatform(BuildTarget p, out int s, out int d) { s = d = 0; } internal static void SetBatchingForPlatform(BuildTarget p, int s, int d) {} public static void SetMobileMTRendering(NamedBuildTarget t, bool e) {} public static void SetStackTraceLogType(UnityEngine.LogType l, UnityEngine.StackTraceLogType s) {}
  public static void SetApplicationIdentifier(NamedBuildTarget t, string id) {} public static void SetScriptingBackend(NamedBuildTarget t, ScriptingImplementation s) {}
  public static void SetIl2CppCompilerConfiguration(NamedBuildTarget t, Il2CppCompilerConfiguration c) {} public static void SetManagedStrippingLevel(NamedBuildTarget t, ManagedStrippingLevel l) {}
  public static void SetUseDefaultGraphicsAPIs(BuildTarget p, bool v) {} public static void SetGraphicsAPIs(BuildTarget p, GraphicsDeviceType[] a) {}
  public static void SetIconsForTargetGroup(BuildTargetGroup p, Texture2D[] icons) {}
  public static class Android { public static int bundleVersionCode; public static AndroidArchitecture targetArchitectures; public static AndroidSdkVersions minSdkVersion, targetSdkVersion; public static AndroidPreferredInstallLocation preferredInstallLocation; public static bool forceInternetPermission, startInFullscreen, renderOutsideSafeArea, useCustomKeystore; public static float maxAspectRatio; public static bool optimizedFramePacing; public static bool androidIsGame; public static AndroidBlitType blitType; }
  public static class SplashScreen { public static bool show, showUnityLogo; }
 }
 public enum AndroidBlitType { Always = 0, Never = 1, Auto = 2 }
 public static class EditorUserBuildSettings { public static MobileTextureSubtarget androidBuildSubtarget; public static bool buildAppBundle, exportAsGoogleAndroidProject, development; public static BuildTarget activeBuildTarget; public static bool SwitchActiveBuildTarget(BuildTargetGroup g, BuildTarget t) => true; }
 public struct AssetBundleBuild { public string assetBundleName; public string[] assetNames; }
 public struct BuildPlayerOptions { public string[] scenes; public string locationPathName; public BuildTarget target; public BuildTargetGroup targetGroup; public BuildOptions options; }
 public static class BuildPipeline { public static bool IsBuildTargetSupported(BuildTargetGroup g, BuildTarget t) => true; public static AssetBundleManifest BuildAssetBundles(string o, AssetBundleBuild[] b, BuildAssetBundleOptions op, BuildTarget t) => null; public static BuildReport BuildPlayer(BuildPlayerOptions o) => null; }
 public class EditorBuildSettingsScene { public bool enabled; public string path; } public static class EditorBuildSettings { public static EditorBuildSettingsScene[] scenes; }
 public static class EditorApplication { public static void Exit(int c) {} }
 public static class EditorUtility { public static void DisplayProgressBar(string a, string b, float p) {} public static void ClearProgressBar() {} public static bool DisplayDialog(string a, string b, string c) => true; }
 public static class EditorPrefs { public static string GetString(string k, string d) => d; public static void SetString(string k, string v) {} }
 public static class AssetDatabase { public static T LoadAssetAtPath<T>(string p) where T : UnityEngine.Object => null; public static void SaveAssets() {} public static void Refresh() {} public static string[] FindAssets(string f, string[] folders) => null; public static string GUIDToAssetPath(string g) => null; public static bool IsValidFolder(string p) => false; public static void StartAssetEditing() {} public static void StopAssetEditing() {} public static void ImportAsset(string p, ImportAssetOptions o) {} }
 public class AssetImporter : UnityEngine.Object { public string assetPath => null; public static AssetImporter GetAtPath(string p) => null; }
 public class TextureImporterPlatformSettings { public bool overridden; public TextureImporterFormat format; public int maxTextureSize, compressionQuality; public TextureImporterCompression textureCompression; }
 public class TextureImporter : AssetImporter { public int maxTextureSize; public TextureImporterPlatformSettings GetPlatformTextureSettings(string p) => null; public void SetPlatformTextureSettings(TextureImporterPlatformSettings s) {} }
 public struct AudioImporterSampleSettings { public AudioCompressionFormat compressionFormat; public float quality; }
 public class AudioImporter : AssetImporter { public AudioImporterSampleSettings defaultSampleSettings; public AudioImporterSampleSettings GetOverrideSampleSettings(string p) => default; public bool ContainsSampleSettingsOverride(string p) => false; public bool SetOverrideSampleSettings(string p, AudioImporterSampleSettings s) => true; }
 public class AssetPostprocessor { public string assetPath => null; public AssetImporter assetImporter => null; }
 [AttributeUsage(AttributeTargets.Method)] public class MenuItem : Attribute { public MenuItem(string s) {} }
}
