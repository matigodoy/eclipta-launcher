package ar.com.eclipta.launcher;

/** Density that makes a designW x designH dp layout fit the panel. Pure math, unit tested. */
final class ScreenFit {
    private ScreenFit() {}

    /** Returns current when it is already within tolerance, so the reference unit is untouched. */
    static float density(int widthPx, int heightPx, float designW, float designH, float current, float tolerance) {
        float fit = Math.min(widthPx / designW, heightPx / designH);
        return Math.abs(fit - current) / current <= tolerance ? current : fit;
    }
}
