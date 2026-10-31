package ar.com.eclipta.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A vehicle pack: a folder with pack.json, base.webp and paint.webp. Built-in packs live in
 * assets/packs, imported ones in files/packs. Parsed only when needed, never kept alive.
 */
final class Pack {
    static final class Option {
        final String id, name;
        final int color; // paints only

        Option(String id, String name, int color) {
            this.id = id;
            this.name = name;
            this.color = color;
        }
    }

    String id = "", title = "", brand = "", defBody = "", defEngine = "", defYear = "";
    String[] bodies = new String[0], engines = new String[0];
    float refWidth, gain, gamma, spec;
    final List<Option> paints = new ArrayList<>();
    final List<Option> wheels = new ArrayList<>();
    /** cx, cy, rx, ry, angle per wheel, in ref_width pixels. */
    float[][] wheelCenters = new float[0][];
    /** Four x,y corners in ref_width pixels, clockwise from top left. Null = no plate spot. */
    float[] plate;
    private File dir; // null = assets

    static List<String> ids(Context c) {
        List<String> out = new ArrayList<>();
        try {
            String[] builtin = c.getAssets().list("packs");
            if (builtin != null) for (String s : builtin) out.add(s);
        } catch (IOException ignored) {
        }
        String[] ext = new File(c.getFilesDir(), "packs").list();
        if (ext != null) for (String s : ext) if (!out.contains(s)) out.add(s);
        return out;
    }

    /** Falls back to the default pack, then any pack, then an empty one: never null. */
    static Pack load(Context c, String id) {
        Pack p = tryLoad(c, id);
        if (p == null) p = tryLoad(c, Profile.DEFAULT_PACK);
        if (p == null) {
            for (String other : ids(c)) {
                p = tryLoad(c, other);
                if (p != null) break;
            }
        }
        return p != null ? p : new Pack();
    }

    /** Folder name under assets/brands: "Alfa Romeo" is alfa-romeo. Empty when the pack has no usable brand. */
    String brandKey() {
        String key = brand.trim().toLowerCase(java.util.Locale.ROOT).replace(' ', '-');
        return key.matches("[a-z0-9-]+") ? key : "";
    }

    /** A file in an imported pack's folder. Null for built-in packs. */
    File localFile(String name) {
        return dir == null ? null : new File(dir, name);
    }

    private static Pack tryLoad(Context c, String id) {
        try {
            Pack p = new Pack();
            File ext = new File(new File(c.getFilesDir(), "packs"), id);
            if (new File(ext, "pack.json").isFile()) p.dir = ext;
            p.parse(new JSONObject(new String(readAll(p.open(c, "pack.json", id)), "UTF-8")), id);
            return p;
        } catch (Throwable t) {
            DebugLog.log("pack " + id + " error: " + t);
            return null;
        }
    }

    private void parse(JSONObject o, String fallbackId) throws JSONException {
        id = o.optString("id", fallbackId);
        title = o.getString("title");
        brand = o.optString("brand", "").trim();
        refWidth = (float) o.optDouble("ref_width", 920);
        JSONObject d = o.optJSONObject("defaults");
        defBody = d == null ? "" : d.optString("body");
        defEngine = d == null ? "" : d.optString("engine");
        defYear = d == null ? "" : d.optString("year");
        bodies = strings(o.optJSONArray("bodies"));
        engines = strings(o.optJSONArray("engines"));
        JSONObject paint = o.optJSONObject("paint");
        gain = paint == null ? 1f : (float) paint.optDouble("gain", 1);
        gamma = paint == null ? 1.5f : (float) paint.optDouble("gamma", 1.5);
        spec = paint == null ? 0.25f : (float) paint.optDouble("spec", 0.25);
        options(o.optJSONArray("paints"), paints);
        options(o.optJSONArray("wheels"), wheels);
        JSONArray wc = o.optJSONArray("wheel_centers");
        if (wc != null) {
            wheelCenters = new float[wc.length()][];
            for (int i = 0; i < wc.length(); i++) {
                JSONObject w = wc.getJSONObject(i);
                wheelCenters[i] = new float[]{(float) w.getDouble("cx"), (float) w.getDouble("cy"),
                        (float) w.getDouble("rx"), (float) w.getDouble("ry"), (float) w.optDouble("angle", 0)};
            }
        }
        JSONArray pl = o.optJSONArray("plate");
        if (pl != null && pl.length() == 8) {
            plate = new float[8];
            for (int i = 0; i < 8; i++) plate[i] = (float) pl.getDouble(i);
        }
    }

    private static String[] strings(JSONArray a) throws JSONException {
        if (a == null) return new String[0];
        String[] out = new String[a.length()];
        for (int i = 0; i < out.length; i++) out[i] = a.getString(i);
        return out;
    }

    private static void options(JSONArray a, List<Option> out) throws JSONException {
        if (a == null) return;
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.getJSONObject(i);
            int color = 0;
            if (o.has("color")) color = android.graphics.Color.parseColor(o.getString("color"));
            out.add(new Option(o.getString("id"), o.getString("name"), color));
        }
    }

    Option paint(String paintId) {
        for (Option o : paints) if (o.id.equals(paintId)) return o;
        return null;
    }

    private InputStream open(Context c, String file, String packId) throws IOException {
        if (dir != null) return new FileInputStream(new File(dir, file));
        return c.getAssets().open("packs/" + packId + "/" + file);
    }

    Bitmap bitmap(Context c, String file) throws IOException {
        InputStream in = open(c, file, id);
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inMutable = true;
            o.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeStream(in, null, o);
        } finally {
            in.close();
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toByteArray();
        } finally {
            in.close();
        }
    }
}
