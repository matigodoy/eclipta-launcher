package ar.com.eclipta.launcher;

import android.app.Activity;
import android.graphics.ImageDecoder;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

/** Wake animation: plays the brand's sting once over whatever is on screen, then finishes itself (and opens CarPlay if asked). */
public class SplashActivity extends Activity {
    private static final long FALLBACK_MS = 4000;

    private final Handler handler = new Handler();
    private AnimatedImageDrawable anim;
    private ImageView image;

    private final Runnable finisher = new Runnable() {
        @Override
        public void run() {
            if (isFinishing()) return;
            finish();
            if (Profile.load(SplashActivity.this).carplayOnAcc) Launch.start(SplashActivity.this, Launch.CARPLAY);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        setContentView(image, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        image.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finisher.run();
            }
        });
        handler.postDelayed(finisher, FALLBACK_MS);
        try {
            Profile p = Profile.load(this);
            Drawable d = ImageDecoder.decodeDrawable(Brand.sting(this, Pack.load(this, p.pack), p));
            image.setImageDrawable(d);
            if (d instanceof AnimatedImageDrawable) {
                anim = (AnimatedImageDrawable) d;
                anim.setRepeatCount(0);
                anim.registerAnimationCallback(new Animatable2.AnimationCallback() {
                    @Override
                    public void onAnimationEnd(Drawable drawable) {
                        handler.removeCallbacks(finisher);
                        finisher.run();
                    }
                });
                anim.start();
            }
        } catch (Throwable t) {
            DebugLog.init(this);
            DebugLog.log("splash decode error: " + t);
            handler.removeCallbacks(finisher);
            finisher.run();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Never linger in the background.
        if (!isFinishing()) finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(finisher);
        if (anim != null) {
            anim.clearAnimationCallbacks();
            anim.stop();
            anim = null;
        }
        if (image != null) image.setImageDrawable(null);
        super.onDestroy();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }
}
