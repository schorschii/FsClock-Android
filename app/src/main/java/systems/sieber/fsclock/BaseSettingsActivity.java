package systems.sieber.fsclock;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.UiModeManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.accessibility.AccessibilityManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

public class BaseSettingsActivity extends AppCompatActivity {

    static final String SHARED_PREF_DOMAIN = "CLOCK";

    static final int PERMISSION_REQUEST = 0;
    static final int PICK_CLOCK_FACE_REQUEST = 1;
    static final int PICK_HOURS_HAND_REQUEST = 2;
    static final int PICK_MINUTES_HAND_REQUEST = 3;
    static final int PICK_SECONDS_HAND_REQUEST = 4;
    static final int PICK_BACKGROUND_REQUEST = 5;

    StorageControl mStorage = new StorageControl(this);
    Gson mGson = new Gson();
    ArrayList<Event> mEvents = new ArrayList<>();
    SharedPreferences mSharedPref;

    LinearLayout mLinearLayoutPurchaseContainer;
    LinearLayout mLinearLayoutSettingsContainer;
    Button mButtonUnlockSettings;
    CheckBox mCheckBoxKeepScreenOn;
    CheckBox mCheckBoxShowBatteryInfo;
    CheckBox mCheckBoxShowBatteryInfoWhenCharging;
    CheckBox mCheckBoxBurnInPrevention;
    CheckBox mCheckBoxForceLandscape;
    CheckBox mCheckBoxDarkMode;
    CheckBox mCheckBoxAnalogClockShow;
    CheckBox mCheckBoxAnalogClockShowSeconds;
    Spinner mSpinnerDesignAnalogFace;
    Spinner mSpinnerDesignAnalogHours;
    Spinner mSpinnerDesignAnalogMinutes;
    Spinner mSpinnerDesignAnalogSeconds;
    CheckBox mCheckBoxDigitalClockShow;
    CheckBox mCheckBoxDateShow;
    EditText mEditTextDateFormat;
    RadioButton mRadioButtonGregorianCalendar;
    RadioButton mRadioButtonHijriCalendar;
    CheckBox mCheckBoxDigitalClockShowSeconds;
    CheckBox mCheckBoxDigitalClock24Format;
    View mColorChangerAnalogFace;
    View mColorPreviewAnalogFace;
    View mColorChangerAnalogHours;
    View mColorPreviewAnalogHours;
    View mColorChangerAnalogMinutes;
    View mColorPreviewAnalogMinutes;
    View mColorChangerAnalogSeconds;
    View mColorPreviewAnalogSeconds;
    int mColorAnalogFace;
    int mColorAnalogHours;
    int mColorAnalogMinutes;
    int mColorAnalogSeconds;
    boolean mCustomColorAnalogFace;
    boolean mCustomColorAnalogHours;
    boolean mCustomColorAnalogMinutes;
    boolean mCustomColorAnalogSeconds;
    View mColorChangerDigital;
    View mColorPreviewDigital;
    int mColorDigital;
    View mColorChangerBack;
    View mColorPreviewBack;
    int mColorBack;
    Spinner mSpinnerDesignBack;
    boolean mBackStretch;
    CheckBox mCheckBoxShowAlarms;
    Button mButtonNewEvent;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // init toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // display version & flavor
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            setTitle(getTitle() + " " + pInfo.versionName);
            ((TextView) findViewById(R.id.textViewBuildInfo)).setText(
                    getString(R.string.version) + " " + pInfo.versionName + " (" + pInfo.versionCode + ") " + BuildConfig.BUILD_TYPE + " " + BuildConfig.FLAVOR
            );
        } catch(PackageManager.NameNotFoundException ignored) { }

        // hide dream settings button if not supported
        UiModeManager uiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2
                || uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            findViewById(R.id.buttonDreamSettings).setVisibility(View.GONE);
        }

        // show screensaver note on FireTV
        if(getPackageManager().hasSystemFeature("amazon.hardware.fire_tv")) {
            findViewById(R.id.textViewFireTvNotes).setVisibility(View.VISIBLE);
            findViewById(R.id.buttonDreamSettings).setVisibility(View.GONE);
        }

        // show info regarding "High Contrast Text" system setting
        if(isHighContrastTextEnabled(getBaseContext())) {
            findViewById(R.id.textViewHighContrastNotes).setVisibility(View.VISIBLE);
        }

        // find views
        mLinearLayoutPurchaseContainer = findViewById(R.id.linearLayoutInAppPurchase);
        mLinearLayoutSettingsContainer = findViewById(R.id.linearLayoutSettings);
        mButtonUnlockSettings = findViewById(R.id.buttonUnlockSettings);
        mCheckBoxKeepScreenOn = findViewById(R.id.checkBoxKeepScreenOn);
        mCheckBoxShowBatteryInfo = findViewById(R.id.checkBoxShowBatteryInfo);
        mCheckBoxShowBatteryInfoWhenCharging = findViewById(R.id.checkBoxShowBatteryInfoWhenCharging);
        mCheckBoxBurnInPrevention = findViewById(R.id.checkBoxBurnInPrevention);
        mCheckBoxForceLandscape = findViewById(R.id.checkBoxForceLandscape);
        mCheckBoxDarkMode = findViewById(R.id.checkBoxDarkMode);
        mCheckBoxAnalogClockShow = findViewById(R.id.checkBoxShowAnalogClock);
        mCheckBoxAnalogClockShowSeconds = findViewById(R.id.checkBoxSecondsAnalog);
        mSpinnerDesignAnalogFace = findViewById(R.id.spinnerDesignAnalogFace);
        mSpinnerDesignAnalogHours = findViewById(R.id.spinnerDesignAnalogHours);
        mSpinnerDesignAnalogMinutes = findViewById(R.id.spinnerDesignAnalogMinutes);
        mSpinnerDesignAnalogSeconds = findViewById(R.id.spinnerDesignAnalogSeconds);
        mCheckBoxDigitalClockShow = findViewById(R.id.checkBoxShowDigitalClock);
        mEditTextDateFormat = findViewById(R.id.editTextDateFormat);
        mRadioButtonGregorianCalendar = findViewById(R.id.radioButtonGregorianCalendar);
        mRadioButtonHijriCalendar = findViewById(R.id.radioButtonHijriCalendar);
        mCheckBoxDateShow = findViewById(R.id.checkBoxShowDate);
        mCheckBoxDigitalClockShowSeconds = findViewById(R.id.checkBoxSecondsDigital);
        mCheckBoxDigitalClock24Format = findViewById(R.id.checkBox24HrsFormat);
        mColorChangerAnalogFace = findViewById(R.id.viewColorChangerAnalogFace);
        mColorPreviewAnalogFace = findViewById(R.id.viewColorPreviewAnalogFace);
        mColorChangerAnalogHours = findViewById(R.id.viewColorChangerAnalogHour);
        mColorPreviewAnalogHours = findViewById(R.id.viewColorPreviewAnalogHour);
        mColorChangerAnalogMinutes = findViewById(R.id.viewColorChangerAnalogMinute);
        mColorPreviewAnalogMinutes = findViewById(R.id.viewColorPreviewAnalogMinute);
        mColorChangerAnalogSeconds = findViewById(R.id.viewColorChangerAnalogSecond);
        mColorPreviewAnalogSeconds = findViewById(R.id.viewColorPreviewAnalogSecond);
        mColorChangerDigital = findViewById(R.id.viewColorChangerDigital);
        mColorPreviewDigital = findViewById(R.id.viewColorPreviewDigital);
        mSpinnerDesignBack = findViewById(R.id.spinnerDesignBack);
        mColorChangerBack = findViewById(R.id.viewColorChangerBack);
        mColorPreviewBack = findViewById(R.id.viewColorPreviewBack);
        mCheckBoxShowAlarms = findViewById(R.id.checkBoxShowAlarms);
        mButtonNewEvent = findViewById(R.id.buttonNewEvent);

        // init settings
        mSharedPref = getSharedPreferences(SHARED_PREF_DOMAIN, Context.MODE_PRIVATE);
        mCheckBoxKeepScreenOn.setChecked( mSharedPref.getBoolean("keep-screen-on", true) );
        mCheckBoxShowBatteryInfo.setChecked( mSharedPref.getBoolean("show-battery-info", true) );
        mCheckBoxShowBatteryInfoWhenCharging.setChecked( mSharedPref.getBoolean("show-battery-info-when-charging", false) );
        mCheckBoxBurnInPrevention.setChecked( mSharedPref.getBoolean("burn-in-prevention", false) );
        mCheckBoxForceLandscape.setChecked( mSharedPref.getBoolean("force-landscape", false) );
        mCheckBoxDarkMode.setChecked( mSharedPref.getBoolean("dark-mode", false) );
        mCheckBoxAnalogClockShow.setChecked( mSharedPref.getBoolean("show-analog", true) );
        mCheckBoxAnalogClockShowSeconds.setChecked( mSharedPref.getBoolean("show-seconds-analog", true) );
        mCustomColorAnalogFace = mSharedPref.getBoolean("own-color-analog-clock-face", false);
        mCustomColorAnalogHours = mSharedPref.getBoolean("own-color-analog-hours", false);
        mCustomColorAnalogMinutes = mSharedPref.getBoolean("own-color-analog-minutes", false);
        mCustomColorAnalogSeconds = mSharedPref.getBoolean("own-color-analog-seconds", false);
        mCheckBoxDigitalClockShow.setChecked( mSharedPref.getBoolean("show-digital", true) );
        mCheckBoxDateShow.setChecked( mSharedPref.getBoolean("show-date", true) );
        mEditTextDateFormat.setText( mSharedPref.getString("date-format", FsClockView.getDefaultDateFormat(this)) );
        mRadioButtonGregorianCalendar.setChecked( !mSharedPref.getBoolean("use-hijri", false) );
        mRadioButtonHijriCalendar.setChecked( mSharedPref.getBoolean("use-hijri", false) );
        mCheckBoxDigitalClockShowSeconds.setChecked( mSharedPref.getBoolean("show-seconds-digital", true) );
        mCheckBoxDigitalClock24Format.setChecked( mSharedPref.getBoolean("24hrs", true) );
        mColorAnalogFace = mSharedPref.getInt("color-analog-face", 0xffffffff);
        mColorAnalogHours = mSharedPref.getInt("color-analog-hours", 0xffffffff);
        mColorAnalogMinutes = mSharedPref.getInt("color-analog-minutes", 0xffffffff);
        mColorAnalogSeconds = mSharedPref.getInt("color-analog-seconds", 0xffffffff);
        mColorDigital = mSharedPref.getInt("color-digital", 0xffffffff);
        mColorBack = mSharedPref.getInt("color-back", 0xff000000);
        mBackStretch = mSharedPref.getBoolean("back-stretch", false);
        mCheckBoxShowAlarms.setChecked(mSharedPref.getBoolean("show-alarms", false));

        // init radio button behavior
        mRadioButtonGregorianCalendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRadioButtonHijriCalendar.setChecked(false);
            }
        });
        mRadioButtonHijriCalendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRadioButtonGregorianCalendar.setChecked(false);
            }
        });

        // load events
        Event[] eventsArray = mGson.fromJson(mSharedPref.getString("events",""), Event[].class);
        if(eventsArray != null) {
            mEvents = new ArrayList<>(Arrays.asList(eventsArray));
        }
        displayEvents();

        // init UI elements
        initColorPreview();
        initImageSpinner();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_settings_done:
                save();
                setResult(RESULT_OK);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case(PICK_CLOCK_FACE_REQUEST):
                if(resultCode == RESULT_OK) {
                    mStorage.processImage(StorageControl.FILENAME_CLOCK_FACE, data);
                } else {
                    mSpinnerDesignAnalogFace.setSelection(mStorage.existsImage(StorageControl.FILENAME_CLOCK_FACE) ? 1 : 0, false);
                }
                break;
            case(PICK_HOURS_HAND_REQUEST):
                if(resultCode == RESULT_OK) {
                    mStorage.processImage(StorageControl.FILENAME_HOURS_HAND, data);
                } else {
                    mSpinnerDesignAnalogHours.setSelection(mStorage.existsImage(StorageControl.FILENAME_HOURS_HAND) ? 1 : 0, false);
                }
                break;
            case(PICK_MINUTES_HAND_REQUEST):
                if(resultCode == RESULT_OK) {
                    mStorage.processImage(StorageControl.FILENAME_MINUTES_HAND, data);
                } else {
                    mSpinnerDesignAnalogMinutes.setSelection(mStorage.existsImage(StorageControl.FILENAME_MINUTES_HAND) ? 1 : 0, false);
                }
                break;
            case(PICK_SECONDS_HAND_REQUEST):
                if(resultCode == RESULT_OK) {
                    mStorage.processImage(StorageControl.FILENAME_SECONDS_HAND, data);
                } else {
                    mSpinnerDesignAnalogSeconds.setSelection(mStorage.existsImage(StorageControl.FILENAME_SECONDS_HAND) ? 1 : 0, false);
                }
                break;
            case(PICK_BACKGROUND_REQUEST):
                if(resultCode == RESULT_OK) {
                    mStorage.processImage(StorageControl.FILENAME_BACKGROUND_IMAGE, data);
                    mBackStretch = mSpinnerDesignBack.getSelectedItemPosition() == 1;
                } else {
                    mSpinnerDesignBack.setSelection(mStorage.existsImage(StorageControl.FILENAME_SECONDS_HAND) ? 1 : 0, false);
                }
                break;
        }
    }

    protected void enableDisableAllSettings(boolean state) {
        mCheckBoxKeepScreenOn.setEnabled(state);
        mCheckBoxShowBatteryInfo.setEnabled(state);
        mCheckBoxShowBatteryInfoWhenCharging.setEnabled(state);
        mCheckBoxBurnInPrevention.setEnabled(state);
        mCheckBoxForceLandscape.setEnabled(state);
        mCheckBoxDarkMode.setEnabled(state);
        mCheckBoxAnalogClockShow.setEnabled(state);
        mCheckBoxAnalogClockShowSeconds.setEnabled(state);
        mSpinnerDesignAnalogFace.setEnabled(state);
        mSpinnerDesignAnalogHours.setEnabled(state);
        mSpinnerDesignAnalogMinutes.setEnabled(state);
        mSpinnerDesignAnalogSeconds.setEnabled(state);
        mCheckBoxDigitalClockShow.setEnabled(state);
        mCheckBoxDateShow.setEnabled(state);
        mEditTextDateFormat.setEnabled(state);
        mRadioButtonGregorianCalendar.setEnabled(state);
        mRadioButtonHijriCalendar.setEnabled(state);
        mCheckBoxDigitalClockShowSeconds.setEnabled(state);
        mCheckBoxDigitalClock24Format.setEnabled(state);
        mColorChangerAnalogFace.setEnabled(state);
        mColorChangerAnalogHours.setEnabled(state);
        mColorChangerAnalogMinutes.setEnabled(state);
        mColorChangerAnalogSeconds.setEnabled(state);
        mColorChangerDigital.setEnabled(state);
        mColorChangerBack.setEnabled(state);
        mSpinnerDesignBack.setEnabled(state);
        mCheckBoxShowAlarms.setEnabled(state);
        mButtonNewEvent.setEnabled(state);
    }

    private void initImageSpinner() {
        String[] optionsAnalog = {
                getString(R.string.default_design),
                getString(R.string.custom_image)
        };
        String[] optionsBack = {
                getString(R.string.no_image),
                getString(R.string.custom_image_stretch),
                getString(R.string.custom_image_zoom)
        };
        ArrayAdapter dataAdapterAnalog = new ArrayAdapter(this, android.R.layout.simple_spinner_item, optionsAnalog);
        dataAdapterAnalog.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ArrayAdapter dataAdapterBack = new ArrayAdapter(this, android.R.layout.simple_spinner_item, optionsBack);
        dataAdapterBack.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mSpinnerDesignAnalogFace.setAdapter(dataAdapterAnalog);
        mSpinnerDesignAnalogFace.setSelection(mStorage.existsImage(StorageControl.FILENAME_CLOCK_FACE) ? 1 : 0, false);
        mSpinnerDesignAnalogHours.setAdapter(dataAdapterAnalog);
        mSpinnerDesignAnalogHours.setSelection(mStorage.existsImage(StorageControl.FILENAME_HOURS_HAND) ? 1 : 0, false);
        mSpinnerDesignAnalogMinutes.setAdapter(dataAdapterAnalog);
        mSpinnerDesignAnalogMinutes.setSelection(mStorage.existsImage(StorageControl.FILENAME_MINUTES_HAND) ? 1 : 0, false);
        mSpinnerDesignAnalogSeconds.setAdapter(dataAdapterAnalog);
        mSpinnerDesignAnalogSeconds.setSelection(mStorage.existsImage(StorageControl.FILENAME_SECONDS_HAND) ? 1 : 0, false);
        mSpinnerDesignBack.setAdapter(dataAdapterBack);
        mSpinnerDesignBack.setSelection(mStorage.existsImage(StorageControl.FILENAME_BACKGROUND_IMAGE) ? (mBackStretch ? 1 : 2) : 0, false);

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(adapterView.getId() == mSpinnerDesignAnalogFace.getId()) {
                    if(i == 0) mStorage.removeImage(StorageControl.FILENAME_CLOCK_FACE);
                    else chooseImage(PICK_CLOCK_FACE_REQUEST);
                } else if(adapterView.getId() == mSpinnerDesignAnalogHours.getId()) {
                    if(i == 0) mStorage.removeImage(StorageControl.FILENAME_HOURS_HAND);
                    else chooseImage(PICK_HOURS_HAND_REQUEST);
                } else if(adapterView.getId() == mSpinnerDesignAnalogMinutes.getId()) {
                    if(i == 0) mStorage.removeImage(StorageControl.FILENAME_MINUTES_HAND);
                    else chooseImage(PICK_MINUTES_HAND_REQUEST);
                } else if(adapterView.getId() == mSpinnerDesignAnalogSeconds.getId()) {
                    if(i == 0) mStorage.removeImage(StorageControl.FILENAME_SECONDS_HAND);
                    else chooseImage(PICK_SECONDS_HAND_REQUEST);
                } else if(adapterView.getId() == mSpinnerDesignBack.getId()) {
                    if(i == 0) mStorage.removeImage(StorageControl.FILENAME_BACKGROUND_IMAGE);
                    else chooseImage(PICK_BACKGROUND_REQUEST);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) { }
        };
        mSpinnerDesignAnalogFace.setOnItemSelectedListener(listener);
        mSpinnerDesignAnalogHours.setOnItemSelectedListener(listener);
        mSpinnerDesignAnalogMinutes.setOnItemSelectedListener(listener);
        mSpinnerDesignAnalogSeconds.setOnItemSelectedListener(listener);
        mSpinnerDesignBack.setOnItemSelectedListener(listener);
    }
    private void initColorPreview() {
        // analog color
        updateColorPreview(mColorAnalogFace, mColorPreviewAnalogFace, null);
        mColorChangerAnalogFace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showColorDialog(mCustomColorAnalogFace, mColorAnalogFace, new ColorDialogCallback() {
                    @Override
                    public void ok(boolean customColor, int red, int green, int blue, boolean applyForAll) {
                        if(applyForAll) {
                            applyColorForAllAnalog(customColor, Color.argb(0xff, red, green, blue));
                        } else {
                            mColorAnalogFace = Color.argb(0xff, red, green, blue);
                            mCustomColorAnalogFace = customColor;
                            updateColorPreview(mColorAnalogFace, mColorPreviewAnalogFace, null);
                        }
                    }
                });
            }
        });
        updateColorPreview(mColorAnalogHours, mColorPreviewAnalogHours, null);
        mColorChangerAnalogHours.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showColorDialog(mCustomColorAnalogHours, mColorAnalogHours, new ColorDialogCallback() {
                    @Override
                    public void ok(boolean customColor, int red, int green, int blue, boolean applyForAll) {
                        if(applyForAll) {
                            applyColorForAllAnalog(customColor, Color.argb(0xff, red, green, blue));
                        } else {
                            mColorAnalogHours = Color.argb(0xff, red, green, blue);
                            mCustomColorAnalogHours = customColor;
                            updateColorPreview(mColorAnalogHours, mColorPreviewAnalogHours, null);
                        }
                    }
                });
            }
        });
        updateColorPreview(mColorAnalogMinutes, mColorPreviewAnalogMinutes, null);
        mColorChangerAnalogMinutes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showColorDialog(mCustomColorAnalogMinutes, mColorAnalogMinutes, new ColorDialogCallback() {
                    @Override
                    public void ok(boolean customColor, int red, int green, int blue, boolean applyForAll) {
                        if(applyForAll) {
                            applyColorForAllAnalog(customColor, Color.argb(0xff, red, green, blue));
                        } else {
                            mColorAnalogMinutes = Color.argb(0xff, red, green, blue);
                            mCustomColorAnalogMinutes = customColor;
                            updateColorPreview(mColorAnalogMinutes, mColorPreviewAnalogMinutes, null);
                        }
                    }
                });
            }
        });
        updateColorPreview(mColorAnalogSeconds, mColorPreviewAnalogSeconds, null);
        mColorChangerAnalogSeconds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showColorDialog(mCustomColorAnalogSeconds, mColorAnalogSeconds, new ColorDialogCallback() {
                    @Override
                    public void ok(boolean customColor, int red, int green, int blue, boolean applyForAll) {
                        if(applyForAll) {
                            applyColorForAllAnalog(customColor, Color.argb(0xff, red, green, blue));
                        } else {
                            mColorAnalogSeconds = Color.argb(0xff, red, green, blue);
                            mCustomColorAnalogSeconds = customColor;
                            updateColorPreview(mColorAnalogSeconds, mColorPreviewAnalogSeconds, null);
                        }
                    }
                });
            }
        });

        // digital color
        updateColorPreview(mColorDigital, mColorPreviewDigital, null);
        mColorChangerDigital.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showColorDialog(null, mColorDigital, new ColorDialogCallback() {
                    @Override
                    public void ok(boolean customColor, int red, int green, int blue, boolean applyForAll) {
                        mColorDigital = Color.argb(0xff, red, green, blue);
                        updateColorPreview(mColorDigital, mColorPreviewDigital, null);
                    }
                });
            }
        });

        // background color
        updateColorPreview(mColorBack, mColorPreviewBack, null);
        mColorChangerBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showColorDialog(null, mColorBack, new ColorDialogCallback() {
                    @Override
                    public void ok(boolean customColor, int red, int green, int blue, boolean applyForAll) {
                        mColorBack = Color.argb(0xff, red, green, blue);
                        updateColorPreview(mColorBack, mColorPreviewBack, null);
                    }
                });
            }
        });
    }
    private void applyColorForAllAnalog(boolean customColor, int color) {
        mColorAnalogFace = color;
        mCustomColorAnalogFace = customColor;
        updateColorPreview(mColorAnalogFace, mColorPreviewAnalogFace, null);
        mColorAnalogHours = color;
        mCustomColorAnalogHours = customColor;
        updateColorPreview(mColorAnalogHours, mColorPreviewAnalogHours, null);
        mColorAnalogMinutes = color;
        mCustomColorAnalogMinutes = customColor;
        updateColorPreview(mColorAnalogMinutes, mColorPreviewAnalogMinutes, null);
        mColorAnalogSeconds = color;
        mCustomColorAnalogSeconds = customColor;
        updateColorPreview(mColorAnalogSeconds, mColorPreviewAnalogSeconds, null);
    }
    private void updateColorPreview(int color, View previewView, EditText hexTextBox) {
        previewView.setBackgroundColor(Color.argb(0xff, Color.red(color), Color.green(color), Color.blue(color)));
        if(hexTextBox != null) hexTextBox.setText(String.format("#%06X", (0xFFFFFF & color)));
    }
    interface ColorDialogCallback {
        void ok(boolean customColor, int red, int green, int blue, boolean applyForAll);
    }
    private void showColorDialog(Boolean customColor, int initialColor, final ColorDialogCallback colorDialogFinished) {
        final Dialog ad = new Dialog(this);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_color);
        final CheckBox checkBoxCustomColor = ad.findViewById(R.id.checkBoxCustomColor);
        final EditText editTextColorHex = ad.findViewById(R.id.editTextColorHex);
        final SeekBar seekBarRed = ad.findViewById(R.id.seekBarRed);
        final SeekBar seekBarGreen = ad.findViewById(R.id.seekBarGreen);
        final SeekBar seekBarBlue = ad.findViewById(R.id.seekBarBlue);
        final View colorPreview = ad.findViewById(R.id.viewColorPreview);
        final Button buttonOkForAll = ad.findViewById(R.id.buttonOkForAll);
        if(customColor == null) {
            checkBoxCustomColor.setVisibility(View.GONE);
            buttonOkForAll.setVisibility(View.GONE);
        } else {
            checkBoxCustomColor.setChecked(customColor);
            if(!customColor) {
                seekBarRed.setEnabled(false);
                seekBarGreen.setEnabled(false);
                seekBarBlue.setEnabled(false);
                editTextColorHex.setEnabled(false);
            }
        }
        checkBoxCustomColor.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                seekBarRed.setEnabled(b);
                seekBarGreen.setEnabled(b);
                seekBarBlue.setEnabled(b);
                editTextColorHex.setEnabled(b);
            }
        });
        seekBarRed.setProgress(Color.red(initialColor));
        seekBarGreen.setProgress(Color.green(initialColor));
        seekBarBlue.setProgress(Color.blue(initialColor));
        seekBarRed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateColorPreview(Color.argb(0xff, seekBarRed.getProgress(), seekBarGreen.getProgress(), seekBarBlue.getProgress()), colorPreview, editTextColorHex);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        seekBarGreen.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateColorPreview(Color.argb(0xff, seekBarRed.getProgress(), seekBarGreen.getProgress(), seekBarBlue.getProgress()), colorPreview, editTextColorHex);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        seekBarBlue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateColorPreview(Color.argb(0xff, seekBarRed.getProgress(), seekBarGreen.getProgress(), seekBarBlue.getProgress()), colorPreview, editTextColorHex);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        editTextColorHex.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                try {
                    int newColor = Color.parseColor(charSequence.toString());
                    seekBarRed.setProgress(Color.red(newColor));
                    seekBarGreen.setProgress(Color.green(newColor));
                    seekBarBlue.setProgress(Color.blue(newColor));
                    //updateColorPreview(Color.argb(0xff, seekBarRed.getProgress(), seekBarGreen.getProgress(), seekBarBlue.getProgress()), colorPreview, null);
                } catch(Exception ignored) { }
            }
            @Override
            public void afterTextChanged(Editable editable) { }
        });
        updateColorPreview(Color.argb(0xff, seekBarRed.getProgress(), seekBarGreen.getProgress(), seekBarBlue.getProgress()), colorPreview, editTextColorHex);
        ad.show();
        ad.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ad.findViewById(R.id.buttonOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(colorDialogFinished != null) {
                    colorDialogFinished.ok(checkBoxCustomColor.isChecked(), seekBarRed.getProgress(), seekBarGreen.getProgress(), seekBarBlue.getProgress(), false);
                }
                ad.dismiss();
            }
        });
        ad.findViewById(R.id.buttonOkForAll).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(colorDialogFinished != null) {
                    colorDialogFinished.ok(checkBoxCustomColor.isChecked(), seekBarRed.getProgress(), seekBarGreen.getProgress(), seekBarBlue.getProgress(), true);
                }
                ad.dismiss();
            }
        });
    }

    private void save() {
        SharedPreferences.Editor editor = mSharedPref.edit();

        editor.putBoolean("keep-screen-on", mCheckBoxKeepScreenOn.isChecked());
        editor.putBoolean("show-battery-info", mCheckBoxShowBatteryInfo.isChecked());
        editor.putBoolean("show-battery-info-when-charging", mCheckBoxShowBatteryInfoWhenCharging.isChecked());
        editor.putBoolean("burn-in-prevention", mCheckBoxBurnInPrevention.isChecked());
        editor.putBoolean("force-landscape", mCheckBoxForceLandscape.isChecked());
        editor.putBoolean("dark-mode", mCheckBoxDarkMode.isChecked());
        editor.putBoolean("show-analog", mCheckBoxAnalogClockShow.isChecked());
        editor.putBoolean("own-color-analog-clock-face", mCustomColorAnalogFace);
        editor.putBoolean("own-color-analog-hours", mCustomColorAnalogHours);
        editor.putBoolean("own-color-analog-minutes", mCustomColorAnalogMinutes);
        editor.putBoolean("own-color-analog-seconds", mCustomColorAnalogSeconds);
        editor.putBoolean("show-digital", mCheckBoxDigitalClockShow.isChecked());
        editor.putBoolean("show-date", mCheckBoxDateShow.isChecked());
        editor.putString("date-format", mEditTextDateFormat.getText().toString());
        editor.putBoolean("use-hijri", mRadioButtonHijriCalendar.isChecked());
        editor.putBoolean("show-seconds-analog", mCheckBoxAnalogClockShowSeconds.isChecked());
        editor.putBoolean("show-seconds-digital", mCheckBoxDigitalClockShowSeconds.isChecked());
        editor.putBoolean("24hrs", mCheckBoxDigitalClock24Format.isChecked());
        editor.putString("events", mGson.toJson(mEvents.toArray()));
        editor.putInt("color-analog-face", mColorAnalogFace);
        editor.putInt("color-analog-hours", mColorAnalogHours);
        editor.putInt("color-analog-minutes", mColorAnalogMinutes);
        editor.putInt("color-analog-seconds", mColorAnalogSeconds);
        editor.putInt("color-digital", mColorDigital);
        editor.putInt("color-back", mColorBack);
        editor.putBoolean("back-stretch", mBackStretch);
        editor.putBoolean("show-alarms", mCheckBoxShowAlarms.isChecked());

        editor.apply();
    }

    private void chooseImage(int requestId) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), requestId);
        if(requestId != PICK_BACKGROUND_REQUEST) {
            Toast.makeText(this, getString(R.string.own_images_note), Toast.LENGTH_LONG).show();
        }
    }

    private void displayEvents() {
        LinearLayout llEvents = findViewById(R.id.linearLayoutSettingsEvents);
        llEvents.removeAllViews();
        for(final Event e : mEvents) {
            Button b = new Button(this);
            b.setAllCaps(false);
            b.setText(e.toString());
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickEditEvent(e);
                }
            });
            llEvents.addView(b);
        }
    }
    public void onClickNewEvent(View v) {
        final Dialog ad = new Dialog(this);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_event);
        //ad.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        ((NumberPicker) ad.findViewById(R.id.numberPickerHour)).setMaxValue(23);
        ((NumberPicker) ad.findViewById(R.id.numberPickerMinute)).setMaxValue(59);
        ad.findViewById(R.id.buttonNewEventRemove).setVisibility(View.GONE);
        ad.show();
        ad.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ad.findViewById(R.id.buttonNewEventOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEvents.add(new Event(
                        ((NumberPicker) ad.findViewById(R.id.numberPickerHour)).getValue(),
                        ((NumberPicker) ad.findViewById(R.id.numberPickerMinute)).getValue(),
                        ((EditText) ad.findViewById(R.id.editTextTitleNewEvent)).getText().toString(),
                        ((EditText) ad.findViewById(R.id.editTextSpeakNewEvent)).getText().toString(),
                        ((CheckBox) ad.findViewById(R.id.checkBoxAlarmNewEvent)).isChecked(),
                        ((CheckBox) ad.findViewById(R.id.checkBoxDisplayNewEvent)).isChecked(),
                        ((CheckBox) ad.findViewById(R.id.checkBoxShowUntilConfirmed)).isChecked() ? 0 : 15
                ));
                ad.dismiss();
                displayEvents();
            }
        });
    }
    public void onClickEditEvent(final Event e) {
        final Dialog ad = new Dialog(this);
        ad.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ad.setContentView(R.layout.dialog_event);
        //ad.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        ((NumberPicker) ad.findViewById(R.id.numberPickerHour)).setMaxValue(23);
        ((NumberPicker) ad.findViewById(R.id.numberPickerMinute)).setMaxValue(59);
        ((NumberPicker) ad.findViewById(R.id.numberPickerHour)).setValue(e.triggerHour);
        ((NumberPicker) ad.findViewById(R.id.numberPickerMinute)).setValue(e.triggerMinute);
        ((EditText) ad.findViewById(R.id.editTextTitleNewEvent)).setText(e.title);
        ((EditText) ad.findViewById(R.id.editTextSpeakNewEvent)).setText(e.speakText);
        ((CheckBox) ad.findViewById(R.id.checkBoxAlarmNewEvent)).setChecked(e.playAlarm);
        ((CheckBox) ad.findViewById(R.id.checkBoxDisplayNewEvent)).setChecked(e.showOnScreen);
        ((CheckBox) ad.findViewById(R.id.checkBoxShowUntilConfirmed)).setChecked(e.hideAfter == 0);
        ad.show();
        ad.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ad.findViewById(R.id.buttonNewEventOK).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                e.triggerHour = ((NumberPicker) ad.findViewById(R.id.numberPickerHour)).getValue();
                e.triggerMinute = ((NumberPicker) ad.findViewById(R.id.numberPickerMinute)).getValue();
                e.title = ((EditText) ad.findViewById(R.id.editTextTitleNewEvent)).getText().toString();
                e.speakText = ((EditText) ad.findViewById(R.id.editTextSpeakNewEvent)).getText().toString();
                e.playAlarm = ((CheckBox) ad.findViewById(R.id.checkBoxAlarmNewEvent)).isChecked();
                e.showOnScreen = ((CheckBox) ad.findViewById(R.id.checkBoxDisplayNewEvent)).isChecked();
                e.hideAfter = ((CheckBox) ad.findViewById(R.id.checkBoxShowUntilConfirmed)).isChecked() ? 0 : 15;
                ad.dismiss();
                displayEvents();
            }
        });
        ad.findViewById(R.id.buttonNewEventRemove).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEvents.remove(e);
                ad.dismiss();
                displayEvents();
            }
        });
    }

    public void onClickDreamSettings(View v) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            try {
                startActivity(new Intent(Settings.ACTION_DREAM_SETTINGS));
            } catch(ActivityNotFoundException e) {
                infoDialog(null, getString(R.string.screensaver_not_supported));
            }
        }
    }

    public void onClickDateFormatHelp(View v) {
        StringBuilder sb = new StringBuilder();
        for(String s : getString(R.string.date_format_placeholders_help).split("\n")) {
            sb.append(s.trim()).append("\n");
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.date_format_placeholders_help_title));
        builder.setMessage(sb.toString());
        builder.setPositiveButton(getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.setNeutralButton(getString(R.string.reset_default),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mEditTextDateFormat.setText(FsClockView.getDefaultDateFormat(getBaseContext()));
                    }
                });
        builder.setCancelable(true);
        AlertDialog dialog = builder.create();
        dialog.show();
        TextView messageView = dialog.findViewById(android.R.id.message);
        messageView.setTypeface(Typeface.MONOSPACE);
    }
    public void onClickCustomAnalogImagesHelp(View v) {
        infoDialog("", getString(R.string.own_images_note));
    }

    public final static String APPID_CUSTOMERDB    = "de.georgsieber.customerdb";
    public final static String APPID_REMOTEPOINTER = "systems.sieber.remotespotlight";
    public final static String APPID_BALLBREAK     = "de.georgsieber.ballbreak";
    public final static String URL_OCO             = "https://github.com/schorschii/oco-server";
    public final static String URL_MASTERPLAN      = "https://github.com/schorschii/masterplan";

    public void onClickGithub(View v) {
        openBrowser(getString(R.string.project_website));
    }
    public void onClickCustomerDatabaseApp(View v) {
        openPlayStore(APPID_CUSTOMERDB);
    }
    public void onClickRemotePointerApp(View v) {
        openPlayStore(APPID_REMOTEPOINTER);
    }
    public void onClickBallBreakApp(View v) {
        openPlayStore(APPID_BALLBREAK);
    }
    public void onClickOco(View v) {
        openBrowser(URL_OCO);
    }
    public void onClickMasterplan(View v) {
        openBrowser(URL_MASTERPLAN);
    }
    void openBrowser(String url) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        } catch(SecurityException ignored) {
            infoDialog(getString(R.string.no_web_browser_found), url);
        } catch(ActivityNotFoundException ignored) {
            infoDialog(getString(R.string.no_web_browser_found), url);
        }
    }
    private void openPlayStore(String appId) {
        try {
            this.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appId)));
        } catch(android.content.ActivityNotFoundException anfe) {
            this.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appId)));
        }
    }

    public void onClickAllowSystemCalendarAccess(View v) {
        int permissionCheckResult = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR);
        if(permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
            infoDialog(null, getString(R.string.access_already_granted));
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR}, PERMISSION_REQUEST);
        }
    }

    private void infoDialog(String title, String text) {
        final AlertDialog.Builder dlg = new AlertDialog.Builder(this);
        if(title != null) dlg.setTitle(title);
        if(text != null) dlg.setMessage(text);
        dlg.setPositiveButton(getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        dlg.setCancelable(true);
        dlg.create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PERMISSION_REQUEST) {
            for(int i = 0, len = permissions.length; i < len; i++) {
                String permission = permissions[i];
                if(grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        boolean showRationale = shouldShowRequestPermissionRationale(permission);
                        if(!showRationale) {
                            // user also CHECKED "never ask again"
                            infoDialog(null, getString(R.string.access_denied_by_user));
                        }
                    }
                }
            }
        }
    }

    public static boolean isHighContrastTextEnabled(Context context) {
        if(context != null) {
            AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
            Method m = null;
            if(am != null) {
                try {
                    m = am.getClass().getMethod("isHighTextContrastEnabled", null);
                } catch(NoSuchMethodException ignored) { }
            }
            Object result;
            if(m != null) {
                try {
                    result = m.invoke(am, (Object[]) null);
                    if(result instanceof Boolean) {
                        return (Boolean) result;
                    }
                } catch(Exception ignored) { }
            }
        }
        return false;
    }

}
