package com.mati.a3launcher;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * "Personalizar". Only exists while open. Changes are saved right away and the car picture is
 * re-baked once, in the background, shortly after the last change.
 */
public class SettingsActivity extends Activity {
    private static final int REQ_STORAGE = 1;
    private static final long BAKE_DELAY_MS = 350;

    private interface OnChoice {
        void chosen(String id);
    }

    private final Handler main = new Handler(Looper.getMainLooper());
    private Profile profile;
    private Pack pack;
    private LinearLayout form;
    private ImageView previewCar;
    private Bitmap previewBitmap;
    private float dp;
    private volatile boolean destroyed;
    private int bakeGeneration;

    private final Runnable bakeNow = new Runnable() {
        @Override
        public void run() {
            final int generation = ++bakeGeneration;
            final Profile snapshot = Profile.load(SettingsActivity.this);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    CarImage.bake(getApplicationContext(), snapshot);
                    final Bitmap b = CarImage.loadForDisplay(getApplicationContext(), dp);
                    main.post(new Runnable() {
                        @Override
                        public void run() {
                            if (destroyed || generation != bakeGeneration) {
                                if (b != null) b.recycle();
                                return;
                            }
                            showPreview(b);
                        }
                    });
                }
            }, "a3l-bake").start();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dp = getResources().getDisplayMetrics().density;
        getWindow().setBackgroundDrawable(new GlowBackground(dp));
        setContentView(R.layout.activity_settings);
        profile = Profile.load(this);
        pack = Pack.load(this, profile.pack);
        form = findViewById(R.id.form);
        // No field takes focus (and pops the keyboard) until touched.
        form.setFocusableInTouchMode(true);
        form.requestFocus();
        previewCar = findViewById(R.id.previewCar);
        findViewById(R.id.btnClose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        buildForm();
        updateTexts();
        changed();
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        main.removeCallbacksAndMessages(null);
        previewCar.setImageDrawable(null);
        if (previewBitmap != null) previewBitmap.recycle();
        super.onDestroy();
    }

    @Override
    public void finish() {
        setResult(RESULT_OK);
        super.finish();
    }

    private void showPreview(Bitmap b) {
        previewCar.setImageBitmap(b);
        if (previewBitmap != null) previewBitmap.recycle();
        previewBitmap = b;
    }

    /** Persists the profile and schedules one bake. */
    private void changed() {
        profile.save(this);
        updateTexts();
        main.removeCallbacks(bakeNow);
        main.postDelayed(bakeNow, BAKE_DELAY_MS);
    }

    private void updateTexts() {
        ((TextView) findViewById(R.id.previewTitle)).setText(pack.title);
        ((TextView) findViewById(R.id.previewSub)).setText(profile.subtitle(pack));
        ((TextView) findViewById(R.id.previewGreeting)).setText(
                profile.greeting.isEmpty() ? "" : getString(R.string.hello, profile.greeting));
    }

    private void buildForm() {
        form.removeAllViews();
        section(R.string.sec_car);
        List<String> ids = Pack.ids(this);
        List<String> names = new ArrayList<>();
        for (String id : ids) {
            Pack p = Pack.load(this, id);
            names.add(p.title);
        }
        chips(R.string.f_model, ids, names, profile.pack, new OnChoice() {
            @Override
            public void chosen(String id) {
                profile.pack = id;
                profile.engine = "";
                profile.year = "";
                profile.body = "";
                pack = Pack.load(SettingsActivity.this, id);
                buildForm();
                changed();
            }
        });
        chips(R.string.f_body, list(pack.bodies), list(pack.bodies), pick(profile.body, pack.defBody), new OnChoice() {
            @Override
            public void chosen(String id) {
                profile.body = id;
                changed();
            }
        });
        chips(R.string.f_engine, list(pack.engines), list(pack.engines), pick(profile.engine, pack.defEngine), new OnChoice() {
            @Override
            public void chosen(String id) {
                profile.engine = id;
                changed();
            }
        });
        field(R.string.f_year, pick(profile.year, pack.defYear), InputType.TYPE_CLASS_NUMBER, 4, new OnChoice() {
            @Override
            public void chosen(String v) {
                profile.year = v;
                changed();
            }
        });
        label(R.string.f_photo);
        LinearLayout photo = row();
        photo.addView(chip(getString(R.string.photo_pick), false, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickPhoto();
            }
        }));
        if (profile.customPhoto) {
            photo.addView(chip(getString(R.string.photo_reset), false, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    profile.customPhoto = false;
                    CarImage.customFile(SettingsActivity.this).delete();
                    buildForm();
                    changed();
                }
            }));
        }
        form.addView(photo);
        hint(R.string.photo_tip);

        section(R.string.sec_you);
        field(R.string.f_greeting, profile.greeting, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS, 20, new OnChoice() {
            @Override
            public void chosen(String v) {
                profile.greeting = v.trim();
                changed();
            }
        });
        field(R.string.f_plate, profile.plate, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS, 10, new OnChoice() {
            @Override
            public void chosen(String v) {
                profile.plate = v;
                changed();
            }
        });
        check(R.string.f_plate_show, profile.plateShow, new OnChoice() {
            @Override
            public void chosen(String v) {
                profile.plateShow = v != null;
                changed();
            }
        });

        section(R.string.sec_start);
        check(R.string.f_splash, profile.splash, new OnChoice() {
            @Override
            public void chosen(String v) {
                profile.splash = v != null;
                changed();
            }
        });
        check(R.string.f_carplay_acc, profile.carplayOnAcc, new OnChoice() {
            @Override
            public void chosen(String v) {
                profile.carplayOnAcc = v != null;
                changed();
            }
        });

        section(R.string.sec_rail);
        hint(R.string.rail_tip);
        LinearLayout rail = row();
        rail.addView(chip(getString(R.string.rail_reset), false, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < Profile.SLOTS; i++) profile.slots[i] = "";
                changed();
                Toast.makeText(SettingsActivity.this, R.string.done, Toast.LENGTH_SHORT).show();
            }
        }));
        form.addView(rail);
    }

    private static String pick(String a, String fallback) {
        return a == null || a.isEmpty() ? fallback : a;
    }

    private static List<String> list(String[] a) {
        List<String> out = new ArrayList<>();
        for (String s : a) out.add(s);
        return out;
    }

    // ---------------------------------------------------------------- photo

    private void pickPhoto() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_STORAGE);
            return;
        }
        final List<File> files = Files.find(".png", ".webp", ".jpg", ".jpeg");
        if (files.isEmpty()) {
            Toast.makeText(this, R.string.photo_none, Toast.LENGTH_LONG).show();
            return;
        }
        CharSequence[] names = new CharSequence[files.size()];
        for (int i = 0; i < names.length; i++) names[i] = files.get(i).getName();
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle(R.string.f_photo)
                .setItems(names, (d, which) -> importPhoto(files.get(which)))
                .show();
    }

    private void importPhoto(final File f) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean ok = CarImage.importPhoto(getApplicationContext(), f.getPath());
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        if (destroyed) return;
                        if (!ok) {
                            Toast.makeText(SettingsActivity.this, R.string.photo_error, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        profile.customPhoto = true;
                        buildForm();
                        changed();
                    }
                });
            }
        }, "a3l-photo").start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != REQ_STORAGE) return;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) pickPhoto();
        else Toast.makeText(this, R.string.need_storage, Toast.LENGTH_SHORT).show();
    }

    // ---------------------------------------------------------------- form helpers

    private int px(int v) {
        return Math.round(v * dp);
    }

    private void section(int text) {
        TextView t = new TextView(this);
        t.setText(getString(text).toUpperCase());
        t.setTextAppearance(R.style.Label);
        t.setPadding(0, px(18), 0, px(2));
        form.addView(t);
    }

    private void label(int text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(getColor(R.color.text2));
        t.setTextSize(13);
        t.setPadding(0, px(10), 0, px(6));
        form.addView(t);
    }

    private void hint(int text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(getColor(R.color.text2));
        t.setTextSize(12);
        t.setPadding(0, px(6), 0, px(6));
        form.addView(t);
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }

    private TextView chip(String text, boolean selected, View.OnClickListener click) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(getColor(R.color.text));
        t.setTextSize(14);
        t.setGravity(Gravity.CENTER);
        t.setBackgroundResource(R.drawable.chip_bg);
        t.setSelected(selected);
        t.setMinHeight(px(44));
        t.setPadding(px(16), 0, px(16), 0);
        t.setOnClickListener(click);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, px(44));
        lp.rightMargin = px(8);
        t.setLayoutParams(lp);
        return t;
    }

    private void chips(int title, List<String> ids, List<String> names, String selected, final OnChoice cb) {
        if (ids.isEmpty()) return;
        label(title);
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        final LinearLayout l = row();
        for (int i = 0; i < ids.size(); i++) {
            final String id = ids.get(i);
            l.addView(chip(names.get(i), id.equals(selected), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for (int k = 0; k < l.getChildCount(); k++) l.getChildAt(k).setSelected(false);
                    v.setSelected(true);
                    cb.chosen(id);
                }
            }));
        }
        scroll.addView(l);
        form.addView(scroll);
    }

    private void field(int title, String value, int inputType, int maxLen, final OnChoice cb) {
        label(title);
        EditText e = new EditText(this);
        e.setText(value);
        e.setSingleLine(true);
        e.setInputType(inputType);
        e.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(maxLen)});
        e.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_ACTION_DONE);
        e.setTextColor(getColor(R.color.text));
        e.setTextSize(16);
        e.setBackgroundResource(R.drawable.field_bg);
        e.setPadding(px(14), 0, px(14), 0);
        e.setMinHeight(px(44));
        e.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                cb.chosen(s.toString());
            }
        });
        form.addView(e, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, px(44)));
    }

    /** cb gets "" when checked and null when not. */
    private void check(int title, boolean value, final OnChoice cb) {
        CheckBox c = new CheckBox(this);
        c.setText(title);
        c.setChecked(value);
        c.setTextColor(getColor(R.color.text));
        c.setTextSize(15);
        c.setMinHeight(px(44));
        c.setOnCheckedChangeListener((b, on) -> cb.chosen(on ? "" : null));
        form.addView(c);
    }
}
