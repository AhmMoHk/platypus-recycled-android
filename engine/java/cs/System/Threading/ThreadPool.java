package cs.System.Threading;

public final class ThreadPool {
    private ThreadPool() {}
    private static final java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newCachedThreadPool(new java.util.concurrent.ThreadFactory() {
        public java.lang.Thread newThread(Runnable r) { java.lang.Thread t = new java.lang.Thread(r, "pool"); t.setDaemon(true); return t; }
    });
    public static boolean QueueUserWorkItem(final WaitCallback cb) { return QueueUserWorkItem(cb, null); }
    public static boolean QueueUserWorkItem(final WaitCallback cb, final Object state) {
        pool.execute(new Runnable() {
            public void run() { try { cb.Invoke(state); } catch (Throwable e) { cs.UnityEngine.Debug.logExceptionAnyThread(e); } }
        });
        return true;
    }
}
