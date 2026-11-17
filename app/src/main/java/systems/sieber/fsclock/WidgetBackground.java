package systems.sieber.fsclock;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;

/** Draws the background in bitmap space, compensating for the ImageView's fitCenter scale. */
final class WidgetBackground {
    private WidgetBackground() {}

    static Bitmap draw(Context context, AppWidgetManager manager, int id, Bitmap clock, int color) {
        Bundle options = manager.getAppWidgetOptions(id);
        boolean landscape = context.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        float widthDp = options.getInt(landscape ? AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH
                : AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        float heightDp = options.getInt(landscape ? AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
                : AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
        float density = context.getResources().getDisplayMetrics().density;
        AppWidgetProviderInfo info = manager.getAppWidgetInfo(id);
        // During initial configuration some launchers have not supplied size options yet.
        if(widthDp <= 0) widthDp = info != null && info.minWidth > 0
                ? info.minWidth / density : clock.getWidth() / density;
        if(heightDp <= 0) heightDp = info != null && info.minHeight > 0
                ? info.minHeight / density : clock.getHeight() / density;
        float scaleDp = Math.min(widthDp / clock.getWidth(), heightDp / clock.getHeight());
        float radius = 8f / scaleDp;

        Bitmap bitmap = Bitmap.createBitmap(clock.getWidth(), clock.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        canvas.drawRoundRect(new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()), radius, radius, paint);
        canvas.drawBitmap(clock, 0, 0, null);
        return bitmap;
    }
}
