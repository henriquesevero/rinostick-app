package com.rinostick.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;

/** Ações que mexem no sistema do Fire TV (e como desfazê-las). */
final class Device {

    static final String AMAZON_LAUNCHER = HomeRedirectService.AMAZON_LAUNCHER;
    static final String SS_KEY = "screensaver_components";

    private Device() { }

    static String ourScreensaver(Context c) {
        return new ComponentName(c, Screensaver.class).flattenToString();
    }

    static boolean serviceEnabled(Context c) {
        String en = Settings.Secure.getString(c.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return en != null && en.contains(c.getPackageName() + "/");
    }

    /**
     * Primeiro nome da conta Amazon logada (ex.: "Maria"). Se o sistema não
     * expuser a conta, tenta o nome do aparelho ("Fire TV Stick de Maria").
     */
    static String firstName(Context c) {
        try {
            for (android.accounts.Account a : android.accounts.AccountManager.get(c).getAccountsByType("com.amazon.account")) {
                String n = a.name != null ? a.name.trim() : "";
                if (n.isEmpty() || n.contains("@") || !Character.isLetter(n.charAt(0))) continue;
                return n.split("\\s+")[0];
            }
        } catch (Exception ignored) { }
        ContentResolver cr = c.getContentResolver();
        String dev = null;
        try { dev = Settings.Global.getString(cr, "device_name"); } catch (Exception ignored) { }
        if (dev == null || !dev.contains(" de ")) {
            try { dev = Settings.Secure.getString(cr, "bluetooth_name"); } catch (Exception ignored) { }
        }
        if (dev != null) {
            int i = dev.lastIndexOf(" de ");
            if (i >= 0) {
                String n = dev.substring(i + 4).trim();
                if (!n.isEmpty()) return n.split("\\s+")[0];
            }
        }
        return "";
    }

    /** IP da TV na rede local (Wi-Fi ou cabo), para o usuário digitar no computador. */
    static String localIp() {
        try {
            for (java.net.NetworkInterface ni : java.util.Collections.list(java.net.NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (java.net.InetAddress ad : java.util.Collections.list(ni.getInetAddresses())) {
                    if (ad instanceof java.net.Inet4Address && !ad.isLoopbackAddress()) return ad.getHostAddress();
                }
            }
        } catch (Exception ignored) { }
        return null;
    }

    // ------------------------------------------------------------------ protetor de tela

    /** Na primeira execução, guarda o protetor original e liga o nosso. */
    static void initScreensaver(Context c) {
        SharedPreferences p = Ui.prefs(c);
        if (p.getBoolean("ssInit", false)) return;
        String cur = Settings.Secure.getString(c.getContentResolver(), SS_KEY);
        p.edit().putString("ssOrig", cur).putBoolean("ssInit", true).apply();
        setOurScreensaver(c, true);
    }

    static boolean ourScreensaverActive(Context c) {
        String cur = Settings.Secure.getString(c.getContentResolver(), SS_KEY);
        return cur != null && cur.equals(ourScreensaver(c));
    }

    static boolean setOurScreensaver(Context c, boolean ours) {
        SharedPreferences p = Ui.prefs(c);
        String orig = p.getString("ssOrig", null);
        try {
            Settings.Secure.putString(c.getContentResolver(), SS_KEY,
                    ours ? ourScreensaver(c) : orig);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ------------------------------------------------------------------ atalhos

    static void openFireSettings(Activity a) {
        try {
            a.startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            Intent i = a.getPackageManager().getLeanbackLaunchIntentForPackage("com.amazon.tv.settings.v2");
            if (i != null) a.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    static void openNetwork(Activity a) {
        try {
            a.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            openFireSettings(a);
        }
    }

    /** Libera a tela da Amazon (loja, busca) por 10 minutos. */
    static void openAmazonHome(Activity a) {
        HomeRedirectService.pauseUntil = System.currentTimeMillis() + 10 * 60 * 1000L;
        try {
            a.startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .setPackage(AMAZON_LAUNCHER)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            Toast.makeText(a, "Tela da Amazon liberada por 10 minutos", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            HomeRedirectService.pauseUntil = 0;
        }
    }

    static void appInfo(Activity a, String pkg) {
        try {
            a.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", pkg, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            Toast.makeText(a, "Indisponível", Toast.LENGTH_SHORT).show();
        }
    }

    static void sleep(Activity a) {
        if (!HomeRedirectService.sleep()) {
            Toast.makeText(a, "Para dormir, segure o botão Home e escolha \"Suspender\"",
                    Toast.LENGTH_LONG).show();
        }
    }

    // ------------------------------------------------------------------ restauração

    static volatile boolean restoreInProgress;

    /** Desfaz tudo o que este launcher mudou e pede a desinstalação dele. */
    static void restoreOriginal(Activity a) {
        ContentResolver cr = a.getContentResolver();
        String me = a.getPackageName() + "/";
        try {
            String en = Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            StringBuilder rest = new StringBuilder();
            if (en != null) {
                for (String s : en.split(":")) {
                    if (s.isEmpty() || s.startsWith(me)) continue;
                    if (rest.length() > 0) rest.append(':');
                    rest.append(s);
                }
            }
            Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    rest.length() == 0 ? null : rest.toString());
            if (rest.length() == 0) Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 0);
        } catch (Exception ignored) { }
        if (ourScreensaverActive(a)) setOurScreensaver(a, false);
        HomeRedirectService.shutdown();
        HomeRedirectService.pauseUntil = Long.MAX_VALUE;

        restoreInProgress = true;
        try {
            a.startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + a.getPackageName()))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            Toast.makeText(a, "Desinstale \"RinoStick\" em Configurações → Aplicativos", Toast.LENGTH_LONG).show();
            openAmazonHome(a);
        }
    }

    /** Usuário cancelou a desinstalação: religa tudo como estava. */
    static void undoRestoreIfNeeded(Activity a) {
        if (!restoreInProgress) return;
        restoreInProgress = false;
        try {
            ContentResolver cr = a.getContentResolver();
            String svc = a.getPackageName() + "/" + HomeRedirectService.class.getName();
            String en = Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (en == null || en.isEmpty() || "null".equals(en)) en = svc;
            else if (!en.contains(svc)) en = en + ":" + svc;
            Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, en);
            Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 1);
        } catch (Exception ignored) { }
        if (Ui.prefs(a).getBoolean("ssWanted", true)) setOurScreensaver(a, true);
        HomeRedirectService.pauseUntil = 0;
        Toast.makeText(a, "Restauração cancelada", Toast.LENGTH_SHORT).show();
    }
}
