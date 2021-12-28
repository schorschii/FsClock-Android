package systems.sieber.fsclock;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import androidx.appcompat.app.AppCompatDelegate;

public class FsClockApp extends Application {

    @Override
    public void onCreate() {
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

}
