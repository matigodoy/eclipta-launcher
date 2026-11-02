package ar.com.eclipta.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

import java.util.Calendar;

/** Picks the palette. Palettes are plain theme styles: switching costs nothing while running. */
final class Theming {
    static final String KEY_LAT = "sun_lat";
    static final String KEY_LON = "sun_lon";
    static final String KEY_HEADLIGHTS = "headlights";
    private static final int NIGHT_FROM_HOUR = 20, NIGHT_TO_HOUR = 7;

    private Theming() {}

    static int style(Profile p, boolean night) {
        if (night) return R.style.Theme_Launcher_Night;
        if ("blue".equals(p.accent)) return R.style.Theme_Launcher_Blue;
        if ("gray".equals(p.accent)) return R.style.Theme_Launcher_Gray;
        return R.style.Theme_Launcher;
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

    /**
     * The accent glow behind the car, built here instead of in XML: on Android 9 a radial gradient with a
     * theme color loses its radius when the theme is applied, and inflating the layout crashes.
     */
    static Drawable carGlow(Context c) {
        GradientDrawable g = new GradientDrawable() {
            @Override
            protected void onBoundsChange(Rect r) {
                super.onBoundsChange(r);
                setGradientRadius(Math.min(r.width(), r.height()) / 2f);
            }
        };
        g.setShape(GradientDrawable.OVAL);
        g.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        g.setColors(new int[]{color(c, R.attr.uiAccent), 0});
        return g;
    }
}
