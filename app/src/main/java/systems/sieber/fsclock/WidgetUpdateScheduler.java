package systems.sieber.fsclock;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.View;

/** Minute-aligned, one-shot alarms shared by both bitmap clock widgets. */
final class WidgetUpdateScheduler {
    private WidgetUpdateScheduler() {}

    static void schedule(Context context, PendingIntent update) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long now = System.currentTimeMillis();
        long nextMinute = now - now % 60_000L + 60_000L;
        // Repeating alarms are inexact from API 19 onward. RTC deliberately does not
        // wake a sleeping device: the pending update can run when it wakes again.
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && canScheduleExact(alarms)) {
            try {
                alarms.setExact(AlarmManager.RTC, nextMinute, update);
                return;
            } catch(SecurityException ignored) {
                // Access may have been revoked between the check and scheduling.
            }
        }
        // Pre-KitKat set() is exact; newer devices without access use best effort.
        alarms.set(AlarmManager.RTC, nextMinute, update);
    }

    static void cancel(Context context, PendingIntent update) {
        ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).cancel(update);
    }

    private static boolean canScheduleExact(AlarmManager alarms) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms();
    }

    static boolean isRescheduleAction(String action) {
        return Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    && AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED.equals(action));
    }

    static void configurePermissionButton(Activity activity) {
        View button = activity.findViewById(R.id.buttonWidgetExactAlarm);
        AlarmManager alarms = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
        button.setVisibility(canScheduleExact(alarms) ? View.GONE : View.VISIBLE);
        button.setOnClickListener(view -> {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + activity.getPackageName()));
                try {
                    activity.startActivity(intent);
                } catch(ActivityNotFoundException ignored) {
                    // Some vendor builds do not expose this settings page.
                }
            }
        });
    }
}
