// Platypus Reclayed Android port: the game's log on the phone, for finding problems without a PC.
//  - written to Android/data/<package>/files/platypus_log.txt (the run before: platypus_log_prev.txt);
//    reachable from Windows over USB (phone storage > Android > data > <package> > files)
//  - hold four fingers on the screen for two seconds to share it (Telegram, mail, ...)
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
        float holdStart = -1f;

        [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.SubsystemRegistration)]
        static void Open()
        {
            try
            {
                string dir = Application.persistentDataPath;
                path = Path.Combine(dir, "platypus_log.txt");
                string prev = Path.Combine(dir, "platypus_log_prev.txt");
                if (File.Exists(path)) { if (File.Exists(prev)) File.Delete(prev); File.Move(path, prev); }
                writer = new StreamWriter(new FileStream(path, FileMode.Create, FileAccess.Write, FileShare.ReadWrite), new UTF8Encoding(false)) { AutoFlush = true };
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
                    writer.WriteLine($"{Time.realtimeSinceStartup,8:0.00} {t}{message}");
                    if ((type == LogType.Exception || type == LogType.Error) && !string.IsNullOrEmpty(stack)) writer.WriteLine(stack.TrimEnd());
                }
                catch { }
            }
        }

        void Update()
        {
            if (Input.touchCount >= 4)
            {
                if (holdStart < 0f) holdStart = Time.unscaledTime;
                else if (Time.unscaledTime - holdStart > 2f) { holdStart = float.MaxValue; Share(); }
            }
            else holdStart = -1f;
        }

        static void Share()
        {
            if (Application.platform != RuntimePlatform.Android || path == null) return;
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
