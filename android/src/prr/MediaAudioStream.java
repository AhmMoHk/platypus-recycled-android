package prr;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/** Incremental MediaCodec decoder for streamed clips (music): decodes as the mixer needs samples. */
final class MediaAudioStream implements cs.UnityEngine.AudioStream {
    private final MediaExtractor ex = new MediaExtractor();
    private MediaCodec dec;
    private int channels, rate;
    private boolean inEos, outEos;
    private ShortBuffer pending;
    private int pendingIndex = -1;
    private final MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

    MediaAudioStream(String path) throws Exception {
        ex.setDataSource(path);
        MediaFormat fmt = null;
        for (int i = 0; i < ex.getTrackCount(); i++) {
            MediaFormat f = ex.getTrackFormat(i);
            if (f.getString(MediaFormat.KEY_MIME).startsWith("audio/")) { ex.selectTrack(i); fmt = f; break; }
        }
        if (fmt == null) throw new IllegalArgumentException("no audio track");
        channels = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        rate = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        dec = MediaCodec.createDecoderByType(fmt.getString(MediaFormat.KEY_MIME));
        dec.configure(fmt, null, null, 0);
        dec.start();
    }

    public int channels() { return channels; }
    public int rate() { return rate; }

    public int read(short[] dst, int max) {
        if (pending == null) {
            if (outEos) return -1;
            if (!inEos) {
                int ii = dec.dequeueInputBuffer(2000);
                if (ii >= 0) {
                    ByteBuffer in = dec.getInputBuffer(ii);
                    int n = ex.readSampleData(in, 0);
                    if (n < 0) { dec.queueInputBuffer(ii, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM); inEos = true; }
                    else { dec.queueInputBuffer(ii, 0, n, ex.getSampleTime(), 0); ex.advance(); }
                }
            }
            int oi = dec.dequeueOutputBuffer(info, 2000);
            if (oi == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat of = dec.getOutputFormat();
                channels = of.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                rate = of.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                return 0;
            }
            if (oi < 0) return 0;
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outEos = true;
            ByteBuffer b = dec.getOutputBuffer(oi);
            b.position(info.offset);
            b.limit(info.offset + info.size);
            pending = b.order(ByteOrder.nativeOrder()).asShortBuffer();
            pendingIndex = oi;
        }
        int n = Math.min(max, pending.remaining());
        pending.get(dst, 0, n);
        if (!pending.hasRemaining()) {
            dec.releaseOutputBuffer(pendingIndex, false);
            pending = null;
            pendingIndex = -1;
        }
        return n == 0 && outEos ? -1 : n;
    }

    public void rewind() {
        if (pending != null) { dec.releaseOutputBuffer(pendingIndex, false); pending = null; pendingIndex = -1; }
        dec.flush();
        ex.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        inEos = outEos = false;
    }

    public void close() {
        try { dec.stop(); } catch (RuntimeException ignored) {}
        try { dec.release(); } catch (RuntimeException ignored) {}
        ex.release();
    }
}
