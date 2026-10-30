package com.mati.a3launcher;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** HOME screen. All bindings, listeners and timers live only between onStart and onStop. */
public class MainActivity extends Activity implements JanCar.MediaListener, JanCar.GpsListener {
    private static final Locale ES = new Locale("es", "AR");
    private static final long GPS_STALE_MS = 10_000;
    private static final long GEOCODE_INTERVAL_MS = 10 * 60_000;
    private static final long GPS_LOG_INTERVAL_MS = 30_000;
    private static final int MEDIA_LOG_LIMIT = 20;
    private static final int REQ_LOCATION = 1;

    private static final String PKG_CARPLAY = "com.autochips.carplayapp";
    private static final String PKG_AA = "com.google.android.projection.sink";
    private static final String PKG_RADIO = "com.jancar.radio";
    private static final String PKG_MUSIC = "com.jancar.music";
    private static final String PKG_SETTINGS = "com.jancar.settings";

    private final Handler main = new Handler(Looper.getMainLooper());
    private final JanCar.MediaLink mediaLink = new JanCar.MediaLink(this);
    private final JanCar.SystemLink systemLink = new JanCar.SystemLink(this);
    private final AccReceiver accReceiver = new AccReceiver();
    private SharedPreferences prefs;
    private AudioManager audio;
    private LocationManager locationManager;
    private boolean started;
    private float density;

    // views
    private TextView time, greeting, date, city, speed, noGps, title, artist, posText, durText;
    private TextView radioFreq, radioUnit, radioBand, radioLabel;
    private View locationPill, artNote;
    private ImageView art;
    private ImageButton btnPlay;
    private ProgressBar speedBar, mediaBar;

    // speed / gps (main thread)
    private int shownSpeed = -1;
    private long lastFixAt;
    private boolean noGpsShown;
    private double lastLat, lastLon;
    private boolean haveLatLon;
    private long lastGeocodeAt;
    private volatile boolean geocoding;
    private long lastGpsLogAt;

    // media (main thread)
    private int mediaType = -1;
    private String shownTitle, shownArtist;
    private boolean playing;
    private int position, duration;
    private long positionAt;
    private Bitmap artBitmap;
    private int mediaLogCount; // binder threads, approximate
    private String lastMediaLogKey, lastStateLogKey;

    // radio (main thread)
    private String radioBandValue, radioFreqValue;

