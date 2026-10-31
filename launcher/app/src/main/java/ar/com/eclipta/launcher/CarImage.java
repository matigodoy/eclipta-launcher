package ar.com.eclipta.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * The car picture. All layers are combined once when the profile changes ("baked") into a single
 * PNG, so the home screen only ever decodes one small bitmap. Call bake() off the main thread.
 */
final class CarImage {
    private static final int BAKE_WIDTH = 640;
    private static final int SHOW_WIDTH_DP = 460;

    private CarImage() {}

    static File bakedFile(Context c) {
        return new File(c.getFilesDir(), "car_baked.png");
    }

    static File customFile(Context c) {
        return new File(c.getFilesDir(), "car_custom.png");
    }

    /** For the home screen: the baked picture, scaled to what the screen shows. */
    static Bitmap loadForDisplay(Context c, float density) {
        File f = bakedFile(c);
        Bitmap b = f.isFile() ? BitmapFactory.decodeFile(f.getPath()) : null;
        if (b == null) {
            try {
                Profile p = Profile.load(c);
                b = Pack.load(c, p.pack).bitmap(c, "base.webp");
            } catch (Throwable t) {
                DebugLog.log("car load error: " + t);
                return null;
            }
        }
        int w = Math.round(SHOW_WIDTH_DP * density);
        if (b == null || b.getWidth() <= w) return b;
        Bitmap s = Bitmap.createScaledBitmap(b, w, Math.round(b.getHeight() * (float) w / b.getWidth()), true);
        if (s != b) b.recycle();
        return s;
    }

    /** Rebuilds car_baked.png from the profile. Returns true on success. */
    static boolean bake(Context c, Profile p) {
        try {
            Bitmap out;
            if (p.customPhoto && customFile(c).isFile()) {
                out = BitmapFactory.decodeFile(customFile(c).getPath());
                if (out == null) return false;
                out = out.copy(Bitmap.Config.ARGB_8888, true);
            } else {
                Pack pack = Pack.load(c, p.pack);
                out = pack.bitmap(c, "base.webp");
                Pack.Option paint = pack.paint(p.paint);
                if (paint != null && paint.color != 0) paint(out, pack.bitmap(c, "paint.webp"), paint.color, pack);
                drawWheels(c, out, pack, p.wheels);
                drawPlate(out, pack, p);
            }
            out = withShadow(out);
            File tmp = new File(c.getFilesDir(), "car_baked.tmp");
            try (FileOutputStream os = new FileOutputStream(tmp)) {
                out.compress(Bitmap.CompressFormat.PNG, 100, os);
            }
            out.recycle();
            return tmp.renameTo(bakedFile(c));
        } catch (Throwable t) {
            DebugLog.log("bake error: " + t);
            return false;
        }
    }

    /**
     * Recolors the body. Keeps the photo's shading through its luminance and adds a little
     * white back on the highlights so dark paints still show reflections.
     */
    private static void paint(Bitmap car, Bitmap mask, int color, Pack pack) {
        int w = car.getWidth(), h = car.getHeight();
        Bitmap m = mask.getWidth() == w && mask.getHeight() == h ? mask : Bitmap.createScaledBitmap(mask, w, h, true);
        int[] px = new int[w * h];
        int[] mx = new int[w * h];
        car.getPixels(px, 0, w, 0, 0, w, h);
        m.getPixels(mx, 0, w, 0, 0, w, h);
        PaintMath math = new PaintMath(color, pack.gain, pack.gamma, pack.spec);
        for (int i = 0; i < px.length; i++) {
            int amount = mx[i] & 0xFF;
            if (amount == 0) continue;
            px[i] = math.apply(px[i], amount);
        }
        car.setPixels(px, 0, w, 0, 0, w, h);
        if (m != mask) m.recycle();
        mask.recycle();
    }

    private static void drawWheels(Context c, Bitmap car, Pack pack, String wheelId) {
        if (wheelId == null || wheelId.isEmpty() || "original".equals(wheelId) || pack.wheelCenters.length == 0) return;
        Bitmap wheel = null;
        try (InputStream in = c.getAssets().open("wheels/" + wheelId + ".webp")) {
            wheel = BitmapFactory.decodeStream(in);
        } catch (IOException e) {
            DebugLog.log("wheel " + wheelId + " error: " + e);
        }
        if (wheel == null) return;
        float k = car.getWidth() / pack.refWidth;
        Canvas canvas = new Canvas(car);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
        Matrix m = new Matrix();
        for (float[] w : pack.wheelCenters) {
            m.reset();
            m.setTranslate(-wheel.getWidth() / 2f, -wheel.getHeight() / 2f);
            m.postScale(w[2] * 2 * k / wheel.getWidth(), w[3] * 2 * k / wheel.getHeight());
            m.postRotate(w[4]);
            m.postTranslate(w[0] * k, w[1] * k);
            canvas.drawBitmap(wheel, m, paint);
        }
        wheel.recycle();
    }

