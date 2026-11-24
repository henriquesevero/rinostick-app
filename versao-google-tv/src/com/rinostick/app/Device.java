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

/** Ações que mexem no sistema da TV (Google TV / Android TV) e como desfazê-las. */
final class Device {

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
     * Nome da saudação. O Google não deixa apps lerem o nome da conta, então vale o
     * nome escolhido em Ajustes → Seu nome; sem ele, tenta o nome do aparelho
     * ("TV de Maria"). Vazio = saudação sem nome.
     */
    static String firstName(Context c) {
        String saved = Ui.prefs(c).getString("userName", null);
        if (saved != null) return saved.trim();
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

    static void openTvSettings(Activity a) {
        try {
            a.startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            Intent i = a.getPackageManager().getLeanbackLaunchIntentForPackage("com.android.tv.settings");
            if (i != null) a.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    static void openNetwork(Activity a) {
        try {
            a.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            openTvSettings(a);
        }
    }

    /** Abre a Google Play para instalar apps. */
    static void openPlayStore(Activity a) {
        Intent i = a.getPackageManager().getLeanbackLaunchIntentForPackage("com.android.vending");
        if (i == null) i = a.getPackageManager().getLaunchIntentForPackage("com.android.vending");
        if (i != null) {
            a.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else {
            Toast.makeText(a, "Google Play indisponível", Toast.LENGTH_SHORT).show();
        }
    }

    /** Libera a tela original do Google (busca, recomendações) por 10 minutos. */
    static void openSystemHome(Activity a) {
        String pkg = HomeRedirectService.systemLauncher(a);
        if (pkg == null) {
            Toast.makeText(a, "Tela original não encontrada", Toast.LENGTH_SHORT).show();
            return;
        }
        HomeRedirectService.pauseUntil = System.currentTimeMillis() + 10 * 60 * 1000L;
        try {
            a.startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .setPackage(pkg)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            Toast.makeText(a, "Tela do Google liberada por 10 minutos", Toast.LENGTH_LONG).show();
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
            Toast.makeText(a, "Desinstale \"RinoStick\" em Configurações → Apps", Toast.LENGTH_LONG).show();
            openSystemHome(a);
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
