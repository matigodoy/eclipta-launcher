package ar.com.eclipta.launcher;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Finds files on USB sticks and internal storage. Head units often lack a system file picker. */
final class Files {
    static final String FOLDER = "EcliptaLauncher";
    /** Sticks prepared for the app's previous name keep working. */
    private static final String[] READ_FOLDERS = {FOLDER, "A3Launcher"};
    private static final long MAX_PACK_FILE = 5 * 1024 * 1024;
    private static final long MAX_LOGO = 100 * 1024, MAX_STING = 2 * 1024 * 1024;
    static final String PACK_ID = "[a-z0-9][a-z0-9-]{0,63}";
    private static final String[] PACK_FILES = {"pack.json", "base.webp", "paint.webp", "logo.webp", "sting.webp"};

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

    /**
     * Saves into EcliptaLauncher/ on the first root that lets us write. Android blocks plain writes on
     * most USB sticks, except for the app's own folder there, so that is the fallback.
     */
    static File write(String name, String content, Context c) {
        List<File> dirs = new ArrayList<>();
        for (File root : roots()) dirs.add(new File(root, FOLDER));
        File[] own = c.getExternalFilesDirs(null);
        if (own != null) for (File d : own) if (d != null) dirs.add(d);
        for (File dir : dirs) {
            if (!dir.isDirectory() && !dir.mkdirs()) continue;
            File f = new File(dir, name);
            try (FileOutputStream os = new FileOutputStream(f)) {
                os.write(content.getBytes("UTF-8"));
                return f;
            } catch (IOException e) {
                DebugLog.log("write " + f + " failed: " + e);
            }
        }
        return null;
    }

    static String readText(File f) throws IOException {
        if (f.length() > MAX_PACK_FILE) throw new IOException("too big");
        byte[] b = new byte[(int) f.length()];
        try (FileInputStream in = new FileInputStream(f)) {
            int off = 0, n;
            while (off < b.length && (n = in.read(b, off, b.length - off)) > 0) off += n;
        }
        return new String(b, "UTF-8");
    }

    /** Copies every EcliptaLauncher/packs/<id>/ with a valid pack.json into private storage. */
    static int importPacks(Context c) {
        int count = 0;
        for (File root : roots()) {
            for (String folder : READ_FOLDERS) {
                File[] packs = new File(new File(root, folder), "packs").listFiles();
                if (packs == null) continue;
                for (File dir : packs) if (copyPack(c, dir)) count++;
            }
        }
        return count;
    }

    private static boolean copyPack(Context c, File src) {
        if (!src.isDirectory() || !src.getName().matches(PACK_ID) || !new File(src, "pack.json").isFile()) return false;
        try {
            if (!new File(src, "base.webp").isFile() || !new File(src, "paint.webp").isFile()) return false;
            if (!Pack.parses(readText(new File(src, "pack.json")), src.getName())) return false;
            File dst = new File(new File(c.getFilesDir(), "packs"), src.getName());
            if (!dst.isDirectory() && !dst.mkdirs()) return false;
            for (String name : PACK_FILES) {
                File from = new File(src, name);
                if (!acceptable(from, name)) continue;
                try (FileInputStream in = new FileInputStream(from);
                     FileOutputStream out = new FileOutputStream(new File(dst, name))) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                }
            }
            return new File(dst, "base.webp").isFile();
        } catch (Exception e) {
            DebugLog.log("pack import " + src + " error: " + e);
            return false;
        }
    }

    /** Nothing validates a stick's pack before it reaches Home and the wake animation, so limit what those can cost. */
    private static boolean acceptable(File f, String name) {
        if (!f.isFile() || f.length() > MAX_PACK_FILE) return false;
        if (name.equals("logo.webp")) return f.length() <= MAX_LOGO && fits(f, 184, 184);
        if (name.equals("sting.webp")) return f.length() <= MAX_STING && fits(f, 1024, 600);
        return true;
    }

    private static boolean fits(File f, int maxW, int maxH) {
        android.graphics.BitmapFactory.Options o = new android.graphics.BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        android.graphics.BitmapFactory.decodeFile(f.getPath(), o);
        return o.outWidth > 0 && o.outWidth <= maxW && o.outHeight <= maxH;
    }

    /** Looks in each root, its EcliptaLauncher folder, Download and Pictures. Not recursive: stays fast. */
    static List<File> find(String... extensions) {
        List<File> out = new ArrayList<>();
        for (File root : roots()) {
            for (File dir : new File[]{root, new File(root, FOLDER), new File(root, READ_FOLDERS[1]),
                    new File(root, "Download"), new File(root, "Pictures")}) {
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
