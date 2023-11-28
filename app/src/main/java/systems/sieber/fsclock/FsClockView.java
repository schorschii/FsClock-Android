package systems.sieber.fsclock;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
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
import java.util.Timer;
import java.util.TimerTask;

public class FsClockView extends FrameLayout {
    AppCompatActivity mActivity;

    SharedPreferences mSharedPref;

    View mRootView;
    ImageView mBackgroundImage;
    View mBatteryView;
    TextView mBatteryText;
    ImageView mBatteryImage;
    TextView mClockText;
    TextView mSecondsText;
    TextView mDateText;
    TextView mTextViewEvents;
    ImageView mClockFace;
    ImageView mSecondsHand;
    ImageView mMinutesHand;
    ImageView mHoursHand;

    Timer timerAnalogClock;
    Timer timerCalendarUpdate;
    Timer timerCheckEvent;

    TextToSpeech tts;
    Event[] events;
    boolean format24hrs;

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
        mBackgroundImage = findViewById(R.id.imageViewBackground);
        mClockText = findViewById(R.id.textViewClock);
        mSecondsText = findViewById(R.id.textViewClockSeconds);
        mDateText = findViewById(R.id.textViewDate);
        mTextViewEvents = findViewById(R.id.textViewEvents);
        mClockFace = findViewById(R.id.imageViewClockFace);
        mHoursHand = findViewById(R.id.imageViewClockHoursHand);
        mMinutesHand = findViewById(R.id.imageViewClockMinutesHand);
        mSecondsHand = findViewById(R.id.imageViewClockSecondsHand);
        mBatteryView = findViewById(R.id.linearLayoutBattery);
        mBatteryText = findViewById(R.id.textViewBattery);
        mBatteryImage = findViewById(R.id.imageViewBattery);
        mBatteryImage.setImageResource(R.drawable.ic_battery_full_black_24dp);

        // init font
        final Typeface fontLed = ResourcesCompat.getFont(c, R.font.dseg7classic_regular);
        final Typeface fontDate = ResourcesCompat.getFont(c, R.font.cairo_regular);
        mClockText.setTypeface(fontLed);
        mSecondsText.setTypeface(fontLed);
        mDateText.setTypeface(fontDate);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // auto-calc text size
            mClockText.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
            mSecondsText.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
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
                    int clockTextContainerWidth = mClockText.getWidth();
                    int secondsContainerWidth = mSecondsText.getWidth();
                    int dateContainerWidth = mDateText.getWidth();
                    String clockText = mClockText.getText().toString();
                    String secondsText = mSecondsText.getText().toString();
                    String dateText = mDateText.getText().toString();
                    for(int i = 140; i >= 50; i-=2) {
                        int textWidth = getTextWidth(getContext(), clockText, i, size, fontLed);
                        if(textWidth < clockTextContainerWidth) {
                            mClockText.setTextSize(i);
                            Log.i("CALC_TSIZE_CLOCK", i+" => "+textWidth+" (max "+clockTextContainerWidth+") @ "+clockText);
                            break;
                        }
                    }
                    for(int i = 80; i >= 20; i-=2) {
                        int textWidth = getTextWidth(getContext(), secondsText, i, size, fontLed);
                        if(textWidth < secondsContainerWidth) {
                            mSecondsText.setTextSize(i);
                            Log.i("CALC_TSIZE_SECS", i+" => "+textWidth+" (max "+secondsContainerWidth+") @ "+secondsText);
                            break;
                        }
                    }
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

