package com.isy.worththehours;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;

/** Shown when the jar widget is added: pick which jar item it tracks. */
public class JarConfigActivity extends Activity {
    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private int dp(float v) { return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics())); }
    private TextView label(String s, int weight, float sp, int color) {
        TextView t = new TextView(this);
        t.setText(s); t.setTypeface(Ui.face(this, weight)); t.setTextSize(sp); t.setTextColor(color);
        return t;
    }
    private GradientDrawable card(int fill, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill); g.setCornerRadius(dp(18)); g.setStroke(dp(1), stroke);
        return g;
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Bundle ex = getIntent().getExtras();
        if (ex != null) widgetId = ex.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        setResult(RESULT_CANCELED, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId));
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return; }

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Ui.VIOLET);
        sv.setFitsSystemWindows(true);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(20), dp(28), dp(20), dp(28));
        sv.addView(col);

        TextView eye = label("IN THE JAR", 700, 11, Ui.ROSE);
        eye.setLetterSpacing(0.14f);
        col.addView(eye);
        TextView h = label("Which item should this widget track?", 800, 24, Ui.INK);
        h.setPadding(0, dp(6), 0, dp(18));
        col.addView(h);

        List<Store.Plan> plans = Store.activePlans(Store.state(this));
        if (plans.isEmpty()) {
            TextView none = label("Nothing in the jar yet. Open the app, pause on something, then stash it. Come back here to pick it.", 500, 15, Ui.MUTED);
            none.setLineSpacing(0, 1.25f);
            col.addView(none);
            TextView open = label("Open Worth the Hours", 700, 15, Ui.VIOLET);
            open.setGravity(Gravity.CENTER);
            open.setPadding(dp(16), dp(14), dp(16), dp(14));
            GradientDrawable g = card(Ui.ROSE, Ui.ROSE);
            open.setBackground(g);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = dp(20);
            col.addView(open, lp);
            open.setOnClickListener(v -> { startActivity(new Intent(this, MainActivity.class)); finish(); });
        }
        for (Store.Plan p : plans) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(16), dp(14), dp(16), dp(14));
            row.setBackground(card(0x8C2C365E, 0x29C5979D));
            row.addView(label(p.name, 700, 16, Ui.INK));
            String sub = Store.money(p.per) + "/wk · " + (p.n > 1 ? "week " + Math.min(p.n, p.paid + (p.paidThis ? 0 : 1)) + " of " + p.n : "this week")
                    + " · " + Store.money(p.price) + " total";
            TextView s = label(sub, 500, 13, Ui.CYAN_LT);
            s.setPadding(0, dp(3), 0, 0);
            row.addView(s);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = dp(10);
            col.addView(row, lp);
            row.setOnClickListener(v -> pick(p.id));
        }
        setContentView(sv);
    }

    private void pick(String itemId) {
        JarWidget.setItem(this, widgetId, itemId);
        JarWidget.render(this, AppWidgetManager.getInstance(this), widgetId, -1, 0, 0);
        setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId));
        finish();
    }
}
