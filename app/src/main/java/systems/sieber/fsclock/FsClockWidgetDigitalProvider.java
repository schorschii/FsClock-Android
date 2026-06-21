package systems.sieber.fsclock;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.content.res.ResourcesCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class FsClockWidgetDigitalProvider extends AppWidgetProvider {

    static final String ACTION_DIGITAL_WIDGET_UPDATE = "systems.sieber.fsclock.DIGITAL_CLOCK_WIDGET_UPDATE";

    private static final int BITMAP_W = 1200;
    private static final int BITMAP_H = 600;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        scheduleUpdates(context);
        updateAllWidgets(context, appWidgetManager, appWidgetIds);
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
        if(ACTION_DIGITAL_WIDGET_UPDATE.equals(action)) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(new ComponentName(context, FsClockWidgetDigitalProvider.class));
            updateAllWidgets(context, manager, ids);
        } else if(Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(new ComponentName(context, FsClockWidgetDigitalProvider.class));
            if(ids.length > 0) {
                scheduleUpdates(context);
                updateAllWidgets(context, manager, ids);
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    static void updateAllWidgets(Context context, AppWidgetManager manager, int[] ids) {
        SharedPreferences prefs = context.getSharedPreferences(BaseSettingsActivity.SHARED_PREF_DOMAIN, Context.MODE_PRIVATE);
        boolean format24hrs = prefs.getBoolean("24hrs", true);
        boolean showDate = prefs.getBoolean("show-date", true);
        int colorClock = prefs.getInt("color-digital-clock", 0xffffffff);
        int colorDate = prefs.getInt("color-digital-date", 0xffffffff);
        String dateFormat = prefs.getString("date-format", FsClockView.getDefaultDateFormat(context));

        FontOption fontOptionClock = FontOptions.getById(prefs.getInt("font-digital-clock", FontOptions.DSEG7_CLASSIC));
        Typeface typefaceClock = ResourcesCompat.getFont(context, fontOptionClock.mResourceId);
        FontOption fontOptionDate = FontOptions.getById(prefs.getInt("font-digital-date", FontOptions.CAIRO_REGULAR));
        Typeface typefaceDate = ResourcesCompat.getFont(context, fontOptionDate.mResourceId);

        Calendar cal = Calendar.getInstance();
        String timeStr = new SimpleDateFormat(format24hrs ? "HH:mm" : "h:mm").format(cal.getTime());
        String dateStr;
        try {
            dateStr = new SimpleDateFormat(dateFormat, Locale.getDefault()).format(cal.getTime());
        } catch(IllegalArgumentException e) {
            dateStr = "";
        }

        boolean drawBackground = prefs.getBoolean(FsClockWidgetDigitalConfigActivity.PREF_KEY, false);
        int colorBack = prefs.getInt("color-back", 0xFF000000);

        Bitmap bitmap = renderClock(context,
                timeStr, typefaceClock, fontOptionClock.mXCorr, colorClock,
                showDate ? dateStr : "", typefaceDate, colorDate,
                drawBackground, colorBack);

        PendingIntent launchIntent = PendingIntent.getActivity(
                context, 0,
                new Intent(context, FullscreenActivity.class),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);

        for(int id : ids) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_fsclock_digital);
            views.setImageViewBitmap(R.id.widgetClock, bitmap);
            views.setOnClickPendingIntent(R.id.widgetClock, launchIntent);
            manager.updateAppWidget(id, views);
        }
    }

    private static Bitmap renderClock(Context context,
            String timeStr, Typeface typefaceClock, float xCorr,
            int colorClock, String dateStr, Typeface typefaceDate, int colorDate,
            boolean drawBackground, int colorBack) {

        boolean hasDate = !dateStr.isEmpty();
        int timeH = hasDate ? (int) (BITMAP_H * 0.65f) : BITMAP_H;

        DigitalClockView dv = new DigitalClockView(context);
        dv.setTypeface(typefaceClock, xCorr);
        dv.setColor(colorClock);
        dv.setText(timeStr, "00");
        dv.setShowSec(false);
        dv.measure(
                View.MeasureSpec.makeMeasureSpec(BITMAP_W, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(timeH, View.MeasureSpec.EXACTLY)
        );
        dv.layout(0, 0, BITMAP_W, timeH);

        Bitmap bitmap = Bitmap.createBitmap(BITMAP_W, BITMAP_H, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        if(drawBackground) canvas.drawColor(colorBack);
        dv.draw(canvas);

        if(hasDate) {
            int dateAreaH = BITMAP_H - timeH;
            Paint datePaint = new Paint();
            datePaint.setColor(colorDate);
            datePaint.setTypeface(typefaceDate);
            datePaint.setAntiAlias(true);
            // scale to 90% of width
            datePaint.setTextSize(DigitalClockView.TEST_TEXT_SIZE);
            float desiredSize = DigitalClockView.TEST_TEXT_SIZE * BITMAP_W * 0.9f
                    / datePaint.measureText(dateStr);
            datePaint.setTextSize(desiredSize);
            // clamp so text fits vertically in the date area
            Rect bounds = new Rect();
            datePaint.getTextBounds(dateStr, 0, dateStr.length(), bounds);
            if(bounds.height() > dateAreaH * 0.85f) {
                datePaint.setTextSize(desiredSize * (dateAreaH * 0.85f) / bounds.height());
                datePaint.getTextBounds(dateStr, 0, dateStr.length(), bounds);
            }
            float x = (BITMAP_W - bounds.width()) / 2f - bounds.left;
            float y = timeH + (dateAreaH + bounds.height()) / 2f;
            canvas.drawText(dateStr, x, y, datePaint);
        }

        return bitmap;
    }

    static void scheduleUpdates(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = buildUpdatePendingIntent(context);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.MINUTE, 1);
        am.setRepeating(AlarmManager.RTC, cal.getTimeInMillis(), 60_000L, pi);
    }

    static void cancelUpdates(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(buildUpdatePendingIntent(context));
    }

    private static PendingIntent buildUpdatePendingIntent(Context context) {
        Intent intent = new Intent(context, FsClockWidgetDigitalProvider.class);
        intent.setAction(ACTION_DIGITAL_WIDGET_UPDATE);
        return PendingIntent.getBroadcast(context, 0, intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
    }
}
