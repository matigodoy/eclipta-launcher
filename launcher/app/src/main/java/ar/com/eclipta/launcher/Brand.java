package ar.com.eclipta.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A pack's brand: header logo and wake animation. None ships in the APK: files come from an
 * imported pack's folder or an imported brand folder (files/brands/<key>/), and without them the
 * app shows its own mark.
 */
final class Brand {
    private static final String LOGO = "logo.webp", STING = "sting.webp";
    private static final int MAX_LOGO_WIDTH = 184;

    private Brand() {}

    /** The caller tints it. */
    static Drawable logo(Context c, Pack pack, Profile p) {
        if (!p.customCar()) {
            try {
                Bitmap b = decodeLogo(c, pack);
                if (b != null) return new BitmapDrawable(c.getResources(), b);
            } catch (Throwable t) {
                DebugLog.log("logo error: " + t);
            }
        }
        return c.getDrawable(R.drawable.ic_eclipta);
    }

    /** Null when there is no animation to play. */
    static ImageDecoder.Source sting(Context c, Pack pack, Profile p) {
        if (p.customCar()) return null;
        File f = find(c, pack, STING);
        return f == null ? null : ImageDecoder.createSource(f);
    }

    private static Bitmap decodeLogo(Context c, Pack pack) throws IOException {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        try (InputStream in = openLogo(c, pack)) {
            if (in == null) return null;
            BitmapFactory.decodeStream(in, null, o);
        }
        o.inJustDecodeBounds = false;
        o.inSampleSize = 1;
        while (o.outWidth / o.inSampleSize > MAX_LOGO_WIDTH) o.inSampleSize *= 2;
        try (InputStream in = openLogo(c, pack)) {
            return BitmapFactory.decodeStream(in, null, o);
        }
    }

    private static InputStream openLogo(Context c, Pack pack) throws IOException {
        File f = find(c, pack, LOGO);
        return f == null ? null : new FileInputStream(f);
    }

    /** The pack's own file first, then the one its brand shares with the other packs. */
    private static File find(Context c, Pack pack, String name) {
        File local = pack.localFile(name);
        if (local != null && local.isFile()) return local;
        String key = pack.brandKey();
        if (key.isEmpty()) return null;
        File shared = new File(new File(Files.brandsDir(c), key), name);
        return shared.isFile() ? shared : null;
    }
}
