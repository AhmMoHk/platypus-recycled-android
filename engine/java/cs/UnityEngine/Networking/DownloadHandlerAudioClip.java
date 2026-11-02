package cs.UnityEngine.Networking;

public class DownloadHandlerAudioClip extends DownloadHandler {
    cs.UnityEngine.AudioClip clip;
    public DownloadHandlerAudioClip() {}
    public DownloadHandlerAudioClip(String uri, int type) {}
    public cs.UnityEngine.AudioClip get_audioClip() { return clip; }
    public static cs.UnityEngine.AudioClip GetContent(UnityWebRequest r) {
        DownloadHandler h = r.handler;
        if (!(h instanceof DownloadHandlerAudioClip)) throw new cs.System.InvalidOperationException("not an audio request");
        return ((DownloadHandlerAudioClip) h).clip;
    }
}
