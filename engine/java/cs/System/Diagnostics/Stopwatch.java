package cs.System.Diagnostics;

public class Stopwatch {
    private long start, elapsed;
    private boolean running;
    public static final long Frequency = 1000000000L;
    public static final boolean IsHighResolution = true;
    public Stopwatch() {}
    public static Stopwatch StartNew() { Stopwatch s = new Stopwatch(); s.Start(); return s; }
    public static long GetTimestamp() { return System.nanoTime(); }
    public void Start() { if (!running) { start = System.nanoTime(); running = true; } }
    public void Stop() { if (running) { elapsed += System.nanoTime() - start; running = false; } }
    public void Reset() { elapsed = 0; running = false; }
    public void Restart() { elapsed = 0; start = System.nanoTime(); running = true; }
    private long nanos() { return elapsed + (running ? System.nanoTime() - start : 0); }
    public long get_ElapsedMilliseconds() { return nanos() / 1000000; }
    public long get_ElapsedTicks() { return nanos(); }
    public cs.System.TimeSpan get_Elapsed() { return new cs.System.TimeSpan(nanos() / 100); }
    public boolean get_IsRunning() { return running; }
}