    /** Draws the plate into the pack's plate quad with the photo's perspective. */
    private static void drawPlate(Bitmap car, Pack pack, Profile p) {
        if (!p.plateShow || pack.plate == null || p.plate == null || p.plate.trim().isEmpty()) return;
        String text = PlateFormat.normalize(p.plate);
        Bitmap plate = plateBitmap(text);
        float k = car.getWidth() / pack.refWidth;
        float[] dst = new float[8];
        for (int i = 0; i < 8; i++) dst[i] = pack.plate[i] * k;
        float w = plate.getWidth(), h = plate.getHeight();
        Matrix m = new Matrix();
        m.setPolyToPoly(new float[]{0, 0, w, 0, w, h, 0, h}, 0, dst, 0, 4);
        new Canvas(car).drawBitmap(plate, m, new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG));
        plate.recycle();
    }

    private static Bitmap plateBitmap(String text) {
        boolean mercosur = PlateFormat.isMercosur(text);
        int w = 400, h = 120;
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        RectF r = new RectF(0, 0, w, h);
        p.setColor(mercosur ? 0xFFF4F4F4 : 0xFF111114);
        c.drawRoundRect(r, 10, 10, p);
        int top = 0;
        if (mercosur) {
            p.setColor(0xFF1D3F99);
            c.drawRect(0, 0, w, 26, p);
            top = 26;
        }
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(5);
        p.setColor(mercosur ? 0xFF111111 : 0xFFD9D9D9);
        c.drawRoundRect(new RectF(3, 3, w - 3, h - 3), 9, 9, p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(mercosur ? 0xFF111111 : 0xFFF4F4F4);
        p.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        p.setTextSize(72);
        float tw = p.measureText(text);
        if (tw > w - 24) p.setTextSize(72 * (w - 24) / tw);
        p.setTextAlign(Paint.Align.CENTER);
        Rect bounds = new Rect();
        p.getTextBounds("A0", 0, 2, bounds);
        c.drawText(text, w / 2f, top + (h - top + bounds.height()) / 2f, p);
        return b;
    }

    /** Adds the soft contact shadow the cutout photos lack. Needs room below the car. */
    private static Bitmap withShadow(Bitmap car) {
        int extra = Math.round(car.getHeight() * 0.06f);
        Bitmap out = Bitmap.createBitmap(car.getWidth(), car.getHeight() + extra, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        float cx = car.getWidth() / 2f, cy = car.getHeight() * 0.97f;
        float rx = car.getWidth() * 0.52f, ry = car.getHeight() * 0.11f;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Matrix m = new Matrix();
        m.setScale(rx, ry);
        m.postTranslate(cx, cy);
        RadialGradient g = new RadialGradient(0, 0, 1, new int[]{0xB0000000, 0x60000000, Color.TRANSPARENT},
                new float[]{0f, 0.65f, 1f}, Shader.TileMode.CLAMP);
        g.setLocalMatrix(m);
        p.setShader(g);
        c.drawOval(new RectF(cx - rx, cy - ry, cx + rx, cy + ry), p);
        c.drawBitmap(car, 0, 0, null);
        car.recycle();
        return out;
    }

    /** Imports the user's own photo: scales it once, trims transparent edges, keeps a private copy. */
    static boolean importPhoto(Context ctx, String path) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, o);
            if (o.outWidth <= 0) return false;
            o.inJustDecodeBounds = false;
            o.inSampleSize = 1;
            while (o.outWidth / (o.inSampleSize * 2) >= BAKE_WIDTH) o.inSampleSize *= 2;
            Bitmap b = BitmapFactory.decodeFile(path, o);
            if (b == null) return false;
            Bitmap t = trim(b);
            int h = Math.round(t.getHeight() * (float) BAKE_WIDTH / t.getWidth());
            Bitmap s = Bitmap.createScaledBitmap(t, BAKE_WIDTH, h, true);
            try (FileOutputStream os = new FileOutputStream(customFile(ctx))) {
                s.compress(Bitmap.CompressFormat.PNG, 100, os);
            }
            return true;
        } catch (IOException | RuntimeException | OutOfMemoryError e) {
            DebugLog.log("import photo error: " + e);
            return false;
        }
    }

    private static Bitmap trim(Bitmap b) {
        if (!b.hasAlpha()) return b;
        int w = b.getWidth(), h = b.getHeight(), l = w, r = -1, t = h, u = -1;
        int[] row = new int[w];
        for (int y = 0; y < h; y++) {
            b.getPixels(row, 0, w, 0, y, w, 1);
            for (int x = 0; x < w; x++) {
                if ((row[x] >>> 24) > 16) {
                    if (x < l) l = x;
                    if (x > r) r = x;
                    if (y < t) t = y;
                    u = y;
                }
            }
        }
        if (r < l || u < t) return b;
        return Bitmap.createBitmap(b, l, t, r - l + 1, u - t + 1);
    }
}
