package ar.com.eclipta.launcher;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/** Mantenimiento: the list on the left, what's selected on the right. */
final class MaintenanceSection extends SettingsSection {
    private final List<Reminders.Item> items;
    /** Previous due and done dates of what was marked done on this page, so it can be undone. */
    private final Map<String, String[]> undo = new HashMap<>();
    private final TimeZone tz = TimeZone.getDefault();
    private Reminders.Item selected; // null = how to notify

    MaintenanceSection(SettingsActivity a) {
        super(a);
        items = Reminders.load(a);
        if (a.deepLinked) {
            List<Reminders.Item> due = Reminders.upcoming(items, p.remindLead, now(), tz);
            if (!due.isEmpty()) selected = due.get(0);
        }
    }

    @Override
    int title() {
        return R.string.sec_maint;
    }

    @Override
    int description() {
        return R.string.sec_maint_desc;
    }

    @Override
    void attach(FrameLayout content) {
        roots(f.columns(content, true, 42, 58));
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private void save() {
        Reminders.save(a, items);
    }

    @Override
    void build() {
        f.into(column(0));
        listRow(a.getString(R.string.rem_howto), null, 0, selected == null, null, v -> select(null));
        for (Reminders.Item item : items) itemRow(item);
        listRow(a.getString(R.string.rem_add), null, 0, false, null, v -> addCustom());
        f.into(column(1));
        if (selected == null) buildHowTo();
        else buildItem(selected);
    }

    private void select(Reminders.Item item) {
        selected = item;
        rebuild();
    }

    // ---------------------------------------------------------------- left: the list

    private void itemRow(final Reminders.Item item) {
        int days = Reminders.daysLeft(item.due, now(), tz);
        boolean close = item.notify && days != Reminders.DUE_NONE && days <= p.remindLead;
        TextView chip = null;
        if (undo.containsKey(item.id)) {
            chip = f.chip(a.getString(R.string.rem_undo), false, v -> undoDone(item));
        } else if (close) {
            chip = f.chip(a.getString(R.string.rem_done), false, v -> markDone(item));
        }
        listRow(Reminders.name(a, item), Reminders.capitalized(statusOf(item)), close ? R.attr.uiAccent : R.attr.uiText2,
                item == selected, chip, v -> select(item));
    }

    private void listRow(String name, String status, int statusColor, boolean isSelected, TextView chip, View.OnClickListener click) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(f.px(60));
        row.setPadding(f.px(12), f.px(6), f.px(8), f.px(6));
        row.setBackgroundResource(R.drawable.list_row_bg);
        row.setSelected(isSelected);
        row.setOnClickListener(click);
        LinearLayout texts = new LinearLayout(a);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(f.text(name, 16, R.attr.uiText));
        if (status != null) texts.addView(f.text(status, 13, statusColor));
        row.addView(texts, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        if (chip != null) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, f.px(44));
            lp.leftMargin = f.px(8);
            row.addView(chip, lp);
        }
        f.add(row);
    }

    /** What a row says right after "Ya lo hice", or its normal status. */
    private String statusOf(Reminders.Item item) {
        if (undo.containsKey(item.id)) {
            return item.due.isEmpty() ? a.getString(R.string.rem_done_none)
                    : a.getString(R.string.rem_done_next, Reminders.shortDate(a, item.due, tz));
        }
        return Reminders.status(a, item, p.remindLead, now(), tz);
    }

    private void markDone(Reminders.Item item) {
        undo.put(item.id, new String[]{item.due, item.doneAt});
        Reminders.markDone(item, now(), tz);
        save();
        rebuild();
    }

    private void undoDone(Reminders.Item item) {
        String[] old = undo.remove(item.id);
        if (old != null) {
            item.due = old[0];
            item.doneAt = old[1];
            save();
        }
        rebuild();
    }

    private void addCustom() {
        f.askText(a.getString(R.string.rem_add), "", R.string.rem_add_hint,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, Reminders.MAX_NAME, name -> {
                    if (name.isEmpty()) return;
                    Reminders.Item item = Reminders.addCustom(items, name);
                    if (item == null) {
                        Toast.makeText(a, R.string.rem_add_limit, Toast.LENGTH_LONG).show();
                        return;
                    }
                    save();
                    select(item);
                });
    }

    // ---------------------------------------------------------------- right: how to notify

    private void buildHowTo() {
        f.group(R.string.rem_howto);
        f.switchRow(R.string.rem_home, a.getString(R.string.rem_home_desc), p.remindHome, on -> {
            p.remindHome = on;
            a.saved();
        });
        f.switchRow(R.string.rem_alert, a.getString(R.string.rem_alert_desc), p.remindAlert, on -> {
            p.remindAlert = on;
            a.saved();
        });
        List<String> ids = new ArrayList<>(), names = new ArrayList<>();
        for (int days : Reminders.LEAD_DAYS) {
            ids.add(String.valueOf(days));
            names.add(a.getString(R.string.rem_lead_days, days));
        }
        f.chips(R.string.rem_lead, R.string.rem_lead_hint, ids, names, String.valueOf(p.remindLead), id -> {
            p.remindLead = Integer.parseInt(id);
            a.saved();
            rebuild();
        });
        f.hint(R.string.rem_km_hint);
    }

    // ---------------------------------------------------------------- right: one item

    private void buildItem(final Reminders.Item item) {
        TextView name = f.text(Reminders.name(a, item), 20, R.attr.uiText);
        name.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        f.add(name);
        int days = Reminders.daysLeft(item.due, now(), tz);
        boolean close = item.notify && days != Reminders.DUE_NONE && days <= p.remindLead;
        f.add(f.text(Reminders.capitalized(statusOf(item)), 15, close ? R.attr.uiAccent : R.attr.uiText2));

        f.label(R.string.rem_date);
        FlowLayout dates = f.chipFlow();
        dates.addView(f.chip(item.due.isEmpty() ? a.getString(R.string.rem_date_none) : Reminders.shortDate(a, item.due, tz),
                !item.due.isEmpty(), v -> pickDate(item)));
        if (!item.due.isEmpty()) {
            dates.addView(f.chip(a.getString(R.string.rem_date_clear_button), false, v -> {
                item.due = "";
                undo.remove(item.id);
                save();
                rebuild();
            }));
        }
        if (Reminders.goesByKm(item)) f.hint(R.string.rem_km_item);

        List<String> ids = new ArrayList<>(), names = new ArrayList<>();
        for (int months : Reminders.EVERY_MONTHS) {
            ids.add(String.valueOf(months));
            names.add(everyName(months));
        }
        if (!ids.contains(String.valueOf(item.every))) {
            ids.add(String.valueOf(item.every));
            names.add(everyName(item.every));
        }
        f.chips(R.string.rem_every, R.string.rem_every_hint, ids, names, String.valueOf(item.every), id -> {
            item.every = Integer.parseInt(id);
            save();
        });
        f.switchRow(R.string.rem_notify, a.getString(R.string.rem_notify_desc), item.notify, on -> {
            item.notify = on;
            save();
            rebuild();
        });
        f.textRow(R.string.rem_note, a.getString(R.string.rem_note_desc), item.note, item.note, R.string.rem_note_hint,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, Reminders.MAX_NOTE, v -> {
                    item.note = v;
                    save();
                    rebuild();
                });
        if (undo.containsKey(item.id)) {
            f.actionRow(R.string.rem_done, R.string.rem_done_desc, R.string.rem_undo, () -> undoDone(item));
        } else if (!item.due.isEmpty()) {
            f.actionRow(R.string.rem_done, R.string.rem_done_desc, R.string.rem_done_button, () -> markDone(item));
        }
        if (item.custom()) {
            f.actionRow(R.string.rem_delete, 0, R.string.rem_delete_button, () -> confirmDelete(item));
        }
    }

    private String everyName(int months) {
        if (months == 0) return a.getString(R.string.rem_every_none);
        if (months % 12 == 0) return a.getResources().getQuantityString(R.plurals.rem_every_years, months / 12, months / 12);
        return a.getResources().getQuantityString(R.plurals.rem_every_months, months, months);
    }

    private void pickDate(final Reminders.Item item) {
        Calendar c = new GregorianCalendar(tz);
        int[] ymd = parse(item.due);
        int y = ymd != null ? ymd[0] : c.get(Calendar.YEAR);
        int m = ymd != null ? ymd[1] - 1 : c.get(Calendar.MONTH);
        int d = ymd != null ? ymd[2] : c.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog dialog = new DatePickerDialog(a, (view, year, month, day) -> {
            item.due = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
            undo.remove(item.id);
            save();
            rebuild();
        }, y, m, d);
        dialog.show();
    }

    private static int[] parse(String date) {
        if (!Reminders.validDate(date)) return null;
        String[] parts = date.split("-");
        return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])};
    }

    private void confirmDelete(final Reminders.Item item) {
        new AlertDialog.Builder(a)
                .setMessage(a.getString(R.string.rem_delete_confirm, item.name))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.rem_delete_button, (d, w) -> {
                    items.remove(item);
                    undo.remove(item.id);
                    save();
                    select(null);
                })
                .show();
    }
}
