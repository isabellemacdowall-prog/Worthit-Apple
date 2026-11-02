package com.isy.worththehours;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private WebView web;
    private String pendingLink;
    private boolean pageReady;

    /** Lets the page share its data with the widgets, and read shop links. */
    public class Bridge {
        @JavascriptInterface public String load() { return Store.load(getApplicationContext()); }
        @JavascriptInterface public void sync(String json) {
            Store.save(getApplicationContext(), json);
            Store.afterChange(getApplicationContext());
        }
        @JavascriptInterface public void fetchMeta(String id, String link) {
            new Thread(() -> {
                JSONObject d = Meta.fetch(link);
                String js = "window.__meta(" + JSONObject.quote(id) + "," + d.toString() + ")";
                runOnUiThread(() -> { if (web != null) web.evaluateJavascript(js, null); });
            }).start();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        web.setBackgroundColor(0xFF2B193D);
        web.addJavascriptInterface(new Bridge(), "Android");
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if ("file".equals(u.getScheme())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) { }
                return true; // shop links open in the browser, not inside the app
            }
            @Override public void onPageFinished(WebView v, String url) {
                pageReady = true;
                deliverLink();
            }
        });
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
        handleShare(getIntent());
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleShare(intent);
    }

    /** "Share → Worth the Hours" from Chrome or a shopping app. */
    private void handleShare(Intent intent) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())) return;
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (text == null) return;
        pendingLink = text;
        deliverLink();
    }
    private void deliverLink() {
        if (!pageReady || pendingLink == null || web == null) return;
        String js = "window.__sharedLink&&window.__sharedLink(" + JSONObject.quote(pendingLink) + ")";
        pendingLink = null;
        web.evaluateJavascript(js, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) {
            web.onResume();
            web.evaluateJavascript("window.__reloadFromNative&&window.__reloadFromNative()", null);
        }
    }

    @Override protected void onPause() { if (web != null) web.onPause(); super.onPause(); }
}
