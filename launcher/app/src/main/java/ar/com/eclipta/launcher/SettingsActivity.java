package ar.com.eclipta.launcher;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * "Personalizar": a hub of sections, each one a page of this activity. Only exists while open.
 * Changes are saved right away; the car picture is re-baked in the background after the last one.
 */
public class SettingsActivity extends BaseActivity {
    static final String EXTRA_SECTION = "section";
    static final String SECTION_CAR = "car", SECTION_LOOK = "look", SECTION_RAIL = "rail",
            SECTION_MAINTENANCE = "maintenance", SECTION_DRIVE = "drive", SECTION_SYSTEM = "system";
    private static final String STATE_SECTION = "section";
    private static final int REQ_STORAGE = 1, REQ_PICK = 2;
    private static final long BAKE_DELAY_MS = 350;

    private final Handler main = new Handler(Looper.getMainLooper());
    Profile profile;
    Pack pack;
    /** Opened from a shortcut rather than from the hub. */
    boolean deepLinked;
    private String current;
    private SettingsSection section;
    private ImageView previewCar;
    private Bitmap previewBitmap;
    private int previewGeneration;
    private boolean bakePending, closing;
    private Runnable afterStorage;
    private SettingsSection.OnResult onPicked;

    private final Runnable bakeNow = new Runnable() {
        @Override
        public void run() {
            bakePending = false;
            CarBaker.bake(SettingsActivity.this);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawable(new GlowBackground(this, !night));
        profile = Profile.load(this);
        pack = Pack.load(this, profile.pack);
        CarBaker.setListener(new CarBaker.Listener() {
            @Override
            public void onBaked() {
                if (closing) reallyFinish();
                else loadPreview();
            }
        });
        String start = savedInstanceState != null ? savedInstanceState.getString(STATE_SECTION)
                : getIntent().getStringExtra(EXTRA_SECTION);
        deepLinked = savedInstanceState == null && start != null;
        if (start == null) showHub();
        else showSection(start);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString(STATE_SECTION, current);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Home key, or QuickBoot taking over at ignition off: finish() never runs.
        flushBake();
    }

    @Override
    protected void onDestroy() {
        flushBake();
        CarBaker.setListener(null);
        main.removeCallbacksAndMessages(null);
        releasePreview();
        super.onDestroy();
    }

    /** Home recreates itself when this closes, so it has to find the final picture. */
    @Override
    public void finish() {
        flushBake();
        if (CarBaker.isBusy()) {
            closing = true;
            return;
        }
        reallyFinish();
    }

    private void reallyFinish() {
        setResult(RESULT_OK);
        super.finish();
    }

    @Override
    public void onBackPressed() {
        if (current == null) finish();
        else showHub();
    }

    // ---------------------------------------------------------------- hub

    private void showHub() {
        releasePreview();
        current = null;
        section = null;
        deepLinked = false;
        setContentView(R.layout.settings_hub);
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
        LinearLayout look = findViewById(R.id.rowLook), functions = findViewById(R.id.rowFunctions);
        card(look, SECTION_CAR, R.drawable.ic_car, R.string.sec_car, R.string.sec_car_desc, 0);
        card(look, SECTION_LOOK, R.drawable.ic_palette, R.string.sec_look, R.string.sec_look_desc, 0);
        card(look, SECTION_RAIL, R.drawable.ic_rail, R.string.sec_rail, R.string.sec_rail_desc, 0);
        card(functions, SECTION_MAINTENANCE, R.drawable.ic_wrench, R.string.sec_maint, R.string.sec_maint_desc, expiredCount());
        card(functions, SECTION_DRIVE, R.drawable.ic_key, R.string.sec_drive, R.string.sec_drive_desc, 0);
        card(functions, SECTION_SYSTEM, R.drawable.ic_settings, R.string.sec_system, R.string.sec_system_desc, 0);
    }

    private int expiredCount() {
        long now = System.currentTimeMillis();
        TimeZone tz = TimeZone.getDefault();
        int soonest = Reminders.soonestDays(this, now, tz);
        return soonest == Reminders.DUE_NONE || soonest > 0 ? 0 : Reminders.expired(Reminders.load(this), now, tz).size();
    }

    private void card(LinearLayout row, final String id, int icon, int title, int desc, int badge) {
        View v = LayoutInflater.from(this).inflate(R.layout.item_settings_card, row, false);
        ((ImageView) v.findViewById(R.id.icon)).setImageResource(icon);
        ((TextView) v.findViewById(R.id.title)).setText(title);
        ((TextView) v.findViewById(R.id.desc)).setText(desc);
        TextView b = v.findViewById(R.id.badge);
        if (badge > 0) {
            b.setText(String.valueOf(badge));
            b.setVisibility(View.VISIBLE);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1);
        if (row.getChildCount() > 0) lp.leftMargin = Math.round(12 * getResources().getDisplayMetrics().density);
        v.setOnClickListener(view -> showSection(id));
        row.addView(v, lp);
    }

    // ---------------------------------------------------------------- pages

    private SettingsSection create(String id) {
        switch (id) {
            case SECTION_CAR:
                return new CarSection(this);
            case SECTION_LOOK:
                return new LookSection(this);
            case SECTION_RAIL:
                return new RailSection(this);
            case SECTION_MAINTENANCE:
                return new MaintenanceSection(this);
            case SECTION_DRIVE:
                return new DriveSection(this);
            case SECTION_SYSTEM:
                return new SystemSection(this);
            default:
                return null;
        }
    }

    private void showSection(String id) {
        SettingsSection s = create(id);
        if (s == null) {
            showHub();
            return;
        }
        releasePreview();
        current = id;
        section = s;
        setContentView(s.split() ? R.layout.settings_split : R.layout.settings_full);
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
        findViewById(R.id.btnBack).setOnClickListener(v -> showHub());
        ((TextView) findViewById(R.id.sectionTitle)).setText(s.title());
        ((TextView) findViewById(R.id.sectionDesc)).setText(s.description());
        if (s.split()) {
            previewCar = findViewById(R.id.previewCar);
            if (night) previewCar.setAlpha(0.7f);
            s.attachForm(findViewById(R.id.form));
            updatePreview();
            loadPreview();
        } else {
            s.attach(findViewById(R.id.content));
        }
        s.rebuild();
    }

    void rebuild() {
        if (section != null) section.rebuild();
    }

    /** Saves a change that doesn't touch the car picture. */
    void saved() {
        profile.save(this);
        updatePreview();
    }

    /** Saves a change the car picture depends on, and schedules one bake. */
    void carChanged() {
        profile.save(this);
        CarBaker.markDirty(this);
        updatePreview();
        bakePending = true;
        main.removeCallbacks(bakeNow);
        main.postDelayed(bakeNow, BAKE_DELAY_MS);
    }

    private void flushBake() {
        if (!bakePending) return;
        main.removeCallbacks(bakeNow);
        bakeNow.run();
    }

    // ---------------------------------------------------------------- preview

    /** The texts around the car, plus what each page adds to show its own options. */
    void updatePreview() {
        if (previewCar == null) return;
        ((TextView) findViewById(R.id.previewTitle)).setText(profile.displayTitle(pack));
        ((TextView) findViewById(R.id.previewSub)).setText(profile.subtitle(pack));
        ((TextView) findViewById(R.id.previewGreeting)).setText(
                profile.greeting.isEmpty() ? "" : getString(R.string.hello, profile.greeting));
    }

    /** A small version of Home's header, so the look chips show what they do. */
    void showLookPreview() {
        boolean mmi = "mmi".equals(profile.layout);
        findViewById(R.id.previewPanel).setBackgroundResource(mmi ? R.drawable.mmi_panel : R.drawable.glass_panel);
        Typeface face = Typeface.create(mmi ? "sans-serif-condensed" : "sans-serif-light", Typeface.NORMAL);
        findViewById(R.id.previewHeader).setVisibility(View.VISIBLE);
        ImageView logo = findViewById(R.id.previewLogo);
        logo.setImageDrawable(Brand.logo(this, pack, profile));
        logo.setImageTintList(ColorStateList.valueOf(Theming.color(this, R.attr.uiText)));
        TextView time = findViewById(R.id.previewTime);
        time.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
        time.setTypeface(face);
        TextView hello = findViewById(R.id.previewHello);
        hello.setText(profile.greetingLine(this));
        hello.setTypeface(Typeface.create(mmi ? "sans-serif-condensed" : "sans-serif-medium", Typeface.NORMAL));
        ((TextView) findViewById(R.id.previewTitle)).setTypeface(Typeface.create(mmi ? "sans-serif-condensed" : "sans-serif-medium", Typeface.NORMAL));
        ImageView home = findViewById(R.id.previewHome);
        home.setVisibility(View.VISIBLE);
        home.setBackgroundResource(mmi ? R.drawable.mmi_row_on : R.drawable.accent_circle);
    }

    /** The rail's four shortcuts under the car. */
    void showRailPreview() {
        LinearLayout row = findViewById(R.id.previewRail);
        row.removeAllViews();
        row.setVisibility(View.VISIBLE);
        int size = Math.round(44 * getResources().getDisplayMetrics().density);
        int gap = Math.round(10 * getResources().getDisplayMetrics().density);
        for (int i = 0; i < Profile.SLOTS; i++) {
            ImageView icon = new ImageView(this);
            String flat = Launch.slot(this, profile, i);
            icon.setImageDrawable(Launch.icon(this, flat));
            icon.setBackgroundResource(R.drawable.glass_circle);
            icon.setScaleType(Launch.RADIO.equals(flat) || Launch.MUSIC.equals(flat)
                    ? ImageView.ScaleType.CENTER : ImageView.ScaleType.FIT_CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            if (i > 0) lp.leftMargin = gap;
            row.addView(icon, lp);
        }
    }

    private void loadPreview() {
        if (previewCar == null) return;
        if (CarBaker.needsBake(this)) {
            CarBaker.bake(this);
            return;
        }
        final int generation = ++previewGeneration;
        final float density = getResources().getDisplayMetrics().density;
        final android.content.Context app = getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Bitmap b = CarImage.loadForDisplay(app, density);
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        if (previewCar == null || generation != previewGeneration) {
                            if (b != null) b.recycle();
                            return;
                        }
                        previewCar.setImageBitmap(b);
                        if (previewBitmap != null) previewBitmap.recycle();
                        previewBitmap = b;
                    }
                });
            }
        }, "launcher-preview").start();
    }

    private void releasePreview() {
        previewGeneration++;
        if (previewCar != null) previewCar.setImageDrawable(null);
        previewCar = null;
        if (previewBitmap != null) previewBitmap.recycle();
        previewBitmap = null;
    }

    // ---------------------------------------------------------------- what pages need from the activity

    /** Opens the app drawer in pick mode. */
    void pickApp(SettingsSection.OnResult cb) {
        onPicked = cb;
        startActivityForResult(new Intent(this, AppsActivity.class).putExtra(AppsActivity.EXTRA_PICK, true), REQ_PICK);
        Toast.makeText(this, R.string.pick_for_slot, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        SettingsSection.OnResult cb = onPicked;
        onPicked = null;
        if (requestCode == REQ_PICK && resultCode == RESULT_OK && data != null && cb != null) cb.result(data);
    }

    void withStorage(Runnable action) {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            action.run();
            return;
        }
        afterStorage = action;
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != REQ_STORAGE) return;
        Runnable action = afterStorage;
        afterStorage = null;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && action != null) action.run();
        else Toast.makeText(this, R.string.need_storage, Toast.LENGTH_SHORT).show();
    }

    boolean alive() {
        return !isDestroyed();
    }

    Handler handler() {
        return main;
    }

    /** Packs whose chips belong to one brand. */
    static boolean sameBrand(List<Pack> packs) {
        for (Pack p : packs) if (!p.brand.equals(packs.get(0).brand)) return false;
        return true;
    }
}
