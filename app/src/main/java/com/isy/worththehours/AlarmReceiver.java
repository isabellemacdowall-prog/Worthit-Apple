package com.isy.worththehours;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

/** Timer finished → notification. Also refreshes the ring widget and re-arms alarms after a restart. */
public class AlarmReceiver extends BroadcastReceiver {
    static final String DONE = "com.isy.worththehours.DONE", TICK = "com.isy.worththehours.TICK";

    @Override public void onReceive(Context c, Intent intent) {
        String a = intent.getAction();
        if (DONE.equals(a)) {
            String id = intent.getStringExtra("id");
            for (Store.Cool x : Store.waiting(Store.state(c))) if (x.id.equals(id)) notifyDone(c, x);
            Store.afterChange(c);
        } else if (TICK.equals(a)) {
            CoolWidget.updateAll(c);
            Store.scheduleTick(c);
        } else {
            Store.afterChange(c); // boot or app update
        }
    }

    static void notifyDone(Context c, Store.Cool x) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("timers", "Cooling-off timers", NotificationManager.IMPORTANCE_DEFAULT));
        if (Build.VERSION.SDK_INT >= 33 && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        String body = Store.money(x.price) + (x.rate > 0 ? " is " + Store.hm(x.price / x.rate) + " of your work." : ".") + " Still want it?";
        Notification n = new Notification.Builder(c, "timers")
                .setSmallIcon(R.drawable.ic_stat)
                .setColor(Ui.ROSE)
                .setContentTitle("Time's up: " + x.name)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setContentIntent(CoolWidget.openApp(c, 300))
                .setAutoCancel(true)
                .build();
        nm.notify(x.id.hashCode(), n);
    }
}
