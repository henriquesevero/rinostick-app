package com.rinostick.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ajustes como painel lateral de vidro (estilo da barra lateral do tvOS),
 * flutuando por cima da tela inicial.
 */
public class SettingsActivity extends Activity {

    boolean dark;
    LinearLayout list;
    LinearLayout panel;
    String focusRow;
    boolean closing;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.init(this);
        Ui.initSounds(this);
        build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Device.undoRestoreIfNeeded(this);
        if (Ui.isDark(this) != dark) build();
        else refresh();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.KEYCODE_BACK || e.getKeyCode() == KeyEvent.KEYCODE_MENU) {
            if (e.getAction() == KeyEvent.ACTION_UP) close();
            return true;
        }
        return super.dispatchKeyEvent(e);
    }

    void close() {
        if (closing) return;
        closing = true;
        Ui.back(this);
        float w = panel.getWidth() + Ui.px(40);
        panel.animate().translationX(-w * 0.25f).alpha(0f).setDuration(180).start();
        getWindow().getDecorView().animate().alpha(0f).setDuration(200)
                .withEndAction(() -> {
                    finish();
                    overridePendingTransition(0, 0);
                }).start();
    }

    // ------------------------------------------------------------------ layout

    int glass() { return dark ? 0xF02C2C2F : 0xF5F4F4F6; }
    int pill() { return dark ? 0xF2EDEDF0 : 0xFFFFFFFF; }

    void build() {
        dark = Ui.isDark(this);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(dark ? 0x59000000 : 0x33000000); // leve escurecida na tela inicial

        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(Ui.round(Ui.panel(dark) | 0xFF000000, Ui.px(40))); // sólido: nada de trás vaza
        int pad = (int) Ui.px(30);
        panel.setPadding(pad, (int) Ui.px(40), pad, pad);

        // Cabeçalho: hora e data (estilo Central de Controle)
        TextView title = Ui.text(this, Ui.px(84), Ui.light, Ui.text(dark));
        title.setText(DateFormat.format(DateFormat.is24HourFormat(this) ? "HH:mm" : "h:mm", System.currentTimeMillis()));
        title.setPadding((int) Ui.px(10), 0, 0, 0);
        panel.addView(title);
        TextView sub = Ui.text(this, Ui.px(24), Ui.medium, Ui.text2(dark));
        String ds = new java.text.SimpleDateFormat("EEEE, d 'de' MMMM", new java.util.Locale("pt", "BR")).format(new java.util.Date());
        sub.setText(ds.substring(0, 1).toUpperCase() + ds.substring(1));
        sub.setPadding((int) Ui.px(12), (int) Ui.px(8), 0, (int) Ui.px(24));
        panel.addView(sub);

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setClipToPadding(false);
        sv.setClipChildren(false);
        sv.setPadding(0, (int) Ui.px(6), 0, (int) Ui.px(6));
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setClipChildren(false);
        list.setClipToPadding(false);
        list.setPadding((int) Ui.px(6), 0, (int) Ui.px(6), 0);
        sv.addView(list);
        panel.addView(sv, new LinearLayout.LayoutParams(Ui.MATCH, 0, 1f));

        int m = (int) Ui.px(40);
        FrameLayout.LayoutParams pl = new FrameLayout.LayoutParams((int) Ui.px(600), Ui.MATCH, Gravity.START);
        pl.leftMargin = m;
        pl.topMargin = m;
        pl.bottomMargin = m;
        root.addView(panel, pl);

        setContentView(root);
        refresh();

        // Entrada: painel desliza da direita.
        root.setAlpha(0f);
        root.animate().alpha(1f).setDuration(200).start();
        panel.setTranslationX(-Ui.px(120));
        panel.animate().translationX(0).setDuration(280)
                .setInterpolator(new DecelerateInterpolator(2f)).start();
    }

    /** Ícone "Ajustes" (cartão prateado com engrenagem) — usado também na tela inicial. */
    static FrameLayout settingsIcon(android.content.Context c, int w, int h) {
        FrameLayout card = new FrameLayout(c);
        final float radius = Ui.px(14) * (w / Ui.px(310));
        card.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius);
            }
        });
        card.setClipToOutline(true);
        card.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFFEDEDF0, 0xFFC7C7CC, 0xFFA6A6AB}));
        ImageView g = new ImageView(c);
        g.setImageDrawable(new Glyph(Glyph.Kind.GEAR, 0xFF5A5A5F));
        int gs = (int) (h * 0.62f);
        card.addView(g, new FrameLayout.LayoutParams(gs, gs, Gravity.CENTER));
        return card;
    }

    interface Action { void run(); }

    void row(String key, Glyph.Kind kind, String label, String value, boolean destructive, Action action) {
        final LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setFocusable(true);
        r.setClickable(true);
        r.setTag(key);
        r.setPadding((int) Ui.px(18), 0, (int) Ui.px(24), 0);
        final GradientDrawable bg = Ui.round(Ui.module(dark), Ui.px(26));
        r.setBackground(bg);

        FrameLayout circle = new FrameLayout(this);
        final GradientDrawable cb = new GradientDrawable();
        cb.setShape(GradientDrawable.OVAL);
        cb.setColor(dark ? 0x33FFFFFF : 0x1A000000);
        circle.setBackground(cb);
        final int iconColor = destructive ? Ui.RED : Ui.text(dark);
        final Glyph g = new Glyph(kind, iconColor);
        ImageView iv = new ImageView(this);
        iv.setImageDrawable(g);
        int is = (int) Ui.px(32);
        circle.addView(iv, new FrameLayout.LayoutParams(is, is, Gravity.CENTER));
        int cs = (int) Ui.px(62);
        r.addView(circle, new LinearLayout.LayoutParams(cs, cs));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        final int normal = destructive ? Ui.RED : Ui.text(dark);
        final TextView l = Ui.text(this, Ui.px(28), Ui.medium, normal);
        l.setText(label);
        l.setSingleLine(true);
        l.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(l);
        final TextView v = Ui.text(this, Ui.px(22), Ui.regular, Ui.text2(dark));
        if (value != null) {
            v.setText(value);
            v.setPadding(0, (int) Ui.px(6), 0, 0);
            texts.addView(v);
        }
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, Ui.WRAP, 1f);
        tl.leftMargin = (int) Ui.px(20);
        r.addView(texts, tl);

        r.setOnFocusChangeListener((vv, has) -> {
            bg.setColor(has ? Ui.focusBg(dark) : Ui.module(dark));
            Ui.lift(vv, has, dark);
            l.setTextColor(has ? (destructive ? Ui.RED : Ui.focusText(dark)) : normal);
            v.setTextColor(has ? Ui.focusText2(dark) : Ui.text2(dark));
            g.setColor(has ? (destructive ? Ui.RED : Ui.focusText(dark)) : iconColor);
            cb.setColor(has ? 0x1A000000 : (dark ? 0x33FFFFFF : 0x1A000000));
            vv.animate().scaleX(has ? 1.04f : 1f).scaleY(has ? 1.04f : 1f).setDuration(160).start();
            if (has) {
                focusRow = key;
                Ui.tick(this);
            }
        });
        r.setOnClickListener(x -> {
            Ui.select(this);
            action.run();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.MATCH, (int) Ui.px(96));
        lp.bottomMargin = (int) Ui.px(12);
        list.addView(r, lp);
    }

    void section(String text) {
        TextView t = Ui.text(this, Ui.px(22), Ui.medium, Ui.text2(dark));
        t.setText(text);
        t.setPadding((int) Ui.px(12), (int) Ui.px(18), 0, (int) Ui.px(14));
        list.addView(t);
    }

    void refresh() {
        if (list == null) return;
        list.removeAllViews();
        String ap = Ui.appearance(this);
        int hidden = Ui.prefs(this).getStringSet("hidden", MainActivity.DEFAULT_HIDDEN).size();

        row("appearance", Glyph.Kind.APPEARANCE, "Aparência", Ui.appearanceLabel(ap), false, () -> {
            String next = "dark".equals(ap) ? "light" : "light".equals(ap) ? "auto" : "dark";
            Ui.prefs(this).edit().putString("appearance", next).apply();
            if (Ui.isDark(this) != dark) build(); else refresh();
        });
        row("sounds", Glyph.Kind.SPEAKER, "Sons de navegação", Ui.soundsOn(this) ? "Ativados" : "Desativados", false, () -> {
            Ui.prefs(this).edit().putBoolean("sounds", !Ui.soundsOn(this)).apply();
            refresh();
        });
        row("screensaver", Glyph.Kind.TV, "Protetor de tela", Device.ourScreensaverActive(this) ? "Aurora" : "Padrão da TV", false, () -> {
            boolean ours = !Device.ourScreensaverActive(this);
            if (Device.setOurScreensaver(this, ours)) {
                Ui.prefs(this).edit().putBoolean("ssWanted", ours).apply();
            } else {
                Toast.makeText(this, "Sem permissão para mudar o protetor", Toast.LENGTH_SHORT).show();
            }
            refresh();
        });
        row("weather", Glyph.Kind.PARTLY, "Clima", weatherValue(), false, this::weatherMenu);
        String nm = Device.firstName(this);
        row("name", Glyph.Kind.APPEARANCE, "Seu nome", nm.isEmpty() ? "Sem nome na saudação" : nm, false, () ->
                Ui.prompt(this, "Seu nome (deixe vazio para tirar)", nm, v -> {
                    if (v == null) return;
                    Ui.prefs(this).edit().putString("userName", v.trim()).apply();
                    refresh();
                }));

        section("Apps");
        row("hidden", Glyph.Kind.EYE, "Apps ocultos", String.valueOf(hidden), false, this::showHidden);
        row("order", Glyph.Kind.SORT, "Restaurar ordem", null, false, () -> {
            Ui.prefs(this).edit().remove("order").apply();
            Toast.makeText(this, "Ordem restaurada", Toast.LENGTH_SHORT).show();
        });
        row("play", Glyph.Kind.BAG, "Google Play", "Instalar novos apps", false, () -> Device.openPlayStore(this));
        row("google", Glyph.Kind.TV, "Tela do Google", "Busca e recomendações por 10 min", false, () -> Device.openSystemHome(this));

        section("TV");
        row("network", Glyph.Kind.WIFI, "Rede", "Wi‑Fi e Ethernet", false, () -> Device.openNetwork(this));
        row("tvsettings", Glyph.Kind.GEAR, "Configurações da TV", null, false, () -> Device.openTvSettings(this));
        row("setup", Glyph.Kind.RESTORE, "Configurar o RinoStick", Setup.ready(this) ? "Permissões liberadas" : "Pendente — toque para configurar",
                false, () -> Setup.intro(this, this::refresh));
        row("restore", Glyph.Kind.RESTORE, "Restaurar original", null, true, () ->
                Ui.menu(this, "Restaurar a TV original?\n\nEsta tela inicial será removida, "
                                + "o protetor de tela volta ao padrão e o botão Home volta para a tela do Google.",
                        new String[]{"Cancelar", "!Restaurar e remover"}, w -> {
                            if (w == 1) Device.restoreOriginal(this);
                        }));

        // Dormir: sempre a última opção
        View gap = new View(this);
        list.addView(gap, new LinearLayout.LayoutParams(1, (int) Ui.px(18)));
        row("sleep", Glyph.Kind.POWER, "Dormir", null, false, () -> {
            finish();
            overridePendingTransition(0, 0);
            Device.sleep(this);
        });

        View f = focusRow != null ? list.findViewWithTag(focusRow) : null;
        if (f == null) f = list.findViewWithTag("appearance");
        final View ff = f;
        if (ff != null) ff.post(ff::requestFocus);
    }

    String weatherValue() {
        if (!Weather.enabled(this)) return "Oculto";
        String city = Weather.city(this);
        if (city == null) return "Automático";
        return city + (Weather.manual(this) ? "" : " (automático)");
    }

    void weatherMenu() {
        boolean on = Weather.enabled(this);
        Ui.menu(this, "Clima", new String[]{
                "Escolher cidade…",
                "Detectar automaticamente",
                on ? "Ocultar clima" : "Mostrar clima"}, w -> {
            if (w == 0) {
                Ui.prompt(this, "Nome da cidade", "", name -> {
                    if (name == null || name.trim().isEmpty()) return;
                    Weather.setCity(this, name, label -> {
                        if (label == null) {
                            Toast.makeText(this, "Cidade não encontrada", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Clima de " + label, Toast.LENGTH_SHORT).show();
                            Weather.refresh(this, true, null);
                        }
                        refresh();
                    });
                });
            } else if (w == 1) {
                Weather.setAuto(this);
                Weather.refresh(this, true, this::refresh);
                refresh();
            } else {
                Ui.prefs(this).edit().putBoolean("wOn", !on).apply();
                if (!on) Weather.refresh(this, true, null);
                refresh();
            }
        });
    }

    void showHidden() {
        final Set<String> hidden = new HashSet<>(Ui.prefs(this).getStringSet("hidden", MainActivity.DEFAULT_HIDDEN));
        if (hidden.isEmpty()) {
            Toast.makeText(this, "Nenhum app oculto", Toast.LENGTH_SHORT).show();
            return;
        }
        final List<String> pkgs = new ArrayList<>(hidden);
        String[] names = new String[pkgs.size()];
        for (int i = 0; i < names.length; i++) {
            String label = pkgs.get(i);
            try {
                label = String.valueOf(getPackageManager().getApplicationLabel(
                        getPackageManager().getApplicationInfo(pkgs.get(i), 0)));
            } catch (Exception ignored) { }
            names[i] = "Mostrar  " + label;
        }
        Ui.menu(this, "Apps ocultos", names, w -> {
            hidden.remove(pkgs.get(w));
            Ui.prefs(this).edit().putStringSet("hidden", hidden).apply();
            refresh();
        });
    }
}
