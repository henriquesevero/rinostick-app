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
 * Assistente "Configurar o RinoStick": libera as permissões pelo ADB da própria
 * TV (sem computador). O usuário liga a Depuração USB/ADB e aceita o aviso
 * "Permitir depuração". Funciona onde o ADB atende na porta 5555 (a maioria dos
 * Android TV e o Chromecast com Google TV). TVs que só têm "Depuração sem fio"
 * com código de pareamento precisam do computador (dist/instalar.sh).
 */
final class Setup {

    static final String PKG = "com.rinostick.app";
    static final String GUIDE = "github.com/henriquesevero/rinostick-app";

    static String ipLine() {
        String ip = Device.localIp();
        return ip != null ? "IP desta TV: " + ip : "IP desta TV: Configurações → Rede e Internet";
    }

    /** Comando de shell: se `v` não estiver em `k`, grava direto no banco de ajustes (content insert). */
    static String fixSecure(String k, String v) {
        String ins = "content insert --uri content://settings/secure --bind name:s:" + k + " --bind value:s:";
        return "cur=$(settings get secure " + k + "); case \"$cur\" in *" + v + "*) ;; null|'') " + ins + v
                + " ;; *) " + ins + "\"$cur:" + v + "\" ;; esac";
    }

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
                        + "algumas permissões. Antes, ligue a Depuração USB em Configurações → Sistema → "
                        + "Opções do desenvolvedor.\n\nSem Opções do desenvolvedor? Vá em Configurações → Sistema → "
                        + "Sobre e clique 7 vezes em \"Build do Android TV OS\".\n\n" + ipLine(),
                new String[]{"Configurar agora", "Abrir Opções do desenvolvedor", "Agora não"},
                w -> {
                    if (w == 0) run(a, done);
                    else if (w == 1) openDevSettings(a);
                });
    }

    static void openDevSettings(Activity a) {
        try {
            a.startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            Device.openTvSettings(a);
        }
    }

    static void run(final Activity a, final Runnable done) {
        final Handler ui = new Handler(Looper.getMainLooper());
        final Progress p = new Progress(a);
        p.set("Conectando à TV…");
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
                // Fire OS / Android antigo: "settings put" e "cmd notification" não gravam; confere e grava direto.
                adb.shell(fixSecure("enabled_accessibility_services", svc));
                adb.shell(fixSecure("enabled_notification_listeners", PKG + "/" + PKG + ".NowPlayingListener"));
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
                    p.set("Tudo pronto! ✓\n\nDica: por segurança, você já pode desligar a Depuração USB.");
                    ui.postDelayed(() -> { p.dismiss(); if (done != null) done.run(); }, 3500);
                } else {
                    p.dismiss();
                    Ui.menu(a, "Não deu certo\n\n" + (error != null ? error : "Algumas permissões não foram liberadas.")
                                    + "\n\nConfira se a Depuração USB está ligada e aceite o aviso na TV. "
                                    + "Se não funcionar, configure por um computador (Mac ou Windows). Guia: " + GUIDE + "\n\n" + ipLine(),
                            new String[]{"Tentar de novo", "Abrir Opções do desenvolvedor", "Fechar"},
                            w -> {
                                if (w == 0) run(a, done);
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

        Progress(Activity a) { this(a, "Configurar o RinoStick"); }

        Progress(Activity a, String titleText) {
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
            title.setText(titleText);
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
