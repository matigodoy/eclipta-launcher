package ar.com.eclipta.launcher;

import android.app.AlertDialog;
import android.text.InputType;
import android.widget.Toast;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Mi auto: which car, its color and wheels, a photo of your own, the plate. */
final class CarSection extends SettingsSection {
    CarSection(SettingsActivity a) {
        super(a);
    }

    @Override
    int title() {
        return R.string.sec_car;
    }

    @Override
    int description() {
        return R.string.sec_car_desc;
    }

    @Override
    boolean split() {
        return true;
    }

    @Override
    void build() {
        Pack pack = a.pack;
        f.group(R.string.grp_car);
        modelChips();
        // A photo of your own, or a pack with no list (the generic car), takes free text.
        if (p.customPhoto || pack.bodies.length == 0) {
            textField(R.string.f_body, p.body, 24, v -> p.body = v);
        } else {
            f.chips(R.string.f_body, 0, SettingsForm.list(pack.bodies), SettingsForm.list(pack.bodies), pick(p.body, pack.defBody), id -> {
                p.body = id;
                a.carChanged();
            });
        }
        if (p.customPhoto || pack.engines.length == 0) {
            textField(R.string.f_engine, p.engine, 32, v -> p.engine = v);
        } else {
            f.chips(R.string.f_engine, 0, SettingsForm.list(pack.engines), SettingsForm.list(pack.engines), pick(p.engine, pack.defEngine), id -> {
                p.engine = id;
                a.carChanged();
            });
        }
        String year = pick(p.year, p.customCar() ? "" : pack.defYear);
        f.textRow(R.string.f_year, null, year, year, 0, InputType.TYPE_CLASS_NUMBER, 4, v -> {
            p.year = v;
            a.carChanged();
            a.rebuild();
        });
        String title = p.displayTitle(pack);
        f.textRow(R.string.f_title, a.getString(R.string.f_title_desc), title, p.title, 0,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS, 24, v -> {
                    p.title = v;
                    a.carChanged();
                    a.rebuild();
                });

        if (!p.customPhoto) {
            f.group(R.string.grp_paint);
            List<String> ids = new ArrayList<>(), names = new ArrayList<>();
            int[] colors = new int[pack.paints.size()];
            for (int i = 0; i < colors.length; i++) {
                ids.add(pack.paints.get(i).id);
                names.add(pack.paints.get(i).name);
                colors[i] = pack.paints.get(i).color;
            }
            f.chips(R.string.f_paint, 0, ids, names, colors, p.paint, id -> {
                p.paint = id;
                a.carChanged();
            });
            ids = new ArrayList<>();
            names = new ArrayList<>();
            for (Pack.Option o : pack.wheels) {
                ids.add(o.id);
                names.add(o.name);
            }
            f.chips(R.string.f_wheels, R.string.paint_hint, ids, names, p.wheels, id -> {
                p.wheels = id;
                a.carChanged();
            });
        }

        f.group(R.string.grp_photo);
        f.actionRow(R.string.f_photo, R.string.photo_tip, R.string.photo_pick, () -> a.withStorage(this::pickPhoto));
        if (p.customPhoto) {
            f.actionRow(R.string.photo_reset, 0, R.string.photo_reset_button, () -> {
                // Those texts described the photo, not this model.
                p.customPhoto = false;
                p.title = "";
                p.body = "";
                p.engine = "";
                CarImage.customFile(a).delete();
                a.carChanged();
                a.rebuild();
            });
        }

        if (!p.customPhoto && pack.plate != null) {
            f.group(R.string.grp_plate);
            f.textRow(R.string.f_plate, a.getString(R.string.f_plate_desc), p.plate, p.plate, 0,
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS, 10, v -> {
                        p.plate = v;
                        a.carChanged();
                        a.rebuild();
                    });
            f.switchRow(R.string.f_plate_show, a.getString(R.string.f_plate_show_desc), p.plateShow, on -> {
                p.plateShow = on;
                a.carChanged();
            });
        }
    }

    private interface Setter {
        void set(String v);
    }

    private void textField(int title, String value, int maxLen, Setter setter) {
        f.textRow(title, null, value, value, 0, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS, maxLen, v -> {
            setter.set(v);
            a.carChanged();
            a.rebuild();
        });
    }

    /** Brand chips appear once packs span more than one brand. */
    private void modelChips() {
        List<Pack> packs = new ArrayList<>();
        for (String id : Pack.ids(a)) {
            // Load falls back to another pack when one is broken: that one isn't offered.
            Pack o = Pack.load(a, id);
            if (o.id.equals(id)) packs.add(o);
        }
        // Built-in models first, then imported ones by name: the folder order on disk is arbitrary.
        final Collator byName = Collator.getInstance();
        Collections.sort(packs, (x, y) -> x.builtIn() != y.builtIn() ? (x.builtIn() ? -1 : 1) : byName.compare(x.title, y.title));
        String brand = a.pack.brand;
        if (!packs.isEmpty() && !SettingsActivity.sameBrand(packs)) {
            List<String> ids = new ArrayList<>(), names = new ArrayList<>();
            for (Pack o : packs) {
                if (ids.contains(o.brand)) continue;
                ids.add(o.brand);
                names.add(o.brand.isEmpty() ? a.getString(R.string.brand_other) : o.brand);
            }
            f.chips(R.string.f_brand, 0, ids, names, brand, id -> {
                if (id.equals(brand)) return;
                for (Pack o : packs) {
                    if (o.brand.equals(id)) {
                        choose(o.id);
                        return;
                    }
                }
            });
        }
        List<String> ids = new ArrayList<>(), names = new ArrayList<>();
        for (Pack o : packs) {
            if (!o.brand.equals(brand)) continue;
            ids.add(o.id);
            names.add(o.title);
        }
        // Only the generic car: say where the others come from.
        f.chips(R.string.f_model, packs.size() < 2 ? R.string.f_model_hint : 0, ids, names, p.pack, this::choose);
    }

    private void choose(String id) {
        if (id.equals(p.pack)) return;
        p.pack = id;
        if (!p.customPhoto) p.title = ""; // with a photo, the name is what keeps the pack's logo off it
        p.engine = "";
        p.year = "";
        p.body = "";
        p.paint = "original";
        p.wheels = "original";
        a.pack = Pack.load(a, id);
        a.carChanged();
        a.rebuild();
    }

    private static String pick(String v, String fallback) {
        return v == null || v.isEmpty() ? fallback : v;
    }

    private void pickPhoto() {
        final List<File> files = Files.find(".png", ".webp", ".jpg", ".jpeg");
        if (files.isEmpty()) {
            Toast.makeText(a, R.string.photo_none, Toast.LENGTH_LONG).show();
            return;
        }
        CharSequence[] names = new CharSequence[files.size()];
        for (int i = 0; i < names.length; i++) names[i] = files.get(i).getName();
        new AlertDialog.Builder(a)
                .setTitle(R.string.f_photo)
                .setItems(names, (d, which) -> importPhoto(files.get(which)))
                .show();
    }

    private void importPhoto(final File file) {
        final android.content.Context app = a.getApplicationContext();
        new Thread(() -> {
            final boolean ok = CarImage.importPhoto(app, file.getPath());
            a.handler().post(() -> {
                if (!a.alive()) return;
                if (!ok) {
                    Toast.makeText(a, R.string.photo_error, Toast.LENGTH_SHORT).show();
                    return;
                }
                p.customPhoto = true;
                a.carChanged();
                a.rebuild();
            });
        }, "launcher-photo").start();
    }
}
