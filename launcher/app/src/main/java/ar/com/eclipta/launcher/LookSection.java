package ar.com.eclipta.launcher;

import android.text.InputType;

/** Apariencia: how Home looks, whatever the car. */
final class LookSection extends SettingsSection {
    LookSection(SettingsActivity a) {
        super(a);
    }

    @Override
    int title() {
        return R.string.sec_look;
    }

    @Override
    int description() {
        return R.string.sec_look_desc;
    }

    @Override
    boolean split() {
        return true;
    }

    @Override
    void build() {
        a.showLookPreview();
        f.group(R.string.grp_screen);
        f.chips(R.string.f_layout, R.string.layout_hint, SettingsForm.list("glass", "mmi"),
                f.strings(R.string.layout_glass, R.string.layout_mmi), p.layout, id -> {
                    p.layout = id;
                    a.saved();
                    a.showLookPreview();
                });
        f.group(R.string.grp_color);
        f.chips(R.string.f_accent, R.string.accent_hint, SettingsForm.list("red", "blue", "gray"),
                f.strings(R.string.accent_red, R.string.accent_blue, R.string.accent_gray), p.accent, id -> {
                    p.accent = id;
                    a.saved();
                    a.recreate();
                });
        f.group(R.string.grp_night);
        f.chips(R.string.f_night, R.string.night_hint, SettingsForm.list("off", "on", "sun", "lights"),
                f.strings(R.string.night_off, R.string.night_on, R.string.night_sun, R.string.night_lights), p.night, id -> {
                    p.night = id;
                    a.saved();
                    a.recreate();
                });
        f.group(R.string.grp_greeting);
        f.textRow(R.string.f_greeting, a.getString(R.string.f_greeting_desc), p.greeting, p.greeting, 0,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS, 20, v -> {
                    p.greeting = v;
                    a.saved();
                    a.rebuild();
                });
    }
}
