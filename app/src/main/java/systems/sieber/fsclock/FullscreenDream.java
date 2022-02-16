package systems.sieber.fsclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.service.dreams.DreamService;
import android.view.View;

public class FullscreenDream extends DreamService {

    FsClockView mContentView;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setContentView(R.layout.dream_fullscreen);

        // find views
        mContentView = findViewById(R.id.fullscreen_fsclock_view);

        // hide the system navigation bar with the same flags as done in the Android clock app
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        // init battery info
        registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    public void onDreamingStarted() {
        // start the clock
        mContentView.resume();
    }

    @Override
    public void onDreamingStopped() {
        // stop the clock
        mContentView.pause();
    }

    @Override
    public void onDetachedFromWindow() {
        // unregister battery receiver
        try {
            unregisterReceiver(this.mBatInfoReceiver);
        } catch(IllegalArgumentException ignored) {}
    }

    private final BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            mContentView.updateBattery(plugged, level);
        }
    };

}
