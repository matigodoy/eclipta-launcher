package com.mati.a3launcher;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.WindowManager;

/**
 * The report owners attach to issues. Only hardware and software identity: never a serial,
 * MAC, IMEI, network name, position or anything from the user's profile.
 */
final class Diagnostics {
    private static final String[] PACKAGES = {JanCar.PKG, "com.jancar.radio", "com.jancar.music", "com.can.activity",
            "com.autochips.carplayapp", "com.google.android.projection.sink"};

    private Diagnostics() {}

    static String report(Context c) {
        StringBuilder b = new StringBuilder();
        line(b, "launcher", BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");
        line(b, "android", Build.VERSION.RELEASE + " / SDK " + Build.VERSION.SDK_INT);
        line(b, "manufacturer", Build.MANUFACTURER);
        line(b, "brand", Build.BRAND);
        line(b, "model", Build.MODEL);
        line(b, "board", Build.BOARD);
        line(b, "hardware", Build.HARDWARE);
        line(b, "firmware", Build.DISPLAY);
        line(b, "abi", Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "?");
        DisplayMetrics m = new DisplayMetrics();
        ((WindowManager) c.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(m);
        line(b, "screen", m.widthPixels + "x" + m.heightPixels + " @" + m.densityDpi + "dpi");
        PackageManager pm = c.getPackageManager();
        for (String pkg : PACKAGES) {
            String v = "no";
            try {
                PackageInfo i = pm.getPackageInfo(pkg, 0);
                v = i.versionName;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
            line(b, pkg, v);
        }
        return b.toString();
    }

    private static void line(StringBuilder b, String k, String v) {
        b.append(k).append(": ").append(v).append('\n');
    }
}