        // init preferences
        loadSettings(mActivity);
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
        tts = new TextToSpeech(getContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.SUCCESS) {
                    tts = null;
                }
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        // destroy tts service connection
        tts.stop();
        tts.shutdown();
    }

    @SuppressLint("SimpleDateFormat")
    private void updateClock() {
        final Calendar cal = Calendar.getInstance();

        // compile date format
        final SimpleDateFormat sdfSystem = (SimpleDateFormat) DateFormat.getDateFormat(getContext());
        String strDatePattern = "EEEE, " + sdfSystem.toLocalizedPattern(); // day of week followed by localized date
        if(!strDatePattern.contains("yyyy")) {
            // devices with API level 17 or below return date format already with yyyy,
            // for all other devices we manually replace yy with yyyy
            strDatePattern = strDatePattern.replace("yy", "yyyy");
        }

        final SimpleDateFormat sdfDate = new SimpleDateFormat(strDatePattern, Locale.getDefault());
        final SimpleDateFormat sdfTime = new SimpleDateFormat(format24hrs ? "HH:mm" : "hh:mm");
        final SimpleDateFormat sdfSeconds = new SimpleDateFormat("ss");
        mClockText.setText(sdfTime.format(cal.getTime()));
        mSecondsText.setText(sdfSeconds.format(cal.getTime()));
        mDateText.setText(sdfDate.format(cal.getTime()));
        float secRotation = (cal.get(Calendar.SECOND) + ((float)cal.get(Calendar.MILLISECOND)/1000)) * 360 / 60;
        float minRotation = (cal.get(Calendar.MINUTE) + ((float)cal.get(Calendar.SECOND)/60)) * 360 / 60;
        float hrsRotation = (cal.get(Calendar.HOUR) + ((float)cal.get(Calendar.MINUTE)/60)) * 360 / 12;
        mSecondsHand.setRotation(secRotation);
        mMinutesHand.setRotation(minRotation);
        mHoursHand.setRotation(hrsRotation);
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
                        if(events != null) {
                            for(Event e : events) {
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

        timerAnalogClock = new Timer(false);
        timerCalendarUpdate = new Timer(false);
        timerCheckEvent = new Timer(false);
        timerAnalogClock.schedule(taskAnalogClock, 0, 100);
        timerCalendarUpdate.schedule(taskCalendarUpdate, 0, 10000);
        timerCheckEvent.schedule(taskCheckEvent, 0, 1000);
    }

    void loadSettings(Activity a) {
        if(a != null) {
            if(mSharedPref.getBoolean("keep-screen-on", true)) {
                a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                Log.i("SCREEN", "Keep ON");
            } else {
                a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                Log.i("SCREEN", "Keep OFF");
            }
        }

        Gson gson = new Gson();
        events = gson.fromJson(mSharedPref.getString("events",""), Event[].class);

        format24hrs = mSharedPref.getBoolean("24hrs", true);

        if(mSharedPref.getBoolean("show-analog", true))
            findViewById(R.id.analogClockContainer).setVisibility(View.VISIBLE);
        else
            findViewById(R.id.analogClockContainer).setVisibility(View.GONE);

        if(mSharedPref.getBoolean("show-digital", true))
            findViewById(R.id.digitalClockContainer).setVisibility(View.VISIBLE);
        else
            findViewById(R.id.digitalClockContainer).setVisibility(View.GONE);

        if(mSharedPref.getBoolean("show-date", true))
            mDateText.setVisibility(View.VISIBLE);
        else {
            mDateText.setVisibility(View.INVISIBLE);
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
            findViewById(R.id.textViewClockSeconds).setVisibility(View.VISIBLE);
        else
            findViewById(R.id.textViewClockSeconds).setVisibility(View.GONE);

        // init custom digital color
        int colorDigital = mSharedPref.getInt("color-digital", 0xffffffff);
        mClockText.setTextColor(colorDigital);
        mSecondsText.setTextColor(colorDigital);
        mDateText.setTextColor(colorDigital);
        mTextViewEvents.setTextColor(colorDigital);
        mBatteryText.setTextColor(colorDigital);
        mBatteryImage.setColorFilter(colorDigital, PorterDuff.Mode.SRC_ATOP);

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
        if(tts != null) {
            tts.setLanguage(Locale.GERMANY);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }

    @SuppressLint("SetTextI18n")
    void updateBattery(int plugged, int level) {
        if((plugged == 0 && mSharedPref.getBoolean("show-battery-info", true))
        || (plugged != 0 && mSharedPref.getBoolean("show-battery-info-when-charging", false))) {
            mBatteryText.setText(level + "%");
            mBatteryView.setVisibility(View.VISIBLE);
            if(level < 10) {
                mBatteryImage.setImageResource(R.drawable.ic_baseline_battery_alert_24);
            } else {
                mBatteryImage.setImageResource(R.drawable.ic_battery_full_black_24dp);
            }
        } else {
            mBatteryView.setVisibility(View.INVISIBLE);
        }
    }

    private final static int EVENT_WINDOW_MINUTES = 60;

    @SuppressLint("SetTextI18n")
    void updateEventView() {
        // clear previous event
        mTextViewEvents.setVisibility(View.GONE);

        // 1. check app internal events
        if(events != null) {
            Calendar calNow = Calendar.getInstance();
            long lastDiffMins = EVENT_WINDOW_MINUTES;
            for(Event e : events) {
                if(!e.showOnScreen) continue;
                Calendar calEvent = Calendar.getInstance();
                calEvent.set(Calendar.HOUR_OF_DAY, e.triggerHour);
                calEvent.set(Calendar.MINUTE, e.triggerMinute);
                long diffMins = (calEvent.getTimeInMillis() - calNow.getTimeInMillis()) / 1000 / 60;
                // check if event is in current time window
                if(diffMins > 0 && diffMins < EVENT_WINDOW_MINUTES) {
                    // if multiple events are in current time window: show nearest event
                    if(diffMins < lastDiffMins) {
                        lastDiffMins = diffMins;
                        // show event text
                        mTextViewEvents.setVisibility(View.VISIBLE);
                        mTextViewEvents.setText(e.toString());
                    }
                }
            }
        }

        // 2. check system calendar events
        if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            //SimpleDateFormat sdfLog = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            SimpleDateFormat sdfDisplay = new SimpleDateFormat("HH:mm", Locale.getDefault());

            Calendar start = Calendar.getInstance();
            Calendar end = Calendar.getInstance();
            end.setTime(start.getTime());
            end.set(Calendar.HOUR_OF_DAY, 0);
            end.set(Calendar.MINUTE, 0);
            end.set(Calendar.SECOND, 0);
            end.add(Calendar.DATE, 1);

            String selection = "((dtstart >= "+start.getTimeInMillis()+") AND (dtstart <= "+end.getTimeInMillis()+"))";
            //Log.e("EVENT", sdfLog.format(start.getTime()));
            //Log.e("EVENT", sdfLog.format(end.getTime()));

            Cursor cursor = getContext().getContentResolver().query(
                    Uri.parse("content://com.android.calendar/events"),
                    new String[]{"calendar_id", "title", "description", "dtstart", "dtend", "eventLocation"},
                    selection, null, CalendarContract.Events.DTSTART+" ASC"
            );
            if(cursor == null) return;
            cursor.moveToFirst();
            int length = cursor.getCount();
            for(int i = 0; i < length; i++) {
                mTextViewEvents.setVisibility(View.VISIBLE);
                mTextViewEvents.setText(sdfDisplay.format(cursor.getLong(3)) + " " + cursor.getString(1));
                //Log.e("EVENT",cursor.getString(1) + " "+sdfLog.format(cursor.getLong(3)));
                cursor.moveToNext();
                break;
            }
            cursor.close();
        }
    }

    protected void pause() {
        timerAnalogClock.cancel();
        timerAnalogClock.purge();
        timerCalendarUpdate.cancel();
        timerCalendarUpdate.purge();
        timerCheckEvent.cancel();
        timerCheckEvent.purge();
    }

    protected void resume() {
        loadSettings(mActivity);
        startTimer();
    }
}