    private final BroadcastReceiver clockReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateClock();
        }
    };

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            tick();
            main.postDelayed(this, 1000);
        }
    };

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location l) {
            onSpeed(l.hasSpeed() ? l.getSpeed() * 3.6f : 0f, true);
            setLatLon(l.getLatitude(), l.getLongitude());
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DebugLog.init(this);
        DebugLog.log("MainActivity onCreate");
        density = getResources().getDisplayMetrics().density;
        getWindow().setBackgroundDrawable(new GlowBackground(density));
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences("a3l", MODE_PRIVATE);
        audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        time = findViewById(R.id.time);
        greeting = findViewById(R.id.greeting);
        date = findViewById(R.id.date);
        city = findViewById(R.id.city);
        locationPill = findViewById(R.id.locationPill);
        speed = findViewById(R.id.speed);
        speedBar = findViewById(R.id.speedBar);
        noGps = findViewById(R.id.noGps);
        title = findViewById(R.id.title);
        artist = findViewById(R.id.artist);
        art = findViewById(R.id.art);
        artNote = findViewById(R.id.artNote);
        mediaBar = findViewById(R.id.mediaBar);
        posText = findViewById(R.id.posText);
        durText = findViewById(R.id.durText);
        btnPlay = findViewById(R.id.btnPlay);
        radioFreq = findViewById(R.id.radioFreq);
        radioUnit = findViewById(R.id.radioUnit);
        radioBand = findViewById(R.id.radioBand);
        radioLabel = findViewById(R.id.radioLabel);

        setupRail();
        setupMedia();

        String cachedCity = prefs.getString("city", null);
        if (!TextUtils.isEmpty(cachedCity)) showCity(cachedCity);
        showRadio(prefs.getString("radio_band", null), prefs.getString("radio_freq", null));
        showNothingPlaying();

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
        }
    }

    // ---------------------------------------------------------------- lifecycle

    @Override
    protected void onStart() {
        super.onStart();
        started = true;
        updateClock();
        registerReceiver(clockReceiver, timeFilter());
        try {
            IntentFilter wake = new IntentFilter(JanCar.ACTION_ACC);
            wake.addAction(JanCar.ACTION_QB_POWERON);
            registerReceiver(accReceiver, wake);
        } catch (Throwable ignored) {
        }
        mediaLink.bind(this);
        systemLink.bind(this);
        startLocation();
        lastFixAt = SystemClock.elapsedRealtime();
        main.post(ticker);
    }

    @Override
    protected void onStop() {
        started = false;
        main.removeCallbacks(ticker);
        stopLocation();
        mediaLink.unbind(this);
        systemLink.unbind(this);
        try {
            unregisterReceiver(clockReceiver);
        } catch (Throwable ignored) {
        }
        try {
            unregisterReceiver(accReceiver);
        } catch (Throwable ignored) {
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        main.removeCallbacksAndMessages(null);
        art.setImageDrawable(null);
        if (artBitmap != null) {
            artBitmap.recycle();
            artBitmap = null;
        }
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    @Override
    public void onBackPressed() {
        // Home screen: back does nothing.
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQ_LOCATION && started) startLocation();
    }

    private static IntentFilter timeFilter() {
        IntentFilter f = new IntentFilter(Intent.ACTION_TIME_TICK);
        f.addAction(Intent.ACTION_TIME_CHANGED);
        f.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        return f;
    }

    // ---------------------------------------------------------------- header

    private void updateClock() {
        Date now = new Date();
        time.setText(new SimpleDateFormat("HH:mm", ES).format(now));
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String g = h >= 6 && h < 13 ? "Buen día" : h >= 13 && h < 20 ? "Buenas tardes" : "Buenas noches";
        String name = getString(R.string.greeting_name);
        greeting.setText(name.isEmpty() ? g : g + ", " + name);
        String d = new SimpleDateFormat("EEEE d 'de' MMMM", ES).format(now);
        date.setText(d.isEmpty() ? d : Character.toUpperCase(d.charAt(0)) + d.substring(1));
    }

    private void showCity(String name) {
        city.setText(name);
        locationPill.setVisibility(View.VISIBLE);
    }

    private void setLatLon(double lat, double lon) {
        if (lat == 0 && lon == 0) return;
        lastLat = lat;
        lastLon = lon;
        haveLatLon = true;
        maybeGeocode();
    }

    private void maybeGeocode() {
        long now = SystemClock.elapsedRealtime();
        if (!started || !haveLatLon || geocoding || !Geocoder.isPresent()) return;
        if (lastGeocodeAt != 0 && now - lastGeocodeAt < GEOCODE_INTERVAL_MS) return;
        lastGeocodeAt = now;
        geocoding = true;
        final double lat = lastLat, lon = lastLon;
        final Context app = getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String name = null;
                try {
                    List<Address> list = new Geocoder(app, ES).getFromLocation(lat, lon, 1);
                    if (list != null && !list.isEmpty()) {
                        Address a = list.get(0);
                        name = a.getLocality();
                        if (TextUtils.isEmpty(name)) name = a.getSubAdminArea();
                        if (TextUtils.isEmpty(name)) name = a.getAdminArea();
                    }
                } catch (Throwable t) {
                    DebugLog.log("geocoder error: " + t);
                }
                final String result = name;
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        geocoding = false;
                        if (!TextUtils.isEmpty(result)) {
                            prefs.edit().putString("city", result).apply();
                            showCity(result);
                        }
                    }
                });
            }
        }, "a3l-geocoder").start();
    }

    // ---------------------------------------------------------------- rail

    private void setupRail() {
        ImageButton carplay = findViewById(R.id.btnCarPlay);
        ImageButton aa = findViewById(R.id.btnAndroidAuto);
        setAppIcon(carplay, PKG_CARPLAY);
        setAppIcon(aa, PKG_AA);

        findViewById(R.id.btnHome).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Already home.
            }
        });
        carplay.setOnClickListener(launcher(PKG_CARPLAY, PKG_CARPLAY + ".CarPlayActivity"));
        aa.setOnClickListener(launcher(PKG_AA, PKG_AA + ".ui.AndroidAutoActivity"));
        findViewById(R.id.btnRadio).setOnClickListener(launcher(PKG_RADIO, PKG_RADIO + ".activity.RadioMainActivity"));
        findViewById(R.id.btnMusic).setOnClickListener(launcher(PKG_MUSIC, PKG_MUSIC + ".activity.MainActivity"));
        findViewById(R.id.btnSettings).setOnClickListener(launcher(PKG_SETTINGS, PKG_SETTINGS + ".ui.activity.MainActivity"));
        findViewById(R.id.btnApps).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AppsActivity.class));
            }
        });
    }

    private void setAppIcon(ImageButton b, String pkg) {
        try {
            b.setImageDrawable(getPackageManager().getApplicationIcon(pkg));
        } catch (PackageManager.NameNotFoundException e) {
            b.setImageResource(R.drawable.ic_apps);
            b.setScaleType(ImageView.ScaleType.CENTER);
        }
    }

    private View.OnClickListener launcher(final String pkg, final String cls) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launch(pkg, cls);
            }
        };
    }

    private void launch(String pkg, String cls) {
        Intent i = new Intent(Intent.ACTION_MAIN)
                .setComponent(new ComponentName(pkg, cls))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            startActivity(i);
            return;
        } catch (Throwable t) {
            DebugLog.log("launch " + cls + " failed: " + t);
        }
        try {
            Intent fallback = getPackageManager().getLaunchIntentForPackage(pkg);
            if (fallback != null) {
                startActivity(fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return;
            }
        } catch (Throwable t) {
            DebugLog.log("launch " + pkg + " fallback failed: " + t);
        }
        Toast.makeText(this, R.string.cannot_open, Toast.LENGTH_SHORT).show();
    }

    // ---------------------------------------------------------------- speed

    @Override
    public void onGps(String longitude, String latitude, float accuracy, double altitude, final float kmh) {
        // Binder thread.
        long now = SystemClock.elapsedRealtime();
        if (now - lastGpsLogAt >= GPS_LOG_INTERVAL_MS) {
            lastGpsLogAt = now;
            DebugLog.log("gps lon=" + longitude + " lat=" + latitude + " acc=" + accuracy + " alt=" + altitude + " speed=" + kmh);
        }
        final double lat = parse(latitude), lon = parse(longitude);
        // Guess: JanCar reports "0"/empty coordinates without a fix.
        final boolean fix = lat != 0 || lon != 0;
        main.post(new Runnable() {
            @Override
            public void run() {
                onSpeed(kmh, fix);
                if (fix) setLatLon(lat, lon);
            }
        });
    }

    private static double parse(String s) {
        if (s == null) return 0;
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void onSpeed(float kmh, boolean fix) {
        if (!fix) return;
        lastFixAt = SystemClock.elapsedRealtime();
        if (noGpsShown) {
            noGpsShown = false;
            noGps.setVisibility(View.INVISIBLE);
        }
        int v = Math.max(0, Math.round(kmh));
        if (v == shownSpeed) return;
        shownSpeed = v;
        speed.setText(String.valueOf(v));
        speedBar.setProgress(Math.min(200, v));
    }

    private void startLocation() {
        if (locationManager == null
                || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener, Looper.getMainLooper());
            Location last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (last != null) setLatLon(last.getLatitude(), last.getLongitude());
        } catch (Throwable t) {
            DebugLog.log("location error: " + t);
        }
    }

    private void stopLocation() {
        if (locationManager == null) return;
        try {
            locationManager.removeUpdates(locationListener);
        } catch (Throwable ignored) {
        }
    }

    // ---------------------------------------------------------------- media

    private void setupMedia() {
        findViewById(R.id.btnPrev).setOnClickListener(mediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS));
        btnPlay.setOnClickListener(mediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
        findViewById(R.id.btnNext).setOnClickListener(mediaKey(KeyEvent.KEYCODE_MEDIA_NEXT));

        View.OnClickListener openRadio = launcher(PKG_RADIO, PKG_RADIO + ".activity.RadioMainActivity");
        findViewById(R.id.radioCard).setOnClickListener(openRadio);
        findViewById(R.id.btnSeekBack).setOnClickListener(radioSeek(KeyEvent.KEYCODE_MEDIA_PREVIOUS, openRadio));
        findViewById(R.id.btnSeekFwd).setOnClickListener(radioSeek(KeyEvent.KEYCODE_MEDIA_NEXT, openRadio));
    }

    private View.OnClickListener mediaKey(final int code) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMediaKey(code);
            }
        };
    }

    private View.OnClickListener radioSeek(final int code, final View.OnClickListener openRadio) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaType == JanCar.TYPE_RADIO) sendMediaKey(code);
                else openRadio.onClick(v);
            }
        };
    }

    private void sendMediaKey(int code) {
        try {
            long t = SystemClock.uptimeMillis();
            audio.dispatchMediaKeyEvent(new KeyEvent(t, t, KeyEvent.ACTION_DOWN, code, 0));
            audio.dispatchMediaKeyEvent(new KeyEvent(t, t, KeyEvent.ACTION_UP, code, 0));
        } catch (Throwable e) {
            DebugLog.log("media key error: " + e);
        }
    }

    @Override
    public void onMediaChange(final int type, final String name, final String info, int artW, int artH,
                              byte[] artData, int index, int total, boolean popup) {
        // Binder thread: decode art here, then post a minimal update.
        // JanCar re-sends the same info every few seconds: only log changes.
        String key = type + "|" + name + "|" + info;
        if (mediaLogCount < MEDIA_LOG_LIMIT && !key.equals(lastMediaLogKey)) {
            lastMediaLogKey = key;
            mediaLogCount++;
            DebugLog.log("onMediaChange type=" + type + " name=" + name + " info=" + info
                    + " art=" + artW + "x" + artH + "/" + (artData == null ? 0 : artData.length)
                    + " index=" + index + "/" + total + " popup=" + popup);
        }
        final Bitmap bmp = (type == JanCar.TYPE_MUSIC || type == JanCar.TYPE_A2DP) ? decodeArt(artW, artH, artData) : null;
        main.post(new Runnable() {
            @Override
            public void run() {
                applyMedia(type, name, info, bmp);
            }
        });
    }

    private Bitmap decodeArt(int w, int h, byte[] data) {
        if (w <= 0 || h <= 0 || data == null || data.length < w * h * 4) return null;
        try {
            Bitmap full = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            full.copyPixelsFromBuffer(ByteBuffer.wrap(data, 0, w * h * 4));
            int size = Math.round(84 * density);
            if (w == size && h == size) return full;
            Bitmap scaled = Bitmap.createScaledBitmap(full, size, size, true);
            if (scaled != full) full.recycle();
            return scaled;
        } catch (Throwable t) {
            DebugLog.log("art decode error: " + t);
            return null;
        }
    }

    private void applyMedia(int type, String name, String info, Bitmap bmp) {
        boolean typeChanged = type != mediaType;
        mediaType = type;
        switch (type) {
            case JanCar.TYPE_MUSIC:
            case JanCar.TYPE_A2DP:
                setTexts(TextUtils.isEmpty(name) ? getString(R.string.nothing_playing) : stripExtension(name), info);
                setArt(bmp);
                break;
            case JanCar.TYPE_CARPLAY:
                setTexts(getString(R.string.carplay), getString(R.string.carplay_sub));
                setArt(null);
                break;
            case JanCar.TYPE_RADIO:
                String[] parts = name == null ? new String[0] : name.trim().split("\\s+");
                if (parts.length >= 2) {
                    showRadio(parts[0], parts[1]);
                    prefs.edit().putString("radio_band", parts[0]).putString("radio_freq", parts[1]).apply();
                }
                setTexts("Radio " + (radioFreqValue == null ? "" : radioFreqValue + " " + radioUnit.getText()),
                        radioBandValue);
                setArt(null);
                break;
            default:
                if (TextUtils.isEmpty(name)) showNothingPlaying();
                else {
                    setTexts(name, info);
                    setArt(null);
                }
                break;
        }
        if (typeChanged) {
            position = duration = 0;
            positionAt = SystemClock.elapsedRealtime();
            updateProgress();
        }
    }

    /** Local music without tags arrives as the file name: "Song_name.mp3" -> "Song name". */
    private static String stripExtension(String name) {
        String n = name.replaceFirst("(?i)\\.(mp3|flac|wav|m4a|aac|ogg|wma|ape|opus)$", "");
        return n.equals(name) ? name : n.replace('_', ' ').trim();
    }

    private void showNothingPlaying() {
        setTexts(getString(R.string.nothing_playing), null);
        setArt(null);
    }

    private void setTexts(String t, String a) {
        if (!TextUtils.equals(t, shownTitle)) {
            shownTitle = t;
            title.setText(t);
        }
        if (!TextUtils.equals(a, shownArtist)) {
            shownArtist = a;
            artist.setText(a);
        }
    }

    private void setArt(Bitmap bmp) {
        Bitmap old = artBitmap;
        if (bmp == null && old == null) return;
        artBitmap = bmp;
        art.setImageBitmap(bmp);
        art.setVisibility(bmp == null ? View.GONE : View.VISIBLE);
        artNote.setVisibility(bmp == null ? View.VISIBLE : View.GONE);
        if (old != null && old != bmp) old.recycle();
    }

    @Override
    public void onPlayStateChange(final int type, final int state, final int pos, final int dur) {
        String key = type + "|" + state;
        if (mediaLogCount < MEDIA_LOG_LIMIT && !key.equals(lastStateLogKey)) {
            lastStateLogKey = key;
            mediaLogCount++;
            DebugLog.log("onPlayStateChange type=" + type + " state=" + state + " pos=" + pos + " dur=" + dur);
        }
        main.post(new Runnable() {
            @Override
            public void run() {
                boolean p = state == JanCar.STATE_PLAYING;
                if (p != playing) {
                    playing = p;
                    btnPlay.setImageResource(p ? R.drawable.ic_pause : R.drawable.ic_play);
                }
                position = Math.max(0, pos);
                duration = Math.max(0, dur);
                positionAt = SystemClock.elapsedRealtime();
                updateProgress();
            }
        });
    }

    private int lastProgress = -1;
    private String lastPosText, lastDurText;

    private void updateProgress() {
        int pos = position;
        if (playing && duration > 0) {
            pos = (int) Math.min(duration, position + (SystemClock.elapsedRealtime() - positionAt));
        }
        int p = duration > 0 ? (int) (pos * 1000L / duration) : 0;
        if (p != lastProgress) {
            lastProgress = p;
            mediaBar.setProgress(p);
        }
        String ps = duration > 0 ? fmt(pos) : "";
        String ds = duration > 0 ? fmt(duration) : "";
        if (!ps.equals(lastPosText)) {
            lastPosText = ps;
            posText.setText(ps);
        }
        if (!ds.equals(lastDurText)) {
            lastDurText = ds;
            durText.setText(ds);
        }
    }

    private static String fmt(int ms) {
        int s = ms / 1000;
        return String.format(Locale.US, "%d:%02d", s / 60, s % 60);
    }

    // ---------------------------------------------------------------- radio

    private void showRadio(String band, String freq) {
        if (TextUtils.isEmpty(band) || TextUtils.isEmpty(freq)) return;
        radioBandValue = band;
        radioFreqValue = freq;
        boolean fm = "FM".equalsIgnoreCase(band) || band.toUpperCase(Locale.US).startsWith("FM");
        radioFreq.setText(freq);
        radioUnit.setText(fm ? "MHz" : "kHz");
        radioBand.setText(band);
        radioLabel.setText(fm ? "RADIO FM" : "RADIO " + band.toUpperCase(Locale.US));
    }

    // ---------------------------------------------------------------- ticker

    private void tick() {
        if (playing) updateProgress();
        boolean stale = SystemClock.elapsedRealtime() - lastFixAt > GPS_STALE_MS;
        if (stale != noGpsShown) {
            noGpsShown = stale;
            noGps.setVisibility(stale ? View.VISIBLE : View.INVISIBLE);
        }
    }
}
