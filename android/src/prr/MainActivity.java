package prr;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import cs.Rewired.TouchInput;
import cs.UnityEngine.Application;
import cs.UnityEngine.Boot;
import cs.UnityEngine.Eng;
import cs.UnityEngine.Gfx;
import cs.UnityEngine.Host;
import cs.UnityEngine.Input;
import cs.UnityEngine.Render;
import cs.UnityEngine.Screen;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** The player: GL surface, frame loop, audio output, touch controls, fullscreen at any aspect ratio. */
public final class MainActivity extends Activity {
    private GLSurfaceView view;
    private final GlesGfx gfx = new GlesGfx();
    private final TouchControls controls = new TouchControls();
    private AndroidHost host;
    private volatile int targetFps = 60;
    private volatile boolean dataReady, booted, surfaceLost;
    private long lastFrameNanos;
    private Thread audioThread;
    private volatile boolean audioRun;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Settings.init(this);
        targetFps = Settings.fps();
        host = new AndroidHost(this);
        host.activity = this;
        Host.set(host);
        Gfx.set(gfx);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            try {
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.getClass().getField("layoutInDisplayCutoutMode").setInt(lp, 1);   // SHORT_EDGES: draw under the notch
                getWindow().setAttributes(lp);
            } catch (ReflectiveOperationException ignored) {}
        }
        immersive();
        dataReady = DataImport.isReady(host);
        view = new GLSurfaceView(this);
        view.setEGLContextClientVersion(2);
        view.setEGLConfigChooser(8, 8, 8, 8, 0, 8);
        view.setPreserveEGLContextOnPause(true);
        view.setRenderer(new GLSurfaceView.Renderer() {
            @Override public void onSurfaceCreated(GL10 gl, EGLConfig cfg) {
                gfx.init();
                surfaceLost = booted;
            }
            @Override public void onSurfaceChanged(GL10 gl, int w, int h) {
                host.w = w;
                host.h = h;
                Screen.$setSize(w, h);
                controls.layout();
            }
            @Override public void onDrawFrame(GL10 gl) { frame(); }
        });
        view.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        setContentView(view);
        if (!dataReady) DataImport.prompt(this, host);
    }

    private void immersive() {
        View d = getWindow().getDecorView();
        d.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    void setTargetFps(final int fps) {
        if (fps <= 0) return;
        targetFps = fps;
        runOnUiThread(new Runnable() { public void run() { pickDisplayMode(fps); } });
    }

    /** ask the display for a refresh rate that can show the chosen frame rate (90/120/144 Hz panels) */
    private void pickDisplayMode(int fps) {
        try {
            android.view.Display d = getWindowManager().getDefaultDisplay();
            android.view.Display.Mode cur = d.getMode(), best = null;
            for (android.view.Display.Mode m : d.getSupportedModes()) {
                if (m.getPhysicalWidth() != cur.getPhysicalWidth() || m.getPhysicalHeight() != cur.getPhysicalHeight()) continue;
                float hz = m.getRefreshRate();
                if (hz + 0.5f < fps) continue;
                if (best == null || hz < best.getRefreshRate()) best = m;
            }
            if (best == null) for (android.view.Display.Mode m : d.getSupportedModes())
                if (m.getPhysicalWidth() == cur.getPhysicalWidth() && (best == null || m.getRefreshRate() > best.getRefreshRate())) best = m;
            if (best != null) {
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.preferredDisplayModeId = best.getModeId();
                getWindow().setAttributes(lp);
            }
        } catch (RuntimeException e) {
            android.util.Log.w("prr", "display mode: " + e);
        }
    }

    /** native settings dialog (gear button): frame rate, 4K assets, touch controls, unlock everything */
    void openSettings() {
        runOnUiThread(new Runnable() {
            public void run() {
                final android.widget.LinearLayout l = new android.widget.LinearLayout(MainActivity.this);
                l.setOrientation(android.widget.LinearLayout.VERTICAL);
                int pad = (int) (16 * getResources().getDisplayMetrics().density);
                l.setPadding(pad, pad, pad, pad);
                android.widget.TextView fl = new android.widget.TextView(MainActivity.this);
                fl.setText("Frame rate / معدل الإطارات");
                l.addView(fl);
                final android.widget.RadioGroup rg = new android.widget.RadioGroup(MainActivity.this);
                rg.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                for (int f : Settings.FPS_OPTIONS) {
                    android.widget.RadioButton rb = new android.widget.RadioButton(MainActivity.this);
                    rb.setText(f + "");
                    rb.setId(f);
                    rg.addView(rb);
                }
                rg.check(Settings.fps());
                l.addView(rg);
                final android.widget.CheckBox tc = new android.widget.CheckBox(MainActivity.this);
                tc.setText("Touch controls / أزرار اللمس");
                tc.setChecked(Settings.showControls());
                l.addView(tc);
                final android.widget.CheckBox ua = new android.widget.CheckBox(MainActivity.this);
                ua.setText("Unlock everything (100%) / فتح كل شي");
                ua.setChecked(Settings.unlockAll());
                l.addView(ua);
                new android.app.AlertDialog.Builder(MainActivity.this)
                    .setTitle("Platypus Recycled")
                    .setView(l)
                    .setPositiveButton("OK", new android.content.DialogInterface.OnClickListener() {
                        public void onClick(android.content.DialogInterface di, int w) {
                            final int fps = rg.getCheckedRadioButtonId() > 0 ? rg.getCheckedRadioButtonId() : 60;
                            Settings.setFps(fps);
                            Settings.setShowControls(tc.isChecked());
                            final boolean unlock = ua.isChecked() && !Settings.unlockAll();
                            Settings.setUnlockAll(ua.isChecked());
                            immersive();
                            view.queueEvent(new Runnable() {
                                public void run() {
                                    cs.UnityEngine.Application.set_targetFrameRate(fps);
                                    controls.setVisible(tc.isChecked());
                                    if (unlock) Settings.unlockEverything();
                                }
                            });
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
                        public void onDismiss(android.content.DialogInterface di) { immersive(); }
                    })
                    .show();
            }
        });
    }

    // ------------------------------------------------------------------ frame loop
    private void frame() {
        if (!dataReady) {
            gfx.begin(Screen.get_width(), Screen.get_height());
            gfx.clear(true, true, 0.05f, 0.05f, 0.07f, 1f);
            gfx.end();
            sleepToFps();
            return;
        }
        try {
            if (!booted) {
                Boot.start(0);
                Settings.apply();
                controls.load(host);
                controls.setVisible(Settings.showControls());
                controls.onSettings = new Runnable() { public void run() { openSettings(); } };
                startAudio();
                booted = true;
                lastFrameNanos = System.nanoTime();
            }
            long now = System.nanoTime();
            float dt = (float) ((now - lastFrameNanos) / 1e9);
            lastFrameNanos = now;
            if (dt <= 0 || dt > 0.5f) dt = 1f / targetFps;
            if (backPulse > 0) { TouchInput.back = true; backPulse--; } else TouchInput.back = false;
            Eng.frame(dt);
            Render.renderFrame();
            controls.draw(gfx);
            Eng.endOfFrame();
        } catch (Throwable e) {
            cs.UnityEngine.Debug.logExceptionAnyThread(e);
        }
        sleepToFps();
    }

    private void sleepToFps() {
        long target = 1000000000L / Math.max(1, targetFps);
        long elapsed = System.nanoTime() - lastFrameNanos;
        long left = target - elapsed;
        if (left > 1000000L) {
            try { Thread.sleep(left / 1000000L); } catch (InterruptedException ignored) {}
        }
    }

    // ------------------------------------------------------------------ audio
    private void startAudio() {
        final int rate = 48000;
        int min = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        final int frames = Math.max(1024, min / 4);
        audioRun = true;
        audioThread = new Thread(new Runnable() {
            public void run() {
                AudioTrack t = new AudioTrack(AudioManager.STREAM_MUSIC, rate, AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT, Math.max(frames * 4, 8192), AudioTrack.MODE_STREAM);
                t.play();
                short[] buf = new short[frames * 2];
                while (audioRun) {
                    try {
                        cs.UnityEngine.AudioMix.mix(buf, frames, rate);
                        t.write(buf, 0, buf.length);
                    } catch (Throwable e) {
                        cs.UnityEngine.Debug.logExceptionAnyThread(e);
                        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                    }
                }
                t.stop();
                t.release();
            }
        }, "prr-audio");
        audioThread.setDaemon(true);
        audioThread.start();
    }

    // ------------------------------------------------------------------ input
    @Override public boolean onTouchEvent(MotionEvent e) {
        int h = Screen.get_height();
        int action = e.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int i = e.getActionIndex();
                int id = e.getPointerId(i);
                float x = e.getX(i), y = h - e.getY(i);
                if (!controls.down(id, x, y)) Input.$touch(id, 0, x, y);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                for (int i = 0; i < e.getPointerCount(); i++) {
                    int id = e.getPointerId(i);
                    float x = e.getX(i), y = h - e.getY(i);
                    if (!controls.move(id, x, y)) Input.$touch(id, 1, x, y);
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                int i = e.getActionIndex();
                int id = e.getPointerId(i);
                float x = e.getX(i), y = h - e.getY(i);
                if (!controls.up(id)) Input.$touch(id, 2, x, y);
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                controls.cancelAll();
                for (int i = 0; i < e.getPointerCount(); i++) Input.$touch(e.getPointerId(i), 3, 0, 0);
                break;
            }
        }
        return true;
    }

    private volatile int backPulse;

    @Override public boolean onKeyDown(int code, KeyEvent e) {
        if (code == KeyEvent.KEYCODE_BACK) {
            backPulse = 2;
            return true;
        }
        Input.$key(androidToUnity(code), true);
        int u = e.getUnicodeChar();
        if (u > 0) Input.$char((char) u);
        return true;
    }

    @Override public boolean onKeyUp(int code, KeyEvent e) {
        if (code == KeyEvent.KEYCODE_BACK) return true;
        Input.$key(androidToUnity(code), false);
        return true;
    }

    /** Android key codes -> Unity KeyCode values for the few keys the game checks */
    private static int androidToUnity(int code) {
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_UP: return 273;
            case KeyEvent.KEYCODE_DPAD_DOWN: return 274;
            case KeyEvent.KEYCODE_DPAD_RIGHT: return 275;
            case KeyEvent.KEYCODE_DPAD_LEFT: return 276;
            case KeyEvent.KEYCODE_ENTER: case KeyEvent.KEYCODE_DPAD_CENTER: case KeyEvent.KEYCODE_BUTTON_A: return 13;
            case KeyEvent.KEYCODE_ESCAPE: case KeyEvent.KEYCODE_BUTTON_B: return 27;
            case KeyEvent.KEYCODE_SPACE: return 32;
            case KeyEvent.KEYCODE_DEL: return 8;
            case KeyEvent.KEYCODE_TAB: return 9;
            default:
                if (code >= KeyEvent.KEYCODE_A && code <= KeyEvent.KEYCODE_Z) return 'a' + (code - KeyEvent.KEYCODE_A);
                if (code >= KeyEvent.KEYCODE_0 && code <= KeyEvent.KEYCODE_9) return '0' + (code - KeyEvent.KEYCODE_0);
                return 0;
        }
    }

    // ------------------------------------------------------------------ lifecycle
    @Override protected void onResume() {
        super.onResume();
        immersive();
        view.onResume();
        if (booted) {
            Application.$setPaused(false);
            Application.$setFocus(true);
        }
        dataReady = DataImport.isReady(host);
    }

    @Override protected void onPause() {
        super.onPause();
        controls.cancelAll();
        if (booted) {
            Application.$setFocus(false);
            Application.$setPaused(true);
        }
        view.onPause();
    }

    @Override protected void onDestroy() {
        audioRun = false;
        super.onDestroy();
    }

    @Override protected void onActivityResult(int req, int res, android.content.Intent data) {
        super.onActivityResult(req, res, data);
        if (req != DataImport.REQ) return;
        if (res != RESULT_OK || data == null || data.getData() == null) {
            DataImport.prompt(this, host);
            return;
        }
        DataImport.onResult(this, host, data.getData(), new Runnable() {
            public void run() { dataReady = true; }
        });
    }

    @Override public void onWindowFocusChanged(boolean has) {
        super.onWindowFocusChanged(has);
        if (has) immersive();
        if (booted) Application.$setFocus(has);
    }
}
