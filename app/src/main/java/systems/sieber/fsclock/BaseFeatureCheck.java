package systems.sieber.fsclock;

import android.content.Context;
import android.content.SharedPreferences;

class BaseFeatureCheck {

    /*  It is not allowed to modify this file in order to bypass license checks.
        I made this app open source hoping people will learn something from this project.
        But keep in mind: open source means free as "free speech" but not as in "free beer".
        At first glance, it may not look like it, but even this "tiny" clock app takes a lot of time to maintain because of the
        quirks of different Android versions and devices in combination with auto text sizing and the DreamService implementation (FireTV!!!).
        Please be so kind and support further development by purchasing the in-app purchase in one of the app stores.
        It's up to you how long this app will be maintained. Thanks for your support.
    */

    Context mContext;
    SharedPreferences mSettings;

    BaseFeatureCheck(Context c) {
        mContext = c;
    }

    featureCheckReadyListener listener = null;
    public interface featureCheckReadyListener {
        void featureCheckReady(boolean fetchSuccess);
    }
    void setFeatureCheckReadyListener(featureCheckReadyListener listener) {
        this.listener = listener;
    }

    void init() {
        // get settings (faster than google play - after purchase done, billing client needs minutes to realize the purchase)
        mSettings = mContext.getSharedPreferences(SettingsActivity.SHARED_PREF_DOMAIN, 0);
        unlockedSettings = mSettings.getBoolean("purchased-settings", true);
    }

    boolean isReady = false;

    boolean unlockedSettings = true;

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    void unlockPurchase(String sku) {
        SharedPreferences.Editor editor = mSettings.edit();
        switch(sku) {
            case "settings":
                unlockedSettings = true;
                editor.putBoolean("purchased-settings", true);
                editor.apply();
                break;
        }
    }

}
