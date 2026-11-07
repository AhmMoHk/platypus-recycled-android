// Platypus Reclayed Android port: the game's log on the phone, for finding problems without a PC.
//  - written to Android/data/<package>/files/platypus_log.txt (the run before: platypus_log_prev.txt);
//    reachable from Windows over USB (phone storage > Android > data > <package> > files)
//  - hold four fingers on the screen for two seconds to share it (Telegram, mail, ...)
//  - lines are buffered and written once a second (at once for errors), so logging never stalls a frame
using System;
using System.IO;
using System.Text;
using UnityEngine;

namespace PlatypusAndroid
{
    public class LogFile : MonoBehaviour
    {
        static StreamWriter writer;
        static readonly object gate = new object();
        static string path;
        static readonly System.Diagnostics.Stopwatch clock = System.Diagnostics.Stopwatch.StartNew();   // usable from any thread
        float holdStart = -1f, nextFlush;
        int perfFrames, perfHitches; float perfTime, perfWorst, perfNext = 15f;

        [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.SubsystemRegistration)]
        static void Open()
        {
            try
            {
                string dir = Application.persistentDataPath;
                path = Path.Combine(dir, "platypus_log.txt");
                string prev = Path.Combine(dir, "platypus_log_prev.txt");
                if (File.Exists(path)) { if (File.Exists(prev)) File.Delete(prev); File.Move(path, prev); }
                writer = new StreamWriter(new FileStream(path, FileMode.Create, FileAccess.Write, FileShare.ReadWrite), new UTF8Encoding(false)) { AutoFlush = false };
                writer.WriteLine($"Platypus Reclayed Android {Application.version}  Unity {Application.unityVersion}  {SystemInfo.deviceModel}  " +
                                 $"Android {SystemInfo.operatingSystem}  {SystemInfo.graphicsDeviceName} ({SystemInfo.graphicsDeviceVersion})  " +
                                 $"RAM {SystemInfo.systemMemorySize} MB  {Screen.width}x{Screen.height}  {DateTime.Now:yyyy-MM-dd HH:mm:ss}");
                Application.logMessageReceivedThreaded += Write;
            }
            catch (Exception e) { Debug.LogWarning("[LogFile] " + e.Message); }
        }

