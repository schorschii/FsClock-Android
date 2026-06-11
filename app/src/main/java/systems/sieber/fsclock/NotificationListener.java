package systems.sieber.fsclock;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationListener extends NotificationListenerService {

    static final String BROADCAST_ACTION = "systems.sieber.fsclock.notifi";

    Context mContext;
    ArrayList<Integer> mObservedNotifications = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
    }

    public void onNotificationPosted(StatusBarNotification sbn) {
        if(sbn == null) return;
        mObservedNotifications.add(sbn.getNotification().number);

        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra("count", 1);
        mContext.sendBroadcast(intent);
    }

    public void onNotificationRemoved(StatusBarNotification sbn) {
        if(sbn == null) return;
        mObservedNotifications.remove(Integer.valueOf(sbn.getNotification().number));
    }

    public void reset() {
        mObservedNotifications.clear();
    }

}
