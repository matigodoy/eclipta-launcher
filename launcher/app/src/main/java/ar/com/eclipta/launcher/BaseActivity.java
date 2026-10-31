package ar.com.eclipta.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;

import java.util.Locale;

/**
 * Applies the user's language, the screen fit and the palette before any view is created.
 * The layouts are designed for 1024x600dp: on other panels the density is scaled so they fit.
 */
abstract class BaseActivity extends Activity {
    private static final float DESIGN_W = 1024f, DESIGN_H = 600f;
    private static final float KEEP_WITHIN = 0.03f;

    protected boolean night;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Configuration o = new Configuration();
        Locale locale = Lang.resolve(base);
        if (locale != null) o.setLocale(locale);
        DisplayMetrics m = new DisplayMetrics();
        ((WindowManager) base.getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(m);
        float fit = ScreenFit.density(Math.max(m.widthPixels, m.heightPixels), Math.min(m.widthPixels, m.heightPixels),
                DESIGN_W, DESIGN_H, m.density, KEEP_WITHIN);
        if (fit != m.density) {
            o.densityDpi = Math.round(fit * 160);
            o.screenWidthDp = Math.round(Math.max(m.widthPixels, m.heightPixels) / fit);
            o.screenHeightDp = Math.round(Math.min(m.widthPixels, m.heightPixels) / fit);
            o.smallestScreenWidthDp = o.screenHeightDp;
        }
        applyOverrideConfiguration(o);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Profile p = Profile.load(this);
        night = Theming.isNight(this, p);
        setTheme(Theming.style(p, night));
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }
}
