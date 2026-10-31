package ar.com.eclipta.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

/**
 * Maintenance dates: VTV, insurance, timing belt and so on. Checked only when the home screen opens
 * or the car wakes: no alarms, nothing in the background. By date only, the car doesn't report km.
 */
final class Reminders {
    static final int DUE_NONE = Integer.MAX_VALUE;
    static final int MAX_CUSTOM = 12, MAX_NAME = 24, MAX_NOTE = 60, MAX_EVERY = 120;
    static final int[] LEAD_DAYS = {7, 15, 30, 60};
    static final int[] EVERY_MONTHS = {0, 6, 12, 24, 36, 48, 60};
    private static final long DAY = 86_400_000L;
    private static final String KEY = "reminders", KEY_NEXT = "rem_next";
    private static final String[] IDS = {"vtv", "insurance", "service", "oil", "belt", "brakes", "tires", "battery", "extinguisher"};
    private static final int[] EVERY = {12, 12, 12, 12, 60, 24, 48, 36, 12};
    private static final int[] NAMES = {R.string.rem_vtv, R.string.rem_insurance, R.string.rem_service, R.string.rem_oil,
            R.string.rem_belt, R.string.rem_brakes, R.string.rem_tires, R.string.rem_battery, R.string.rem_extinguisher};

    static final class Item {
        String id = "", name = "", due = "", note = "", doneAt = "";
        int every;
        boolean notify = true;

        boolean custom() {
            return catalogIndex(id) < 0;
        }
    }

    private Reminders() {}

    // ---------------------------------------------------------------- dates

    /** Whole calendar days from nowMs to a yyyy-MM-dd date. DUE_NONE if empty or invalid. */
    static int daysLeft(String date, long nowMs, TimeZone tz) {
        if (date == null || date.isEmpty()) return DUE_NONE;
        try {
            SimpleDateFormat f = format(tz);
            long due = f.parse(date).getTime();
            long today = f.parse(f.format(new java.util.Date(nowMs))).getTime();
            return (int) Math.round((due - today) / (double) DAY);
        } catch (ParseException e) {
            return DUE_NONE;
        }
    }

    static String today(long nowMs, TimeZone tz) {
        return format(tz).format(new java.util.Date(nowMs));
    }

    /** Jan 31 plus a month is the last day of February. Empty if the date is invalid. */
    static String addMonths(String date, int months, TimeZone tz) {
        try {
            SimpleDateFormat f = format(tz);
            Calendar c = new GregorianCalendar(tz);
            c.setTime(f.parse(date));
            c.add(Calendar.MONTH, months);
            return f.format(c.getTime());
        } catch (ParseException e) {
            return "";
        }
    }

    static boolean validDate(String date) {
        return date != null && date.matches("\\d{4}-\\d{2}-\\d{2}") && daysLeft(date, 0, TimeZone.getTimeZone("UTC")) != DUE_NONE;
    }

