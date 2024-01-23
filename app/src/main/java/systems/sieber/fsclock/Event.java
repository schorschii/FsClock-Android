package systems.sieber.fsclock;

import android.annotation.SuppressLint;

class Event {
    int triggerHour;
    int triggerMinute;
    public String title;
    String speakText;
    boolean playAlarm;
    boolean showOnScreen;
    int hideAfter;

    Event(int triggerHour, int triggerMinute, String title, String speakText, boolean playAlarm, boolean showOnScreen, int hideAfter) {
        this.triggerHour = triggerHour;
        this.triggerMinute = triggerMinute;
        this.title = title;
        this.speakText = speakText;
        this.playAlarm = playAlarm;
        this.showOnScreen = showOnScreen;
        this.hideAfter = hideAfter;
    }

    @SuppressLint("DefaultLocale")
    public String toString() {
        return Integer.toString(triggerHour)+":"+String.format("%02d", triggerMinute)+" "+title;
    }
}
