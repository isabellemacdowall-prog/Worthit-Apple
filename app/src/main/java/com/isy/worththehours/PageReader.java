package com.isy.worththehours;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import org.json.JSONObject;

/** Opens a shop page in an invisible browser view and reads its product details, like Chrome would see them. */
final class PageReader {
    interface Done { void got(JSONObject d); }

    static final String EXTRACT = "(function(){try{"
        + "function m(k){var e=document.querySelector('meta[property=\"'+k+'\"],meta[name=\"'+k+'\"]');return e?(e.getAttribute('content')||''):''}"
        + "var o={title:m('og:title')||m('twitter:title'),image:m('og:image')||m('og:image:secure_url')||m('twitter:image'),site:m('og:site_name'),"
        + "price:m('product:price:amount')||m('og:price:amount'),currency:m('product:price:currency')||m('og:price:currency')};"
        + "function walk(n){if(!n||typeof n!=='object')return;if(Array.isArray(n)){n.forEach(walk);return}"
        + "var t=[].concat(n['@type']||[]).join(' ');"
        + "if(/Product/i.test(t)){if(!o.title&&n.name)o.title=n.name;"
        + "if(!o.image&&n.image){var im=[].concat(n.image)[0];o.image=typeof im==='string'?im:(im&&(im.url||im.contentUrl))||''}"
        + "var of=[].concat(n.offers||[])[0];if(of){var p=of.price||of.lowPrice||(of.priceSpecification&&[].concat(of.priceSpecification)[0].price);"
        + "if(!o.price&&p)o.price=String(p);if(!o.currency&&of.priceCurrency)o.currency=of.priceCurrency}}"
        + "if(n['@graph'])walk(n['@graph']);}"
        + "document.querySelectorAll('script[type=\"application/ld+json\"]').forEach(function(s){try{walk(JSON.parse(s.textContent))}catch(e){}});"
        + "if(!o.price){var ip=document.querySelector('[itemprop=price]');if(ip)o.price=ip.getAttribute('content')||ip.textContent||''}"
        + "if(!o.currency){var ic=document.querySelector('[itemprop=priceCurrency]');if(ic)o.currency=ic.getAttribute('content')||ic.textContent||''}"
        + "if(!o.title)o.title=document.title||'';"
        + "if(!o.image){var best=null,bs=0;document.querySelectorAll('img').forEach(function(i){var a=(i.naturalWidth||i.width)*(i.naturalHeight||i.height);if(a>bs&&i.currentSrc){bs=a;best=i.currentSrc}});if(best&&bs>40000)o.image=best}"
        + "if(o.image)try{o.image=new URL(o.image,location.href).href}catch(e){}"
        + "o.price=String(o.price||'').replace(/[^0-9.]/g,'');"
        + "return JSON.stringify(o)}catch(e){return '{}'}})()";

    @SuppressLint("SetJavaScriptEnabled")
    static void read(Activity a, String url, Done done) {
        Handler h = new Handler(Looper.getMainLooper());
        h.post(() -> {
            ViewGroup root = a.findViewById(android.R.id.content);
            WebView w = new WebView(a);
            w.setVisibility(View.INVISIBLE);
            root.addView(w, new FrameLayout.LayoutParams(400, 800));
            WebSettings s = w.getSettings();
            s.setJavaScriptEnabled(true);
            s.setDomStorageEnabled(true);
            s.setLoadsImagesAutomatically(false);
            final boolean[] finished = {false};
            Runnable[] grab = new Runnable[1];
            Runnable cleanup = () -> { try { root.removeView(w); w.stopLoading(); w.destroy(); } catch (Exception ignored) { } };
            grab[0] = () -> w.evaluateJavascript(EXTRACT, res -> {
                if (finished[0]) return;
                finished[0] = true;
                JSONObject d;
                try { d = new JSONObject(new JSONObject("{\"v\":" + res + "}").getString("v")); } catch (Exception e) { d = new JSONObject(); }
                cleanup.run();
                done.got(d);
            });
            w.setWebViewClient(new WebViewClient() {
                @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                    String sc = r.getUrl().getScheme();
                    return !("http".equals(sc) || "https".equals(sc)); // stay in the page, ignore app links
                }
                @Override public void onPageFinished(WebView v, String u) {
                    h.postDelayed(() -> { if (!finished[0]) grab[0].run(); }, 2500); // give scripts time to fill in details
                }
            });
            h.postDelayed(() -> { if (!finished[0]) grab[0].run(); }, 14000); // give up waiting, read what's there
            w.loadUrl(url);
        });
    }
}