    private static SimpleDateFormat format(TimeZone tz) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        f.setLenient(false);
        f.setTimeZone(tz);
        return f;
    }

    // ---------------------------------------------------------------- logic

    /** The service was done: the next due date counts from today, or there is none without a repeat. */
    static void markDone(Item item, long nowMs, TimeZone tz) {
        item.doneAt = today(nowMs, tz);
        item.due = item.every > 0 ? addMonths(item.doneAt, item.every, tz) : "";
    }

    /** What the start-up alert lists: due today or earlier, most overdue first. */
    static List<Item> expired(List<Item> items, long nowMs, TimeZone tz) {
        return within(items, 0, nowMs, tz);
    }

    /** What the home screen warns about, most urgent first. */
    static List<Item> upcoming(List<Item> items, int leadDays, long nowMs, TimeZone tz) {
        return within(items, leadDays, nowMs, tz);
    }

    private static List<Item> within(List<Item> items, int days, final long nowMs, final TimeZone tz) {
        List<Item> out = new ArrayList<>();
        for (Item i : items) {
            int d = daysLeft(i.due, nowMs, tz);
            if (i.notify && d != DUE_NONE && d <= days) out.add(i);
        }
        Collections.sort(out, new Comparator<Item>() {
            @Override
            public int compare(Item a, Item b) {
                return a.due.compareTo(b.due);
            }
        });
        return out;
    }

    /** The soonest date of anything that notifies. Saved apart so the common case never parses the list. */
    static String soonest(List<Item> items) {
        String next = "";
        for (Item i : items) {
            if (i.notify && validDate(i.due) && (next.isEmpty() || i.due.compareTo(next) < 0)) next = i.due;
        }
        return next;
    }

    /** Service-type items that usually go by km. */
    static boolean goesByKm(Item item) {
        switch (item.id) {
            case "service":
            case "oil":
            case "belt":
            case "brakes":
            case "tires":
                return true;
            default:
                return false;
        }
    }

    private static int catalogIndex(String id) {
        for (int i = 0; i < IDS.length; i++) if (IDS[i].equals(id)) return i;
        return -1;
    }

    static List<Item> catalog() {
        List<Item> out = new ArrayList<>();
        for (int i = 0; i < IDS.length; i++) {
            Item item = new Item();
            item.id = IDS[i];
            item.every = EVERY[i];
            out.add(item);
        }
        return out;
    }

    /** Null when the list is already at the limit. The id stays unique even after deleting items. */
    static Item addCustom(List<Item> items, String name) {
        int count = 0, max = 0;
        for (Item i : items) {
            if (!i.custom()) continue;
            count++;
            max = Math.max(max, Integer.parseInt(i.id.substring(1)));
        }
        if (count >= MAX_CUSTOM) return null;
        Item item = new Item();
        item.id = "c" + (max + 1);
        item.name = cut(name.trim(), MAX_NAME);
        items.add(item);
        return item;
    }

    // ---------------------------------------------------------------- JSON

    /** Catalog merged with stored values, so catalog entries added later show up. Anything invalid is dropped. */
    static List<Item> fromJson(JSONArray stored) {
        List<Item> out = catalog();
        if (stored == null) return out;
        Set<String> seen = new HashSet<>();
        int customs = 0;
        for (int i = 0; i < stored.length(); i++) {
            JSONObject o = stored.optJSONObject(i);
            if (o == null) continue;
            String id = text(o, "id", 24);
            if (!id.matches("[a-z0-9-]{1,24}") || !seen.add(id)) continue;
            Item item;
            int index = catalogIndex(id);
            if (index >= 0) {
                item = out.get(index);
            } else {
                String name = text(o, "name", MAX_NAME).trim();
                if (!id.matches("c\\d{1,9}") || name.isEmpty() || customs >= MAX_CUSTOM) continue;
                customs++;
                item = new Item();
                item.id = id;
                item.name = name;
                out.add(item);
            }
            String due = text(o, "due", 10);
            item.due = validDate(due) ? due : "";
            String done = text(o, "doneAt", 10);
            item.doneAt = validDate(done) ? done : "";
            item.every = Math.max(0, Math.min(MAX_EVERY, o.optInt("every", item.every)));
            item.notify = o.optBoolean("notify", true);
            item.note = text(o, "note", MAX_NOTE);
        }
        return out;
    }

    static JSONArray toJson(List<Item> items) throws JSONException {
        JSONArray a = new JSONArray();
        for (Item i : items) {
            JSONObject o = new JSONObject().put("id", i.id).put("due", i.due).put("every", i.every).put("notify", i.notify);
            if (i.custom()) o.put("name", i.name);
            if (!i.note.isEmpty()) o.put("note", i.note);
            if (!i.doneAt.isEmpty()) o.put("doneAt", i.doneAt);
            a.put(o);
        }
        return a;
    }

    /**
     * From a perfil.json. The new format replaces the list. The old one only knew three dates, so
     * everything else stays as it is. Null if the file says nothing about reminders.
     */
    static List<Item> fromProfile(JSONObject o, List<Item> current) {
        JSONArray all = o.optJSONArray("reminders");
        if (all != null) return fromJson(all);
        String[][] old = {{"dueVtv", "vtv"}, {"dueInsurance", "insurance"}, {"dueService", "service"}};
        boolean found = false;
        for (String[] pair : old) {
            if (!o.has(pair[0])) continue;
            found = true;
            String due = text(o, pair[0], 10);
            if (!validDate(due)) continue;
            for (Item item : current) if (item.id.equals(pair[1])) item.due = due;
        }
        return found ? current : null;
    }

    private static String text(JSONObject o, String key, int maxLen) {
        if (!o.has(key) || o.isNull(key)) return "";
        return cut(o.optString(key, ""), maxLen);
    }

    private static String cut(String s, int maxLen) {
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }

    // ---------------------------------------------------------------- storage

    static List<Item> load(Context c) {
        String raw = prefs(c).getString(KEY, null);
        if (raw == null) return catalog();
        try {
            return fromJson(new JSONArray(raw));
        } catch (JSONException e) {
            DebugLog.log("reminders error: " + e);
            return catalog();
        }
    }

    static void save(Context c, List<Item> items) {
        try {
            prefs(c).edit().putString(KEY, toJson(items).toString()).putString(KEY_NEXT, soonest(items)).apply();
        } catch (JSONException e) {
            DebugLog.log("reminders error: " + e);
        }
    }

    /** Days to the soonest date that notifies, without parsing the list. DUE_NONE if there is none. */
    static int soonestDays(Context c, long nowMs, TimeZone tz) {
        return daysLeft(prefs(c).getString(KEY_NEXT, ""), nowMs, tz);
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(Profile.PREFS, Context.MODE_PRIVATE);
    }

    // ---------------------------------------------------------------- text

    static String name(Context c, Item item) {
        int index = catalogIndex(item.id);
        return index >= 0 ? c.getString(NAMES[index]) : item.name;
    }

    /** "venció hace 3 días", "vence hoy", "faltan 12 días", "vence el 31 oct 2027", "sin fecha". */
    static String status(Context c, Item item, int leadDays, long nowMs, TimeZone tz) {
        int d = daysLeft(item.due, nowMs, tz);
        if (d == DUE_NONE) return c.getString(R.string.rem_none);
        if (d < 0) return c.getResources().getQuantityString(R.plurals.rem_overdue, -d, -d);
        if (d == 0) return c.getString(R.string.rem_today);
        if (d <= leadDays) return c.getResources().getQuantityString(R.plurals.rem_left, d, d);
        return c.getString(R.string.rem_on, shortDate(c, item.due, tz));
    }

    /** "31 oct 2027" in the interface's language. */
    static String shortDate(Context c, String date, TimeZone tz) {
        try {
            Locale locale = c.getResources().getConfiguration().getLocales().get(0);
            SimpleDateFormat show = new SimpleDateFormat("d MMM yyyy", locale);
            show.setTimeZone(tz);
            return show.format(format(tz).parse(date));
        } catch (ParseException e) {
            return date;
        }
    }

    static String capitalized(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
