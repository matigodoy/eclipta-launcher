package com.mati.a3launcher;

import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Finds files on USB sticks and internal storage. Head units often lack a system file picker. */
final class Files {
    static final String FOLDER = "A3Launcher";

    private Files() {}

    /** Mounted storage roots: every readable /storage/* plus internal storage. */
    static List<File> roots() {
        Set<File> out = new LinkedHashSet<>();
        for (String base : new String[]{"/storage", "/mnt/media_rw", "/mnt/usb_storage"}) {
            File[] kids = new File(base).listFiles();
            if (kids == null) continue;
            for (File k : kids) {
                String n = k.getName();
                if (n.equals("self") || n.equals("emulated")) continue;
                if (k.isDirectory() && k.canRead()) out.add(k);
            }
        }
        out.add(Environment.getExternalStorageDirectory());
        return new ArrayList<>(out);
    }

    /** Looks in each root, its A3Launcher folder, Download and Pictures. Not recursive: stays fast. */
    static List<File> find(String... extensions) {
        List<File> out = new ArrayList<>();
        for (File root : roots()) {
            for (File dir : new File[]{root, new File(root, FOLDER), new File(root, "Download"), new File(root, "Pictures")}) {
                File[] files = dir.listFiles();
                if (files == null) continue;
                Arrays.sort(files);
                for (File f : files) {
                    String n = f.getName().toLowerCase(Locale.US);
                    if (!f.isFile() || n.startsWith(".")) continue;
                    for (String e : extensions) {
                        if (n.endsWith(e)) {
                            out.add(f);
                            break;
                        }
                    }
                }
            }
        }
        return out;
    }
}
