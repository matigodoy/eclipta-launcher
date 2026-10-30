package com.mati.a3launcher;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * Static window background: flat #060708 plus three soft elliptical glows.
 * Each glow only paints its own oval, so the cost is one fill plus three small ovals.
 */
final class GlowBackground extends Drawable {
    private static final int BG = 0xFF060708;

    private final float density;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final Matrix matrix = new Matrix();
    private final RadialGradient[] shaders = new RadialGradient[3];

    GlowBackground(float density) {
        this.density = density;
        // Unit-radius gradients, scaled into ellipses at draw time.
        shaders[0] = gradient(0xE3263A, 0.30f);
        shaders[1] = gradient(0xE3263A, 0.20f);
        shaders[2] = gradient(0x8A93A3, 0.12f);
    }

    private static RadialGradient gradient(int rgb, float alpha) {
        int a = Math.round(alpha * 255);
        return new RadialGradient(0, 0, 1,
                new int[]{(a << 24) | rgb, ((a / 2) << 24) | rgb, rgb},
                new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP);
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        canvas.drawColor(BG);
        float d = density;
        glow(canvas, 0, b.left + 340 * d, b.top + 340 * d, 280 * d, 200 * d);
        glow(canvas, 1, b.right, b.bottom, 200 * d, 160 * d);
        glow(canvas, 2, b.left + 760 * d, b.top, 160 * d, 120 * d);
    }

    private void glow(Canvas c, int i, float cx, float cy, float rx, float ry) {
        matrix.setScale(rx, ry);
        matrix.postTranslate(cx, cy);
        shaders[i].setLocalMatrix(matrix);
        paint.setShader(shaders[i]);
        oval.set(cx - rx, cy - ry, cx + rx, cy + ry);
        c.drawOval(oval, paint);
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}
