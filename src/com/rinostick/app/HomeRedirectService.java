package com.rinostick.app;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Fire OS 8 não deixa trocar o launcher padrão. Este serviço observa quando a
 * tela inicial da Amazon vem para frente (botão Home, boot, saída de apps) e
 * abre o nosso launcher por cima dela imediatamente.
 */
public class HomeRedirectService extends AccessibilityService {

    static final String AMAZON_LAUNCHER = "com.amazon.tv.launcher";

    /** Permite abrir a tela da Amazon de propósito por um tempo. */
    static volatile long pauseUntil = 0;

    static volatile HomeRedirectService instance;

    @Override
    protected void onServiceConnected() {
        instance = this;
    }

    @Override
    public void onDestroy() {
        if (instance == this) instance = null;
        super.onDestroy();
    }

    /** Desliga o serviço (usado ao restaurar o Fire TV original). */
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
        if (!AMAZON_LAUNCHER.contentEquals(pkg)) return;
        if (!cls.toString().contains("HomeActivity")) return;
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
