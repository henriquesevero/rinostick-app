package com.rinostick.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Assistente "Configurar o RinoStick": libera as permissões pelo ADB do próprio
 * Fire TV (sem computador). O usuário só precisa ligar a Depuração ADB e aceitar
 * o aviso "Permitir depuração" na TV.
 */
final class Setup {

    static final String PKG = "com.rinostick.app";
    static final String[] CLEAN_APPS = {
            "com.amazon.spiderpork", "com.amazon.bueller.music", "com.amazon.shoptv.firetv.client",
            "com.amazon.smarthomemapviewapp", "com.amazon.bueller.photos", "com.amazon.tv.earlyaccess",
            "com.amazon.minitv.android.app", "com.amazon.gamehub", "com.amazon.tahoe",
            "com.amazon.whasettings", "com.amazon.tv.releasenotes", "com.amazon.tv.ftvambient"};
    static final String[] AD_KEYS = {
            "advertisingIdApp:interestBasedAds:value", "amz_limit_ad_tracking", "limit_ad_tracking",
            "USAGE_METRICS_UPLOAD_ENABLED", "pact_usage_collection_enabled"};

    static boolean asked; // pergunta automática só uma vez por sessão

    private Setup() { }

    // ------------------------------------------------------------------ estado

    static boolean listenerEnabled(Context c) {
        String s = Settings.Secure.getString(c.getContentResolver(), "enabled_notification_listeners");
        return s != null && s.contains(PKG + "/");
    }

    static boolean canWriteSecure(Context c) {
        return c.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED;
    }

    static boolean ready(Context c) {
        return Device.serviceEnabled(c) && listenerEnabled(c) && canWriteSecure(c);
    }

    // ------------------------------------------------------------------ fluxo

    static void intro(final Activity a, final Runnable done) {
        asked = true;
        Ui.menu(a, "Configurar o RinoStick\n\n"
                        + "Para o botão Home, a música e o protetor de tela funcionarem, o RinoStick precisa de "
                        + "algumas permissões. Antes, ligue a Depuração ADB em Configurações → Minha Fire TV → "
                        + "Opções do desenvolvedor.",
                new String[]{"Configurar agora", "Configurar e remover anúncios", "Abrir Opções do desenvolvedor", "Agora não"},
                w -> {
                    if (w == 0) run(a, false, done);
                    else if (w == 1) confirmClean(a, done);
                    else if (w == 2) openDevSettings(a);
                });
    }

    static void confirmClean(final Activity a, final Runnable done) {
        Ui.menu(a, "Remover anúncios e apps sem uso?\n\n"
                        + "Desliga os anúncios personalizados, a coleta de dados de uso e os apps: Luna, Amazon Music, "
                        + "Amazon Fotos, Shop, Mini TV, Amazon Kids, Central de jogos, Acesso antecipado, "
                        + "Novidades, Tela ambiente, Casa inteligente e Áudio multiambiente. Dá para religar depois.",
                new String[]{"Confirmar", "Cancelar"},
                w -> { if (w == 0) run(a, true, done); });
    }

    static void openDevSettings(Activity a) {
        try {
            a.startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            Device.openFireSettings(a);
        }
    }

