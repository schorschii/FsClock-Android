package systems.sieber.fsclock;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.provider.CalendarContract;
import android.speech.tts.TextToSpeech;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.gson.Gson;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class FsClockView extends FrameLayout {

    final static int BURN_IN_PREVENTION_DEVIATION = 20; /*px*/
    final static int BURN_IN_PREVENTION_CHANGE = 100000; /*ms*/
    boolean mBurnInPrevention = false;

    Random mRand = new Random();

    AppCompatActivity mActivity;

    SharedPreferences mSharedPref;

    View mRootView;
    View mMainView;
    Float mMainViewDefaultX;
    Float mMainViewDefaultY;
    ImageView mBackgroundImage;
    View mBatteryView;
    TextView mBatteryText;
    ImageView mBatteryImage;
    View mAlarmView;
    TextView mAlarmText;
    ImageView mAlarmImage;
    DigitalClockView mDigitalClock;
    TextView mDateText;
    TextView mTextViewEvents;
    ImageView mClockFace;
    ImageView mSecondsHand;
    ImageView mMinutesHand;
    ImageView mHoursHand;

    Timer mTimerAnalogClock;
    Timer mTimerCalendarUpdate;
    Timer mTimerCheckEvent;
    Timer mTimerBurnInPreventionRotation;

    TextToSpeech mTts;
    Event[] mEvents;
    boolean mFormat24hrs;
    boolean mShowAnalog;
    boolean mSmoothSeconds;
    boolean mShowDigital;
    boolean mShowDate;
    boolean mShowAlarms;

    public FsClockView(Context c, AttributeSet attrs) {
        super(c, attrs);
        commonInit(c);
    }
    private void commonInit(Context c) {
        inflate(getContext(), R.layout.view_fsclock, this);

        // init settings
        mSharedPref = c.getSharedPreferences(SettingsActivity.SHARED_PREF_DOMAIN, Context.MODE_PRIVATE);

        // find views
        mRootView = findViewById(R.id.fsclockRootView);
        mMainView = findViewById(R.id.linearLayoutMain);
        mBackgroundImage = findViewById(R.id.imageViewBackground);
        mDigitalClock = findViewById(R.id.digitalClock);
        mDateText = findViewById(R.id.textViewDate);
        mTextViewEvents = findViewById(R.id.textViewEvents);
        mClockFace = findViewById(R.id.imageViewClockFace);
        mHoursHand = findViewById(R.id.imageViewClockHoursHand);
        mMinutesHand = findViewById(R.id.imageViewClockMinutesHand);
        mSecondsHand = findViewById(R.id.imageViewClockSecondsHand);
        mBatteryView = findViewById(R.id.linearLayoutBattery);
        mBatteryText = findViewById(R.id.textViewBattery);
        mBatteryImage = findViewById(R.id.imageViewBattery);
        mBatteryImage.setImageResource(R.drawable.ic_battery_full_white_24dp);
        mAlarmView = findViewById(R.id.linearLayoutAlarm);
        mAlarmText = findViewById(R.id.textViewAlarm);
        mAlarmImage = findViewById(R.id.imageViewAlarm);
        mAlarmImage.setImageResource(R.drawable.ic_alarm_white_24dp);

        // init font
        final Typeface fontLed = ResourcesCompat.getFont(c, R.font.dseg7classic_regular);
        final Typeface fontDate = ResourcesCompat.getFont(c, R.font.cairo_regular);
        mDigitalClock.setTypeface(fontLed);
        mDateText.setTypeface(fontDate);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // auto-calc text size - must be necessarily set programmatically for screensaver mode!
            mDateText.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        } else {
            // calc text sizes manually on older Android versions
            updateClock();
            WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            final Point size = new Point();
            display.getSize(size);
            getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    int dateContainerWidth = mDateText.getWidth();
                    String dateText = mDateText.getText().toString();
                    for(int i = 120; i >= 20; i-=2) {
                        int textWidth = getTextWidth(getContext(), dateText, i, size, fontDate);
                        if(textWidth < dateContainerWidth) {
                            mDateText.setTextSize(i);
                            Log.i("CALC_TSIZE_DATE", i+" => "+textWidth+" (max "+dateContainerWidth+") @ "+dateText);
                            break;
                        }
                    }
                }
            });
        }

        // init layout listener
        mMainView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        // layout has happened here
                        mMainViewDefaultX = mMainView.getX();
                        mMainViewDefaultY = mMainView.getY();
                        mMainView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });
    }

    public static int getTextWidth(Context context, CharSequence text, int textSize, Point deviceSize, Typeface typeface) {
        TextView textView = new TextView(context);
        textView.setTypeface(typeface);
        textView.setText(text, TextView.BufferType.SPANNABLE);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(deviceSize.x, View.MeasureSpec.AT_MOST);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(deviceSize.y, View.MeasureSpec.UNSPECIFIED);
        textView.measure(widthMeasureSpec, heightMeasureSpec);
        return textView.getMeasuredWidth();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        // init text to speech
        mTts = new TextToSpeech(getContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.SUCCESS) {
                    mTts = null;
                }
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        // destroy tts service connection
        if(mTts != null) {
            mTts.stop();
            mTts.shutdown();
        }
    }

    static String getDefaultDateFormat(Context c) {
        final SimpleDateFormat sdfSystem = (SimpleDateFormat) DateFormat.getDateFormat(c);
        String strDatePattern = "EEEE, " + sdfSystem.toLocalizedPattern(); // day of week followed by localized date
        if(!strDatePattern.contains("yyyy")) {
            // devices with API level 17 or below return date format already with yyyy,
            // for all other devices we manually replace yy with yyyy
            strDatePattern = strDatePattern.replace("yy", "yyyy");
        }
        return strDatePattern;
    }

    @SuppressLint("SimpleDateFormat")
    private void updateClock() {
        final Calendar cal = Calendar.getInstance();

        if(mShowDate) {
            try {
                String strDatePattern = mSharedPref.getString("date-format", getDefaultDateFormat(getContext()));
                final SimpleDateFormat sdfDate = new SimpleDateFormat(strDatePattern, Locale.getDefault());
                mDateText.setText(sdfDate.format(cal.getTime()));
            } catch(IllegalArgumentException ignored) {
                mDateText.setText("---");
            }
        }

        if(mShowDigital) {
            final SimpleDateFormat sdfTime = new SimpleDateFormat(mFormat24hrs ? "HH:mm" : "hh:mm");
            final SimpleDateFormat sdfSeconds = new SimpleDateFormat("ss");
            mDigitalClock.setText(sdfTime.format(cal.getTime()), sdfSeconds.format(cal.getTime()));
        }

        if(mShowAnalog) {
            float secRotation = (cal.get(Calendar.SECOND) + ((float)cal.get(Calendar.MILLISECOND)/1000)) * 360 / 60;
            float minRotation = (cal.get(Calendar.MINUTE) + ((float)cal.get(Calendar.SECOND)/60)) * 360 / 60;
            float hrsRotation = (cal.get(Calendar.HOUR) + ((float)cal.get(Calendar.MINUTE)/60)) * 360 / 12;
            mSecondsHand.setRotation(secRotation);
            mMinutesHand.setRotation(minRotation);
            mHoursHand.setRotation(hrsRotation);
        }
    }
    private void startTimer() {
        TimerTask taskAnalogClock = new TimerTask() {
            @SuppressLint("SimpleDateFormat")
            @Override
            public void run() {
                post(new Runnable() {
                    @Override
                    public void run() {
                        updateClock();
                    }
                });
            }
        };
        TimerTask taskCalendarUpdate = new TimerTask() {
            @Override
            public void run() {
                post(new Runnable() {
                    @Override
                    public void run() {
                        updateEventView();
                    }
                });
            }
        };
        TimerTask taskCheckEvent = new TimerTask() {
            @Override
            public void run() {
                post(new Runnable() {
                    final Calendar cal = Calendar.getInstance();
                    @Override
                    public void run() {
                        if(mEvents != null) {
                            for(Event e : mEvents) {
                                if(cal.get(Calendar.HOUR_OF_DAY) == e.triggerHour
                                        && cal.get(Calendar.MINUTE) == e.triggerMinute
                                        && cal.get(Calendar.SECOND) == 0) {
                                    doEventStuff(e);
                                }
                            }
                        }
                    }
                });
            }
        };
        TimerTask taskBurnInAvoidRotation = new TimerTask() {
            @Override
            public void run() {
                post(new Runnable() {
                    @Override
                    public void run() {
                        if(mMainViewDefaultX == null || mMainViewDefaultY == null) {
                            return;
                        }
                        if(mBurnInPrevention) {
                            mMainView.animate()
                                    .x(mMainViewDefaultX + mRand.nextInt(BURN_IN_PREVENTION_DEVIATION*2) - BURN_IN_PREVENTION_DEVIATION)
                                    .y(mMainViewDefaultY + mRand.nextInt(BURN_IN_PREVENTION_DEVIATION*2) - BURN_IN_PREVENTION_DEVIATION)
                                    .setDuration(1000)
                                    .start();
                        } else {
                            // reset position after setting changed
                            mMainView.animate()
                                    .x(mMainViewDefaultX)
                                    .y(mMainViewDefaultY)
                                    .setDuration(1000)
                                    .start();
                        }
                    }
                });
            }
        };

        mTimerAnalogClock = new Timer(false);
        mTimerCalendarUpdate = new Timer(false);
        mTimerCheckEvent = new Timer(false);
        mTimerBurnInPreventionRotation = new Timer(false);
        mTimerAnalogClock.schedule(taskAnalogClock, 0, mSmoothSeconds ? 100 : 1000);
        mTimerCalendarUpdate.schedule(taskCalendarUpdate, 0, 10000);
        mTimerCheckEvent.schedule(taskCheckEvent, 0, 1000);
        mTimerBurnInPreventionRotation.schedule(taskBurnInAvoidRotation, 1000, BURN_IN_PREVENTION_CHANGE);

        // instant refresh so that the user does not see "00:00:00"
        if(!mSmoothSeconds) {
            updateClock();
        }
    }

    void loadSettings() {
        if(mActivity != null) {
            if(mSharedPref.getBoolean("keep-screen-on", true)) {
                mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                Log.i("SCREEN", "Keep ON");
            } else {
                mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                Log.i("SCREEN", "Keep OFF");
            }
        }

        WindowManager.LayoutParams layout = null;
        if(mActivity != null) layout = mActivity.getWindow().getAttributes();
        if(mSharedPref.getBoolean("dark-mode", false)) {
            // in normal mode, we can set the display brightness to lowest (not possible in screensaver mode)
            if(layout != null) {
                layout.screenBrightness = 0;
                mActivity.getWindow().setAttributes(layout);
                Log.i("SCREEN", "Dark Mode enabled: set display brightness to lowest");
            }
            // dim the colors of all UI elements
            dimClockView(mRootView, true);
        } else {
            if(layout != null) {
                layout.screenBrightness = -1;
                mActivity.getWindow().setAttributes(layout);
            }
            dimClockView(mRootView, false);
        }

        mBurnInPrevention = mSharedPref.getBoolean("burn-in-prevention", mBurnInPrevention);

        Gson gson = new Gson();
        mEvents = gson.fromJson(mSharedPref.getString("events",""), Event[].class);
        mShowAlarms = mSharedPref.getBoolean("show-alarms", false);

        mFormat24hrs = mSharedPref.getBoolean("24hrs", true);

        if(mSharedPref.getBoolean("show-analog", true)) {
            mShowAnalog = true;
            findViewById(R.id.analogClockContainer).setVisibility(View.VISIBLE);
        } else {
            mShowAnalog = false;
            findViewById(R.id.analogClockContainer).setVisibility(View.GONE);
        }

        if(mSharedPref.getBoolean("show-digital", true)) {
            mShowDigital = true;
            mDigitalClock.setVisibility(View.VISIBLE);
        } else {
            mShowDigital = false;
            mDigitalClock.setVisibility(View.GONE);
        }

        if(mSharedPref.getBoolean("show-date", true)) {
            mShowDate = true;
            mDateText.setVisibility(View.VISIBLE);
        } else {
            mShowDate = false;
            mDateText.setVisibility(View.GONE);
        }

        if(!mSharedPref.getBoolean("show-digital", true) && !mSharedPref.getBoolean("show-date", true))
            findViewById(R.id.digitalClockAndDateContainer).setVisibility(GONE);
        else
            findViewById(R.id.digitalClockAndDateContainer).setVisibility(VISIBLE);

        if(mSharedPref.getBoolean("show-seconds-analog", true))
            findViewById(R.id.imageViewClockSecondsHand).setVisibility(View.VISIBLE);
        else
            findViewById(R.id.imageViewClockSecondsHand).setVisibility(View.GONE);

        if(mSharedPref.getBoolean("show-seconds-digital", true))
            mDigitalClock.setShowSec(true);
        else
            mDigitalClock.setShowSec(false);

        mSmoothSeconds = mSharedPref.getBoolean("show-seconds-analog", true)
            && mSharedPref.getBoolean("show-analog", true);

        // init custom digital color
        int colorDigital = mSharedPref.getInt("color-digital", 0xffffffff);
        mDigitalClock.setColor(colorDigital);
        mDateText.setTextColor(colorDigital);
        mTextViewEvents.setTextColor(colorDigital);
        mBatteryText.setTextColor(colorDigital);
        mBatteryImage.setColorFilter(colorDigital, PorterDuff.Mode.SRC_ATOP);
        mAlarmText.setTextColor(colorDigital);
        mAlarmImage.setColorFilter(colorDigital, PorterDuff.Mode.SRC_ATOP);

        // init custom analog color
        if(mSharedPref.getBoolean("own-color-analog-clock-face", false)) {
            mClockFace.setColorFilter(mSharedPref.getInt("color-analog-face", 0xffffffff), PorterDuff.Mode.SRC_ATOP);
        } else {
            mClockFace.clearColorFilter();
        }
        if(mSharedPref.getBoolean("own-color-analog-hours", false)) {
            mHoursHand.setColorFilter(mSharedPref.getInt("color-analog-hours", 0xffffffff), PorterDuff.Mode.SRC_ATOP);
        } else {
            mHoursHand.clearColorFilter();
        }
        if(mSharedPref.getBoolean("own-color-analog-minutes", false)) {
            mMinutesHand.setColorFilter(mSharedPref.getInt("color-analog-minutes", 0xffffffff), PorterDuff.Mode.SRC_ATOP);
        } else {
            mMinutesHand.clearColorFilter();
        }
        if(mSharedPref.getBoolean("own-color-analog-seconds", false)) {
            mSecondsHand.setColorFilter(mSharedPref.getInt("color-analog-seconds", 0xffffffff), PorterDuff.Mode.SRC_ATOP);
        } else {
            mSecondsHand.clearColorFilter();
        }

        // init custom background color
        mRootView.setBackgroundColor(mSharedPref.getInt("color-back", 0xff000000));

        // init custom background image
        StorageControl sc = new StorageControl(getContext());
        mBackgroundImage.setImageBitmap(null);
        File img = sc.getStorage(StorageControl.FILENAME_BACKGROUND_IMAGE);
        if(img.exists()) {
            try {
                Bitmap myBitmap = BitmapFactory.decodeFile(img.getAbsolutePath());
                mBackgroundImage.setImageBitmap(myBitmap);
                if(mSharedPref.getBoolean("back-stretch", false)) {
                    mBackgroundImage.setScaleType(ImageView.ScaleType.FIT_XY);
                } else {
                    mBackgroundImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                }
            } catch(Exception ignored) {
                Toast.makeText(getContext(), "Image corrupted or too large", Toast.LENGTH_SHORT).show();
            }
        }

        // init (custom) analog clock images
        mClockFace.setImageResource(R.drawable.ic_bg);
        mHoursHand.setImageResource(R.drawable.ic_h);
        mMinutesHand.setImageResource(R.drawable.ic_m);
        mSecondsHand.setImageResource(R.drawable.ic_s);

        img = sc.getStorage(StorageControl.FILENAME_CLOCK_FACE);
        if(img.exists()) {
            try {
                Bitmap myBitmap = BitmapFactory.decodeFile(img.getAbsolutePath());
                mClockFace.setImageBitmap(myBitmap);
            } catch(Exception ignored) {
                Toast.makeText(getContext(), "Image corrupted or too large", Toast.LENGTH_SHORT).show();
            }
        }

        img = sc.getStorage(StorageControl.FILENAME_HOURS_HAND);
        if(img.exists()) {
            try {
                Bitmap myBitmap = BitmapFactory.decodeFile(img.getAbsolutePath());
                mHoursHand.setImageBitmap(myBitmap);
            } catch(Exception ignored) {
                Toast.makeText(getContext(), "Image corrupted or too large", Toast.LENGTH_SHORT).show();
            }
        }

        img = sc.getStorage(StorageControl.FILENAME_MINUTES_HAND);
        if(img.exists()) {
            try {
                Bitmap myBitmap = BitmapFactory.decodeFile(img.getAbsolutePath());
                mMinutesHand.setImageBitmap(myBitmap);
            } catch(Exception ignored) {
                Toast.makeText(getContext(), "Image corrupted or too large", Toast.LENGTH_SHORT).show();
            }
        }

        img = sc.getStorage(StorageControl.FILENAME_SECONDS_HAND);
        if(img.exists()) {
            try {
                Bitmap myBitmap = BitmapFactory.decodeFile(img.getAbsolutePath());
                mSecondsHand.setImageBitmap(myBitmap);
            } catch(Exception ignored) {
                Toast.makeText(getContext(), "Image corrupted or too large", Toast.LENGTH_SHORT).show();
            }
        }
    }

    void dimClockView(View clockView, boolean enabled) {
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setColorFilter(
                new PorterDuffColorFilter(enabled ? 0x40FFFFFF : 0xFFFFFFFF, PorterDuff.Mode.MULTIPLY)
        );
        clockView.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
    }

    void doEventStuff(Event e) {
        if(e.playAlarm) {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            final Ringtone r = RingtoneManager.getRingtone(getContext(), notification);
            r.play();
            TimerTask taskEndAlarm = new TimerTask() {
                @Override
                public void run() {
                    if(r.isPlaying()) r.stop();
                }
            };
            new Timer(false).schedule(taskEndAlarm, 10000);
        }
        if(e.speakText != null && !e.speakText.trim().equals("")) {
            speak(e.speakText);
        }
    }

    private void speak(String text) {
        if(mTts != null) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mTts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                mTts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }

    @SuppressLint("SetTextI18n")
    void updateBattery(int plugged, int level) {
        if((plugged == 0 && mSharedPref.getBoolean("show-battery-info", true))
        || (plugged != 0 && mSharedPref.getBoolean("show-battery-info-when-charging", false))) {
            mBatteryText.setText(level + "%");
            mBatteryView.setVisibility(View.VISIBLE);
            if(plugged == 0) {
                if(level < 5) {
                    mBatteryImage.setImageResource(R.drawable.ic_battery_0_bar_white_24dp);
                } else if(level < 10) {
                    mBatteryImage.setImageResource(R.drawable.ic_battery_1_bar_white_24dp);
                } else if(level < 25) {
                    mBatteryImage.setImageResource(R.drawable.ic_battery_2_bar_white_24dp);
                } else if(level < 40) {
                    mBatteryImage.setImageResource(R.drawable.ic_battery_3_bar_white_24dp);
                } else if(level < 55) {
                    mBatteryImage.setImageResource(R.drawable.ic_battery_4_bar_white_24dp);
                } else if(level < 70) {
                    mBatteryImage.setImageResource(R.drawable.ic_battery_5_bar_white_24dp);
                } else if(level < 85) {
                    mBatteryImage.setImageResource(R.drawable.ic_battery_6_bar_white_24dp);
                } else {
                    mBatteryImage.setImageResource(R.drawable.ic_battery_full_white_24dp);
                }
            } else {
                mBatteryImage.setImageResource(R.drawable.ic_battery_charging_white_24dp);
            }
        } else {
            mBatteryView.setVisibility(View.GONE);
        }
    }

    private final static int EVENT_WINDOW_MINUTES = 60;

    @SuppressLint("SetTextI18n")
    void updateEventView() {
        SimpleDateFormat sdfDisplay = new SimpleDateFormat("HH:mm", Locale.getDefault());
        //SimpleDateFormat sdfLog = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

        // clear previous event
        mTextViewEvents.setVisibility(View.GONE);
        mAlarmView.setVisibility(View.GONE);

        // 1. check app internal events
        if(mEvents != null) {
            boolean eventFound = false;
            Calendar calNow = Calendar.getInstance();
            long lastDiffMins = EVENT_WINDOW_MINUTES;
            for(Event e : mEvents) {
                if(!e.showOnScreen) continue;
                Calendar calEvent = Calendar.getInstance();
                calEvent.set(Calendar.HOUR_OF_DAY, e.triggerHour);
                calEvent.set(Calendar.MINUTE, e.triggerMinute);
                long diffMins = (calEvent.getTimeInMillis() - calNow.getTimeInMillis()) / 1000 / 60;
                // check if event is in current time window
                if(diffMins > 0 && diffMins < EVENT_WINDOW_MINUTES) {
                    // if multiple events are in current time window: show nearest event
                    if(diffMins < lastDiffMins) {
                        eventFound = true;
                        lastDiffMins = diffMins;
                        mTextViewEvents.setVisibility(View.VISIBLE);
                        mTextViewEvents.setText(e.toString());
                    }
                }
            }
            if(eventFound) {
                return;
            }
        }

        // 2. check system alarms
        if(mShowAlarms && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            final AlarmManager m = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
            AlarmManager.AlarmClockInfo alarmInfo = m.getNextAlarmClock();
            if(alarmInfo != null) {
                Long systemAlarmTime = alarmInfo.getTriggerTime();
                mAlarmText.setText(sdfDisplay.format(systemAlarmTime));
                mAlarmView.setVisibility(View.VISIBLE);
            }
        }

        // 3. check system calendar events
        if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            Calendar start = Calendar.getInstance();
            Calendar end = Calendar.getInstance();
            end.setTime(start.getTime());
            end.set(Calendar.HOUR_OF_DAY, 0);
            end.set(Calendar.MINUTE, 0);
            end.set(Calendar.SECOND, 0);
            end.add(Calendar.DATE, 1);

            String selection = "((dtstart >= "+start.getTimeInMillis()+") AND (dtstart <= "+end.getTimeInMillis()+"))";
            Cursor cursor = getContext().getContentResolver().query(
                    Uri.parse("content://com.android.calendar/events"),
                    new String[]{"calendar_id", "title", "description", "dtstart", "dtend", "eventLocation"},
                    selection, null, CalendarContract.Events.DTSTART+" ASC"
            );
            if(cursor == null) return;
            cursor.moveToFirst();
            int length = cursor.getCount();
            if(length != 0) {
                for(int i = 0; i < length; i++) {
                    mTextViewEvents.setVisibility(View.VISIBLE);
                    mTextViewEvents.setText(sdfDisplay.format(cursor.getLong(3)) + " " + cursor.getString(1));
                    //Log.e("EVENT", cursor.getString(1)+" "+sdfDisplay.format(cursor.getLong(3))+" "+sdfDisplay.format(systemAlarmTime));
                    //cursor.moveToNext();
                    cursor.close();
                    return;
                }
            }
        }
    }

    protected void pause() {
        mTimerAnalogClock.cancel();
        mTimerAnalogClock.purge();
        mTimerCalendarUpdate.cancel();
        mTimerCalendarUpdate.purge();
        mTimerCheckEvent.cancel();
        mTimerCheckEvent.purge();
        mTimerBurnInPreventionRotation.cancel();
        mTimerBurnInPreventionRotation.purge();
    }

    protected void resume() {
        loadSettings();
        startTimer();
    }
}
