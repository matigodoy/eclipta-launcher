package ar.com.eclipta.launcher;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/** Acerca de: who made this, and where to find more. */
final class AboutSection extends SettingsSection {
    private static final String AUTHOR = "Matias Godoy";
    private View page;

    AboutSection(SettingsActivity a) {
        super(a);
    }

    @Override
    int title() {
        return R.string.sec_about;
    }

    @Override
    int description() {
        return R.string.sec_about_desc;
    }

    @Override
    void attach(FrameLayout content) {
        page = LayoutInflater.from(a).inflate(R.layout.settings_about, content, true);
    }

    @Override
    void build() {
        ((TextView) page.findViewById(R.id.aboutVersion)).setText(a.getString(R.string.about_version, BuildConfig.VERSION_NAME));
        ((TextView) page.findViewById(R.id.aboutBy)).setText(a.getString(R.string.about_by, AUTHOR));
        if (a.isNight()) {
            ((ImageView) page.findViewById(R.id.aboutLogo)).setImageTintList(
                    ColorStateList.valueOf(Theming.color(a, R.attr.uiText)));
        }
        card(R.id.cardPortfolio, R.drawable.qr_portfolio, R.string.about_portfolio, R.string.about_portfolio_url);
        card(R.id.cardGithub, R.drawable.qr_github, R.string.about_github, R.string.about_github_url);
    }

    private void card(int id, int qr, int title, int url) {
        View card = page.findViewById(id);
        ((ImageView) card.findViewById(R.id.qr)).setImageResource(qr);
        ((TextView) card.findViewById(R.id.qrTitle)).setText(title);
        ((TextView) card.findViewById(R.id.qrUrl)).setText(url);
    }
}
