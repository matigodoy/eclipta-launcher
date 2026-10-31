package com.mati.a3launcher;

import android.app.Activity;
import android.os.Bundle;

/** Applies the user's palette before any view is created. */
abstract class BaseActivity extends Activity {
    protected boolean night;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Profile p = Profile.load(this);
        night = Theming.isNight(this, p);
        setTheme(Theming.style(p, night));
        super.onCreate(savedInstanceState);
    }
}
