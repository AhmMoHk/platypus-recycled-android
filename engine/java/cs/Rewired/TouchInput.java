package cs.Rewired;

/** Values written by the host's on-screen controls (any thread); sampled once per frame. */
public final class TouchInput {
    private TouchInput() {}
    public static volatile float moveX, moveY;
    public static volatile boolean fire, pause, back;
    /** a touch started anywhere this frame (for "press any button") */
    public static volatile boolean anyTap;
}
