package cs.UnityEngine;

/** An incremental decoder for a long clip (music): the host decodes while it plays instead of holding
 *  minutes of PCM in memory. */
public interface AudioStream {
    int channels();
    int rate();
    /** up to max interleaved samples into dst; 0 = nothing yet, -1 = end of stream */
    int read(short[] dst, int max);
    void rewind();
    void close();
}
