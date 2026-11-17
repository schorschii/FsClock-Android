package systems.sieber.fsclock;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.widget.RemoteViews;

import androidx.appcompat.content.res.AppCompatResources;

import java.io.File;
import java.util.Calendar;

public class FsClockWidgetAnalogProvider extends AppWidgetProvider {

    static final String ACTION_ANALOG_WIDGET_UPDATE = "systems.sieber.fsclock.ANALOG_CLOCK_WIDGET_UPDATE";
    private static final int BITMAP_SIZE = 800;

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        scheduleUpdates(context);
        updateAllWidgets(context, manager, appWidgetIds);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager,
                                          int appWidgetId, Bundle newOptions) {
        updateAllWidgets(context, manager, new int[] {appWidgetId});
    }

    @Override
    public void onEnabled(Context context) {
        scheduleUpdates(context);
    }

    @Override
    public void onDisabled(Context context) {
        cancelUpdates(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if(ACTION_ANALOG_WIDGET_UPDATE.equals(action)
                || WidgetUpdateScheduler.isRescheduleAction(action)) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(new ComponentName(context, FsClockWidgetAnalogProvider.class));
            if(ids.length > 0) {
                // Re-arm before rendering so a rendering failure cannot stop the clock.
                scheduleUpdates(context);
                updateAllWidgets(context, manager, ids);
            } else {
                cancelUpdates(context);
            }
        }
    }

    static void updateAllWidgets(Context context, AppWidgetManager manager, int[] ids) {
        if(ids.length == 0) return;
        Bitmap clockBitmap = renderAnalogClock(context);
        SharedPreferences prefs = context.getSharedPreferences(BaseSettingsActivity.SHARED_PREF_DOMAIN, Context.MODE_PRIVATE);
        boolean drawBackground = prefs.getBoolean(FsClockWidgetAnalogConfigActivity.PREF_KEY, false);
        int colorBack = prefs.getInt("color-back", 0xFF000000);

        PendingIntent launchIntent = PendingIntent.getActivity(
                context, 0,
                new Intent(context, FullscreenActivity.class),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);

        for(int id : ids) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_fsclock_analog);
            views.setImageViewBitmap(R.id.widgetAnalogClock, drawBackground
                    ? WidgetBackground.draw(context, manager, id, clockBitmap, colorBack) : clockBitmap);
            views.setOnClickPendingIntent(R.id.widgetAnalogClock, launchIntent);
            manager.updateAppWidget(id, views);
        }
    }

    private static Bitmap renderAnalogClock(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(BaseSettingsActivity.SHARED_PREF_DOMAIN, Context.MODE_PRIVATE);
        StorageControl sc = new StorageControl(context);

        Bitmap bitmap = Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Clock face
        Drawable face = loadClockDrawable(context, sc,
                StorageControl.FILENAME_CLOCK_FACE,
                prefs.getInt("clock-analog-face", 0),
                GraphicSelectionAdapter.CLOCK_FACES,
                R.drawable.analog_classic_bg);
        if(prefs.getBoolean("own-color-analog-clock-face", false)) {
            face.setColorFilter(new PorterDuffColorFilter(
                    prefs.getInt("color-analog-face", 0xffffffff), PorterDuff.Mode.SRC_ATOP));
        }
        face.setBounds(0, 0, BITMAP_SIZE, BITMAP_SIZE);
        face.draw(canvas);

        Calendar cal = Calendar.getInstance();
        float minAngle = cal.get(Calendar.MINUTE) * 360f / 60;
        float hrAngle = (cal.get(Calendar.HOUR) + cal.get(Calendar.MINUTE) / 60f) * 360f / 12;

        // Hours hand
        Drawable hoursHand = loadClockDrawable(context, sc,
                StorageControl.FILENAME_HOURS_HAND,
                prefs.getInt("clock-analog-hours", 0),
                GraphicSelectionAdapter.HOUR_HANDS,
                R.drawable.analog_classic_h);
        if(prefs.getBoolean("own-color-analog-hours", false)) {
            hoursHand.setColorFilter(new PorterDuffColorFilter(
                    prefs.getInt("color-analog-hours", 0xffffffff), PorterDuff.Mode.SRC_ATOP));
        }
        drawRotated(canvas, hoursHand, hrAngle);

        // Minutes hand
        Drawable minutesHand = loadClockDrawable(context, sc,
                StorageControl.FILENAME_MINUTES_HAND,
                prefs.getInt("clock-analog-minutes", 0),
                GraphicSelectionAdapter.MINUTE_HANDS,
                R.drawable.analog_classic_m);
        if(prefs.getBoolean("own-color-analog-minutes", false)) {
            minutesHand.setColorFilter(new PorterDuffColorFilter(
                    prefs.getInt("color-analog-minutes", 0xffffffff), PorterDuff.Mode.SRC_ATOP));
        }
        drawRotated(canvas, minutesHand, minAngle);

        return bitmap;
    }

    private static Drawable loadClockDrawable(Context context, StorageControl sc,
                                               String filename, int prefId,
                                               GraphicItem[] items, int defaultResId) {
        File customFile = sc.getStorage(filename);
        if(customFile.exists()) {
            Bitmap bmp = BitmapFactory.decodeFile(customFile.getAbsolutePath());
            if(bmp != null) {
                return new BitmapDrawable(context.getResources(), bmp);
            }
        }
        GraphicItem gi = GraphicSelectionAdapter.getById(prefId, items);
        int resId = (gi != null && gi.mGraphicResourceId != null) ? gi.mGraphicResourceId : defaultResId;
        return AppCompatResources.getDrawable(context, resId);
    }

    private static void drawRotated(Canvas canvas, Drawable drawable, float degrees) {
        canvas.save();
        canvas.rotate(degrees, BITMAP_SIZE / 2f, BITMAP_SIZE / 2f);
        drawable.setBounds(0, 0, BITMAP_SIZE, BITMAP_SIZE);
        drawable.draw(canvas);
        canvas.restore();
    }

    static void scheduleUpdates(Context context) {
        WidgetUpdateScheduler.schedule(context, buildUpdatePendingIntent(context));
    }

    static void cancelUpdates(Context context) {
        WidgetUpdateScheduler.cancel(context, buildUpdatePendingIntent(context));
    }

    private static PendingIntent buildUpdatePendingIntent(Context context) {
        Intent intent = new Intent(context, FsClockWidgetAnalogProvider.class);
        intent.setAction(ACTION_ANALOG_WIDGET_UPDATE);
        return PendingIntent.getBroadcast(context, 0, intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
    }
}
