package ar.com.eclipta.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

/**
 * Wake signals: JanCar ACC broadcast "com.jancar.services.action.acc" (boolean extra "acc")
 * and AutoChips "autochips.intent.action.QB_POWERON". On wake, shows the animation once
 * (debounced) and optionally opens CarPlay. Returns immediately.
 */
public class AccReceiver extends BroadcastReceiver {
    private static final long DEBOUNCE_MS = 20_000;
    private static long lastShown = -DEBOUNCE_MS;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        boolean acc;
        DebugLog.init(context);
        if (JanCar.ACTION_QB_POWERON.equals(action)) {
            acc = true;
            DebugLog.log("quick boot power on");
        } else if (JanCar.ACTION_ACC.equals(action)) {
            boolean hasExtra = intent.hasExtra(JanCar.EXTRA_ACC);
            acc = intent.getBooleanExtra(JanCar.EXTRA_ACC, false);
            DebugLog.log("acc broadcast: acc=" + acc + (hasExtra ? "" : " (no extra)"));
        } else {
            return;
        }
        if (!acc) return;
        long now = SystemClock.elapsedRealtime();
        if (now - lastShown < DEBOUNCE_MS) return;
        lastShown = now;
        Profile p = Profile.load(context);
        if (!p.splash || Brand.sting(context, Pack.load(context, p.pack), p) == null) {
            if (p.carplayOnAcc) Launch.start(context, Launch.CARPLAY);
            return;
        }
        try {
            context.startActivity(new Intent(context, SplashActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION));
        } catch (Throwable t) {
            DebugLog.log("splash start error: " + t);
        }
    }
}
