package com.mati.a3launcher;

import android.content.Context;
import android.content.res.Resources;

import java.util.Locale;

/** Interface language. "auto": Portuguese on Portuguese units, else Spanish (these units ship in en-US). */
final class Lang {
    private Lang() {}

    static Locale resolve(Context c) {
        String l = c.getSharedPreferences(Profile.PREFS, Context.MODE_PRIVATE).getString("p_lang", "auto");
        if ("en".equals(l)) return new Locale("en", "US");
        if ("pt".equals(l)) return new Locale("pt", "BR");
        if ("es".equals(l)) return new Locale("es", "AR");
        Locale system = Resources.getSystem().getConfiguration().getLocales().get(0);
        return "pt".equals(system.getLanguage()) ? new Locale("pt", "BR") : new Locale("es", "AR");
    }
}