        [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.AfterSceneLoad)]
        static void Boot()
        {
            var go = new GameObject("[LogFile]");
            DontDestroyOnLoad(go);
            go.hideFlags = HideFlags.HideInHierarchy;
            go.AddComponent<LogFile>();
            var d = Display.main;
            Debug.Log($"[Display] screen {Screen.width}x{Screen.height} dpi {Screen.dpi:0} safe {Screen.safeArea} rendering {d.renderingWidth}x{d.renderingHeight} " +
                      $"system {d.systemWidth}x{d.systemHeight} colour space {QualitySettings.activeColorSpace} {SystemInfo.graphicsDeviceType}");
        }

        static void Write(string message, string stack, LogType type)
        {
            if (writer == null) return;
            lock (gate)
            {
                try
                {
                    string t = type == LogType.Log ? "" : type == LogType.Warning ? "W " : "E ";
                    writer.WriteLine($"{clock.Elapsed.TotalSeconds,8:0.00} {t}{message}");
                    if (type == LogType.Exception || type == LogType.Error || type == LogType.Assert)
                    {
                        if (!string.IsNullOrEmpty(stack)) writer.WriteLine(stack.TrimEnd());
                        writer.Flush();
                    }
                }
                catch { }
            }
        }

        static void Flush() { if (writer == null) return; lock (gate) { try { writer.Flush(); } catch { } } }

        void OnApplicationPause(bool paused) { if (paused) Flush(); }
        void OnApplicationQuit() => Flush();

        void Update()
        {
            if (Time.unscaledTime >= nextFlush) { nextFlush = Time.unscaledTime + 1f; Flush(); }
            Perf();
            if (Input.touchCount >= 4)
            {
                if (holdStart < 0f) holdStart = Time.unscaledTime;
                else if (Time.unscaledTime - holdStart > 2f) { holdStart = float.MaxValue; Share(); }
            }
            else holdStart = -1f;
        }

        /// <summary>Every 15 s: average frame rate, the slowest frame and how many frames took over twice the target.</summary>
        void Perf()
        {
            float dt = Time.unscaledDeltaTime;
            int target = Application.targetFrameRate > 0 ? Application.targetFrameRate : 60;
            perfFrames++; perfTime += dt; perfWorst = Mathf.Max(perfWorst, dt);
            if (dt > 2f / target) perfHitches++;
            if (Time.unscaledTime < perfNext || perfTime <= 0f) return;
            perfNext = Time.unscaledTime + 15f;
            Debug.Log($"[Perf] {perfFrames / perfTime:0.0} FPS (target {target}), slowest frame {perfWorst * 1000f:0} ms, " +
                      $"{perfHitches} long frames, drawing {Screen.width}x{Screen.height}; loading {LoadingScreen.IsLoading}; {LoadWatch.Scenes()}");
            perfFrames = perfHitches = 0; perfTime = perfWorst = 0f;
            if (!stateDumped && Time.unscaledTime > 40f) { stateDumped = true; State(); }
        }

        bool stateDumped;

        /// <summary>What is on screen: every camera and every root canvas (for black / missing pictures).</summary>
        static void State()
        {
            try
            {
                var sb = new StringBuilder("[State] cameras:");
                foreach (Camera c in Camera.allCameras)
                    sb.Append($"\n   {c.name} ({c.gameObject.scene.name}) depth {c.depth} clear {c.clearFlags} mask 0x{c.cullingMask:X} rect {c.rect} " +
                              $"{(c.targetTexture != null ? "to texture " + c.targetTexture.name : "to screen")}");
                sb.Append("\n  canvases:");
                foreach (Canvas cv in FindObjectsOfType<Canvas>())
                    if (cv.isRootCanvas && cv.isActiveAndEnabled)
                        sb.Append($"\n   {cv.name} ({cv.gameObject.scene.name}) {cv.renderMode} order {cv.sortingOrder} camera {(cv.worldCamera != null ? cv.worldCamera.name : "-")}");
                Debug.Log(sb.ToString());
            }
            catch (Exception e) { Debug.LogWarning("[State] " + e.Message); }
        }

        static void Share()
        {
            if (Application.platform != RuntimePlatform.Android || path == null) return;
            State();
            Flush();
            try
            {
                string text;
                using (var fs = new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.ReadWrite))
                using (var r = new StreamReader(fs)) text = r.ReadToEnd();
                if (text.Length > 200000) text = text.Substring(0, 30000) + "\n...\n" + text.Substring(text.Length - 160000);   // an Intent carries < 1 MB
                using (var intentClass = new AndroidJavaClass("android.content.Intent"))
                using (var intent = new AndroidJavaObject("android.content.Intent", intentClass.GetStatic<string>("ACTION_SEND")))
                using (var player = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
                using (var activity = player.GetStatic<AndroidJavaObject>("currentActivity"))
                {
                    intent.Call<AndroidJavaObject>("setType", "text/plain");
                    intent.Call<AndroidJavaObject>("putExtra", intentClass.GetStatic<string>("EXTRA_SUBJECT"), "Platypus Reclayed log");
                    intent.Call<AndroidJavaObject>("putExtra", intentClass.GetStatic<string>("EXTRA_TEXT"), text);
                    using (var chooser = intentClass.CallStatic<AndroidJavaObject>("createChooser", intent, "Share the game log"))
                        activity.Call("startActivity", chooser);
                }
            }
            catch (Exception e) { Debug.LogWarning("[LogFile] share failed: " + e.Message); }
        }
    }
}
