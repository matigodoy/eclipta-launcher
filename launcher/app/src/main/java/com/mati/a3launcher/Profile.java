package com.mati.a3launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

/** The user's car and launcher preferences. Plain fields, stored in SharedPreferences. */
final class Profile {
    static final String PREFS = "a3l";
    static final int SLOTS = 4;
    static final String DEFAULT_PACK = "audi-a3-8p";

    String greeting;
    String pack;
    String body, engine, year;      // empty = pack default
    String plate;
    boolean plateShow;
    boolean customPhoto;
    boolean splash;
    boolean carplayOnAcc;
    final String[] slots = new String[SLOTS]; // flattened component, empty = default

    static Profile load(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Profile o = new Profile();
        o.greeting = p.getString("p_greeting", c.getString(R.string.greeting_name));
        o.pack = p.getString("p_pack", DEFAULT_PACK);
        o.body = p.getString("p_body", "");
        o.engine = p.getString("p_engine", "");
        o.year = p.getString("p_year", "");
        o.plate = p.getString("p_plate", "");
        o.plateShow = p.getBoolean("p_plate_show", false);
        o.customPhoto = p.getBoolean("p_custom_photo", false);
        o.splash = p.getBoolean("p_splash", true);
        o.carplayOnAcc = p.getBoolean("p_carplay_acc", false);
        for (int i = 0; i < SLOTS; i++) o.slots[i] = p.getString("p_slot" + i, "");
        return o;
    }

    void save(Context c) {
        SharedPreferences.Editor e = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        e.putString("p_greeting", greeting).putString("p_pack", pack)
                .putString("p_body", body).putString("p_engine", engine).putString("p_year", year)
                .putString("p_plate", plate).putBoolean("p_plate_show", plateShow)
                .putBoolean("p_custom_photo", customPhoto).putBoolean("p_splash", splash)
                .putBoolean("p_carplay_acc", carplayOnAcc);
        for (int i = 0; i < SLOTS; i++) e.putString("p_slot" + i, slots[i]);
        e.apply();
    }

    /** "Sportback · 2.0 TDI · 2008", skipping empty parts. */
    String subtitle(Pack p) {
        StringBuilder b = new StringBuilder();
        for (String s : new String[]{or(body, p.defBody), or(engine, p.defEngine), or(year, p.defYear)}) {
            if (TextUtils.isEmpty(s)) continue;
            if (b.length() > 0) b.append(" · ");
            b.append(s);
        }
        return b.toString();
    }

    private static String or(String a, String b) {
        return TextUtils.isEmpty(a) ? b : a;
    }
}
