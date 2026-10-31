package com.mati.a3launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;

import java.util.Calendar;

/** Picks the palette. Palettes are plain theme styles: switching costs nothing while running. */
final class Theming {
    static final String KEY_LAT = "sun_lat";
    static final String KEY_LON = "sun_lon";
    static final String KEY_HEADLIGHTS = "headlights";
    private static final int NIGHT_FROM_HOUR = 20, NIGHT_TO_HOUR = 7;

    private Theming() {}

    static int style(Profile p, boolean night) {
        if (night) return R.style.Theme_A3_Night;
        if ("blue".equals(p.accent)) return R.style.Theme_A3_Blue;
        if ("gray".equals(p.accent)) return R.style.Theme_A3_Gray;
        return R.style.Theme_A3;
    }

    static boolean isNight(Context c, Profile p) {
        SharedPreferences prefs = c.getSharedPreferences(Profile.PREFS, Context.MODE_PRIVATE);
        switch (p.night) {
            case "on":
                return true;
            case "lights":
                return prefs.getBoolean(KEY_HEADLIGHTS, false);
            case "sun":
                if (prefs.contains(KEY_LAT)) {
                    return Sun.isDark(System.currentTimeMillis(), prefs.getFloat(KEY_LAT, 0), prefs.getFloat(KEY_LON, 0));
                }
                int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                return h >= NIGHT_FROM_HOUR || h < NIGHT_TO_HOUR;
            default:
                return false;
        }
    }

    static int color(Context c, int attr) {
        TypedArray a = c.obtainStyledAttributes(new int[]{attr});
        int v = a.getColor(0, 0xFFFFFFFF);
        a.recycle();
        return v;
    }
}
