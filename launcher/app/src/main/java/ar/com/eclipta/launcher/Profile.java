package ar.com.eclipta.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** The user's car and launcher preferences. Plain fields, stored in SharedPreferences. */
final class Profile {
    static final String PREFS = "launcher";
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
    String paint;                   // pack paint id
    String wheels;                  // wheel id, "original" = the photo's
    String layout;                  // "glass" or "mmi"
    String accent;                  // "red", "blue" or "gray"
    String night;                   // "off", "on", "sun" or "lights"
    int speedLimit;                 // km/h, 0 = off
    String lang;                    // "auto", "es", "en" or "pt"
    String dueVtv, dueInsurance, dueService; // yyyy-MM-dd, empty = off

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
        o.paint = p.getString("p_paint", "original");
        o.wheels = p.getString("p_wheels", "original");
        o.layout = p.getString("p_layout", "glass");
        o.accent = p.getString("p_accent", "red");
        o.night = p.getString("p_night", "off");
        o.speedLimit = p.getInt("p_speed_limit", 0);
        o.lang = p.getString("p_lang", "auto");
        o.dueVtv = p.getString("p_due_vtv", "");
        o.dueInsurance = p.getString("p_due_insurance", "");
        o.dueService = p.getString("p_due_service", "");
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
        e.putString("p_paint", paint).putString("p_wheels", wheels).putString("p_layout", layout)
                .putString("p_accent", accent).putString("p_night", night).putInt("p_speed_limit", speedLimit)
                .putString("p_lang", lang).putString("p_due_vtv", dueVtv).putString("p_due_insurance", dueInsurance)
                .putString("p_due_service", dueService);
        e.apply();
    }

    /** The file users share: perfil.json. Paths and photos stay out of it. */
    JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("greeting", greeting).put("pack", pack).put("body", body).put("engine", engine).put("year", year);
        o.put("plate", new JSONObject().put("text", plate).put("show", plateShow));
        o.put("paint", paint).put("wheels", wheels).put("layout", layout).put("accent", accent).put("night", night).put("lang", lang);
        o.put("splash", splash).put("carplayOnAcc", carplayOnAcc).put("speedLimit", speedLimit);
        o.put("dueVtv", dueVtv).put("dueInsurance", dueInsurance).put("dueService", dueService);
        JSONArray rail = new JSONArray();
        for (String s : slots) rail.put(s);
        o.put("rail", rail);
        return o;
    }

    /** Applies a perfil.json. Unknown or missing keys leave the current value alone. */
    void applyJson(JSONObject o) {
        greeting = text(o, "greeting", greeting, 20);
        pack = text(o, "pack", pack, 64);
        body = text(o, "body", body, 24);
        engine = text(o, "engine", engine, 32);
        year = text(o, "year", year, 4);
        JSONObject pl = o.optJSONObject("plate");
        if (pl != null) {
            plate = text(pl, "text", plate, 10);
            plateShow = pl.optBoolean("show", plateShow);
        }
        paint = text(o, "paint", paint, 32);
        wheels = text(o, "wheels", wheels, 32);
        layout = oneOf(o, "layout", layout, "glass", "mmi");
        accent = oneOf(o, "accent", accent, "red", "blue", "gray");
        night = oneOf(o, "night", night, "off", "on", "sun", "lights");
        lang = oneOf(o, "lang", lang, "auto", "es", "en", "pt");
        splash = o.optBoolean("splash", splash);
        carplayOnAcc = o.optBoolean("carplayOnAcc", carplayOnAcc);
        speedLimit = Math.max(0, Math.min(300, o.optInt("speedLimit", speedLimit)));
        dueVtv = text(o, "dueVtv", dueVtv, 10);
        dueInsurance = text(o, "dueInsurance", dueInsurance, 10);
        dueService = text(o, "dueService", dueService, 10);
        JSONArray rail = o.optJSONArray("rail");
        if (rail != null) {
            for (int i = 0; i < SLOTS && i < rail.length(); i++) slots[i] = rail.optString(i, slots[i]);
        }
    }

    private static String text(JSONObject o, String key, String current, int maxLen) {
        if (!o.has(key)) return current;
        String v = o.optString(key, current);
        return v.length() > maxLen ? v.substring(0, maxLen) : v;
    }

    private static String oneOf(JSONObject o, String key, String current, String... allowed) {
        String v = o.optString(key, current);
        for (String a : allowed) if (a.equals(v)) return v;
        return current;
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
