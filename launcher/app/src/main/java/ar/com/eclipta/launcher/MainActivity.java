package ar.com.eclipta.launcher;

import android.Manifest;
import android.content.BroadcastReceiver;
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
import android.os.PowerManager;
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
public class MainActivity extends BaseActivity implements JanCar.MediaListener, JanCar.GpsListener {
    static final String EXTRA_OPEN_MAINTENANCE = "open_maintenance";
    // JanCar restarts Home during power-off and just before the wake broadcast: let those pass first.
    private static final long ALERT_CHECK_DELAY_MS = 3000;
    private static final long OPEN_DELAY_MS = 800;
    private static final long JANCAR_GPS_GRACE_MS = 15_000;
    private static final long TICK_PLAYING_MS = 1000, TICK_IDLE_MS = 5000;
    private static final int WIDE_DP = 1180;
    private static final long GPS_STALE_MS = 10_000;
    private static final long GEOCODE_INTERVAL_MS = 10 * 60_000;
    private static final long GPS_LOG_INTERVAL_MS = 30_000;
    private static final int MEDIA_LOG_LIMIT = 20;
    private static final int REQ_LOCATION = 1;

    private static final int REQ_PICK_SLOT = 2;
    private static final int REQ_CUSTOMIZE = 3;
    private static final int MOVING_KMH = 10;
    private static final int[] SLOT_IDS = {R.id.slot0, R.id.slot1, R.id.slot2, R.id.slot3};

    private final Handler main = new Handler(Looper.getMainLooper());
    private final JanCar.MediaLink mediaLink = new JanCar.MediaLink(this);
    private final JanCar.SystemLink systemLink = new JanCar.SystemLink(this);
    private final JanCar.CarLink carLink = new JanCar.CarLink(new JanCar.LightsListener() {
        @Override
        public void onHeadlights(final boolean on) {
            main.post(new Runnable() {
                @Override
                public void run() {
                    prefs.edit().putBoolean(Theming.KEY_HEADLIGHTS, on).apply();
                    checkNight();
                }
            });
        }
    });
    private final AccReceiver accReceiver = new AccReceiver();
    private SharedPreferences prefs;
    private Profile profile;
    private Bitmap carBitmap;
    private AudioManager audio;
    private LocationManager locationManager;
    private boolean started;
    private boolean jancar;
    private Locale locale;
    private volatile boolean jancarGpsSeen;
    private boolean locationStarted;
    private long startedAt;
    private boolean trimmed;
    private float density;

    // views
    private TextView time, greeting, date, city, speed, noGps, title, artist, posText, durText;
    private TextView reminderPill;
    private TextView radioFreq, radioUnit, radioBand, radioLabel;
    private View locationPill, artNote;
    private ImageView art;
    private ImageButton btnPlay;
    private ProgressBar speedBar, mediaBar;

