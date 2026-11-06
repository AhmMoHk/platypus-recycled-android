package prr;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayDeque;

/**
 * On-device diagnostics: keeps the last log lines (also in Android/data/<pkg>/files/prr-log.txt), and a
 * watchdog that shows on screen where the game thread is stuck when no frame has finished for a few seconds,
 * or the latest errors when loading does not progress. Tapping the panel shares the log.
 */
final class Diag {
    private Diag() {}
    private static final ArrayDeque<String> lines = new ArrayDeque<>();
    private static FileWriter file;
    static volatile long heartbeat = System.nanoTime();
    static volatile Thread gameThread;
    private static TextView panel;
    private static Activity act;

    static synchronized void add(String s) {
        String l = (System.currentTimeMillis() / 1000 % 100000) + " " + s;
        lines.addLast(l);
        while (lines.size() > 400) lines.removeFirst();
        try {
            if (file != null) { file.write(l); file.write('\n'); file.flush(); }
        } catch (Exception ignored) {}
    }

    static synchronized String text(int last) {
        StringBuilder sb = new StringBuilder();
        int skip = Math.max(0, lines.size() - last), i = 0;
        for (String l : lines) if (i++ >= skip) sb.append(l).append('\n');
        return sb.toString();
    }

    static void init(final Activity a, android.widget.FrameLayout root) {
        act = a;
        try { file = new FileWriter(new File(a.getExternalFilesDir(null), "prr-log.txt"), false); } catch (Exception ignored) {}
        panel = new TextView(a);
        panel.setTextColor(Color.WHITE);
        panel.setBackgroundColor(0xCC000000);
        panel.setTextSize(10);
        panel.setTypeface(android.graphics.Typeface.MONOSPACE);
        int pad = (int) (8 * a.getResources().getDisplayMetrics().density);
        panel.setPadding(pad, pad, pad, pad);
        panel.setVisibility(View.GONE);
        panel.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { share(a); } });
        root.addView(panel, new android.widget.FrameLayout.LayoutParams(-1, -1));
        Thread w = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try { Thread.sleep(1000); } catch (InterruptedException e) { return; }
                    long stall = (System.nanoTime() - heartbeat) / 1000000L;
                    Thread g = gameThread;
                    if (g != null && stall > 6000 && !paused) {
                        StringBuilder sb = new StringBuilder("The game thread has not finished a frame for " + stall / 1000 + " s. It is at:\n");
                        StackTraceElement[] st = g.getStackTrace();
                        for (int i = 0; i < Math.min(18, st.length); i++) sb.append("  ").append(st[i]).append('\n');
                        if (!stallShown) add(sb.toString());
                        sb.append("\nLast log:\n").append(text(14)).append("\n(tap to share the log)");
                        show(sb.toString());
                    } else if (stall < 2000) {
                        hideIfStall();
                    }
                }
            }
        }, "prr-watchdog");
        w.setDaemon(true);
        w.start();
    }

    static volatile boolean paused;
    private static volatile boolean stallShown;

    static void show(final String s) {
        stallShown = true;
        if (act != null) act.runOnUiThread(new Runnable() { public void run() { panel.setText(s); panel.setVisibility(View.VISIBLE); } });
    }

    /** loading did not progress: show the latest errors */
    static void showErrors(String title) {
        show(title + "\n\n" + text(30) + "\n(tap to share the log)");
    }

    private static void hideIfStall() {
        if (!stallShown || act == null) return;
        stallShown = false;
        act.runOnUiThread(new Runnable() { public void run() { panel.setVisibility(View.GONE); } });
    }

    static void share(Activity a) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, "Platypus Recycled log");
        i.putExtra(Intent.EXTRA_TEXT, text(400));
        a.startActivity(Intent.createChooser(i, "Share log"));
    }
}
