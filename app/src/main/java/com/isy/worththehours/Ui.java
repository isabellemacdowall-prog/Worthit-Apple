package com.isy.worththehours;

import android.content.Context;
import android.graphics.*;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import java.util.HashMap;
import java.util.Map;

/** Draws the widgets in the app's palette with Figtree. */
final class Ui {
    static final int VIOLET = 0xFF2B193D, INDIGO = 0xFF2C365E, GRAPE = 0xFF484D6D, CYAN = 0xFF4B8F8C,
            ROSE = 0xFFC5979D, INK = 0xFFF4EEF3, MUTED = 0xFFB9B2C9, CYAN_LT = 0xFF7FC4C0;
    private static final Map<Integer, Typeface> faces = new HashMap<>();

    static Typeface face(Context c, int weight) {
        Typeface t = faces.get(weight);
        if (t != null) return t;
        try {
            t = new Typeface.Builder(c.getAssets(), "fonts/Figtree.ttf")
                    .setFontVariationSettings("'wght' " + weight)
                    .setFallback("sans-serif").build();
        } catch (Exception e) { t = null; }
        if (t == null) t = Typeface.create("sans-serif", weight >= 600 ? Typeface.BOLD : Typeface.NORMAL);
        faces.put(weight, t);
        return t;
    }
    static TextPaint text(Context c, int weight, float size, int color) {
        TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        p.setTypeface(face(c, weight)); p.setTextSize(size); p.setColor(color);
        return p;
    }
    static float scale(Context c) { return Math.min(c.getResources().getDisplayMetrics().density, 2.75f); }

