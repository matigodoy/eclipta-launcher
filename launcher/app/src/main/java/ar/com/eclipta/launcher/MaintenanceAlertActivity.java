package ar.com.eclipta.launcher;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.TimeZone;

/**
 * Shown at start-up when something expired. Comes back on every start until each item is marked
 * done. Never holds up CarPlay for more than one tap, and closes itself if nobody answers.
 */
public class MaintenanceAlertActivity extends BaseActivity {
    static final String EXTRA_FROM_WAKE = "from_wake";
    private static final long AUTO_CLOSE_MS = 30_000;

    private final Handler handler = new Handler();
    private final Runnable timeout = new Runnable() {
        @Override
        public void run() {
            close();
        }
    };
    private List<Reminders.Item> all;
    private TextView btnLater;
    private boolean fromWake, stopped;
    private int unanswered;
    private float dp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Startup.alertAlive = true;
        dp = getResources().getDisplayMetrics().density;
        fromWake = getIntent().getBooleanExtra(EXTRA_FROM_WAKE, false);
        getWindow().setBackgroundDrawable(new GlowBackground(this, !night));
        setContentView(R.layout.activity_maintenance_alert);
        btnLater = findViewById(R.id.btnLater);
        btnLater.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                close();
            }
        });
        findViewById(R.id.btnOpen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handler.removeCallbacks(timeout);
                finish();
                startActivity(new Intent(MaintenanceAlertActivity.this, MainActivity.class)
                        .putExtra(MainActivity.EXTRA_OPEN_MAINTENANCE, true)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        });
        all = Reminders.load(this);
        List<Reminders.Item> expired = Reminders.expired(all, System.currentTimeMillis(), TimeZone.getDefault());
        if (expired.isEmpty()) {
            close();
            return;
        }
        unanswered = expired.size();
        ((TextView) findViewById(R.id.alertTitle)).setText(
                getResources().getQuantityString(R.plurals.alert_title, expired.size(), expired.size()));
        LinearLayout rows = findViewById(R.id.rows);
        for (Reminders.Item item : expired) rows.addView(row(item));
        // The panel fits its rows, up to what keeps the buttons on screen.
        if (expired.size() > 3) findViewById(R.id.rowsScroll).getLayoutParams().height = px(330);
        handler.postDelayed(timeout, AUTO_CLOSE_MS);
    }

    private View row(final Reminders.Item item) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, px(8), 0, px(8));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView name = text(Reminders.name(this, item), 18, R.attr.uiText);
        final TextView status = text(statusOf(item), 15, R.attr.uiAccent);
        texts.addView(name);
        texts.addView(status);
        if (!item.note.isEmpty()) texts.addView(text(item.note, 14, R.attr.uiText2));
        row.addView(texts, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        final TextView button = new TextView(this);
        button.setGravity(Gravity.CENTER);
        button.setTextSize(15);
        button.setPadding(px(18), 0, px(18), 0);
        styleDone(button);
        button.setOnClickListener(new View.OnClickListener() {
            private String oldDue, oldDoneAt;

            @Override
            public void onClick(View v) {
                TimeZone tz = TimeZone.getDefault();
                if (oldDue == null) {
                    oldDue = item.due;
                    oldDoneAt = item.doneAt;
                    Reminders.markDone(item, System.currentTimeMillis(), tz);
                    status.setText(item.due.isEmpty() ? getString(R.string.rem_done_none)
                            : getString(R.string.rem_done_next, Reminders.shortDate(MaintenanceAlertActivity.this, item.due, tz)));
                    status.setTextColor(Theming.color(MaintenanceAlertActivity.this, R.attr.uiText2));
                    button.setText(R.string.rem_undo);
                    button.setBackgroundResource(R.drawable.glass_button);
                    button.setTextColor(Theming.color(MaintenanceAlertActivity.this, R.attr.uiText));
                    unanswered--;
                } else {
                    item.due = oldDue;
                    item.doneAt = oldDoneAt;
                    oldDue = null;
                    status.setText(statusOf(item));
                    status.setTextColor(Theming.color(MaintenanceAlertActivity.this, R.attr.uiAccent));
                    styleDone(button);
                    unanswered++;
                }
                Reminders.save(MaintenanceAlertActivity.this, all);
                btnLater.setText(unanswered == 0 ? R.string.alert_continue : R.string.alert_later);
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, px(56));
        lp.leftMargin = px(12);
        row.addView(button, lp);
        return row;
    }

    private void styleDone(TextView button) {
        button.setText(R.string.rem_done);
        button.setBackgroundResource(R.drawable.accent_button);
        button.setTextColor(Theming.color(this, R.attr.uiOnAccent));
    }

    private String statusOf(Reminders.Item item) {
        return Reminders.capitalized(Reminders.status(this, item, 0, System.currentTimeMillis(), TimeZone.getDefault()));
    }

    private TextView text(String s, int sp, int colorAttr) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(Theming.color(this, colorAttr));
        return t;
    }

    private int px(int v) {
        return Math.round(v * dp);
    }

    /** Back, "Ahora no", "Continuar" and the timeout all mean the same. */
    private void close() {
        if (isFinishing()) return;
        handler.removeCallbacks(timeout);
        finish();
        // Something else is on screen: don't pull the chosen app over it.
        if (!stopped) Startup.afterAlert(this, fromWake);
    }

    @Override
    public void onBackPressed() {
        close();
    }

    @Override
    public void onUserInteraction() {
        handler.removeCallbacks(timeout);
        handler.postDelayed(timeout, AUTO_CLOSE_MS);
    }

    @Override
    protected void onStart() {
        super.onStart();
        stopped = false;
    }

    /**
     * On the reference unit CarPlay starts itself when the phone connects, which would turn the alert
     * into a flash nobody can answer. So it comes back once on top; the next time it just closes.
     * The Home key lands here too and can't be told apart: Android reports the same for any app started.
     */
    @Override
    protected void onStop() {
        super.onStop();
        stopped = true;
        if (isFinishing()) return;
        boolean screenOn = ((PowerManager) getSystemService(POWER_SERVICE)).isInteractive();
        if (screenOn && !Startup.alertReshown) {
            Startup.alertReshown = true;
            startActivity(new Intent(this, MaintenanceAlertActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return;
        }
        handler.removeCallbacks(timeout);
        finish();
    }

    @Override
    protected void onDestroy() {
        Startup.alertAlive = false;
        handler.removeCallbacks(timeout);
        super.onDestroy();
    }
}
