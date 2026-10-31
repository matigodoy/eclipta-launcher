package ar.com.eclipta.launcher;

import android.text.InputType;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/** Contacto y manejo: what happens on ignition and while moving. */
final class DriveSection extends SettingsSection {
    private static final String OTHER = "other";

    DriveSection(SettingsActivity a) {
        super(a);
    }

    @Override
    int title() {
        return R.string.sec_drive;
    }

    @Override
    int description() {
        return R.string.sec_drive_desc;
    }

    @Override
    void build() {
        f.into(column(0));
        f.group(R.string.grp_ignition);
        boolean hasSting = hasAnimation();
        View splash = f.switchRow(R.string.f_splash, a.getString(hasSting ? R.string.f_splash_desc : R.string.f_splash_none),
                p.splash && hasSting, on -> {
                    p.splash = on;
                    a.saved();
                });
        if (!hasSting) {
            splash.setEnabled(false);
            splash.setAlpha(0.5f);
        }
        appChips();
        f.switchRow(R.string.rem_alert, a.getString(R.string.rem_alert_desc), p.remindAlert, on -> {
            p.remindAlert = on;
            a.saved();
        });

        f.into(column(1));
        f.group(R.string.grp_driving);
        String shown = p.speedLimit > 0 ? a.getString(R.string.speed_value, p.speedLimit) : a.getString(R.string.speed_off);
        f.textRow(R.string.f_speed, a.getString(R.string.f_speed_desc), shown, p.speedLimit > 0 ? String.valueOf(p.speedLimit) : "",
                0, InputType.TYPE_CLASS_NUMBER, 3, v -> {
                    try {
                        p.speedLimit = v.isEmpty() ? 0 : Math.min(300, Integer.parseInt(v));
                    } catch (NumberFormatException e) {
                        p.speedLimit = 0;
                    }
                    a.saved();
                    a.rebuild();
                });
        f.hint(R.string.drive_hint);
    }

    private boolean hasAnimation() {
        return Brand.sting(a, a.pack, p) != null;
    }

    /** Offers only what's installed; a chosen app outside the list keeps its name on the last chip. */
    private void appChips() {
        List<String> ids = new ArrayList<>(), names = new ArrayList<>();
        ids.add("");
        names.add(a.getString(R.string.acc_none));
        known(ids, names, Launch.CARPLAY);
        known(ids, names, Launch.ANDROID_AUTO);
        known(ids, names, Launch.RADIO);
        known(ids, names, Launch.MUSIC);
        boolean custom = !ids.contains(p.accApp);
        ids.add(OTHER);
        names.add(custom ? Launch.label(a, p.accApp).toString() : a.getString(R.string.acc_other));
        f.chips(R.string.f_acc_app, R.string.f_acc_hint, ids, names, custom ? OTHER : p.accApp, id -> {
            if (OTHER.equals(id)) {
                a.pickApp(data -> {
                    p.accApp = data.getStringExtra(AppsActivity.EXTRA_PICK);
                    a.saved();
                    a.rebuild();
                });
                // The chip stays right whether or not an app gets picked.
                a.handler().post(a::rebuild);
                return;
            }
            p.accApp = id;
            a.saved();
            a.rebuild();
        });
    }

    private void known(List<String> ids, List<String> names, String flat) {
        if (!Launch.installed(a, Launch.resolve(a, flat))) return;
        ids.add(flat);
        names.add(Launch.label(a, flat).toString());
    }
}
