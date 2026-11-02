package cs.UnityEngine.Networking;

public final class UnityWebRequestMultimedia {
    private UnityWebRequestMultimedia() {}
    public static UnityWebRequest GetAudioClip(String uri, int audioType) {
        UnityWebRequest r = new UnityWebRequest(uri);
        r.handler = new DownloadHandlerAudioClip();
        return r;
    }
}
