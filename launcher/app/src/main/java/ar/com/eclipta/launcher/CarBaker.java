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

    private static final String KEY_DIRTY = "bake_dirty", KEY_VERSION = "bake_version";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Object LOCK = new Object();
    private static boolean running, again;
    private static Listener listener, homeListener;

    private CarBaker() {}

    /** Main thread only. */
    static void setListener(Listener l) {
        listener = l;
    }

    /** Home, while visible: Settings can go away (Home key, power-off) with a bake still running. */
    static void setHomeListener(Listener l) {
        homeListener = l;
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

    /** Also after an update: a picture baked by an older version can show a pack that is gone (0.7.0 took the built-in Audi out). */
    static boolean isDirty(Context c) {
        android.content.SharedPreferences p = prefs(c);
        return p.getBoolean(KEY_DIRTY, false) || p.getInt(KEY_VERSION, 0) != BuildConfig.VERSION_CODE;
    }

    static boolean needsBake(Context c) {
        return isDirty(c) || !CarImage.bakedFile(c).isFile();
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
                    CarImage.bake(app, Profile.load(app));
                    synchronized (LOCK) {
                        if (again) continue;
                        running = false;
                    }
                    // Even if it failed: a pack that can't be baked won't get better by retrying.
                    prefs(app).edit().putBoolean(KEY_DIRTY, false).putInt(KEY_VERSION, BuildConfig.VERSION_CODE).apply();
                    break;
                }
                MAIN.post(new Runnable() {
                    @Override
                    public void run() {
                        if (listener != null) listener.onBaked();
                        if (homeListener != null) homeListener.onBaked();
                    }
                });
            }
        }, "launcher-bake").start();
    }

    private static android.content.SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(Profile.PREFS, Context.MODE_PRIVATE);
    }
}
