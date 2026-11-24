package com.rinostick.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Clima atual (Open-Meteo, gratuito e sem chave). Local: cidade escolhida em
 * Ajustes ou, se automático, aproximado pelo IP (ipwho.is). Atualiza a cada 30 min.
 */
final class Weather {

    static final long MAX_AGE = 30 * 60 * 1000L;
    static volatile boolean busy;

    private Weather() { }

    static boolean enabled(Context c) { return Ui.prefs(c).getBoolean("wOn", true); }
    static boolean has(Context c) { return Ui.prefs(c).contains("wTemp"); }
    static int temp(Context c) { return Math.round(Ui.prefs(c).getFloat("wTemp", 0)); }
    static int code(Context c) { return Ui.prefs(c).getInt("wCode", 0); }
    static boolean day(Context c) { return Ui.prefs(c).getBoolean("wDay", true); }
    static String city(Context c) { return Ui.prefs(c).getString("wCity", null); }
    static boolean manual(Context c) { return Ui.prefs(c).getBoolean("wManual", false); }

    /** Atualiza em segundo plano se o dado estiver velho (ou se force); chama done na thread principal. */
    static volatile long lastFail;

    static void refresh(Context ctx, boolean force, Runnable done) {
        final Context c = ctx.getApplicationContext();
        SharedPreferences p = Ui.prefs(c);
        if (!enabled(c) || busy) return;
        long now = System.currentTimeMillis();
        if (!force && now - p.getLong("wTs", 0) < MAX_AGE) return;
        // Sem internet: não tenta a cada minuto (o relógio chama isto 1x/min); espera 5 min.
        if (!force && now - lastFail < 5 * 60 * 1000L) return;
        busy = true;
        new Thread(() -> {
            boolean ok = false;
            try {
                if (!p.contains("wLat")) {
                    JSONObject loc = new JSONObject(get("https://ipwho.is/?fields=success,city,latitude,longitude"));
                    if (!loc.optBoolean("success")) throw new Exception("loc");
                    p.edit().putFloat("wLat", (float) loc.getDouble("latitude"))
                            .putFloat("wLon", (float) loc.getDouble("longitude"))
                            .putString("wCity", loc.optString("city", null))
                            .putBoolean("wManual", false).apply();
                }
                String url = "https://api.open-meteo.com/v1/forecast?latitude=" + p.getFloat("wLat", 0)
                        + "&longitude=" + p.getFloat("wLon", 0)
                        + "&current=temperature_2m,weather_code,is_day&timezone=auto";
                JSONObject cur = new JSONObject(get(url)).getJSONObject("current");
                p.edit().putFloat("wTemp", (float) cur.getDouble("temperature_2m"))
                        .putInt("wCode", cur.getInt("weather_code"))
                        .putBoolean("wDay", cur.optInt("is_day", 1) == 1)
                        .putLong("wTs", System.currentTimeMillis()).apply();
                ok = true;
            } catch (Exception ignored) {
            } finally {
                busy = false;
            }
            lastFail = ok ? 0 : System.currentTimeMillis();
            if (done != null) new Handler(Looper.getMainLooper()).post(done);
        }, "weather").start();
    }

    /** Busca a cidade pelo nome (Open-Meteo Geocoding). Retorna "Nome, Estado" ou null. */
    static void setCity(Context ctx, String name, java.util.function.Consumer<String> done) {
        final Context c = ctx.getApplicationContext();
        new Thread(() -> {
            String result = null;
            try {
                JSONObject r = new JSONObject(get("https://geocoding-api.open-meteo.com/v1/search?count=1&language=pt&name="
                        + URLEncoder.encode(name.trim(), "UTF-8")));
                JSONArray arr = r.optJSONArray("results");
                if (arr != null && arr.length() > 0) {
                    JSONObject o = arr.getJSONObject(0);
                    String label = o.getString("name");
                    String adm = o.optString("admin1", "");
                    if (!adm.isEmpty() && !adm.equals(label)) label += ", " + adm;
                    Ui.prefs(c).edit().putFloat("wLat", (float) o.getDouble("latitude"))
                            .putFloat("wLon", (float) o.getDouble("longitude"))
                            .putString("wCity", label).putBoolean("wManual", true)
                            .putLong("wTs", 0).apply();
                    result = label;
                }
            } catch (Exception ignored) { }
            final String res = result;
            new Handler(Looper.getMainLooper()).post(() -> done.accept(res));
        }, "geocode").start();
    }

    static void setAuto(Context c) {
        Ui.prefs(c).edit().remove("wLat").remove("wLon").remove("wCity")
                .putBoolean("wManual", false).putLong("wTs", 0).apply();
    }

    static String get(String u) throws Exception {
        HttpURLConnection h = (HttpURLConnection) new URL(u).openConnection();
        h.setConnectTimeout(8000);
        h.setReadTimeout(8000);
        h.setRequestProperty("User-Agent", "RinoStick/1.0");
        try (InputStream in = h.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString("UTF-8");
        } finally {
            h.disconnect();
        }
    }

    // ------------------------------------------------------------------ códigos WMO

    static String label(int code) {
        switch (code) {
            case 0: return "Céu limpo";
            case 1: return "Poucas nuvens";
            case 2: return "Parcialmente nublado";
            case 3: return "Nublado";
            case 45: case 48: return "Neblina";
            case 51: case 53: case 55: case 56: case 57: return "Garoa";
            case 61: return "Chuva fraca";
            case 63: case 66: return "Chuva";
            case 65: case 67: return "Chuva forte";
            case 71: case 73: case 75: case 77: case 85: case 86: return "Neve";
            case 80: case 81: return "Pancadas de chuva";
            case 82: return "Pancadas fortes";
            case 95: return "Trovoadas";
            case 96: case 99: return "Trovoadas com granizo";
            default: return "";
        }
    }

    static Glyph.Kind glyph(int code, boolean day) {
        if (code <= 1) return day ? Glyph.Kind.SUN : Glyph.Kind.MOON;
        if (code == 2) return day ? Glyph.Kind.PARTLY : Glyph.Kind.CLOUD;
        if (code == 3) return Glyph.Kind.CLOUD;
        if (code == 45 || code == 48) return Glyph.Kind.FOG;
        if (code >= 95) return Glyph.Kind.STORM;
        if (code >= 71 && code <= 77 || code == 85 || code == 86) return Glyph.Kind.CLOUD;
        return Glyph.Kind.RAIN;
    }
}
