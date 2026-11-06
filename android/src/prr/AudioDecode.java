package prr;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/** Decode compressed audio (Vorbis/MP3/WAV) to interleaved 16-bit PCM with MediaCodec. */
final class AudioDecode {
    private AudioDecode() {}

    static Object[] decode(byte[] bytes) {
        File tmp = null;
        MediaExtractor ex = null;
        MediaCodec dec = null;
        try {
            tmp = File.createTempFile("snd", ".bin");
            FileOutputStream out = new FileOutputStream(tmp);
            try { out.write(bytes); } finally { out.close(); }
            ex = new MediaExtractor();
            ex.setDataSource(tmp.getAbsolutePath());
            int track = -1;
            MediaFormat fmt = null;
            for (int i = 0; i < ex.getTrackCount(); i++) {
                MediaFormat f = ex.getTrackFormat(i);
                if (f.getString(MediaFormat.KEY_MIME).startsWith("audio/")) { track = i; fmt = f; break; }
            }
            if (track < 0) return null;
            ex.selectTrack(track);
            int channels = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            int rate = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            dec = MediaCodec.createDecoderByType(fmt.getString(MediaFormat.KEY_MIME));
            dec.configure(fmt, null, null, 0);
            dec.start();
            ByteBuffer[] in = dec.getInputBuffers(), outBufs = dec.getOutputBuffers();
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            ShortChunks acc = new ShortChunks();
            boolean eos = false;
            while (true) {
                if (!eos) {
                    int ii = dec.dequeueInputBuffer(10000);
                    if (ii >= 0) {
                        int n = ex.readSampleData(in[ii], 0);
                        if (n < 0) {
                            dec.queueInputBuffer(ii, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            eos = true;
                        } else {
                            dec.queueInputBuffer(ii, 0, n, ex.getSampleTime(), 0);
                            ex.advance();
                        }
                    }
                }
                int oi = dec.dequeueOutputBuffer(info, 10000);
                if (oi >= 0) {
                    ByteBuffer b = outBufs[oi];
                    b.position(info.offset);
                    b.limit(info.offset + info.size);
                    ShortBuffer sb = b.order(ByteOrder.nativeOrder()).asShortBuffer();
                    short[] s = new short[sb.remaining()];
                    sb.get(s);
                    acc.add(s);
                    dec.releaseOutputBuffer(oi, false);
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break;
                } else if (oi == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    outBufs = dec.getOutputBuffers();
                } else if (oi == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat of = dec.getOutputFormat();
                    channels = of.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    rate = of.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                } else if (oi == MediaCodec.INFO_TRY_AGAIN_LATER && eos) {
                    break;
                }
            }
            return new Object[]{channels, rate, acc.toArray()};
        } catch (Exception e) {
            android.util.Log.w("prr", "audio decode failed: " + e);
            return null;
        } finally {
            if (dec != null) try { dec.stop(); dec.release(); } catch (RuntimeException ignored) {}
            if (ex != null) ex.release();
            if (tmp != null) tmp.delete();
        }
    }

    static final class ShortChunks {
        private final java.util.ArrayList<short[]> chunks = new java.util.ArrayList<>();
        private int total;
        void add(short[] s) { if (s.length > 0) { chunks.add(s); total += s.length; } }
        short[] toArray() {
            short[] r = new short[total];
            int o = 0;
            for (short[] c : chunks) { System.arraycopy(c, 0, r, o, c.length); o += c.length; }
            return r;
        }
    }
}
