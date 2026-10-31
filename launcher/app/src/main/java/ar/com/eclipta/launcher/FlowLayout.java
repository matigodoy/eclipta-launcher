package ar.com.eclipta.launcher;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/** Children left to right, wrapping to the next line: every option stays in sight. */
final class FlowLayout extends ViewGroup {
    private final int gapX, gapY;

    FlowLayout(Context c, int gapX, int gapY) {
        super(c);
        this.gapX = gapX;
        this.gapY = gapY;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int max = MeasureSpec.getSize(widthSpec);
        int x = 0, y = 0, line = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if (v.getVisibility() == GONE) continue;
            measureChild(v, widthSpec, heightSpec);
            if (x > 0 && x + v.getMeasuredWidth() > max) {
                x = 0;
                y += line + gapY;
                line = 0;
            }
            x += v.getMeasuredWidth() + gapX;
            line = Math.max(line, v.getMeasuredHeight());
        }
        setMeasuredDimension(max, y + line);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int max = r - l;
        int x = 0, y = 0, line = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if (v.getVisibility() == GONE) continue;
            if (x > 0 && x + v.getMeasuredWidth() > max) {
                x = 0;
                y += line + gapY;
                line = 0;
            }
            v.layout(x, y, x + v.getMeasuredWidth(), y + v.getMeasuredHeight());
            x += v.getMeasuredWidth() + gapX;
            line = Math.max(line, v.getMeasuredHeight());
        }
    }
}
