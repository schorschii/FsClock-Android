package systems.sieber.fsclock;

import android.annotation.SuppressLint;

class Event {
    int triggerHour;
    int triggerMinute;
    public String title;
    String speakText;
    boolean playAlarm;
    boolean showOnScreen;

    Event(int triggerHour, int triggerMinute, String title, String speakText, boolean playAlarm, boolean showOnScreen) {
        this.triggerHour = triggerHour;
        this.triggerMinute = triggerMinute;
        this.title = title;
        this.speakText = speakText;
        this.playAlarm = playAlarm;
        this.showOnScreen = showOnScreen;
    }

    @SuppressLint("DefaultLocale")
    public String toString() {
        return Integer.toString(triggerHour)+":"+String.format("%02d", triggerMinute)+" "+title;
    }
}
