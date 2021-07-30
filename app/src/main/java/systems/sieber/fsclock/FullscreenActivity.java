package systems.sieber.fsclock;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.BatteryManager;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;

public class FullscreenActivity extends AppCompatActivity {

    private final FullscreenActivity me = this;

    SharedPreferences mSharedPref;

    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private FsClockView mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            ActionBar actionBar = getSupportActionBar();
            if(actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    ActivityResultLauncher<Intent> settingsActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == Activity.RESULT_OK) {
                        mContentView.loadSettings(me);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);

        // init settings
        mSharedPref = getSharedPreferences(SettingsActivity.SHARED_PREF_DOMAIN, Context.MODE_PRIVATE);

        // find views
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_fsclock_view);
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                me.toggle();
            }
        });

        // init battery info
        registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    public void onPause() {
        super.onPause();
        mContentView.pause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mContentView.resume();
        incrementStartedCounter();
        showAdOtherApps();
    }

    private final BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            mContentView.updateBattery(plugged, level);
        }
    };

    @Override
    public void onRequestPermissionsResult(int callbackId, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(callbackId, permissions, grantResults);
        int i = 0;
        for (String p : permissions) {
            if (p.equals(Manifest.permission.READ_CALENDAR)) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    mContentView.updateEventView();
                }
            }
            i++;
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        hide();
        //delayedHide(100);
    }

    private void toggle() {
        if(mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    public void openSettings(View v) {
        Intent i = new Intent(this, SettingsActivity.class);
        settingsActivityResultLauncher.launch(i);
    }

    private void incrementStartedCounter() {
        int oldStartedValue = mSharedPref.getInt("started", 0);
        Log.i("STARTED",Integer.toString(oldStartedValue));
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putInt("started", oldStartedValue+1);
        editor.apply();
    }
    private void showAdOtherApps() {
        if(mSharedPref.getInt("started", 0) % 12 == 0
                && mSharedPref.getInt("ad-other-apps-shown", 0) < 2) {
            // increment counter
            SharedPreferences.Editor editor = mSharedPref.edit();
            editor.putInt("ad-other-apps-shown", mSharedPref.getInt("ad-other-apps-shown", 0)+1);
            editor.apply();

            // show ad "other apps"
            final Dialog ad = new Dialog(this);
            ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
            ad.setContentView(R.layout.dialog_otherapps);
            ad.setCancelable(true);
            ad.findViewById(R.id.buttonReviewNow).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openPlayStore(me, getPackageName());
                    ad.hide();
                }
            });
            ad.findViewById(R.id.linearLayoutRateStars).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openPlayStore(me, getPackageName());
                    ad.hide();
                }
            });
            ad.show();
        }
    }
    static void openPlayStore(Activity activity, String appId) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appId)));
        } catch (android.content.ActivityNotFoundException ignored) {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appId)));
        }
    }

}
