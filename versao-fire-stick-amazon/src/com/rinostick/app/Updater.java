package com.rinostick.app;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Atualização pelo próprio app: uma vez por dia consulta a última release do
 * GitHub; se houver versão mais nova, avisa (tela inicial e Ajustes) e, quando o
 * usuário pede, baixa o APK e abre a confirmação de instalação do sistema.
 *
 * Regra para publicar: a tag da release é "v" + versionName (ex.: v1.5) e o APK
 * anexado tem o nome {@link #APK} — o mesmo que os guias usam.
 */
final class Updater {

    static final String REPO = "henriquesevero/rinostick-app";
    static final String APK = "RinoStick.apk";
    static final String UNKNOWN_SOURCES_PATH =
            "Configurações → Minha Fire TV → Opções do desenvolvedor → Instalar apps desconhecidos → RinoStick";
    static final long DAY = 24 * 60 * 60 * 1000L;

    private Updater() { }

    // ------------------------------------------------------------------ versão

    static String current(Context c) {
        try {
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "0";
        }
    }

    /** Compara "1.4.5" com "1.5" número a número. */
    static boolean newer(String latest, String cur) {
        String[] a = latest.replaceAll("[^0-9.]", "").split("\\."), b = cur.replaceAll("[^0-9.]", "").split("\\.");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length && !a[i].isEmpty() ? Integer.parseInt(a[i]) : 0;
            int y = i < b.length && !b[i].isEmpty() ? Integer.parseInt(b[i]) : 0;
            if (x != y) return x > y;
        }
        return false;
    }

    /** Versão mais nova disponível (sem o "v"), ou null se o app já está em dia. */
    static String available(Context c) {
        String latest = Ui.prefs(c).getString("updLatest", null);
        return latest != null && newer(latest, current(c)) ? latest : null;
    }

    // ------------------------------------------------------------------ verificação

    /** Consulta o GitHub (no máximo uma vez por dia, a não ser que `force`). */
    static void check(final Context c, boolean force, final Runnable done) {
        final SharedPreferences p = Ui.prefs(c);
        if (!force && System.currentTimeMillis() - p.getLong("updTs", 0) < DAY) {
            if (done != null) done.run();
            return;
        }
        final Handler ui = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                HttpURLConnection h = (HttpURLConnection) new URL(
                        "https://api.github.com/repos/" + REPO + "/releases/latest").openConnection();
                h.setRequestProperty("User-Agent", "RinoStick");
                h.setRequestProperty("Accept", "application/vnd.github+json");
                h.setConnectTimeout(10000);
                h.setReadTimeout(10000);
                int code = h.getResponseCode();
                if (code == 200) {
                    String tag = new JSONObject(read(h.getInputStream())).optString("tag_name", "");
                    if (!tag.isEmpty()) {
                        p.edit().putString("updLatest", tag.startsWith("v") ? tag.substring(1) : tag)
                                .putLong("updTs", System.currentTimeMillis()).apply();
                    }
                }
                else p.edit().putLong("updTs", System.currentTimeMillis() - DAY + 60 * 60 * 1000L).apply(); // tenta de novo em 1 h
                h.disconnect();
            } catch (Exception e) {
                // sem internet: tenta de novo em 1 h (e não a cada volta à tela inicial)
                p.edit().putLong("updTs", System.currentTimeMillis() - DAY + 60 * 60 * 1000L).apply();
            }
            if (done != null) ui.post(done);
        }, "update-check").start();
    }

    // ------------------------------------------------------------------ instalação

    static boolean canInstall(Context c) {
        return Build.VERSION.SDK_INT < 26 || c.getPackageManager().canRequestPackageInstalls();
    }

    /** Fluxo do botão "Atualizar" nos Ajustes. */
    static void start(final Activity a) {
        final String v = available(a);
        if (v == null) return;
        if (!canInstall(a)) {
            Ui.menu(a, "Permitir atualizações\n\nNa primeira vez, o sistema precisa deixar o RinoStick instalar "
                            + "a própria atualização. Na tela que vai abrir, ligue o RinoStick e depois volte aqui "
                            + "e escolha \"Atualizar\" de novo.\n\nSe a tela não abrir: " + UNKNOWN_SOURCES_PATH + ".",
                    new String[]{"Abrir a permissão", "Agora não"},
                    w -> { if (w == 0) openUnknownSources(a); });
            return;
        }
        final Handler ui = new Handler(Looper.getMainLooper());
        final Setup.Progress pr = new Setup.Progress(a, "Atualizar o RinoStick");
        pr.set("Baixando a versão " + v + "…");
        new Thread(() -> {
            String err = null;
            try {
                File f = new File(a.getCacheDir(), "update.apk");
                download("https://github.com/" + REPO + "/releases/latest/download/" + APK, f,
                        pct -> ui.post(() -> pr.set("Baixando a versão " + v + "… " + pct + "%")));
                ui.post(() -> pr.set("Preparando a instalação…"));
                install(a, f);
                f.delete(); // o instalador já copiou o arquivo
            } catch (Exception e) {
                err = e.getMessage() != null ? e.getMessage() : "erro desconhecido";
            }
            final String error = err;
            ui.post(() -> {
                pr.dismiss();
                if (error != null) {
                    Ui.menu(a, "Não deu para atualizar\n\n" + error + "\n\nConfira a internet e tente de novo.",
                            new String[]{"Tentar de novo", "Fechar"}, w -> { if (w == 0) start(a); });
                }
            });
        }, "update-download").start();
    }

    static void openUnknownSources(Activity a) {
        try {
            a.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + a.getPackageName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            Toast.makeText(a, UNKNOWN_SOURCES_PATH, Toast.LENGTH_LONG).show();
        }
    }

    interface Progress { void on(int pct); }

    static void download(String url, File out, Progress pg) throws Exception {
        HttpURLConnection h = (HttpURLConnection) new URL(url).openConnection();
        h.setRequestProperty("User-Agent", "RinoStick");
        h.setInstanceFollowRedirects(true);
        h.setConnectTimeout(15000);
        h.setReadTimeout(30000);
        int code = h.getResponseCode();
        if (code != 200) throw new Exception("O servidor respondeu " + code + ".");
        long total = h.getContentLengthLong(), got = 0;
        int last = -1;
        try (InputStream in = h.getInputStream(); OutputStream o = new FileOutputStream(out)) {
            byte[] buf = new byte[16384];
            int r;
            while ((r = in.read(buf)) > 0) {
                o.write(buf, 0, r);
                got += r;
                int pct = total > 0 ? (int) (got * 100 / total) : -1;
                if (pct != last && pct >= 0) { last = pct; pg.on(pct); }
            }
        } finally {
            h.disconnect();
        }
        if (got < 100_000) throw new Exception("O arquivo baixado está incompleto.");
    }

    /** Entrega o APK ao instalador do sistema; a confirmação chega em {@link Result}. */
    static void install(Context c, File apk) throws Exception {
        PackageInstaller pi = c.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams sp = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        sp.setAppPackageName(c.getPackageName());
        int id = pi.createSession(sp);
        try (PackageInstaller.Session s = pi.openSession(id)) {
            try (OutputStream o = s.openWrite("base.apk", 0, apk.length()); InputStream in = new FileInputStream(apk)) {
                byte[] buf = new byte[65536];
                int r;
                while ((r = in.read(buf)) > 0) o.write(buf, 0, r);
                s.fsync(o);
            }
            int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? 0x02000000 /* FLAG_MUTABLE */ : 0);
            PendingIntent pend = PendingIntent.getBroadcast(c, id, new Intent(c, Result.class), flags);
            s.commit(pend.getIntentSender());
        }
    }

    /** Resposta do instalador: abre a tela de confirmação ou avisa o erro. */
    public static final class Result extends BroadcastReceiver {
        @Override public void onReceive(Context c, Intent i) {
            int st = i.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
            if (st == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                Intent confirm = i.getParcelableExtra(Intent.EXTRA_INTENT);
                if (confirm != null) {
                    confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try { c.startActivity(confirm); } catch (Exception ignored) { }
                }
            } else if (st != PackageInstaller.STATUS_SUCCESS) {
                String m = i.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
                Toast.makeText(c, "Atualização não instalada" + (m != null ? ": " + m : ""), Toast.LENGTH_LONG).show();
            }
        }
    }

    static String read(InputStream in) throws Exception {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        byte[] b = new byte[8192];
        int r;
        while ((r = in.read(b)) > 0) o.write(b, 0, r);
        in.close();
        return o.toString("UTF-8");
    }
}
