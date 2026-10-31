package ar.com.eclipta.launcher;

import android.content.Intent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

/** One page of settings. Lives while the page is open. */
abstract class SettingsSection {
    interface OnResult {
        void result(Intent data);
    }

    final SettingsActivity a;
    final SettingsForm f;
    final Profile p;
    private LinearLayout[] roots = new LinearLayout[0];

    SettingsSection(SettingsActivity a) {
        this.a = a;
        this.f = new SettingsForm(a);
        this.p = a.profile;
    }

    abstract int title();

    abstract int description();

    /** Personalizes the launcher: the page shows the car beside the options. */
    boolean split() {
        return false;
    }

    /** Fills the page. Called again after a change that alters which rows exist. */
    abstract void build();

    /** Full pages create their columns here. Split pages get the options panel. */
    void attach(FrameLayout content) {
        roots(f.columns(content, false, 1, 1));
    }

    void attachForm(LinearLayout form) {
        roots(form);
        f.into(form);
    }

    final void roots(LinearLayout... roots) {
        this.roots = roots;
    }

    final void rebuild() {
        for (LinearLayout l : roots) l.removeAllViews();
        if (roots.length > 0) f.into(roots[0]);
        build();
    }

    final LinearLayout column(int i) {
        return roots[i];
    }
}
