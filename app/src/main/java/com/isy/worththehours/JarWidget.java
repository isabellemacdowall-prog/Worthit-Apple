package com.isy.worththehours;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.RemoteViews;
import org.json.JSONObject;

/** Tracks one jar item. Tapping the pill puts this week's amount away, with a little coin-drop animation. */
public class JarWidget extends AppWidgetProvider {
    static final String PAY = "com.isy.worththehours.PAY";
    static final int FRAMES = 18, FRAME_MS = 50;

    static int[] ids(Context c) {
        return AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c, JarWidget.class));
    }
    static String itemFor(Context c, int id) { return Store.prefs(c).getString("jar_" + id, null); }
    static void setItem(Context c, int id, String item) { Store.prefs(c).edit().putString("jar_" + id, item).apply(); }

    static void updateAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        for (int id : ids(c)) render(c, m, id, -1, 0, 0);
    }

    static void render(Context c, AppWidgetManager m, int id, float t, float oldFrac, double amount) {
        try {
            JSONObject st = Store.state(c);
            String itemId = itemFor(c, id);
            Store.Plan p = Store.plan(st, itemId);
            String empty = itemId == null ? "Pick a jar item" : "No longer in the jar";
            if (p != null && !p.inJar) p = null;
            int[] sz = CoolWidget.size(m, id);
            RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_jar);
            v.setImageViewBitmap(R.id.img, Ui.jar(c, sz[0], sz[1], p, empty, t, oldFrac, amount));
            PendingIntent body = p == null ? configure(c, id) : CoolWidget.openApp(c, 200);
            v.setOnClickPendingIntent(R.id.root, body);
            if (p != null && p.dueThis && !p.paidThis) {
                Intent pay = new Intent(c, JarWidget.class).setAction(PAY).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
                v.setOnClickPendingIntent(R.id.pay, PendingIntent.getBroadcast(c, id, pay, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            } else {
                v.setOnClickPendingIntent(R.id.pay, body);
            }
            m.updateAppWidget(id, v);
        } catch (Exception ignored) { }
    }
    static PendingIntent configure(Context c, int id) {
        Intent it = new Intent(c, JarConfigActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return PendingIntent.getActivity(c, 1000 + id, it, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @Override public void onReceive(Context c, Intent intent) {
        if (PAY.equals(intent.getAction())) {
            int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            String itemId = itemFor(c, id);
            Store.Plan before = Store.plan(Store.state(c), itemId);
            double amount = Store.pay(c, itemId);
            AppWidgetManager m = AppWidgetManager.getInstance(c);
            if (amount < 0 || before == null) { render(c, m, id, -1, 0, 0); return; }
            float oldFrac = before.frac();
            PendingResult done = goAsync();
            Handler h = new Handler(Looper.getMainLooper());
            for (int f = 0; f <= FRAMES; f++) {
                final float t = f / (float) FRAMES;
                h.postDelayed(() -> render(c, m, id, t, oldFrac, amount), (long) f * FRAME_MS);
            }
            h.postDelayed(() -> {
                Store.afterChange(c);
                done.finish();
            }, (long) (FRAMES + 2) * FRAME_MS);
            return;
        }
        super.onReceive(c, intent);
    }
    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) render(c, m, id, -1, 0, 0);
    }
    @Override public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle o) { render(c, m, id, -1, 0, 0); }
    @Override public void onDeleted(Context c, int[] ids) {
        for (int id : ids) Store.prefs(c).edit().remove("jar_" + id).apply();
    }
}
