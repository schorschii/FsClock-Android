package systems.sieber.fsclock;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.BatteryManager;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

public class FullscreenActivity extends AppCompatActivity {

    private final FullscreenActivity me = this;

    SharedPreferences mSharedPref;

    UiModeManager uiModeManager;

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
                    hide();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);
        uiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);

        // init settings
        mSharedPref = getSharedPreferences(SettingsActivity.SHARED_PREF_DOMAIN, Context.MODE_PRIVATE);

        // find views
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_fsclock_view);
        mContentView.mActivity = this;
        if(uiModeManager.getCurrentModeType() != Configuration.UI_MODE_TYPE_TELEVISION) {
            // we do not enable the onTouch event on TVs because this intersects with the onKeyDown event
            mContentView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    me.toggle();
                }
            });
        }

        // apply own background color to navigation bar - especially for Samsung One UI, which displays a white navbar by default
        int colorBack = Color.argb(0xff,
                mSharedPref.getInt("color-red-back", 0),
                mSharedPref.getInt("color-green-back", 0),
                mSharedPref.getInt("color-blue-back", 0)
        );
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(colorBack);
        }

        // stretch activity over the entire screen, even under the status bar with notch or cutout
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        // initial event state update
        mContentView.updateEventView();
    }

    @Override
    public void onPause() {
        super.onPause();

        // stop the clock
        mContentView.pause();

        // unregister receiver
        unregisterReceiver(this.mBatInfoReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();

        // init battery info
        registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        // start the clock
        mContentView.resume();
        incrementStartedCounter();
        showDialogReview();

        // show TV keys info
        if(uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            int tvHintShown = mSharedPref.getInt("tv-hint-shown", 0);
            if(tvHintShown == 0) {
                // increment counter
                SharedPreferences.Editor editor = mSharedPref.edit();
                editor.putInt("tv-hint-shown", tvHintShown + 1);
                editor.apply();
                // show info
                Toast.makeText(this, getString(R.string.tv_settings_note), Toast.LENGTH_LONG).show();
            }
        }
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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = false;
        switch(keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_SETTINGS:
                openSettings(null);
                handled = true; break;
        }
        return handled || super.onKeyDown(keyCode, event);
    }

    @Override
    public void onRequestPermissionsResult(int callbackId, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(callbackId, permissions, grantResults);
        int i = 0;
        for(String p : permissions) {
            if(p.equals(Manifest.permission.READ_CALENDAR)) {
                if(grantResults[i] == PackageManager.PERMISSION_GRANTED) {
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
    private void showDialogReview() {
        if(mSharedPref.getInt("started", 0) % 14 == 0
                && mSharedPref.getInt("ad-other-apps-shown", 0) < 1) {
            // increment counter
            SharedPreferences.Editor editor = mSharedPref.edit();
            editor.putInt("ad-other-apps-shown", mSharedPref.getInt("ad-other-apps-shown", 0)+1);
            editor.apply();

            // show ad "other apps"
            final Dialog ad = new Dialog(this);
            ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
            ad.setContentView(R.layout.dialog_review);
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
