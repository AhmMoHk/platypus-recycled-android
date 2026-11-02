package cs.UnityEngine;

/** UnityEngine.Time: scaled/unscaled/fixed clocks, deltaTime clamped to maximumDeltaTime. */
public final class Time {
    private Time() {}
    static double timeD, unscaledD, fixedD, realD, levelLoadD;
    static float time, deltaTime, unscaledTime, unscaledDeltaTime, fixedTime, realtime;
    static float fixedDeltaTime = 0.02f, timeScale = 1f, maximumDeltaTime = 1f / 3f, smoothDeltaTime;
    static int frameCount;
    static boolean inFixed;

    static void beginFrame(float realDt) {
        realD += realDt;
        realtime = (float) realD;
        float dt = frameCount == 0 ? fixedDeltaTime : Math.min(Math.max(realDt, 0f), maximumDeltaTime);
        unscaledDeltaTime = dt;
        unscaledD += dt;
        unscaledTime = (float) unscaledD;
        deltaTime = dt * timeScale;
        timeD += deltaTime;
        time = (float) timeD;
        smoothDeltaTime = smoothDeltaTime == 0 ? deltaTime : smoothDeltaTime + (deltaTime - smoothDeltaTime) * 0.2f;
        frameCount++;
    }

    static void advanceFixed() {
        fixedD += fixedDeltaTime;
        fixedTime = (float) fixedD;
    }

    static boolean fixedDue() { return fixedD + fixedDeltaTime <= timeD + 1e-7; }
    static void syncFixed() { fixedD = timeD; fixedTime = time; }
    static void levelLoaded() { levelLoadD = timeD; }

    public static float get_time() { return inFixed ? fixedTime : time; }
    public static double get_timeAsDouble() { return inFixed ? fixedD : timeD; }
    public static float get_deltaTime() { return inFixed ? fixedDeltaTime : deltaTime; }
    public static float get_unscaledTime() { return unscaledTime; }
    public static double get_unscaledTimeAsDouble() { return unscaledD; }
    public static float get_unscaledDeltaTime() { return inFixed ? fixedDeltaTime : unscaledDeltaTime; }
    public static float get_fixedTime() { return fixedTime; }
    public static float get_fixedUnscaledTime() { return fixedTime; }
    public static float get_fixedDeltaTime() { return fixedDeltaTime; }
    public static void set_fixedDeltaTime(float v) { fixedDeltaTime = Math.max(v, 0.0001f); }
    public static float get_fixedUnscaledDeltaTime() { return fixedDeltaTime; }
    public static float get_timeScale() { return timeScale; }
    public static void set_timeScale(float v) {
        if (v < 0 || v > 100) throw new cs.System.ArgumentException("Time.timeScale is out of range. Needs to be between 0 and 100.");
        timeScale = v;
    }
    public static float get_maximumDeltaTime() { return maximumDeltaTime; }
    public static void set_maximumDeltaTime(float v) { maximumDeltaTime = v; }
    public static float get_smoothDeltaTime() { return smoothDeltaTime; }
    public static int get_frameCount() { return frameCount; }
    public static int get_renderedFrameCount() { return frameCount; }
    public static float get_realtimeSinceStartup() { return (float) (System.nanoTime() - START) / 1e9f; }
    public static double get_realtimeSinceStartupAsDouble() { return (System.nanoTime() - START) / 1e9; }
    public static float get_timeSinceLevelLoad() { return (float) (timeD - levelLoadD); }
    public static boolean get_inFixedTimeStep() { return inFixed; }
    public static float get_captureDeltaTime() { return 0; }
    public static void set_captureDeltaTime(float v) {}
    private static final long START = System.nanoTime();
}
