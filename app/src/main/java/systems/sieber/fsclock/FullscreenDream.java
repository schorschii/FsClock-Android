package systems.sieber.fsclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
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
        unregisterReceiver(this.mBatInfoReceiver);
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
