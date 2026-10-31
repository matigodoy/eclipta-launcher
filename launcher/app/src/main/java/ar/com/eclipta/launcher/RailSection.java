package ar.com.eclipta.launcher;

import android.widget.Toast;

/** Barra lateral: which app each shortcut on Home's rail opens. */
final class RailSection extends SettingsSection {
    RailSection(SettingsActivity a) {
        super(a);
    }

    @Override
    int title() {
        return R.string.sec_rail;
    }

    @Override
    int description() {
        return R.string.sec_rail_desc;
    }

    @Override
    boolean split() {
        return true;
    }

    @Override
    void build() {
        a.showRailPreview();
        f.group(R.string.grp_rail);
        for (int i = 0; i < Profile.SLOTS; i++) {
            final int slot = i;
            f.actionRow(a.getString(R.string.rail_slot, i + 1), Launch.label(a, Launch.slot(a, p, i)).toString(),
                    a.getString(R.string.rail_change), () -> a.pickApp(data -> {
                        p.slots[slot] = data.getStringExtra(AppsActivity.EXTRA_PICK);
                        a.saved();
                        a.rebuild();
                    }));
        }
        f.hint(R.string.rail_tip);
        f.actionRow(R.string.rail_reset, R.string.rail_reset_desc, R.string.rail_reset_button, () -> {
            for (int i = 0; i < Profile.SLOTS; i++) p.slots[i] = "";
            a.saved();
            a.rebuild();
            Toast.makeText(a, R.string.done, Toast.LENGTH_SHORT).show();
        });
    }
}
