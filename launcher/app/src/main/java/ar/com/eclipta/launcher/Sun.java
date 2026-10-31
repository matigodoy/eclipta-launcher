package ar.com.eclipta.launcher;

/** Sunrise and sunset, offline (NOAA's simplified solar position). Good to a few minutes. */
final class Sun {
    private Sun() {}

    /** Is the sun below the civil horizon at this UTC instant and place? */
    static boolean isDark(long utcMs, double lat, double lon) {
        double days = utcMs / 86_400_000.0 - 10957.5; // since J2000
        double meanLong = Math.toRadians((280.46 + 0.9856474 * days) % 360);
        double anomaly = Math.toRadians((357.528 + 0.9856003 * days) % 360);
        double eclLong = meanLong + Math.toRadians(1.915) * Math.sin(anomaly)
                + Math.toRadians(0.020) * Math.sin(2 * anomaly);
        double obliquity = Math.toRadians(23.439 - 0.0000004 * days);
        double decl = Math.asin(Math.sin(obliquity) * Math.sin(eclLong));
        double ra = Math.atan2(Math.cos(obliquity) * Math.sin(eclLong), Math.cos(eclLong));
        double gmst = Math.toRadians((280.46061837 + 360.98564736629 * days) % 360);
        double hourAngle = gmst + Math.toRadians(lon) - ra;
        double phi = Math.toRadians(lat);
        double altitude = Math.asin(Math.sin(phi) * Math.sin(decl) + Math.cos(phi) * Math.cos(decl) * Math.cos(hourAngle));
        return Math.toDegrees(altitude) < -0.83;
    }
}