    static void run(final Activity a, final boolean clean, final Runnable done) {
        final Handler ui = new Handler(Looper.getMainLooper());
        final Progress p = new Progress(a);
        p.set("Conectando ao Fire TV…");
        new Thread(() -> {
            String err = null;
            try (AdbSelf adb = new AdbSelf(a, () -> ui.post(() -> p.set(
                    "Na TV, aceite o aviso \"Permitir depuração\"\ne marque \"Sempre permitir deste computador\".")))) {
                ui.post(() -> p.set("Configurando…"));
                adb.shell("pm grant " + PKG + " android.permission.WRITE_SECURE_SETTINGS");
                String svc = PKG + "/" + PKG + ".HomeRedirectService";
                adb.shell("cur=$(settings get secure enabled_accessibility_services); case \"$cur\" in *" + svc
                        + "*) ;; null|'') settings put secure enabled_accessibility_services " + svc
                        + " ;; *) settings put secure enabled_accessibility_services \"$cur:" + svc
                        + "\" ;; esac; settings put secure accessibility_enabled 1");
                adb.shell("cmd notification allow_listener " + PKG + "/" + PKG + ".NowPlayingListener");
                if (clean) {
                    ui.post(() -> p.set("Removendo anúncios e apps sem uso…"));
                    StringBuilder q = new StringBuilder();
                    for (String k : AD_KEYS) q.append("echo ").append(k).append(" $(settings get secure ").append(k).append("); ");
                    String orig = adb.shell(q.toString());
                    Ui.prefs(a).edit().putString("cleanBackup", orig).apply();
                    adb.shell("settings put secure advertisingIdApp:interestBasedAds:value 0; "
                            + "settings put secure amz_limit_ad_tracking 1; settings put secure limit_ad_tracking 1; "
                            + "settings put secure USAGE_METRICS_UPLOAD_ENABLED 0; "
                            + "settings put secure pact_usage_collection_enabled 0");
                    StringBuilder d = new StringBuilder("for p in");
                    for (String pk : CLEAN_APPS) d.append(' ').append(pk);
                    d.append("; do pm disable-user --user 0 $p >/dev/null 2>&1 && echo $p; done");
                    String off = adb.shell(d.toString());
                    Ui.prefs(a).edit().putString("cleanDisabled", off).apply();
                }
            } catch (Exception e) {
                err = e.getMessage() != null ? e.getMessage() : "Não foi possível configurar.";
            }
            // O sistema leva um instante para registrar as permissões: espera até 5 s.
            if (err == null) {
                for (int i = 0; i < 25 && !ready(a); i++) {
                    try { Thread.sleep(200); } catch (InterruptedException ignored) { }
                }
            }
            final String error = err;
            ui.post(() -> {
                if (error == null && ready(a)) {
                    if (Ui.prefs(a).getBoolean("ssWanted", true)) Device.setOurScreensaver(a, true);
                    p.set("Tudo pronto! ✓\n\nDica: por segurança, você já pode desligar a Depuração ADB.");
                    ui.postDelayed(() -> { p.dismiss(); if (done != null) done.run(); }, 3500);
                } else {
                    p.dismiss();
                    Ui.menu(a, "Não deu certo\n\n" + (error != null ? error : "Algumas permissões não foram liberadas.")
                                    + "\n\nConfira se a Depuração ADB está ligada e aceite o aviso na TV.",
                            new String[]{"Tentar de novo", "Abrir Opções do desenvolvedor", "Fechar"},
                            w -> {
                                if (w == 0) run(a, clean, done);
                                else if (w == 1) openDevSettings(a);
                            });
                }
            });
        }, "setup").start();
    }

    /** Janelinha de progresso no estilo dos menus. */
    static final class Progress {
        final Dialog d;
        final TextView msg;

        Progress(Activity a) {
            boolean dark = Ui.isDark(a);
            d = Ui.fullDialog(a);
            FrameLayout over = new FrameLayout(a);
            over.setBackgroundColor(Ui.scrim(dark));
            LinearLayout panel = new LinearLayout(a);
            panel.setOrientation(LinearLayout.VERTICAL);
            panel.setBackground(Ui.round(Ui.panel(dark) | 0xFF000000, Ui.px(34)));
            int pad = (int) Ui.px(44);
            panel.setPadding(pad, pad, pad, pad);
            TextView title = Ui.text(a, Ui.px(36), Ui.semibold, Ui.text(dark));
            title.setText("Configurar o RinoStick");
            panel.addView(title);
            msg = Ui.text(a, Ui.px(28), Ui.regular, Ui.text2(dark));
            msg.setLineSpacing(Ui.px(8), 1f);
            msg.setPadding(0, (int) Ui.px(20), 0, 0);
            panel.addView(msg);
            over.addView(panel, new FrameLayout.LayoutParams((int) Ui.px(820), Ui.WRAP, Gravity.CENTER));
            d.setContentView(over);
            d.setOnKeyListener((di, k, e) -> k == KeyEvent.KEYCODE_BACK); // não fecha no meio
            d.show();
        }

        void set(String s) { msg.setText(s); }

        void dismiss() { try { d.dismiss(); } catch (Exception ignored) { } }
    }
}
