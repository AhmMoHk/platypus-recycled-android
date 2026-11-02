package cs.System.Threading;

/** lock(){}: the game runs on one thread; a re-entrant lock keeps loading threads safe. */
public final class Monitor {
    private Monitor() {}
    private static final java.util.WeakHashMap<Object, java.util.concurrent.locks.ReentrantLock> locks = new java.util.WeakHashMap<>();
    private static synchronized java.util.concurrent.locks.ReentrantLock lockOf(Object o) {
        java.util.concurrent.locks.ReentrantLock l = locks.get(o);
        if (l == null) locks.put(o, l = new java.util.concurrent.locks.ReentrantLock());
        return l;
    }
    public static void Enter(Object o, cs.rt.RefZ taken) { lockOf(o).lock(); taken.v = true; }
    public static void Enter(Object o) { lockOf(o).lock(); }
    public static void Exit(Object o) { java.util.concurrent.locks.ReentrantLock l = lockOf(o); if (l.isHeldByCurrentThread()) l.unlock(); }
}
