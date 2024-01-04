package systems.sieber.fsclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.service.dreams.DreamService;
import android.view.View;

public class FullscreenDream extends DreamService {

    FsClockView mContentView;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        //setScreenBright(false); // doesn't seem to make a difference - or I don't understand it
        initContentView();
        registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    private void initContentView() {
        setContentView(R.layout.dream_fullscreen);

        // find views
        mContentView = findViewById(R.id.fullscreen_fsclock_view);

        // hide the system navigation bar with the same flags as done in the pre-installed Android clock app
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();

        // start the clock
        mContentView.resume();
    }

    @Override
    public void onDreamingStopped() {
        super.onDreamingStopped();

        // stop the clock
        mContentView.pause();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        // unregister battery receiver
        try {
            unregisterReceiver(this.mBatInfoReceiver);
        } catch(IllegalArgumentException ignored) {}
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // re-init the content view
        // this is a really annoying Android bug: when rotating in screensaver mode,
        // the view_fsclock layout is broken / not automatically updated to landscape
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                initContentView();
                mContentView.resume();
                mContentView.updateBattery(mBatLastPlugged, mBatLastLevel);
            }
        }, 100);
    }

    int mBatLastPlugged = 0;
    int mBatLastLevel = 100;
    private final BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            mBatLastPlugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
            mBatLastLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            mContentView.updateBattery(mBatLastPlugged, mBatLastLevel);
        }
    };

}
