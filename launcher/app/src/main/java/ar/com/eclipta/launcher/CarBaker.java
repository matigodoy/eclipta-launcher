package ar.com.eclipta.launcher;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

/**
 * Rebuilds the car picture on a thread that exists only while there is work. Requests coalesce, and
 * each pass reads the profile again, so the last picture written always matches the last change.
 */
final class CarBaker {
    interface Listener {
        void onBaked();
    }

    private static final String KEY_DIRTY = "bake_dirty";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Object LOCK = new Object();
    private static boolean running, again;
    private static Listener listener;

    private CarBaker() {}

    /** Main thread only. */
    static void setListener(Listener l) {
        listener = l;
    }

    static boolean isBusy() {
        synchronized (LOCK) {
            return running;
        }
    }

    /** Remembered until a bake succeeds, so a change that got killed halfway isn't lost. */
    static void markDirty(Context c) {
        prefs(c).edit().putBoolean(KEY_DIRTY, true).apply();
    }

    static boolean needsBake(Context c) {
        return prefs(c).getBoolean(KEY_DIRTY, false) || !CarImage.bakedFile(c).isFile();
    }

    static void bake(Context c) {
        final Context app = c.getApplicationContext();
        synchronized (LOCK) {
            if (running) {
                again = true;
                return;
            }
            running = true;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    synchronized (LOCK) {
                        again = false;
                    }
                    boolean ok = CarImage.bake(app, Profile.load(app));
                    synchronized (LOCK) {
                        if (again) continue;
                        running = false;
                    }
                    if (ok) prefs(app).edit().putBoolean(KEY_DIRTY, false).apply();
                    break;
                }
                MAIN.post(new Runnable() {
                    @Override
                    public void run() {
                        if (listener != null) listener.onBaked();
                    }
                });
            }
        }, "launcher-bake").start();
    }

    private static android.content.SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(Profile.PREFS, Context.MODE_PRIVATE);
    }
}
