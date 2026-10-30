package com.mati.a3launcher;

import android.content.Context;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Tiny append-only debug log in getExternalFilesDir(null)/a3l-log.txt.
 * Single background writer thread, at most MAX_LINES per process, file truncated above MAX_BYTES.
 * Set ENABLED = false for v1.
 */
final class DebugLog {
    static final boolean ENABLED = true;
    private static final int MAX_LINES = 200;
    private static final long MAX_BYTES = 500 * 1024;

    private static File file;
    private static ExecutorService writer;
    private static int lines;

    private DebugLog() {}

    static synchronized void init(Context ctx) {
        if (!ENABLED || file != null) return;
        File dir = ctx.getApplicationContext().getExternalFilesDir(null);
        if (dir == null) return;
        file = new File(dir, "a3l-log.txt");
        writer = Executors.newSingleThreadExecutor();
    }

    static synchronized void log(String msg) {
        if (!ENABLED || file == null || lines >= MAX_LINES) return;
        lines++;
        final String line = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date())
                + "  " + msg + (lines == MAX_LINES ? "  [log cap reached]" : "") + "\n";
        final File f = file;
        writer.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean append = f.length() < MAX_BYTES;
                    FileWriter w = new FileWriter(f, append);
                    try {
                        w.write(line);
                    } finally {
                        w.close();
                    }
                } catch (Throwable ignored) {
                }
            }
        });
    }
}