    // speed / gps (main thread)
    private int shownSpeed = -1;
    private boolean speedOver;
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
            if (!Intent.ACTION_TIME_TICK.equals(intent.getAction())) {
                updateReminder();
                main.removeCallbacks(alertCheck);
                main.postDelayed(alertCheck, ALERT_CHECK_DELAY_MS);
            }
        }
    };

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            tick();
            main.postDelayed(this, playing ? TICK_PLAYING_MS : TICK_IDLE_MS);
        }
    };

    private final Runnable alertCheck = new Runnable() {
        @Override
        public void run() {
            if (started && ((PowerManager) getSystemService(POWER_SERVICE)).isInteractive()) {
                Startup.fromHome(MainActivity.this, isMoving());
            }
        }
    };

    private final Runnable openMaintenance = new Runnable() {
        @Override
        public void run() {
            openCustomize(SettingsActivity.SECTION_MAINTENANCE);
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
        jancar = JanCar.available(this);
        locale = getResources().getConfiguration().getLocales().get(0);
        profile = Profile.load(this);
        boolean mmi = "mmi".equals(profile.layout);
        getWindow().setBackgroundDrawable(new GlowBackground(this, !night && !mmi));
        setContentView(mmi ? R.layout.activity_main_mmi : R.layout.activity_main);
        prefs = getSharedPreferences(Profile.PREFS, MODE_PRIVATE);
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

        reminderPill = findViewById(R.id.reminderPill);
        setupRail();
        setupMedia();
        showCar();

        String cachedCity = prefs.getString("city", null);
        if (CityName.isPlace(cachedCity)) showCity(cachedCity);
        showRadio(prefs.getString("radio_band", null), prefs.getString("radio_freq", null));
        showNothingPlaying();

        if (!jancar) {
            // Without JanCar there is no radio or now-playing feed: media controls take the whole column.
            findViewById(R.id.radioCard).setVisibility(View.GONE);
            askLocation();
        }
        fitWideScreen();
        handleOpenExtra(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleOpenExtra(intent);
    }

    /** One shot: recreate() keeps the same Intent, and Settings must not reopen by itself. */
    private void handleOpenExtra(Intent intent) {
        if (intent == null || !intent.hasExtra(EXTRA_OPEN_MAINTENANCE)) return;
        intent.removeExtra(EXTRA_OPEN_MAINTENANCE);
        main.removeCallbacks(openMaintenance);
        main.postDelayed(openMaintenance, OPEN_DELAY_MS);
    }

    /** JanCar gives speed and position without any permission. Other units need this. */
    private void askLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
        }
    }

    /** On very wide panels the car card grows along with the right column. */
    private void fitWideScreen() {
        View card = findViewById(R.id.carCard);
        if (card == null || getResources().getConfiguration().screenWidthDp < WIDE_DP) return;
        android.widget.LinearLayout.LayoutParams lp = (android.widget.LinearLayout.LayoutParams) card.getLayoutParams();
        lp.width = 0;
        lp.weight = 1;
        card.setLayoutParams(lp);
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
        startedAt = SystemClock.elapsedRealtime();
        shownSpeed = -1; // whatever it was the last time Home was visible
        Startup.noteBoot(this);
        main.postDelayed(alertCheck, ALERT_CHECK_DELAY_MS);
        if (trimmed) {
            trimmed = false;
            showCar();
        }
        if (jancar) {
            mediaLink.bind(this);
            systemLink.bind(this);
        }
        if (jancar && "lights".equals(profile.night)) carLink.bind(this);
        updateReminder();
        if (!jancar) startLocation();
        lastFixAt = SystemClock.elapsedRealtime();
        main.post(ticker);
    }

    @Override
    protected void onStop() {
        started = false;
        main.removeCallbacks(ticker);
        main.removeCallbacks(alertCheck);
        stopLocation();
        mediaLink.unbind(this);
        systemLink.unbind(this);
        carLink.unbind(this);
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

    /** With CarPlay in front the home screen gives back its bitmaps; they return on the next onStart. */
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level < TRIM_MEMORY_UI_HIDDEN || started) return;
        trimmed = true;
        ((ImageView) findViewById(R.id.car)).setImageDrawable(null);
        ((ImageView) findViewById(R.id.logo)).setImageDrawable(null);
        if (carBitmap != null) {
            carBitmap.recycle();
            carBitmap = null;
        }
        setArt(null);
    }

    @Override
    protected void onDestroy() {
        main.removeCallbacksAndMessages(null);
        art.setImageDrawable(null);
        ((ImageView) findViewById(R.id.car)).setImageDrawable(null);
        if (carBitmap != null) {
            carBitmap.recycle();
            carBitmap = null;
        }
        if (artBitmap != null) {
            artBitmap.recycle();
            artBitmap = null;
        }
        super.onDestroy();
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

    /** Follows the night setting once a minute, and only while visible. */
    private void checkNight() {
        if (started && Theming.isNight(this, profile) != night) recreate();
    }

    private void updateReminder() {
        View touch = findViewById(R.id.reminderTouch);
        long now = System.currentTimeMillis();
        java.util.TimeZone tz = java.util.TimeZone.getDefault();
        // The common case: nothing close. No need to parse the list.
        int soonest = Reminders.soonestDays(this, now, tz);
        if (!profile.remindHome || soonest == Reminders.DUE_NONE || soonest > profile.remindLead) {
            touch.setVisibility(View.GONE);
            return;
        }
        List<Reminders.Item> due = Reminders.upcoming(Reminders.load(this), profile.remindLead, now, tz);
        if (due.isEmpty()) {
            touch.setVisibility(View.GONE);
            return;
        }
        Reminders.Item first = due.get(0);
        String line = getString(R.string.rem_pill, Reminders.name(this, first),
                Reminders.status(this, first, profile.remindLead, now, tz));
        reminderPill.setText(due.size() > 1 ? line + getString(R.string.rem_pill_more, due.size() - 1) : line);
        touch.setVisibility(View.VISIBLE);
    }

    private void updateClock() {
        if ("lights".equals(profile.night)) carLink.readHeadlights();
        checkNight();
        if (isFinishing()) return;
        Date now = new Date();
        time.setText(new SimpleDateFormat("HH:mm", locale).format(now));
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String g = getString(h >= 6 && h < 13 ? R.string.good_morning : h >= 13 && h < 20 ? R.string.good_afternoon : R.string.good_evening);
        String name = profile.greeting;
        greeting.setText(name.isEmpty() ? g : g + ", " + name);
        String d = new SimpleDateFormat(getString(R.string.date_pattern), locale).format(now);
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
        if (!haveLatLon && "sun".equals(profile.night)) {
            // City-level precision is plenty for sunset, and it stays on this device.
            prefs.edit().putFloat(Theming.KEY_LAT, Math.round(lat * 10) / 10f)
                    .putFloat(Theming.KEY_LON, Math.round(lon * 10) / 10f).apply();
        }
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
        final Locale geoLocale = locale;
        new Thread(new Runnable() {
            @Override
            public void run() {
                String name = null;
                try {
                    List<Address> list = new Geocoder(app, geoLocale).getFromLocation(lat, lon, 1);
                    if (list != null && !list.isEmpty()) {
                        Address a = list.get(0);
                        name = CityName.pick(a.getLocality(), a.getSubAdminArea(), a.getAdminArea());
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
        }, "launcher-geocoder").start();
    }

    // ---------------------------------------------------------------- rail

    private void setupRail() {
        for (int i = 0; i < Profile.SLOTS; i++) setupSlot(i);
        findViewById(R.id.btnHome).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Already home.
            }
        });
        findViewById(R.id.btnSettings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Launch.openSettings(MainActivity.this);
            }
        });
        findViewById(R.id.btnApps).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AppsActivity.class));
            }
        });
        findViewById(R.id.btnCustomize).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCustomize(null);
            }
        });
        findViewById(R.id.reminderTouch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCustomize(SettingsActivity.SECTION_MAINTENANCE);
            }
        });
    }

    private void openCustomize(String section) {
        if (isMoving()) {
            Toast.makeText(this, R.string.stopped_only, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(this, SettingsActivity.class);
        if (section != null) i.putExtra(SettingsActivity.EXTRA_SECTION, section);
        startActivityForResult(i, REQ_CUSTOMIZE);
    }

    private void setupSlot(final int i) {
        View b = findViewById(SLOT_IDS[i]);
        final String flat = Launch.slot(this, profile, i);
        if (b instanceof ImageButton) {
            ImageButton ib = (ImageButton) b;
            ib.setImageDrawable(Launch.icon(this, flat));
            ib.setScaleType(Launch.RADIO.equals(flat) || Launch.MUSIC.equals(flat)
                    ? ImageView.ScaleType.CENTER : ImageView.ScaleType.FIT_CENTER);
        } else {
            ((TextView) b).setText(Launch.label(this, flat));
        }
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Launch.installed(MainActivity.this, flat)) Launch.open(MainActivity.this, flat);
                else pickForSlot(i);
            }
        });
        b.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                pickForSlot(i);
                return true;
            }
        });
    }

    private void pickForSlot(int i) {
        Intent pick = new Intent(this, AppsActivity.class).putExtra(AppsActivity.EXTRA_PICK, true);
        startActivityForResult(pick, REQ_PICK_SLOT + (i << 8));
        Toast.makeText(this, R.string.pick_for_slot, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CUSTOMIZE) {
            // Whatever changed in Personalizar shows up on a fresh screen.
            recreate();
        } else if ((requestCode & 0xFF) == REQ_PICK_SLOT && resultCode == RESULT_OK && data != null) {
            int slot = requestCode >> 8;
            profile.slots[slot] = data.getStringExtra(AppsActivity.EXTRA_PICK);
            profile.save(this);
            setupSlot(slot);
        }
    }

    private boolean isMoving() {
        return shownSpeed >= MOVING_KMH && !noGpsShown;
    }

    private void showCar() {
        Pack pack = Pack.load(this, profile.pack);
        ImageView logo = findViewById(R.id.logo);
        logo.setImageDrawable(Brand.logo(this, pack, profile));
        logo.setImageTintList(android.content.res.ColorStateList.valueOf(Theming.color(this, R.attr.uiText)));
        ((TextView) findViewById(R.id.carTitle)).setText(profile.displayTitle(pack));
        ((TextView) findViewById(R.id.carSub)).setText(profile.subtitle(pack));
        carBitmap = CarImage.loadForDisplay(this, density);
        ImageView car = findViewById(R.id.car);
        car.setImageBitmap(carBitmap);
        if (carBitmap != null) {
            // Floor line just under the wheels, whatever the photo's proportions.
            if (!"mmi".equals(profile.layout)) {
                float h = carBitmap.getHeight() * (440f * density / carBitmap.getWidth());
                findViewById(R.id.floor).setTranslationY(h / 2 - 2 * density);
            } else {
                findViewById(R.id.floor).setVisibility(View.GONE);
            }
            if (night) car.setAlpha(0.7f);
        }
    }

    // ---------------------------------------------------------------- speed

    @Override
    public void onGps(String longitude, String latitude, float accuracy, double altitude, final float kmh) {
        // Binder thread.
        jancarGpsSeen = true;
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
        boolean over = profile.speedLimit > 0 && v > profile.speedLimit;
        if (over != speedOver) {
            speedOver = over;
            speed.setTextColor(Theming.color(this, over ? R.attr.uiAccent : R.attr.uiText));
        }
        speedBar.setProgress(Math.min(200, v));
    }

    private void startLocation() {
        if (locationManager == null
                || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            locationStarted = true;
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener, Looper.getMainLooper());
            Location last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (last != null) setLatLon(last.getLatitude(), last.getLongitude());
        } catch (Throwable t) {
            DebugLog.log("location error: " + t);
        }
    }

    private void stopLocation() {
        if (locationManager == null || !locationStarted) return;
        locationStarted = false;
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

        View.OnClickListener openRadio = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Launch.open(MainActivity.this, Launch.RADIO);
            }
        };
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
                setTexts(getString(R.string.cd_radio) + " " + (radioFreqValue == null ? "" : radioFreqValue + " " + radioUnit.getText()),
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
                    if (started) {
                        main.removeCallbacks(ticker);
                        main.post(ticker);
                    }
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
        radioLabel.setText(getString(R.string.radio_band, band.toUpperCase(Locale.US)));
    }

    // ---------------------------------------------------------------- ticker

    private void tick() {
        // A JanCar unit whose firmware never reports GPS falls back to the standard provider.
        if (jancar && !jancarGpsSeen && !locationStarted
                && SystemClock.elapsedRealtime() - startedAt > JANCAR_GPS_GRACE_MS) {
            askLocation();
            startLocation();
        }
        if (playing) updateProgress();
        boolean stale = SystemClock.elapsedRealtime() - lastFixAt > GPS_STALE_MS;
        if (stale != noGpsShown) {
            noGpsShown = stale;
            noGps.setVisibility(stale ? View.VISIBLE : View.INVISIBLE);
        }
    }
}
