package cs.UnityEngine;

public class AudioClip$PCMReaderCallback extends cs.System.Delegate {
    public void Invoke(float[] data) {
        if ($list != null) { for (cs.System.Delegate d : $list) ((AudioClip$PCMReaderCallback) d).$call(data); return; }
        $call(data);
    }
    protected void $call(float[] data) { throw new IllegalStateException("unbound delegate"); }
    protected cs.System.Delegate $empty() { return new AudioClip$PCMReaderCallback(); }
}
