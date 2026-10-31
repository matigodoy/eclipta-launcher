package ar.com.eclipta.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

public class RemindersTest {
    private static final TimeZone AR = TimeZone.getTimeZone("America/Argentina/Cordoba");

    private static long at(int y, int m, int d) {
        Calendar c = new GregorianCalendar(AR);
        c.clear();
        c.set(y, m - 1, d, 12, 0);
        return c.getTimeInMillis();
    }

    private static Reminders.Item find(List<Reminders.Item> items, String id) {
        for (Reminders.Item i : items) if (i.id.equals(id)) return i;
        return null;
    }

    @Test
    public void addMonthsClampsToMonthEnd() {
        assertEquals("2027-02-28", Reminders.addMonths("2027-01-31", 1, AR));
        assertEquals("2028-02-29", Reminders.addMonths("2028-01-31", 1, AR));
        assertEquals("2031-10-31", Reminders.addMonths("2026-10-31", 60, AR));
        assertEquals("", Reminders.addMonths("nope", 1, AR));
    }

    @Test
    public void markDoneCountsFromToday() {
        Reminders.Item belt = find(Reminders.catalog(), "belt");
        belt.due = "2020-01-01";
        Reminders.markDone(belt, at(2026, 10, 31), AR);
        assertEquals("2026-10-31", belt.doneAt);
        assertEquals("2031-10-31", belt.due);

        Reminders.Item once = new Reminders.Item();
        once.due = "2026-01-01";
        Reminders.markDone(once, at(2026, 10, 31), AR);
        assertEquals("", once.due);
    }

    @Test
    public void filtersRespectNotifyAndOrder() {
        List<Reminders.Item> items = Reminders.catalog();
        find(items, "vtv").due = "2026-11-10";
        find(items, "insurance").due = "2026-10-31";
        find(items, "oil").due = "2026-10-01";
        find(items, "belt").due = "2026-10-02";
        find(items, "belt").notify = false;
        long now = at(2026, 10, 31);

        List<Reminders.Item> expired = Reminders.expired(items, now, AR);
        assertEquals(2, expired.size());
        assertEquals("oil", expired.get(0).id);
        assertEquals("insurance", expired.get(1).id);

        List<Reminders.Item> upcoming = Reminders.upcoming(items, 15, now, AR);
        assertEquals(3, upcoming.size());
        assertEquals("vtv", upcoming.get(2).id);
        assertEquals(2, Reminders.upcoming(items, 7, now, AR).size());
        assertEquals("2026-10-01", Reminders.soonest(items));
    }

    @Test
    public void jsonRoundTripAndMerge() throws Exception {
        List<Reminders.Item> items = Reminders.catalog();
        find(items, "vtv").due = "2027-03-01";
        find(items, "vtv").note = "Planta Sur";
        find(items, "brakes").notify = false;
        Reminders.Item custom = Reminders.addCustom(items, "  Patente  ");
        assertEquals("c1", custom.id);
        custom.every = 6;

        List<Reminders.Item> back = Reminders.fromJson(new JSONArray(Reminders.toJson(items).toString()));
        assertEquals(items.size(), back.size());
        assertEquals("2027-03-01", find(back, "vtv").due);
        assertEquals("Planta Sur", find(back, "vtv").note);
        assertFalse(find(back, "brakes").notify);
        assertEquals("Patente", find(back, "c1").name);
        assertEquals(6, find(back, "c1").every);
        // Catalog entries missing from what's stored still appear, with their defaults.
        assertEquals(60, find(Reminders.fromJson(new JSONArray("[]")), "belt").every);
    }

    @Test
    public void customIdsStayUniqueAfterDeleting() {
        List<Reminders.Item> items = Reminders.catalog();
        Reminders.addCustom(items, "a");
        Reminders.Item b = Reminders.addCustom(items, "b");
        items.remove(find(items, "c1"));
        assertEquals("c2", b.id);
        assertEquals("c3", Reminders.addCustom(items, "c").id);
    }

    @Test
    public void customLimit() {
        List<Reminders.Item> items = Reminders.catalog();
        for (int i = 0; i < Reminders.MAX_CUSTOM; i++) assertNotNull(Reminders.addCustom(items, "x"));
        assertNull(Reminders.addCustom(items, "too many"));
    }

