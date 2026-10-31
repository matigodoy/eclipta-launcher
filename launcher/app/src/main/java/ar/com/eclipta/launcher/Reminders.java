package ar.com.eclipta.launcher;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/** Date reminders. Checked only when the home screen opens: no alarms, nothing in the background. */
final class Reminders {
    static final int DUE_NONE = Integer.MAX_VALUE;
    static final int WARN_DAYS = 30;
    private static final long DAY = 86_400_000L;

    private Reminders() {}

    /** Whole calendar days from nowMs to a yyyy-MM-dd date. DUE_NONE if empty or invalid. */
    static int daysLeft(String date, long nowMs, TimeZone tz) {
        if (date == null || date.isEmpty()) return DUE_NONE;
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            f.setLenient(false);
            f.setTimeZone(tz);
            long due = f.parse(date).getTime();
            long today = f.parse(f.format(new java.util.Date(nowMs))).getTime();
            return (int) Math.round((due - today) / (double) DAY);
        } catch (ParseException e) {
            return DUE_NONE;
        }
    }
}
