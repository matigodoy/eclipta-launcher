package ar.com.eclipta.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;

import java.util.TimeZone;

/**
 * What happens when the car wakes: animation, maintenance alert, then the chosen app.
 * A wake arms the alert; the first evaluation consumes it unless the clock or the speed say to wait.
 * Only runs from the wake broadcast and while Home is visible. Nothing stays behind.
 */
final class Startup {
    static final int NONE = 0, SHOW = 1, WAIT = 2;
    static final long NEVER = -1;
    private static final long GUARD_MS = 120_000, DAY_MS = 86_400_000L;
    private static final String KEY_PENDING = "alert_pending", KEY_LAST = "alert_last", KEY_BOOT = "alert_boot";

    static volatile boolean splashRunning, alertAlive;
    /** Another app covered the alert once already in this wake. */
    static boolean alertReshown;
    /** The car woke while the alert was up: its own close continues the chain. */
    private static boolean wokeDuringAlert;
    private static boolean bootChecked;

    private Startup() {}

    /** Pure, so the odd cases can be unit tested. soonestDays is Reminders.DUE_NONE when nothing has a date. */
    static int decide(boolean pending, boolean clockOk, boolean moving, boolean enabled, int soonestDays, long last, long now) {
        if (!pending) return NONE;
        if (!clockOk || moving) return WAIT;
        if (!enabled || soonestDays > 0 || guarded(last, now)) return NONE;
        return SHOW;
    }

    /** elapsedRealtime restarts on a reboot: a stored time in the future is from before it. */
    static boolean guarded(long last, long now) {
        return last != NEVER && now >= last && now - last < GUARD_MS;
    }

    /** On a real boot the clock starts at the firmware's build date and jumps seconds later. */
    static boolean clockPlausible(long nowMs, long buildTimeMs) {
        return nowMs >= buildTimeMs + DAY_MS;
    }

    /** From AccReceiver, once per wake. */
    static void onWake(Context c) {
        if (alertAlive) {
            wokeDuringAlert = true;
            return;
        }
        alertReshown = false;
        wokeDuringAlert = false;
        SharedPreferences prefs = prefs(c);
        if (!guarded(prefs.getLong(KEY_LAST, NEVER), SystemClock.elapsedRealtime())) {
            prefs.edit().putBoolean(KEY_PENDING, true).apply();
        }
        Profile p = Profile.load(c);
        if (p.splash && Brand.sting(c, Pack.load(c, p.pack), p) != null) {
            try {
                splashRunning = true;
                c.startActivity(new Intent(c, SplashActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION));
                return;
            } catch (Throwable t) {
                splashRunning = false;
                DebugLog.log("splash start error: " + t);
            }
        }
        next(c);
    }

    /** The animation ended, or there was none. */
    static void next(Context c) {
        if (evaluate(c, false) == SHOW) startAlert(c, true);
        else openApp(c);
    }

    /** Another app stopped the animation: the alert goes on top of it, and no app opens afterwards. */
    static void afterInterruptedSplash(Context c) {
        if (evaluate(c, false) == SHOW) startAlert(c, false);
    }

    /** Real boots and units without wake broadcasts. Reads the boot count once per process. */
    static void noteBoot(Context c) {
        if (bootChecked) return;
        bootChecked = true;
        int boot = Settings.Global.getInt(c.getContentResolver(), Settings.Global.BOOT_COUNT, 0);
        SharedPreferences prefs = prefs(c);
        if (prefs.getInt(KEY_BOOT, -1) != boot) {
            prefs.edit().putInt(KEY_BOOT, boot).putBoolean(KEY_PENDING, true).remove(KEY_LAST).apply();
        }
    }

    /** Home has been visible for a moment with the screen on. */
    static void fromHome(Context c, boolean moving) {
        if (splashRunning || alertAlive) return;
        if (evaluate(c, moving) == SHOW) startAlert(c, false);
    }

    /** The alert closed with an answer. Only the wake chain goes on to the chosen app. */
    static void afterAlert(Context c, boolean fromWake) {
        boolean woke = wokeDuringAlert;
        wokeDuringAlert = false;
        if (fromWake || woke) openApp(c);
    }

    static void openApp(Context c) {
        String app = Profile.load(c).accApp;
        if (!app.isEmpty()) Launch.start(c, Launch.resolve(c, app));
    }

    private static int evaluate(Context c, boolean moving) {
        SharedPreferences prefs = prefs(c);
        boolean pending = prefs.getBoolean(KEY_PENDING, false);
        if (!pending) return NONE;
        long nowMs = System.currentTimeMillis(), now = SystemClock.elapsedRealtime();
        int result = decide(true, clockPlausible(nowMs, Build.TIME), moving, Profile.load(c).remindAlert,
                Reminders.soonestDays(c, nowMs, TimeZone.getDefault()), prefs.getLong(KEY_LAST, NEVER), now);
        if (result == SHOW) prefs.edit().putBoolean(KEY_PENDING, false).putLong(KEY_LAST, now).apply();
        else if (result == NONE) prefs.edit().putBoolean(KEY_PENDING, false).apply();
        return result;
    }

    private static void startAlert(Context c, boolean fromWake) {
        try {
            c.startActivity(new Intent(c, MaintenanceAlertActivity.class)
                    .putExtra(MaintenanceAlertActivity.EXTRA_FROM_WAKE, fromWake)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Throwable t) {
            DebugLog.log("alert start error: " + t);
        }
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(Profile.PREFS, Context.MODE_PRIVATE);
    }
}
