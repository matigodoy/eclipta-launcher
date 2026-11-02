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
    String title;                   // empty = pack title
    String body, engine, year;      // empty = pack default
    String plate;
    boolean plateShow;
    boolean customPhoto;
    boolean splash;
    String accApp;                  // flattened component opened on ignition, empty = none
    final String[] slots = new String[SLOTS]; // flattened component, empty = default
    boolean bigRail;                // bigger rail buttons on the glass layout
    String paint;                  // pack paint id
    String wheels;                  // wheel id, "original" = the photo's
    String layout;                  // "glass" or "mmi"
    String accent;                  // "red", "blue" or "gray"
    String night;                   // "off", "on", "sun" or "lights"
    int speedLimit;                 // km/h, 0 = off
    String lang;                    // "auto", "es", "en" or "pt"
    boolean remindHome, remindAlert;
    int remindLead;                 // days before a due date the home screen starts warning

    static Profile load(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Profile o = new Profile();
        o.greeting = p.getString("p_greeting", c.getString(R.string.greeting_name));
        o.pack = p.getString("p_pack", DEFAULT_PACK);
        o.title = p.getString("p_title", "");
        o.body = p.getString("p_body", "");
        o.engine = p.getString("p_engine", "");
        o.year = p.getString("p_year", "");
        o.plate = p.getString("p_plate", "");
        o.plateShow = p.getBoolean("p_plate_show", false);
        o.customPhoto = p.getBoolean("p_custom_photo", false);
        o.splash = p.getBoolean("p_splash", true);
        o.accApp = p.getString("p_acc_app", "");
        for (int i = 0; i < SLOTS; i++) o.slots[i] = p.getString("p_slot" + i, "");
        o.bigRail = p.getBoolean("p_big_rail", true);
        o.paint = p.getString("p_paint", "original");
        o.wheels = p.getString("p_wheels", "original");
        o.layout = p.getString("p_layout", "glass");
        o.accent = p.getString("p_accent", "red");
        o.night = p.getString("p_night", "off");
        o.speedLimit = p.getInt("p_speed_limit", 0);
        o.lang = p.getString("p_lang", "auto");
        o.remindHome = p.getBoolean("p_remind_home", true);
        o.remindAlert = p.getBoolean("p_remind_alert", true);
        o.remindLead = p.getInt("p_remind_lead", 30);
        return o;
    }

    void save(Context c) {
        SharedPreferences.Editor e = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        e.putString("p_greeting", greeting).putString("p_pack", pack).putString("p_title", title)
                .putString("p_body", body).putString("p_engine", engine).putString("p_year", year)
                .putString("p_plate", plate).putBoolean("p_plate_show", plateShow)
                .putBoolean("p_custom_photo", customPhoto).putBoolean("p_splash", splash)
                .putString("p_acc_app", accApp);
        for (int i = 0; i < SLOTS; i++) e.putString("p_slot" + i, slots[i]);
        e.putBoolean("p_big_rail", bigRail);
        e.putString("p_paint", paint).putString("p_wheels", wheels).putString("p_layout", layout)
                .putString("p_accent", accent).putString("p_night", night).putInt("p_speed_limit", speedLimit)
                .putString("p_lang", lang).putBoolean("p_remind_home", remindHome)
                .putBoolean("p_remind_alert", remindAlert).putInt("p_remind_lead", remindLead);
        e.apply();
    }

    /** The file users share: perfil.json. Paths and photos stay out of it. */
    JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("greeting", greeting).put("pack", pack).put("title", title).put("body", body).put("engine", engine).put("year", year);
        o.put("plate", new JSONObject().put("text", plate).put("show", plateShow));
        o.put("paint", paint).put("wheels", wheels).put("layout", layout).put("accent", accent).put("night", night).put("lang", lang);
        o.put("splash", splash).put("accApp", accApp).put("speedLimit", speedLimit);
        o.put("remindHome", remindHome).put("remindAlert", remindAlert).put("remindLead", remindLead);
        JSONArray rail = new JSONArray();
        for (String s : slots) rail.put(s);
        o.put("rail", rail).put("bigRail", bigRail);
        return o;
    }

    /** Applies a perfil.json. Unknown or missing keys leave the current value alone. */
    void applyJson(JSONObject o) {
        greeting = text(o, "greeting", greeting, 20);
        String importedPack = text(o, "pack", pack, 64);
        if (importedPack.matches(Files.PACK_ID)) pack = importedPack;
        title = text(o, "title", title, 24);
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
        if (o.has("accApp")) {
            String app = o.optString("accApp", "");
            if (app.isEmpty() || validApp(app)) accApp = app;
        } else if (o.has("carplayOnAcc")) {
            accApp = o.optBoolean("carplayOnAcc") ? Launch.CARPLAY : "";
        }
        speedLimit = Math.max(0, Math.min(300, o.optInt("speedLimit", speedLimit)));
        remindHome = o.optBoolean("remindHome", remindHome);
        remindAlert = o.optBoolean("remindAlert", remindAlert);
        int lead = o.optInt("remindLead", remindLead);
        for (int allowed : Reminders.LEAD_DAYS) if (allowed == lead) remindLead = lead;
        JSONArray rail = o.optJSONArray("rail");
        if (rail != null) {
            for (int i = 0; i < SLOTS && i < rail.length(); i++) slots[i] = rail.optString(i, slots[i]);
        }
        bigRail = o.optBoolean("bigRail", bigRail);
    }

    /** perfil.json is a file people share, and this one runs by itself on every ignition. */
    static boolean validApp(String flat) {
        return flat.length() <= 200 && flat.matches("[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+")
                && !flat.startsWith(BuildConfig.APPLICATION_ID + "/");
    }

    private static String text(JSONObject o, String key, String current, int maxLen) {
        if (!o.has(key) || o.isNull(key)) return current;
        String v = o.optString(key, current);
        return v.length() > maxLen ? v.substring(0, maxLen) : v;
    }

    private static String oneOf(JSONObject o, String key, String current, String... allowed) {
        String v = o.optString(key, current);
        for (String a : allowed) if (a.equals(v)) return v;
        return current;
    }

    /** "Buenas noches, Mati". */
    String greetingLine(Context c) {
        int h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        String g = c.getString(h >= 6 && h < 13 ? R.string.good_morning : h >= 13 && h < 20 ? R.string.good_afternoon : R.string.good_evening);
        return greeting.isEmpty() ? g : g + ", " + greeting;
    }

    /** A named custom photo: the car isn't the pack's, so nothing of the pack shows up with it. */
    boolean customCar() {
        return customPhoto && !title.isEmpty();
    }

    String displayTitle(Pack p) {
        return title.isEmpty() ? p.title : title;
    }

    /** "Sportback · 2.0 TDI · 2008", skipping empty parts. */
    String subtitle(Pack p) {
        boolean custom = customCar();
        StringBuilder b = new StringBuilder();
        for (String s : new String[]{or(body, custom ? "" : p.defBody), or(engine, custom ? "" : p.defEngine),
                or(year, custom ? "" : p.defYear)}) {
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
