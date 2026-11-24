package com.rinostick.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.text.InputType;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** Fontes, cores (claro/escuro), sons e menus compartilhados pelas telas. */
final class Ui {

    static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    static Typeface light, regular, medium, semibold, bold;
    static int W, H;
    static float s;

    private Ui() { }

    static void init(Context c) {
        DisplayMetrics dm = c.getResources().getDisplayMetrics();
        W = Math.max(dm.widthPixels, dm.heightPixels);
        H = Math.min(dm.widthPixels, dm.heightPixels);
        s = W / 1920f;
        if (regular != null) return;
        AssetManager am = c.getAssets();
        try {
            light = Typeface.createFromAsset(am, "fonts/Inter-Light.ttf");
            regular = Typeface.createFromAsset(am, "fonts/Inter-Regular.ttf");
            medium = Typeface.createFromAsset(am, "fonts/Inter-Medium.ttf");
            semibold = Typeface.createFromAsset(am, "fonts/Inter-SemiBold.ttf");
            bold = Typeface.createFromAsset(am, "fonts/Inter-Bold.ttf");
        } catch (Exception e) {
            light = Typeface.create("sans-serif-light", Typeface.NORMAL);
            regular = Typeface.create("sans-serif", Typeface.NORMAL);
            medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
            semibold = medium;
            bold = Typeface.create("sans-serif", Typeface.BOLD);
        }
    }

