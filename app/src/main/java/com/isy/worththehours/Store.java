package com.isy.worththehours;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** The phone's copy of the app data, shared by the app, widgets and timer alarms. */
final class Store {
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("wth", Context.MODE_PRIVATE); }
    static synchronized String load(Context c) { return prefs(c).getString("state", ""); }
    static synchronized void save(Context c, String json) { prefs(c).edit().putString("state", json).commit(); }
    static JSONObject state(Context c) {
        try { String s = load(c); return s.isEmpty() ? null : new JSONObject(s); } catch (Exception e) { return null; }
    }

    // ---- weeks (Monday start, same as the app) ----
    static String wk(LocalDate d) { return d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString(); }
    static String thisWk() { return wk(LocalDate.now()); }
    static String wkOf(long ms) { return wk(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()); }
    static String addWeeks(String k, int n) { return LocalDate.parse(k).plusWeeks(n).toString(); }

    // ---- formatting ----
    static String money(double v) {
        v = Math.round(v * 100) / 100.0;
        if (Math.abs(v - Math.round(v)) < 0.005) return String.format(Locale.US, "$%,d", Math.round(v));
        return String.format(Locale.US, "$%,.2f", v);
    }
    static String hm(double hours) {
        long mins = Math.round(hours * 60), h = mins / 60, m = mins % 60;
        return h > 0 ? h + "h " + m + "m" : m + "m";
    }
    static String left(long ms) {
        if (ms <= 0) return "Ready";
        long m = (ms + 59999) / 60000, d = m / 1440, h = (m % 1440) / 60, mm = m % 60;
        if (d > 0) return d + "d " + h + "h";
        if (h > 0) return h + "h " + mm + "m";
        return mm + "m";
    }

    // ---- jar plans ----
    static final class Plan {
        JSONObject i; String id, name, start, end; int n, paid;
        double per, price; boolean inJar, stopped, done, paidThis, dueThis;
        float frac() { return done || stopped ? 1f : paid / (float) n; }
    }
    static boolean paidThisWeek(JSONObject st, JSONObject i, String k) {
        JSONObject mv = st.optJSONObject("moved");
        if (mv != null && mv.optBoolean(k, false)) return true;
        JSONArray a = i.optJSONArray("paidWks");
        if (a != null) for (int j = 0; j < a.length(); j++) if (k.equals(a.optString(j))) return true;
        return false;
    }
    static Plan plan(JSONObject st, String id) {
        if (st == null || id == null) return null;
        JSONArray items = st.optJSONArray("items");
        if (items == null) return null;
        for (int j = 0; j < items.length(); j++) {
            JSONObject i = items.optJSONObject(j);
            if (i != null && id.equals(i.optString("id"))) return plan(st, i);
        }
        return null;
    }
    static Plan plan(JSONObject st, JSONObject i) {
        Plan p = new Plan();
        p.i = i; p.id = i.optString("id"); p.name = i.optString("name", "Something"); p.price = i.optDouble("price", 0);
        p.inJar = "stash".equals(i.optString("status"));
        p.n = Math.max(1, i.optInt("weeks", 1));
        p.start = i.has("startWk") ? i.optString("startWk") : wkOf(i.optLong("decided", System.currentTimeMillis()));
        p.per = i.has("per") ? i.optDouble("per") : p.price / p.n;
        p.end = addWeeks(p.start, p.n - 1);
        p.stopped = i.optBoolean("stopped", false);
        String t = thisWk();
        p.paidThis = paidThisWeek(st, i, t);
        String k = p.start; int c = 0;
        while (c < p.n && (k.compareTo(t) < 0 || (k.equals(t) && p.paidThis))) { c++; k = addWeeks(k, 1); }
        p.paid = c;
        p.done = c >= p.n;
        p.dueThis = !p.stopped && p.start.compareTo(t) <= 0 && p.end.compareTo(t) >= 0;
        return p;
    }
    static List<Plan> activePlans(JSONObject st) {
        List<Plan> out = new ArrayList<>();
        if (st == null) return out;
        JSONArray items = st.optJSONArray("items");
        if (items == null) return out;
        for (int j = 0; j < items.length(); j++) {
            JSONObject i = items.optJSONObject(j);
            if (i == null || !"stash".equals(i.optString("status"))) continue;
            Plan p = plan(st, i);
            if (!p.stopped && !p.done) out.add(p);
        }
        return out;
    }
    /** Puts this week's amount for one jar item away. Returns the amount, or -1 if nothing was due. */
    static synchronized double pay(Context c, String id) {
        JSONObject st = state(c);
        Plan p = plan(st, id);
        if (p == null || !p.inJar || !p.dueThis || p.paidThis) return -1;
        try {
            JSONArray a = p.i.optJSONArray("paidWks");
            if (a == null) { a = new JSONArray(); p.i.put("paidWks", a); }
            a.put(thisWk());
            save(c, st.toString());
        } catch (Exception e) { return -1; }
        return p.per;
    }

    // ---- cooling off ----
    static final class Cool { String id, name; double price, rate; long created, ends; }
    static List<Cool> waiting(JSONObject st) {
        List<Cool> out = new ArrayList<>();
        if (st == null) return out;
        JSONArray items = st.optJSONArray("items");
        if (items == null) return out;
        for (int j = 0; j < items.length(); j++) {
            JSONObject i = items.optJSONObject(j);
            if (i == null || !"waiting".equals(i.optString("status"))) continue;
            Cool x = new Cool();
            x.id = i.optString("id"); x.name = i.optString("name", "Something"); x.price = i.optDouble("price", 0);
            x.rate = i.optDouble("rate", 0); x.created = i.optLong("created"); x.ends = i.optLong("ends");
            out.add(x);
        }
        out.sort((a, b) -> Long.compare(a.ends, b.ends));
        return out;
    }

    // ---- alarms: timer-finished notifications and widget ticks ----
    static PendingIntent alarm(Context c, String action, String id, int flags) {
        Intent it = new Intent(c, AlarmReceiver.class).setAction(action)
                .setData(Uri.parse("wth://" + action + "/" + (id == null ? "" : id)));
        if (id != null) it.putExtra("id", id);
        return PendingIntent.getBroadcast(c, 0, it, flags | PendingIntent.FLAG_IMMUTABLE);
    }
    static void scheduleTimers(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        long now = System.currentTimeMillis();
        Set<String> prev = new HashSet<>(prefs(c).getStringSet("alarms", new HashSet<>()));
        Set<String> next = new HashSet<>();
        for (Cool x : waiting(state(c))) {
            if (x.ends <= now) continue;
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, x.ends, alarm(c, AlarmReceiver.DONE, x.id, PendingIntent.FLAG_UPDATE_CURRENT));
            next.add(x.id);
        }
        for (String id : prev) if (!next.contains(id)) {
            PendingIntent pi = alarm(c, AlarmReceiver.DONE, id, PendingIntent.FLAG_NO_CREATE);
            if (pi != null) { am.cancel(pi); pi.cancel(); }
        }
        prefs(c).edit().putStringSet("alarms", next).apply();
    }
    static void scheduleTick(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        PendingIntent pi = alarm(c, AlarmReceiver.TICK, null, PendingIntent.FLAG_UPDATE_CURRENT);
        long now = System.currentTimeMillis();
        Cool soonest = null;
        if (CoolWidget.ids(c).length > 0) for (Cool x : waiting(state(c))) if (x.ends > now) { soonest = x; break; }
        if (soonest == null) { am.cancel(pi); return; }
        long rem = soonest.ends - now;
        long step = rem > 6 * 3600_000L ? 15 * 60_000L : rem > 3600_000L ? 5 * 60_000L : 60_000L;
        am.set(AlarmManager.RTC, Math.min(now + step, soonest.ends), pi);
    }
    /** Call after any data change: refreshes widgets and alarms. */
    static void afterChange(Context c) {
        scheduleTimers(c);
        CoolWidget.updateAll(c);
        JarWidget.updateAll(c);
        scheduleTick(c);
    }
}
