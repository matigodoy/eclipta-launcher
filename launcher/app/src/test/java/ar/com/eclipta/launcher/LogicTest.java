package ar.com.eclipta.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class LogicTest {
    private static final TimeZone AR = TimeZone.getTimeZone("America/Argentina/Cordoba");

    private static long at(int y, int m, int d, int h) {
        Calendar c = new GregorianCalendar(AR);
        c.clear();
        c.set(y, m - 1, d, h, 0);
        return c.getTimeInMillis();
    }

    @Test
    public void reminderDays() {
        long now = at(2026, 10, 31, 23);
        assertEquals(1, Reminders.daysLeft("2026-11-01", now, AR));
        assertEquals(0, Reminders.daysLeft("2026-10-31", now, AR));
        assertEquals(-1, Reminders.daysLeft("2026-10-30", now, AR));
        assertEquals(Reminders.DUE_NONE, Reminders.daysLeft("", now, AR));
        assertEquals(Reminders.DUE_NONE, Reminders.daysLeft("nope", now, AR));
    }

    @Test
    public void sunInCordoba() {
        double lat = -31.4, lon = -64.2;
        assertFalse(Sun.isDark(at(2026, 10, 31, 13), lat, lon));
        assertTrue(Sun.isDark(at(2026, 10, 31, 23), lat, lon));
        assertTrue(Sun.isDark(at(2026, 10, 31, 4), lat, lon));
        // Mid June: dark by 19:00. Late December: still light at 19:30-ish.
        assertTrue(Sun.isDark(at(2026, 6, 21, 19), lat, lon));
        assertFalse(Sun.isDark(at(2026, 12, 21, 19), lat, lon));
    }

    @Test
    public void paintKeepsAlphaAndRespectsMask() {
        PaintMath m = new PaintMath(0xFF0000, 1f, 1.5f, 0.2f);
        assertEquals(0x80FFFFFF, m.apply(0x80FFFFFF, 0));
        int painted = m.apply(0xFF808080, 255);
        assertEquals(0xFF, painted >>> 24);
        assertTrue(((painted >> 16) & 0xFF) > ((painted >> 8) & 0xFF) * 3);
        int white = m.apply(0xFFFFFFFF, 255);
        assertTrue(((white >> 16) & 0xFF) == 255);
    }

    @Test
    public void screenFit() {
        assertEquals(1f, ScreenFit.density(1024, 600, 1024, 600, 1f, 0.03f), 0.001f);
        assertEquals(1f, ScreenFit.density(1024, 590, 1024, 600, 1f, 0.03f), 0.001f);
        assertEquals(1.2f, ScreenFit.density(1280, 720, 1024, 600, 1f, 0.03f), 0.001f);
        assertEquals(1.2f, ScreenFit.density(1920, 720, 1024, 600, 1.5f, 0.03f), 0.001f);
        assertEquals(1.333f, ScreenFit.density(1366, 800, 1024, 600, 1f, 0.03f), 0.01f);
    }
}
