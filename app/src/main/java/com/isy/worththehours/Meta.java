package com.isy.worththehours;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads a shop page and pulls out its name, picture and (when shared) price. */
final class Meta {
    static JSONObject fetch(String link) {
        JSONObject out = new JSONObject();
        try {
            String html = null; URL url = new URL(link);
            for (int hop = 0; hop < 5 && html == null; hop++) {
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setInstanceFollowRedirects(false);
                c.setConnectTimeout(8000); c.setReadTimeout(10000);
                c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36");
                c.setRequestProperty("Accept", "text/html,application/xhtml+xml");
                c.setRequestProperty("Accept-Language", "en-AU,en;q=0.9");
                int code = c.getResponseCode();
                if (code >= 300 && code < 400 && c.getHeaderField("Location") != null) {
                    url = new URL(url, c.getHeaderField("Location")); c.disconnect(); continue;
                }
                if (code >= 400) { c.disconnect(); out.put("error", "http " + code); return out; }
                try (InputStream in = c.getInputStream()) {
                    ByteArrayOutputStream b = new ByteArrayOutputStream(); byte[] buf = new byte[16384]; int n;
                    while ((n = in.read(buf)) > 0 && b.size() < 2_000_000) b.write(buf, 0, n);
                    html = b.toString(StandardCharsets.UTF_8.name());
                }
                c.disconnect();
            }
            if (html == null) { out.put("error", "no page"); return out; }

            String title = first(meta(html, "og:title"), meta(html, "twitter:title"), tag(html, "title"));
            String image = first(meta(html, "og:image"), meta(html, "og:image:secure_url"), meta(html, "twitter:image"), meta(html, "twitter:image:src"), ldImage(html));
            String site = first(meta(html, "og:site_name"), "");
            String price = first(meta(html, "product:price:amount"), meta(html, "og:price:amount"), itemprop(html, "price"), ldField(html, "price"), ldField(html, "lowPrice"));
            String cur = first(meta(html, "product:price:currency"), meta(html, "og:price:currency"), itemprop(html, "priceCurrency"), ldField(html, "priceCurrency"));
            if (!image.isEmpty()) image = new URL(url, decode(image)).toString();
            out.put("title", decode(title)); out.put("image", image); out.put("site", decode(site));
            String p = price.replaceAll("[^0-9.]", "");
            if (!p.isEmpty()) { try { double v = Double.parseDouble(p); if (v > 0) { out.put("price", v); out.put("currency", cur); } } catch (NumberFormatException ignored) { } }
        } catch (Exception e) {
            try { out.put("error", e.getClass().getSimpleName()); } catch (Exception ignored) { }
        }
        return out;
    }

    static String first(String... v) { for (String s : v) if (s != null && !s.trim().isEmpty()) return s.trim(); return ""; }

    static String meta(String html, String key) {
        Matcher m = Pattern.compile("<meta\\b[^>]*>", Pattern.CASE_INSENSITIVE).matcher(html);
        while (m.find()) {
            String t = m.group();
            String k = attr(t, "property"); if (k.isEmpty()) k = attr(t, "name");
            if (k.equalsIgnoreCase(key)) return attr(t, "content");
        }
        return "";
    }
    static String itemprop(String html, String key) {
        Matcher m = Pattern.compile("<[a-z]+\\b[^>]*itemprop=[\"']" + Pattern.quote(key) + "[\"'][^>]*>", Pattern.CASE_INSENSITIVE).matcher(html);
        return m.find() ? attr(m.group(), "content") : "";
    }
    static String attr(String tag, String name) {
        Matcher m = Pattern.compile("\\b" + name + "\\s*=\\s*(\"([^\"]*)\"|'([^']*)')", Pattern.CASE_INSENSITIVE).matcher(tag);
        return m.find() ? (m.group(2) != null ? m.group(2) : m.group(3)) : "";
    }
    static String tag(String html, String name) {
        Matcher m = Pattern.compile("<" + name + "[^>]*>([\\s\\S]*?)</" + name + ">", Pattern.CASE_INSENSITIVE).matcher(html);
        return m.find() ? m.group(1) : "";
    }
    static String ldField(String html, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"?([0-9A-Za-z.,]+)\"?").matcher(html);
        return m.find() ? m.group(1).replace(",", "") : "";
    }
    static String ldImage(String html) {
        Matcher m = Pattern.compile("\"image\"\\s*:\\s*(\\[\\s*)?\"(https?:[^\"]+)\"").matcher(html);
        return m.find() ? m.group(2).replace("\\/", "/") : "";
    }
    static String decode(String s) {
        return s.replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'").replace("&#x27;", "'")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&nbsp;", " ").replaceAll("\\s+", " ").trim();
    }
}
