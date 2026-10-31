package com.mati.a3launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.widget.Toast;

/** Opening other apps and resolving the rail's shortcuts. */
final class Launch {
    static final String CARPLAY = "com.autochips.carplayapp/com.autochips.carplayapp.CarPlayActivity";
    static final String ANDROID_AUTO = "com.google.android.projection.sink/com.google.android.projection.sink.ui.AndroidAutoActivity";
    static final String RADIO = "com.jancar.radio/com.jancar.radio.activity.RadioMainActivity";
    static final String MUSIC = "com.jancar.music/com.jancar.music.activity.MainActivity";
    static final String SETTINGS = "com.jancar.settings/com.jancar.settings.ui.activity.MainActivity";
    private static final String[] DEFAULT_SLOTS = {CARPLAY, ANDROID_AUTO, RADIO, MUSIC};

    private Launch() {}

    static String slot(Profile p, int i) {
        return p.slots[i].isEmpty() ? DEFAULT_SLOTS[i] : p.slots[i];
    }

    static boolean installed(Context c, String flat) {
        ComponentName cn = ComponentName.unflattenFromString(flat);
        if (cn == null) return false;
        try {
            c.getPackageManager().getPackageInfo(cn.getPackageName(), 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /** Vector for JanCar's radio and music (their icons are busy at 50dp), else the app's icon. */
    static Drawable icon(Context c, String flat) {
        if (RADIO.equals(flat)) return c.getDrawable(R.drawable.ic_radio);
        if (MUSIC.equals(flat)) return c.getDrawable(R.drawable.ic_music);
        ComponentName cn = ComponentName.unflattenFromString(flat);
        if (cn != null) {
            PackageManager pm = c.getPackageManager();
            try {
                return pm.getActivityIcon(cn);
            } catch (PackageManager.NameNotFoundException e) {
                try {
                    return pm.getApplicationIcon(cn.getPackageName());
                } catch (PackageManager.NameNotFoundException ignored) {
                }
            }
        }
        return c.getDrawable(R.drawable.ic_apps);
    }

    static void open(Context c, String flat) {
        if (start(c, flat)) return;
        Toast.makeText(c, R.string.cannot_open, Toast.LENGTH_SHORT).show();
    }

    /** Tries the exact activity, then the package's launcher activity. */
    static boolean start(Context c, String flat) {
        ComponentName cn = ComponentName.unflattenFromString(flat);
        if (cn == null) return false;
        Intent i = new Intent(Intent.ACTION_MAIN).setComponent(cn)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            c.startActivity(i);
            return true;
        } catch (Throwable t) {
            DebugLog.log("launch " + flat + " failed: " + t);
        }
        try {
            Intent fallback = c.getPackageManager().getLaunchIntentForPackage(cn.getPackageName());
            if (fallback != null) {
                c.startActivity(fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return true;
            }
        } catch (Throwable t) {
            DebugLog.log("launch " + flat + " fallback failed: " + t);
        }
        return false;
    }
}
