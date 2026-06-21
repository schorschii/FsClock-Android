package systems.sieber.fsclock;

import android.app.UiModeManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class FsClockWidgetAnalogConfigActivity extends AppCompatActivity {

    static final String PREF_KEY = "widget-analog-draw-background";

    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private SharedPreferences mPrefs;

    private CheckBox mCheckBoxDrawBackground;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_config);

        // init toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(R.string.widget_analog_name);

        setResult(RESULT_CANCELED);

        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if(mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        mPrefs = getSharedPreferences(BaseSettingsActivity.SHARED_PREF_DOMAIN, Context.MODE_PRIVATE);
        mCheckBoxDrawBackground = findViewById(R.id.checkBoxDrawBackground);
        mCheckBoxDrawBackground.setChecked(mPrefs.getBoolean(PREF_KEY, false));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);

        // display play/pause icon on TV devices for saving settings
        UiModeManager uiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
        if(uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            menu.findItem(R.id.action_settings_done).setIcon(R.drawable.ic_play_pause_white);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_settings_done:
                saveAndFinish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void saveAndFinish() {
        mPrefs.edit().putBoolean(PREF_KEY, mCheckBoxDrawBackground.isChecked()).apply();

        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        int[] ids = manager.getAppWidgetIds(new ComponentName(this, FsClockWidgetAnalogProvider.class));
        FsClockWidgetAnalogProvider.updateAllWidgets(this, manager, ids);

        Intent result = new Intent();
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, result);
        finish();
    }

}
