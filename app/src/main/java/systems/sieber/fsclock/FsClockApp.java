package systems.sieber.fsclock;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

public class FsClockApp extends Application {

    @Override
    public void onCreate() {
        migrateSettings();
        setAppTheme(getAppTheme(getApplicationContext()));
        super.onCreate();
    }

    public static void setAppTheme(int setting) {
        AppCompatDelegate.setDefaultNightMode(setting);
    }

    public static int getAppTheme(Context context) {
        //SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        //return prefs.getInt("dark-mode-native", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }

    public static boolean isDarkThemeActive(Context context, int setting) {
        if(setting == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
            return isDarkThemeActive(context);
        } else {
            return setting == AppCompatDelegate.MODE_NIGHT_YES;
        }
    }

    public static boolean isDarkThemeActive(Context context) {
        int uiMode = context.getResources().getConfiguration().uiMode;
        return (uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private void migrateSettings() {
        SharedPreferences sharedPref = getSharedPreferences(BaseSettingsActivity.SHARED_PREF_DOMAIN, Context.MODE_PRIVATE);
        if(sharedPref.contains("color-red-analog") && sharedPref.contains("color-green-analog") && sharedPref.contains("color-blue-analog")) {
            SharedPreferences.Editor editor = sharedPref.edit();
            int oldColor = Color.argb(255, sharedPref.getInt("color-red-analog", 255), sharedPref.getInt("color-green-analog", 255), sharedPref.getInt("color-blue-analog", 255));
            editor.putInt("color-analog-face", oldColor);
            editor.putInt("color-analog-hours", oldColor);
            editor.putInt("color-analog-minutes", oldColor);
            editor.putInt("color-analog-seconds", oldColor);
            editor.remove("color-red-analog");
            editor.remove("color-green-analog");
            editor.remove("color-blue-analog");
            editor.apply();
            Log.i("migrate", "color-analog-* MIGRATED");
        }
        if(sharedPref.contains("color-red") && sharedPref.contains("color-green") && sharedPref.contains("color-blue")) {
            SharedPreferences.Editor editor = sharedPref.edit();
            int oldColor = Color.argb(255, sharedPref.getInt("color-red", 255), sharedPref.getInt("color-green", 255), sharedPref.getInt("color-blue", 255));
            editor.putInt("color-digital", oldColor);
            editor.remove("color-red");
            editor.remove("color-green");
            editor.remove("color-blue");
            editor.apply();
            Log.i("migrate", "color-* MIGRATED");
        }
        if(sharedPref.contains("color-red-back") && sharedPref.contains("color-green-back") && sharedPref.contains("color-blue-back")) {
            SharedPreferences.Editor editor = sharedPref.edit();
            int oldColor = Color.argb(255, sharedPref.getInt("color-red-back", 255), sharedPref.getInt("color-green-back", 255), sharedPref.getInt("color-blue-back", 255));
            editor.putInt("color-back", oldColor);
            editor.remove("color-red-back");
            editor.remove("color-green-back");
            editor.remove("color-blue-back");
            editor.apply();
            Log.i("migrate", "color-back-* MIGRATED");
        }
        if(sharedPref.contains("own-color-analog")) {
            SharedPreferences.Editor editor = sharedPref.edit();
            boolean oldValue = sharedPref.getBoolean("own-color-analog", true);
            editor.putBoolean("own-color-analog-hours", oldValue);
            editor.putBoolean("own-color-analog-minutes", oldValue);
            editor.remove("own-color-analog");
            editor.apply();
            Log.i("migrate", "own-color-analog-* MIGRATED");
        }
    }

}