    static void background(Canvas cv, float W, float H, float s) {
        RectF r = new RectF(0, 0, W, H);
        float rad = 22 * s;
        Path clip = new Path();
        clip.addRoundRect(r, rad, rad, Path.Direction.CW);
        cv.save();
        cv.clipPath(clip);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new LinearGradient(0, 0, W * 0.6f, H, INDIGO, VIOLET, Shader.TileMode.CLAMP));
        cv.drawRect(r, p);
        Paint g = new Paint(Paint.ANTI_ALIAS_FLAG);
        g.setShader(new RadialGradient(W, 0, Math.max(W, H) * 0.75f, 0x4DC5979D, 0x00C5979D, Shader.TileMode.CLAMP));
        cv.drawRect(r, g);
        g.setShader(new RadialGradient(0, H, Math.max(W, H) * 0.7f, 0x334B8F8C, 0x004B8F8C, Shader.TileMode.CLAMP));
        cv.drawRect(r, g);
        cv.restore();
        Paint b = new Paint(Paint.ANTI_ALIAS_FLAG);
        b.setStyle(Paint.Style.STROKE); b.setStrokeWidth(s); b.setColor(0x33C5979D);
        float h = s / 2;
        cv.drawRoundRect(new RectF(h, h, W - h, H - h), rad, rad, b);
    }

    static void ring(Canvas cv, float cx, float cy, float r, float stroke, float frac, int from, int to) {
        Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
        t.setStyle(Paint.Style.STROKE); t.setStrokeWidth(stroke); t.setColor(0x99484D6D);
        cv.drawCircle(cx, cy, r, t);
        if (frac <= 0.002f) return;
        Paint a = new Paint(Paint.ANTI_ALIAS_FLAG);
        a.setStyle(Paint.Style.STROKE); a.setStrokeWidth(stroke); a.setStrokeCap(Paint.Cap.ROUND);
        float f = Math.min(1f, frac);
        SweepGradient sg = new SweepGradient(cx, cy, new int[]{from, to, from}, new float[]{0f, Math.max(0.02f, Math.min(0.999f, f)), 1f});
        Matrix m = new Matrix(); m.setRotate(-90, cx, cy); sg.setLocalMatrix(m);
        a.setShader(sg);
        if (f >= 0.999f) { a.setShader(null); a.setColor(to); cv.drawCircle(cx, cy, r, a); }
        else cv.drawArc(new RectF(cx - r, cy - r, cx + r, cy + r), -90, 360 * f, false, a);
    }

    /** Centred single line, shrunk then ellipsised to fit. */
    static void center(Canvas cv, String s, float cx, float base, TextPaint p, float maxW) {
        float size = p.getTextSize();
        while (p.measureText(s) > maxW && p.getTextSize() > size * 0.7f) p.setTextSize(p.getTextSize() * 0.94f);
        CharSequence t = TextUtils.ellipsize(s, p, maxW, TextUtils.TruncateAt.END);
        p.setTextAlign(Paint.Align.CENTER);
        cv.drawText(t, 0, t.length(), cx, base, p);
        p.setTextAlign(Paint.Align.LEFT);
    }
    static StaticLayout block(String s, TextPaint p, float w, int lines) {
        return StaticLayout.Builder.obtain(s, 0, s.length(), p, Math.max(1, (int) w))
                .setMaxLines(lines).setEllipsize(TextUtils.TruncateAt.END)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false).build();
    }
    static float ease(float t) { t = Math.max(0, Math.min(1, t)); return 1 - (float) Math.pow(1 - t, 3); }

    // ================= Cooling-off widget =================
    static Bitmap cool(Context c, int wDp, int hDp, Store.Cool x, int more) {
        float s = scale(c), W = wDp * s, H = hDp * s, pad = 14 * s;
        Bitmap bmp = Bitmap.createBitmap(Math.round(W), Math.round(H), Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        background(cv, W, H, s);
        float textBlock = 38 * s;
        float dia = Math.max(44 * s, Math.min(W - 2 * pad, H - 2 * pad - textBlock));
        float stroke = Math.max(6 * s, dia * 0.085f), r = dia / 2 - stroke / 2;
        float cx = W / 2, cy = pad + dia / 2;
        float nameBase = cy + dia / 2 + 18 * s, subBase = nameBase + 15 * s;
        long now = System.currentTimeMillis();
        if (x == null) {
            ring(cv, cx, cy, r, stroke, 0, GRAPE, ROSE);
            center(cv, "All clear", cx, cy + r * 0.12f, text(c, 800, r * 0.34f, INK), r * 1.6f);
            center(cv, "Nothing cooling off", cx, nameBase, text(c, 700, 13 * s, INK), W - 2 * pad);
            center(cv, "Tap to check a price", cx, subBase, text(c, 500, 11 * s, MUTED), W - 2 * pad);
            return bmp;
        }
        boolean ready = now >= x.ends;
        float frac = x.ends > x.created ? (float) (now - x.created) / (x.ends - x.created) : 1f;
        if (ready) {
            Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
            glow.setShader(new RadialGradient(cx, cy, r, 0x40C5979D, 0x00C5979D, Shader.TileMode.CLAMP));
            cv.drawCircle(cx, cy, r, glow);
        }
        ring(cv, cx, cy, r, stroke, ready ? 1f : frac, GRAPE, ROSE);
        float big = r * (ready ? 0.42f : 0.38f), small = Math.max(9 * s, r * 0.17f);
        float base = cy + big * 0.12f;
        center(cv, ready ? "Ready" : Store.left(x.ends - now), cx, base, text(c, 800, big, ready ? ROSE : INK), r * 1.55f);
        center(cv, ready ? "to decide" : "left", cx, base + small * 1.45f, text(c, 600, small, MUTED), r * 1.4f);
        center(cv, x.name, cx, nameBase, text(c, 700, 13 * s, INK), W - 2 * pad);
        String sub = Store.money(x.price) + (x.rate > 0 ? " · " + Store.hm(x.price / x.rate) + " work" : "") + (more > 0 ? " · +" + more + " more" : "");
        center(cv, sub, cx, subBase, text(c, 500, 11 * s, MUTED), W - 2 * pad);
        return bmp;
    }

    // ================= Jar item widget =================
    /** t < 0: static. 0..1: the put-away animation, ring growing from oldFrac. */
    static Bitmap jar(Context c, int wDp, int hDp, Store.Plan p, String emptyTitle, float t, float oldFrac, double amount) {
        float s = scale(c), W = wDp * s, H = hDp * s, pad = 14 * s;
        Bitmap bmp = Bitmap.createBitmap(Math.round(W), Math.round(H), Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        background(cv, W, H, s);
        TextPaint eye = text(c, 700, 9.5f * s, ROSE);
        eye.setLetterSpacing(0.14f);
        cv.drawText("IN THE JAR", pad, pad + 9 * s, eye);

        float pillH = 36 * s, pillY = H - pad - pillH;
        float rowTop = pad + 18 * s, rowBot = pillY - 10 * s, rowH = Math.max(30 * s, rowBot - rowTop);
        float dia = Math.min(rowH, W * 0.42f);
        float stroke = Math.max(5 * s, dia * 0.1f), r = dia / 2 - stroke / 2;
        float cx = pad + dia / 2, cy = rowTop + rowH / 2;
        float tx = pad + dia + 12 * s, tw = W - tx - pad;

        if (p == null) {
            ring(cv, cx, cy, r, stroke, 0, GRAPE, CYAN);
            center(cv, "+", cx, cy + r * 0.32f, text(c, 700, r * 0.95f, MUTED), r * 1.5f);
            StaticLayout a = block(emptyTitle, text(c, 700, 13 * s, INK), tw, 2);
            StaticLayout b = block("Tap to choose", text(c, 500, 11 * s, MUTED), tw, 1);
            float top = cy - (a.getHeight() + 4 * s + b.getHeight()) / 2;
            cv.save(); cv.translate(tx, top); a.draw(cv); cv.translate(0, a.getHeight() + 4 * s); b.draw(cv); cv.restore();
            return bmp;
        }

        boolean anim = t >= 0;
        float newFrac = p.frac();
        float frac = anim ? oldFrac + (newFrac - oldFrac) * ease((t - 0.35f) / 0.65f) : newFrac;

        // pulse when the coin lands
        if (anim && t > 0.45f && t < 0.95f) {
            float v = (t - 0.45f) / 0.5f;
            Paint pulse = new Paint(Paint.ANTI_ALIAS_FLAG);
            pulse.setStyle(Paint.Style.STROKE); pulse.setStrokeWidth(stroke * (1 - v) + s);
            pulse.setColor(CYAN_LT); pulse.setAlpha((int) (200 * (1 - v)));
            cv.drawCircle(cx, cy, r + v * dia * 0.28f, pulse);
        }
        ring(cv, cx, cy, r, stroke, frac, GRAPE, p.stopped ? MUTED : CYAN_LT);

        // ring centre
        String mid; int midColor = INK;
        if (anim && t >= 0.45f && t < 0.88f) { mid = "+" + Store.money(amount); midColor = CYAN_LT; }
        else if (p.done) { mid = "✓"; midColor = CYAN_LT; }
        else if (p.stopped) mid = "—";
        else mid = p.paid + "/" + p.n;
        center(cv, mid, cx, cy + r * 0.14f, text(c, 800, r * (mid.startsWith("+") ? 0.42f : 0.5f), midColor), r * 1.55f);

        // falling coin
        if (anim && t < 0.5f) {
            float u = t / 0.5f, e = u * u;
            float startY = -dia * 0.3f, cyCoin = startY + (cy - startY) * e;
            float cr = dia * 0.2f * (1 - 0.45f * e);
            Paint coin = new Paint(Paint.ANTI_ALIAS_FLAG);
            coin.setColor(ROSE); coin.setAlpha(u > 0.8f ? (int) (255 * (1 - (u - 0.8f) / 0.2f)) : 255);
            cv.drawCircle(cx, cyCoin, cr, coin);
            TextPaint d = text(c, 800, cr * 1.25f, VIOLET);
            d.setAlpha(coin.getAlpha());
            center(cv, "$", cx, cyCoin + cr * 0.42f, d, cr * 2);
        }

        // text block
        TextPaint nameP = text(c, 700, 13 * s, INK);
        StaticLayout name = block(p.name, nameP, tw, 2);
        TextPaint amt = text(c, 800, 18 * s, p.stopped ? MUTED : CYAN_LT);
        TextPaint unit = text(c, 600, 11 * s, MUTED);
        String amtS = p.done || p.stopped ? Store.money(p.stopped ? p.per * p.paid : p.price) : Store.money(p.per);
        String unitS = p.done ? " saved" : p.stopped ? " saved" : "/wk";
        String subS = p.stopped ? "Stopped" : p.done ? "Fully saved" : p.n > 1 ? "Week " + Math.min(p.n, p.paid + (p.paidThis ? 0 : 1)) + " of " + p.n : "This week";
        StaticLayout sub = block(subS, text(c, 500, 11 * s, MUTED), tw, 1);
        float blockH = name.getHeight() + 6 * s + 20 * s + 3 * s + sub.getHeight();
        float top = cy - blockH / 2;
        cv.save(); cv.translate(tx, top); name.draw(cv); cv.restore();
        float amtBase = top + name.getHeight() + 6 * s + 17 * s;
        cv.drawText(amtS, tx, amtBase, amt);
        float aw = amt.measureText(amtS);
        if (aw + unit.measureText(unitS) < tw) cv.drawText(unitS, tx + aw + 2 * s, amtBase, unit);
        cv.save(); cv.translate(tx, amtBase + 6 * s); sub.draw(cv); cv.restore();

        // pill
        RectF pill = new RectF(pad, pillY, W - pad, pillY + pillH);
        float pr = pillH / 2;
        Paint pp = new Paint(Paint.ANTI_ALIAS_FLAG);
        boolean showPaid = p.paidThis && !(anim && t < 0.6f);
        String label; int labelColor;
        if (p.stopped) {
            pp.setColor(0x22B9B2C9); cv.drawRoundRect(pill, pr, pr, pp);
            label = "Stopped · " + Store.money(p.per * p.paid) + " kept"; labelColor = MUTED;
        } else if (p.done && !anim) {
            pp.setColor(0x334B8F8C); cv.drawRoundRect(pill, pr, pr, pp);
            label = "Fully saved · " + Store.money(p.price); labelColor = CYAN_LT;
        } else if (showPaid || (anim && t >= 0.6f)) {
            pp.setColor(0x334B8F8C); cv.drawRoundRect(pill, pr, pr, pp);
            Paint st = new Paint(Paint.ANTI_ALIAS_FLAG); st.setStyle(Paint.Style.STROKE); st.setStrokeWidth(1.5f * s); st.setColor(CYAN);
            cv.drawRoundRect(pill, pr, pr, st);
            label = "✓ Put away this week"; labelColor = CYAN_LT;
        } else if (p.dueThis) {
            pp.setShader(new LinearGradient(pill.left, 0, pill.right, 0, CYAN, 0xFF5AA5A1, Shader.TileMode.CLAMP));
            if (anim) { float v = Math.min(1, t / 0.15f); pill.inset(v * 3 * s * (1 - t), 0); }
            cv.drawRoundRect(pill, pr, pr, pp);
            label = "Put away " + Store.money(p.per); labelColor = 0xFFF4FBFA;
        } else {
            pp.setColor(0x22B9B2C9); cv.drawRoundRect(pill, pr, pr, pp);
            label = "Nothing due this week"; labelColor = MUTED;
        }
        center(cv, label, pill.centerX(), pill.centerY() + 4.5f * s, text(c, 700, 13 * s, labelColor), pill.width() - 20 * s);
        return bmp;
    }
}
