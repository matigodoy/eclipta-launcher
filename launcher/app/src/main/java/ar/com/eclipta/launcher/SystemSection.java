package ar.com.eclipta.launcher;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.util.List;

/** Sistema: language, copies of the settings, car packs and the support report. */
final class SystemSection extends SettingsSection {
    SystemSection(SettingsActivity a) {
        super(a);
    }

    @Override
    int title() {
        return R.string.sec_system;
    }

    @Override
    int description() {
        return R.string.sec_system_desc;
    }

    @Override
    void build() {
        f.into(column(0));
        f.group(R.string.grp_lang);
        f.chips(R.string.f_lang, R.string.f_lang_hint, SettingsForm.list("auto", "es", "en", "pt"),
                f.strings(R.string.lang_auto, R.string.lang_es, R.string.lang_en, R.string.lang_pt), p.lang, id -> {
                    p.lang = id;
                    a.saved();
                    a.recreate();
                });
        f.group(R.string.grp_packs);
        f.actionRow(R.string.packs_import, R.string.packs_import_desc, R.string.btn_import, () -> a.withStorage(this::importPacks));

        f.into(column(1));
        f.group(R.string.grp_backup);
        f.actionRow(R.string.backup_export, R.string.backup_export_desc, R.string.btn_export, () -> a.withStorage(this::exportProfile));
        f.actionRow(R.string.backup_import, R.string.backup_import_desc, R.string.btn_import, () -> a.withStorage(this::importProfile));
        f.group(R.string.grp_support);
        f.actionRow(R.string.diag_export, R.string.diag_tip, R.string.btn_export, () -> a.withStorage(this::exportDiagnostics));
        f.actionRow(R.string.uninstall, R.string.uninstall_desc, R.string.btn_uninstall, this::uninstall);
    }

    /** These units hide Android's own Settings, so this may be the only way back to the stock home. */
    private void uninstall() {
        try {
            a.startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + a.getPackageName())));
        } catch (RuntimeException e) {
            Toast.makeText(a, R.string.cannot_open, Toast.LENGTH_SHORT).show();
        }
    }

    private void showSaved(File file) {
        Toast.makeText(a, file == null ? a.getString(R.string.backup_failed)
                : a.getString(R.string.backup_saved, file.getPath()), Toast.LENGTH_LONG).show();
    }

    private void exportProfile() {
        try {
            JSONObject o = p.toJson().put("reminders", Reminders.toJson(Reminders.load(a)));
            showSaved(Files.write("perfil.json", o.toString(2), a));
        } catch (org.json.JSONException e) {
            Toast.makeText(a, R.string.backup_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void exportDiagnostics() {
        showSaved(Files.write("diagnostico.txt", Diagnostics.report(a), a));
    }

    private void importProfile() {
        File found = null;
        for (File file : Files.find(".json")) {
            if (file.getName().equalsIgnoreCase("perfil.json")) {
                found = file;
                break;
            }
        }
        if (found == null) {
            Toast.makeText(a, R.string.import_none, Toast.LENGTH_LONG).show();
            return;
        }
        try {
            JSONObject o = new JSONObject(Files.readText(found));
            p.applyJson(o);
            p.customPhoto = p.customPhoto && CarImage.customFile(a).isFile();
            List<Reminders.Item> reminders = Reminders.fromProfile(o, Reminders.load(a));
            if (reminders != null) Reminders.save(a, reminders);
            a.pack = Pack.load(a, p.pack);
            a.carChanged();
            a.recreate();
        } catch (Exception e) {
            Toast.makeText(a, R.string.import_bad, Toast.LENGTH_LONG).show();
        }
    }

    private void importPacks() {
        final Context app = a.getApplicationContext();
        new Thread(() -> {
            final int n = Files.importPacks(app);
            // The text comes from the activity: the app context keeps the system language, not the one picked here.
            a.handler().post(() -> Toast.makeText(app, a.getString(R.string.packs_done, n), Toast.LENGTH_LONG).show());
        }, "launcher-packs").start();
    }
}
