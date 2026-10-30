package com.isy.worththehours;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;
import java.util.List;

/** A ring that fills up as the soonest cooling-off timer counts down. */
public class CoolWidget extends AppWidgetProvider {
    static int[] ids(Context c) {
        return AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c, CoolWidget.class));
    }
    static void updateAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        for (int id : ids(c)) update(c, m, id);
    }
    static int[] size(AppWidgetManager m, int id) {
        Bundle o = m.getAppWidgetOptions(id);
        int w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH), h = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
        if (w <= 0) w = 160; if (h <= 0) h = 160;
        return new int[]{Math.max(100, Math.min(420, w)), Math.max(100, Math.min(420, h))};
    }
    static PendingIntent openApp(Context c, int req) {
        Intent it = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(c, req, it, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    static void update(Context c, AppWidgetManager m, int id) {
        try {
            List<Store.Cool> w = Store.waiting(Store.state(c));
            int[] sz = size(m, id);
            RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_cool);
            v.setImageViewBitmap(R.id.img, Ui.cool(c, sz[0], sz[1], w.isEmpty() ? null : w.get(0), Math.max(0, w.size() - 1)));
            v.setOnClickPendingIntent(R.id.root, openApp(c, 100));
            m.updateAppWidget(id, v);
        } catch (Exception ignored) { }
    }
    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) update(c, m, id);
        Store.scheduleTick(c);
    }
    @Override public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle o) { update(c, m, id); }
    @Override public void onDisabled(Context c) { Store.scheduleTick(c); }
}