    static float px(float v) {
        return v * s;
    }

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("launcher", Context.MODE_PRIVATE);
    }

    // ------------------------------------------------------------------ aparência

    /** "dark", "light" ou "auto" (escuro das 18h às 6h). */
    static String appearance(Context c) {
        return prefs(c).getString("appearance", "dark");
    }

    static boolean isDark(Context c) {
        String a = appearance(c);
        if ("light".equals(a)) return false;
        if ("auto".equals(a)) {
            int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            return h >= 18 || h < 6;
        }
        return true;
    }

    static String appearanceLabel(String a) {
        if ("light".equals(a)) return "Claro";
        if ("auto".equals(a)) return "Automático";
        return "Escuro";
    }

    static int base(boolean d) { return d ? 0xFF141416 : 0xFFE9E9EE; }
    static int text(boolean d) { return d ? 0xFFFFFFFF : 0xFF1C1C1E; }
    static int text2(boolean d) { return d ? 0x99FFFFFF : 0x8C1C1C1E; }
    static int panel(boolean d) { return d ? 0xFA2C2C2E : 0xFAF7F7F9; }
    static int module(boolean d) { return d ? 0x1AFFFFFF : 0x12000000; }
    // Seleção estilo tvOS: branca nos dois temas; no claro ela "flutua" com sombra (ver lift).
    static int focusBg(boolean d) { return d ? 0xFFF2F2F7 : 0xFFFFFFFF; }
    static int focusText(boolean d) { return 0xFF1C1C1E; }
    static int focusText2(boolean d) { return 0x991C1C1E; }

    /** Sombra suave no item selecionado — no tema claro é o que o destaca do fundo. */
    static void lift(View v, boolean has, boolean dark) {
        v.animate().translationZ(has ? px(dark ? 10 : 22) : 0).setDuration(160).start();
    }
    static int scrim(boolean d) { return d ? 0xB3000000 : 0x66000000; }
    static final int RED = 0xFFFF453A;

    static TextView text(Context c, float sizePx, Typeface tf, int color) {
        TextView t = new TextView(c);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx);
        t.setTypeface(tf);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        return t;
    }

    static GradientDrawable round(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        return g;
    }

    // ------------------------------------------------------------------ sons

    private static SoundPool pool;
    private static int sTick;
    private static long lastTick;

    static void initSounds(Context c) {
        if (pool != null) return;
        pool = new SoundPool.Builder().setMaxStreams(3)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .build();
        Context a = c.getApplicationContext();
        sTick = pool.load(a, R.raw.tick, 1);
    }

    static boolean soundsOn(Context c) {
        return prefs(c).getBoolean("sounds", true);
    }

    private static void play(Context c, int id, float vol) {
        if (pool == null || !soundsOn(c)) return;
        pool.play(id, vol, vol, 1, 0, 1f);
    }

    static void tick(Context c) {
        long now = System.currentTimeMillis();
        if (now - lastTick < 45) return;
        lastTick = now;
        play(c, sTick, 0.32f);
    }

    static void select(Context c) { play(c, sTick, 0.32f); }

    static void back(Context c) { play(c, sTick, 0.32f); }

    // ------------------------------------------------------------------ menus

    static Dialog fullDialog(Activity a) {
        return new Dialog(a, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
    }

    /** Item de lista estilo tvOS: transparente, branco ao receber foco. */
    static TextView listItem(final Activity a, String label, final boolean dark, final boolean destructive) {
        final TextView it = text(a, px(30), regular, destructive ? RED : text(dark));
        it.setText(label);
        it.setGravity(Gravity.CENTER_VERTICAL);
        it.setPadding((int) px(26), 0, (int) px(26), 0);
        it.setSingleLine(true);
        it.setEllipsize(TextUtils.TruncateAt.END);
        it.setFocusable(true);
        it.setClickable(true);
        final GradientDrawable ib = round(Color.TRANSPARENT, px(16));
        it.setBackground(ib);
        it.setOnFocusChangeListener((v, has) -> {
            ib.setColor(has ? focusBg(dark) : Color.TRANSPARENT);
            lift(v, has, dark);
            it.setTextColor(has ? (destructive ? RED : focusText(dark)) : (destructive ? RED : text(dark)));
            it.setTypeface(has ? medium : regular);
            v.animate().scaleX(has ? 1.02f : 1f).scaleY(has ? 1.02f : 1f).setDuration(150).start();
            if (has) tick(a);
        });
        return it;
    }

    /**
     * Menu flutuante central. Itens iniciados com "!" aparecem em vermelho
     * (ações destrutivas).
     */
    static Dialog menu(final Activity a, String title, String[] items, final IntConsumer cb) {
        final boolean dark = isDark(a);
        final Dialog d = fullDialog(a);

        FrameLayout over = new FrameLayout(a);
        over.setBackgroundColor(scrim(dark));

        LinearLayout panel = new LinearLayout(a);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(round(panel(dark), px(34)));
        int pad = (int) px(22);
        panel.setPadding(pad, (int) px(30), pad, pad);

        if (title != null) {
            TextView t = text(a, px(26), medium, text2(dark));
            t.setText(title);
            t.setMaxLines(6);
            t.setLineSpacing(px(6), 1f);
            t.setPadding((int) px(26), 0, (int) px(26), (int) px(18));
            panel.addView(t);
        }

        LinearLayout list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        View first = null;
        for (int i = 0; i < items.length; i++) {
            final int idx = i;
            boolean destructive = items[i].startsWith("!");
            String label = destructive ? items[i].substring(1) : items[i];
            TextView it = listItem(a, label, dark, destructive);
            it.setOnClickListener(v -> {
                select(a);
                d.dismiss();
                cb.accept(idx);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, (int) px(78));
            lp.topMargin = (int) px(4);
            list.addView(it, lp);
            if (first == null) first = it;
        }
        ScrollView sv = new ScrollView(a);
        sv.setVerticalScrollBarEnabled(false);
        sv.addView(list);
        panel.addView(sv, new LinearLayout.LayoutParams(MATCH, WRAP));

        FrameLayout.LayoutParams pl = new FrameLayout.LayoutParams((int) px(680), WRAP, Gravity.CENTER);
        pl.topMargin = (int) px(80);
        pl.bottomMargin = (int) px(80);
        over.addView(panel, pl);
        d.setContentView(over);
        d.setOnKeyListener((di, k, e) -> {
            if (k == KeyEvent.KEYCODE_BACK && e.getAction() == KeyEvent.ACTION_UP) back(a);
            return false;
        });
        d.show();

        panel.setAlpha(0f);
        panel.setScaleX(0.94f);
        panel.setScaleY(0.94f);
        panel.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(220)
                .setInterpolator(new DecelerateInterpolator(2f)).start();
        if (first != null) first.requestFocus();
        return d;
    }

    /** Pede um texto (abre o teclado da TV). */
    static void prompt(final Activity a, String title, String initial, final Consumer<String> cb) {
        final boolean dark = isDark(a);
        final Dialog d = fullDialog(a);
        FrameLayout over = new FrameLayout(a);
        over.setBackgroundColor(scrim(dark));

        LinearLayout panel = new LinearLayout(a);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(round(panel(dark), px(34)));
        int pad = (int) px(34);
        panel.setPadding(pad, pad, pad, pad);

        TextView t = text(a, px(26), medium, text2(dark));
        t.setText(title);
        panel.addView(t);

        final EditText et = new EditText(a);
        et.setText(initial);
        et.setSelectAllOnFocus(true);
        et.setSingleLine(true);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        et.setImeOptions(EditorInfo.IME_ACTION_DONE);
        et.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(34));
        et.setTypeface(medium);
        et.setTextColor(text(dark));
        et.setBackground(round(module(dark), px(14)));
        et.setPadding((int) px(22), (int) px(16), (int) px(22), (int) px(16));
        LinearLayout.LayoutParams el = new LinearLayout.LayoutParams(MATCH, WRAP);
        el.topMargin = (int) px(20);
        panel.addView(et, el);

        TextView ok = listItem(a, "Concluir", dark, false);
        LinearLayout.LayoutParams ol = new LinearLayout.LayoutParams(MATCH, (int) px(78));
        ol.topMargin = (int) px(16);
        panel.addView(ok, ol);

        Runnable done = () -> {
            String v = et.getText().toString().trim();
            d.dismiss();
            if (!v.isEmpty()) cb.accept(v);
        };
        ok.setOnClickListener(v -> { select(a); done.run(); });
        et.setOnEditorActionListener((v, id, e) -> {
            done.run();
            return true;
        });

        over.addView(panel, new FrameLayout.LayoutParams((int) px(720), WRAP, Gravity.CENTER));
        d.setContentView(over);
        d.show();
        et.requestFocus();
    }
}
