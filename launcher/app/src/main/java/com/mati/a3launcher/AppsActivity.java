package com.mati.a3launcher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** App drawer: every launchable app, sorted by label, 6 columns. */
public class AppsActivity extends Activity {
    /** With this boolean extra the drawer returns the tapped app (as a flattened component). */
    static final String EXTRA_PICK = "pick";

    private static final class App {
        final String label;
        final ComponentName component;
        final Drawable icon;

        App(String label, ComponentName component, Drawable icon) {
            this.label = label;
            this.component = component;
            this.icon = icon;
        }
    }

    private final List<App> apps = new ArrayList<>();
    private Adapter adapter;
    private volatile boolean destroyed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawable(new GlowBackground(getResources().getDisplayMetrics().density));
        setContentView(R.layout.activity_apps);
        findViewById(R.id.btnClose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        GridView grid = findViewById(R.id.grid);
        adapter = new Adapter();
        grid.setAdapter(adapter);
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                App app = apps.get(position);
                if (getIntent().hasExtra(EXTRA_PICK)) {
                    setResult(RESULT_OK, new Intent().putExtra(EXTRA_PICK, app.component.flattenToString()));
                    finish();
                } else {
                    launch(app);
                }
            }
        });
        loadApps();
    }

    private void loadApps() {
        final PackageManager pm = getPackageManager();
        final String self = getPackageName();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> infos = pm.queryIntentActivities(main, 0);
                final List<App> list = new ArrayList<>(infos.size());
                for (ResolveInfo ri : infos) {
                    if (destroyed) return;
                    if (self.equals(ri.activityInfo.packageName)) continue;
                    CharSequence label = ri.loadLabel(pm);
                    list.add(new App(label == null ? ri.activityInfo.packageName : label.toString(),
                            new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name),
                            ri.loadIcon(pm)));
                }
                final Collator collator = Collator.getInstance();
                Collections.sort(list, new Comparator<App>() {
                    @Override
                    public int compare(App a, App b) {
                        return collator.compare(a.label, b.label);
                    }
                });
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (destroyed) return;
                        apps.clear();
                        apps.addAll(list);
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        }, "a3l-apps").start();
    }

    private void launch(App app) {
        try {
            startActivity(new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(app.component)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
            finish();
        } catch (Throwable t) {
            Toast.makeText(this, R.string.cannot_open, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        apps.clear();
        super.onDestroy();
    }

    private final class Adapter extends BaseAdapter {
        @Override
        public int getCount() {
            return apps.size();
        }

        @Override
        public Object getItem(int position) {
            return apps.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView != null ? convertView
                    : LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
            App app = apps.get(position);
            ((ImageView) v.findViewById(R.id.icon)).setImageDrawable(app.icon);
            ((TextView) v.findViewById(R.id.label)).setText(app.label);
            return v;
        }
    }
}