    @Test
    public void importDropsWhatIsInvalid() throws Exception {
        List<Reminders.Item> back = Reminders.fromJson(new JSONArray("["
                + "{\"id\":\"vtv\",\"due\":\"2027-13-40\",\"every\":9999},"
                + "{\"id\":\"vtv\",\"due\":\"2027-01-01\"},"
                + "{\"id\":\"c1\"},"
                + "{\"id\":\"../x\",\"name\":\"x\"},"
                + "{\"id\":\"unknown\",\"name\":\"x\"},"
                + "{\"id\":\"c2\",\"name\":\"ok\",\"note\":null},"
                + "3]"));
        assertEquals("", find(back, "vtv").due);
        assertEquals(Reminders.MAX_EVERY, find(back, "vtv").every);
        assertNull(find(back, "c1"));
        assertNull(find(back, "unknown"));
        assertEquals("", find(back, "c2").note);
        assertEquals(Reminders.catalog().size() + 1, back.size());
    }

    @Test
    public void oldProfileKeys() throws Exception {
        List<Reminders.Item> back = Reminders.fromProfile(new JSONObject(
                "{\"dueVtv\":\"2026-10-31\",\"dueInsurance\":\"\",\"dueService\":\"2026-11-28\"}"));
        assertEquals("2026-10-31", find(back, "vtv").due);
        assertEquals("", find(back, "insurance").due);
        assertEquals("2026-11-28", find(back, "service").due);
        assertNull(Reminders.fromProfile(new JSONObject("{\"greeting\":\"x\"}")));
    }

    @Test
    public void goesByKm() {
        assertTrue(Reminders.goesByKm(find(Reminders.catalog(), "belt")));
        assertFalse(Reminders.goesByKm(find(Reminders.catalog(), "vtv")));
    }

    @Test
    public void startupDecision() {
        long now = 1_000_000;
        // Consumed with nothing to show: a stale flag can't fire later.
        assertEquals(Startup.NONE, Startup.decide(false, true, false, true, -3, Startup.NEVER, now));
        assertEquals(Startup.NONE, Startup.decide(true, true, false, false, -3, Startup.NEVER, now));
        assertEquals(Startup.NONE, Startup.decide(true, true, false, true, 5, Startup.NEVER, now));
        assertEquals(Startup.NONE, Startup.decide(true, true, false, true, Reminders.DUE_NONE, Startup.NEVER, now));
        // Waits, keeping the flag.
        assertEquals(Startup.WAIT, Startup.decide(true, false, false, true, -3, Startup.NEVER, now));
        assertEquals(Startup.WAIT, Startup.decide(true, true, true, true, -3, Startup.NEVER, now));
        // Shows.
        assertEquals(Startup.SHOW, Startup.decide(true, true, false, true, 0, Startup.NEVER, now));
        assertEquals(Startup.SHOW, Startup.decide(true, true, false, true, -3, now - 120_000, now));
        // Guard: just shown, but a time from before a reboot doesn't count.
        assertEquals(Startup.NONE, Startup.decide(true, true, false, true, -3, now - 1000, now));
        assertEquals(Startup.SHOW, Startup.decide(true, true, false, true, -3, now + 5000, now));
    }

    @Test
    public void clockPlausible() {
        long build = 1_725_654_576_000L;
        assertFalse(Startup.clockPlausible(build + 1000, build));
        assertTrue(Startup.clockPlausible(build + 2 * 86_400_000L, build));
    }

    @Test
    public void importedAppIsValidated() throws Exception {
        assertTrue(Profile.validApp("com.autochips.carplayapp/com.autochips.carplayapp.CarPlayActivity"));
        assertFalse(Profile.validApp("no component"));
        assertFalse(Profile.validApp(BuildConfig.APPLICATION_ID + "/.SplashActivity"));

        Profile p = new Profile();
        p.accApp = "x/.y";
        p.pack = "audi-a3-8p";
        p.title = "";
        p.remindLead = 30;
        p.applyJson(new JSONObject("{\"carplayOnAcc\":false}"));
        assertEquals("", p.accApp);
        p.applyJson(new JSONObject("{\"carplayOnAcc\":true}"));
        assertEquals(Launch.CARPLAY, p.accApp);
        p.applyJson(new JSONObject("{\"accApp\":\"" + BuildConfig.APPLICATION_ID + "/.SplashActivity\",\"carplayOnAcc\":false}"));
        assertEquals(Launch.CARPLAY, p.accApp);
        p.applyJson(new JSONObject("{\"pack\":\"../evil\",\"title\":null,\"remindLead\":11}"));
        assertEquals("audi-a3-8p", p.pack);
        assertEquals("", p.title);
        assertEquals(30, p.remindLead);
    }
}
