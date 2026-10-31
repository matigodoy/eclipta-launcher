package ar.com.eclipta.launcher;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The rows settings are made of. Every option says what it does. Text is typed in a dialog: inline, the keyboard would cover it. */
final class SettingsForm {
    interface OnChoice {
        void chosen(String id);
    }

    interface OnToggle {
        void toggled(boolean on);
    }

    private final Context c;
    private final float dp;
    private LinearLayout target;

    SettingsForm(Context c) {
        this.c = c;
        dp = c.getResources().getDisplayMetrics().density;
    }

    void into(LinearLayout target) {
        this.target = target;
    }

    int px(int v) {
        return Math.round(v * dp);
    }

    // ---------------------------------------------------------------- page structure

    /** Two columns under a single scroll, or with one scroll each when separate is true. */
    LinearLayout[] columns(FrameLayout content, boolean separate, float leftWeight, float rightWeight) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout left = column(), right = column();
        LinearLayout.LayoutParams lpLeft = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, leftWeight);
        LinearLayout.LayoutParams lpRight = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, rightWeight);
        lpRight.leftMargin = px(24);
        if (separate) {
            lpLeft.height = LinearLayout.LayoutParams.MATCH_PARENT;
            lpRight.height = LinearLayout.LayoutParams.MATCH_PARENT;
            row.addView(scroll(left), lpLeft);
            row.addView(scroll(right), lpRight);
            content.addView(row);
        } else {
            row.addView(left, lpLeft);
            row.addView(right, lpRight);
            ScrollView s = scroll(row);
            content.addView(s);
        }
        return new LinearLayout[]{left, right};
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(0, 0, 0, px(16));
        return l;
    }

    private ScrollView scroll(View child) {
        ScrollView s = new ScrollView(c);
        s.setVerticalScrollBarEnabled(false);
        s.setFadingEdgeLength(px(16));
        s.setVerticalFadingEdgeEnabled(true);
        s.addView(child);
        return s;
    }

    void group(int title) {
        group(c.getString(title));
    }

    void group(String title) {
        TextView t = new TextView(c);
        t.setText(title.toUpperCase(Locale.getDefault()));
        t.setTextAppearance(R.style.Label);
        t.setPadding(0, target.getChildCount() == 0 ? px(4) : px(20), 0, px(4));
        target.addView(t);
    }

    void hint(int text) {
        hint(c.getString(text));
    }

    void hint(String text) {
        target.addView(hintView(text));
    }

    private TextView hintView(String text) {
        TextView t = text(text, 13, R.attr.uiText2);
        t.setPadding(0, px(4), 0, px(4));
        return t;
    }

    /** A view of the page's own. */
    void add(View v) {
        target.addView(v);
    }

    // ---------------------------------------------------------------- chips

    TextView chip(String text, boolean selected, View.OnClickListener click) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextColor(c.getColorStateList(R.color.chip_text));
        t.setTextSize(15);
        t.setGravity(Gravity.CENTER);
        t.setBackgroundResource(R.drawable.chip_bg);
        t.setSelected(selected);
        t.setPadding(px(16), 0, px(16), 0);
        t.setOnClickListener(click);
        t.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, px(44)));
        return t;
    }

    FlowLayout chipFlow() {
        FlowLayout flow = new FlowLayout(c, px(8), px(8));
        target.addView(flow, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return flow;
    }

    void chips(int title, int hint, List<String> ids, List<String> names, String selected, OnChoice cb) {
        chips(title, hint, ids, names, null, selected, cb);
    }

    /** colors: an optional dot for each chip, 0 for none. */
    void chips(int title, int hint, List<String> ids, List<String> names, int[] colors, String selected, final OnChoice cb) {
        if (ids.isEmpty()) return;
        label(title);
        final FlowLayout flow = chipFlow();
        for (int i = 0; i < ids.size(); i++) {
            final String id = ids.get(i);
            TextView chip = chip(names.get(i), id.equals(selected), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for (int k = 0; k < flow.getChildCount(); k++) flow.getChildAt(k).setSelected(false);
                    v.setSelected(true);
                    cb.chosen(id);
                }
            });
            if (colors != null && colors[i] != 0) chip.setCompoundDrawables(dot(colors[i]), null, null, null);
            chip.setCompoundDrawablePadding(px(8));
            flow.addView(chip);
        }
        if (hint != 0) hint(hint);
    }

    private Drawable dot(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        d.setStroke(px(1), 0x66FFFFFF);
        d.setBounds(0, 0, px(14), px(14));
        return d;
    }

    void label(int text) {
        TextView t = text(c.getString(text), 15, R.attr.uiText);
        t.setPadding(0, px(10), 0, px(8));
        target.addView(t);
    }

    // ---------------------------------------------------------------- rows

    private LinearLayout row(String title, String desc) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(px(56));
        row.setPadding(0, px(6), 0, px(6));
        LinearLayout texts = new LinearLayout(c);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(text(title, 16, R.attr.uiText));
        if (desc != null) texts.addView(text(desc, 13, R.attr.uiText2));
        row.addView(texts, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        target.addView(row);
        return row;
    }

    View switchRow(int title, String desc, boolean value, final OnToggle cb) {
        LinearLayout row = row(c.getString(title), desc);
        final Switch sw = new Switch(c);
        sw.setChecked(value);
        sw.setClickable(false);
        sw.setFocusable(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = px(12);
        row.addView(sw, lp);
        row.setBackgroundResource(R.drawable.row_bg);
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sw.toggle();
                cb.toggled(sw.isChecked());
            }
        });
        return row;
    }

    /** shown is what the row displays, value what the dialog starts with. */
    void textRow(final int title, String desc, String shown, final String value, final int hint,
                 final int inputType, final int maxLen, final OnChoice cb) {
        LinearLayout row = row(c.getString(title), desc);
        TextView v = text(shown, 15, R.attr.uiText2);
        v.setSingleLine(true);
        v.setEllipsize(android.text.TextUtils.TruncateAt.END);
        v.setMaxWidth(px(170));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = px(12);
        row.addView(v, lp);
        ImageView chevron = new ImageView(c);
        chevron.setImageResource(R.drawable.ic_chevron);
        row.addView(chevron);
        row.setBackgroundResource(R.drawable.row_bg);
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                askText(c.getString(title), value, hint, inputType, maxLen, cb);
            }
        });
    }

    View actionRow(int title, int desc, int button, final Runnable action) {
        return actionRow(c.getString(title), desc == 0 ? null : c.getString(desc), c.getString(button), action);
    }

    View actionRow(String title, String desc, String button, final Runnable action) {
        LinearLayout row = row(title, desc);
        TextView chip = chip(button, false, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action.run();
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, px(44));
        lp.leftMargin = px(12);
        row.addView(chip, lp);
        return row;
    }

    void askText(String title, String value, int hint, int inputType, int maxLen, final OnChoice cb) {
        final EditText e = new EditText(c);
        e.setText(value);
        e.setSelection(e.getText().length());
        e.setSingleLine(true);
        e.setInputType(inputType);
        e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLen)});
        e.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_ACTION_DONE);
        e.setTextColor(Theming.color(c, R.attr.uiText));
        e.setHintTextColor(Theming.color(c, R.attr.uiText2));
        e.setTextSize(18);
        e.setBackgroundResource(R.drawable.field_bg);
        e.setPadding(px(14), 0, px(14), 0);
        if (hint != 0) e.setHint(hint);
        FrameLayout box = new FrameLayout(c);
        box.setPadding(px(22), px(8), px(22), 0);
        box.addView(e, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, px(52)));
        final AlertDialog d = new AlertDialog.Builder(c)
                .setTitle(title)
                .setView(box)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> cb.chosen(e.getText().toString().trim()))
                .create();
        d.setCanceledOnTouchOutside(false);
        d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        e.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) return false;
            d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            return true;
        });
        d.show();
    }

    TextView text(String s, int sp, int colorAttr) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(Theming.color(c, colorAttr));
        return t;
    }

    static List<String> list(String... a) {
        List<String> out = new ArrayList<>();
        for (String s : a) out.add(s);
        return out;
    }

    List<String> strings(int... ids) {
        List<String> out = new ArrayList<>();
        for (int id : ids) out.add(c.getString(id));
        return out;
    }
}
