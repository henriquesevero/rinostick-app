package com.rinostick.app;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.accessibility.AccessibilityEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Google TV / Android TV: o Google TV não deixa trocar a tela inicial pelas
 * configurações. Este serviço observa quando a tela inicial do sistema (Google TV,
 * Android TV ou a do fabricante) vem para frente e abre o RinoStick por cima.
 * Se o usuário escolher o RinoStick como tela inicial padrão (possível em alguns
 * Android TV), o sistema já abre o RinoStick e este serviço nem age.
 */
public class HomeRedirectService extends AccessibilityService {

    /** Telas iniciais conhecidas do Google (as do fabricante são descobertas em tempo real). */
    static final String[] KNOWN_LAUNCHERS = {
            "com.google.android.apps.tv.launcherx",   // Google TV
            "com.google.android.tvlauncher",          // Android TV
            "com.google.android.leanbacklauncher"};   // Android TV antigo

    /** Apps que se declaram "tela inicial" mas não são (telas de emergência do sistema). */
    static final Set<String> NOT_LAUNCHERS = new HashSet<>(java.util.Arrays.asList(
            "android", "com.android.settings", "com.android.tv.settings"));

    /** Permite abrir a tela do Google de propósito por um tempo. */
    static volatile long pauseUntil = 0;

    static volatile HomeRedirectService instance;

    /** "pacote/classe" das telas iniciais do sistema. */
    private Set<String> homes = new HashSet<>();
    private Set<String> homePkgs = new HashSet<>();

    @Override
    protected void onServiceConnected() {
        instance = this;
        refreshHomes();
        reopenAfterUpdate();
    }

    /**
     * Logo depois de uma atualização do RinoStick o app é fechado e o sistema volta
     * para a tela original; nenhum evento de janela acontece até o próximo Home.
     * Se o serviço religou até 2 min após a atualização, abre o RinoStick.
     */
    private void reopenAfterUpdate() {
        try {
            long since = System.currentTimeMillis()
                    - getPackageManager().getPackageInfo(getPackageName(), 0).lastUpdateTime;
            if (since >= 0 && since < 2 * 60 * 1000L) {
                startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION));
            }
        } catch (Exception ignored) { }
    }

    @Override
    public void onDestroy() {
        if (instance == this) instance = null;
        super.onDestroy();
    }

    /** Pacote da tela inicial original do sistema (para abrir de propósito). */
    static String systemLauncher(Context c) {
        PackageManager pm = c.getPackageManager();
        for (String k : KNOWN_LAUNCHERS) {
            try { pm.getPackageInfo(k, 0); return k; } catch (Exception ignored) { }
        }
        Intent q = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
            String p = ri.activityInfo.packageName;
            if (!p.equals(c.getPackageName()) && !NOT_LAUNCHERS.contains(p)) return p;
        }
        return null;
    }

    private void refreshHomes() {
        Set<String> h = new HashSet<>(), hp = new HashSet<>();
        try {
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
            for (ResolveInfo ri : getPackageManager().queryIntentActivities(q, 0)) {
                ActivityInfo ai = ri.activityInfo;
                if (ai.packageName.equals(getPackageName()) || NOT_LAUNCHERS.contains(ai.packageName)) continue;
                h.add(ai.packageName + "/" + ai.name);
                hp.add(ai.packageName);
            }
        } catch (Exception ignored) { }
        for (String k : KNOWN_LAUNCHERS) hp.add(k);
        homes = h;
        homePkgs = hp;
    }

    /** Desliga o serviço (usado ao restaurar a TV original). */
    static void shutdown() {
        HomeRedirectService s = instance;
        if (s != null) {
            try { s.disableSelf(); } catch (Exception ignored) { }
        }
    }

    /** Coloca o aparelho para dormir (ação global de bloqueio de tela). */
    static boolean sleep() {
        HomeRedirectService s = instance;
        if (s == null) return false;
        try {
            return s.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e == null || e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence pkg = e.getPackageName();
        CharSequence cls = e.getClassName();
        if (pkg == null || cls == null) return;
        String p = pkg.toString();
        if (!homePkgs.contains(p)) return;
        String c = cls.toString();
        // Só a tela principal do launcher: outras janelas dele (busca, perfis) passam.
        boolean isHome = homes.contains(p + "/" + c) || c.contains("HomeActivity") || c.endsWith("MainActivity");
        if (!isHome) return;
        if (System.currentTimeMillis() < pauseUntil) return;

        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        try {
            startActivity(i);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onInterrupt() {
    }
}
