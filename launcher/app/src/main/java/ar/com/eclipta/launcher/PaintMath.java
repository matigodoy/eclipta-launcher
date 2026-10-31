package ar.com.eclipta.launcher;

/** Per-pixel body recolor. Plain math with no Android types, so it can be unit tested. */
final class PaintMath {
    private final float r, g, b;
    private final int[] curve = new int[256]; // luminance -> shade * 1024
    private final int[] highlight = new int[256]; // luminance -> added white, 0-255

    PaintMath(int color, float gain, float gamma, float spec) {
        r = (color >> 16) & 0xFF;
        g = (color >> 8) & 0xFF;
        b = color & 0xFF;
        for (int i = 0; i < 256; i++) {
            float l = Math.min(1f, i / 255f * gain);
            curve[i] = Math.round((float) Math.pow(l, gamma) * 1024);
            highlight[i] = Math.round((float) Math.pow(l, 7) * spec * 255);
        }
    }

    /** amount: mask value, 0 keeps the pixel and 255 fully repaints it. Alpha is preserved. */
    int apply(int argb, int amount) {
        int pr = (argb >> 16) & 0xFF, pg = (argb >> 8) & 0xFF, pb = argb & 0xFF;
        int l = (pr * 77 + pg * 150 + pb * 29) >> 8;
        int shade = curve[l], w = highlight[l];
        int nr = clamp((int) (r * shade) / 1024 + w);
        int ng = clamp((int) (g * shade) / 1024 + w);
        int nb = clamp((int) (b * shade) / 1024 + w);
        int inv = 255 - amount;
        return (argb & 0xFF000000)
                | (((pr * inv + nr * amount) / 255) << 16)
                | (((pg * inv + ng * amount) / 255) << 8)
                | ((pb * inv + nb * amount) / 255);
    }

    private static int clamp(int v) {
        return v > 255 ? 255 : v;
    }
}
