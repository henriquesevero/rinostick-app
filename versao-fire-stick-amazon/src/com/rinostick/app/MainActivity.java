package com.rinostick.app;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {

    static final int COLS = 5;
    static final String SETTINGS = "__settings__";
    static final String FOLDER = "f:";
    static final int MATCH = Ui.MATCH, WRAP = Ui.WRAP;

    static final Set<String> DEFAULT_HIDDEN = new HashSet<>(Arrays.asList(
            "com.amazon.shoptv.firetv.client",
            "com.amazon.firetv.troubleshooting",
            "com.amazon.smarthomemapviewapp",
            "com.amazon.tv.earlyaccess",
            "com.amazon.whasettings",
            "com.amazon.spiderpork",      // Luna
            "com.amazon.bueller.music")); // Amazon Music

    static final List<String> DEFAULT_ORDER = Arrays.asList(
            "com.apple.atve.amazon.appletv",
            "com.netflix.ninja",
            "com.amazon.firebat",
            "com.amazon.firetv.youtube",
            "com.spotify.tv.android");

    // ------------------------------------------------------------------ modelo

    static class App {
        String key, label;
        Intent launch;
        Drawable banner, icon;
        int color;
        Bitmap backdrop;
    }

    static class Folder {
        String id, name;
        final List<String> pkgs = new ArrayList<>();
        final List<App> apps = new ArrayList<>();
    }

    static class Item {
        String key;
        App app;
        Folder folder;
        boolean settings;

        String label() {
            return settings ? "Ajustes" : folder != null ? folder.name : app.label;
        }
    }

    boolean dark;
    int tileW, tileH, gap, side, shelfH;
    SharedPreferences prefs;

    final List<Item> items = new ArrayList<>();
    final Map<String, App> cache = new HashMap<>();
    final Map<String, App> visibleApps = new LinkedHashMap<>();
    final Map<String, Folder> folders = new LinkedHashMap<>();

    ShelfView shelf;
    FrameLayout header;
    boolean appsOnly;

    /** Modo "só apps" (abaixo da 2ª fileira): esconde o topo e o fundo colorido. */
    void setAppsOnly(boolean on) {
        if (on == appsOnly || header == null) return;
        appsOnly = on;
        header.animate().cancel();
        header.animate().alpha(on ? 0f : 1f).translationY(on ? -px(40) : 0)
                .setDuration(320).setInterpolator(new DecelerateInterpolator(2f)).start();
        header.setDescendantFocusability(on ? ViewGroup.FOCUS_BLOCK_DESCENDANTS : ViewGroup.FOCUS_AFTER_DESCENDANTS);
        shelf.setHeroVisible(!on);
        fadeRows(true);
    }

    static final float PEEK_ALPHA = 0.25f;

    /** Tela inicial: fileiras da 3ª em diante bem apagadas (só uma dica). Modo só apps: o dock some. */
    void fadeRows(boolean animate) {
        for (int i = 1; i < content.getChildCount(); i++) {
            View r = content.getChildAt(i);
            int rowIndex = i - 1;
            float a = appsOnly ? (rowIndex == 0 ? 0f : 1f) : (rowIndex >= 2 ? PEEK_ALPHA : 1f);
            r.animate().cancel();
            if (animate) r.animate().alpha(a).setDuration(300).start();
            else r.setAlpha(a);
        }
    }
    NowPlayingView nowPlaying;
    TextView title, hint;
    GridScroll scroll;
    TextView clock, greeting, dateLine, weatherText;
    LinearLayout weatherRow;
    Glyph weatherGlyph;

    void updateWeather() {
        if (weatherRow == null) return;
        if (!Weather.enabled(this) || !Weather.has(this)) {
            weatherRow.setVisibility(View.GONE);
            return;
        }
        int code = Weather.code(this);
        weatherGlyph.kindOverride(Weather.glyph(code, Weather.day(this)));
        String l = Weather.label(code);
        weatherText.setText(Weather.temp(this) + "°" + (l.isEmpty() ? "" : "  " + l));
        weatherRow.setVisibility(View.VISIBLE);
    }
    String userName;
    BroadcastReceiver tickRx;
    int rowGap, topMin, bottomPad;
    LinearLayout content; // a fileira do carrossel
    Bitmap settingsBackdrop;

    Item moving;
    ObjectAnimator wiggle;
    String focusKey, heroKey;
    boolean resumed, quiet;
    int dirX, dirY;
    Dialog folderDialog;
    String openFolderId;
    BroadcastReceiver pkgRx;

    static float px(float v) { return Ui.px(v); }

    // ------------------------------------------------------------------ ciclo de vida

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.init(this);
        Ui.initSounds(this);
        prefs = Ui.prefs(this);
        if (!prefs.contains("hidden")) prefs.edit().putStringSet("hidden", DEFAULT_HIDDEN).apply();
        if (prefs.getInt("hiddenVersion", 1) < 2) {
            Set<String> hs = new HashSet<>(prefs.getStringSet("hidden", DEFAULT_HIDDEN));
            hs.add("com.amazon.spiderpork");
            hs.add("com.amazon.bueller.music");
            prefs.edit().putStringSet("hidden", hs).putInt("hiddenVersion", 2).apply();
        }
        Device.initScreensaver(this);

        gap = (int) px(42);
        rowGap = (int) px(44);
        tileW = (int) px(308);
        tileH = tileW * 9 / 16;
        side = (Ui.W - (COLS * tileW + (COLS - 1) * gap)) / 2; // grade centralizada
        shelfH = (int) (Ui.H * 0.55f);  // topo da grade quando há poucas linhas
        topMin = (int) px(150);         // limite superior (abaixo da engrenagem/relógio)
        bottomPad = (int) px(70);

        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        buildUi();
        registerPackageReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        Device.undoRestoreIfNeeded(this);
        HomeRedirectService.pauseUntil = 0;
        if (Ui.isDark(this) != dark) {
            cache.clear();
            buildUi();
        }
        reload();
        updateHint();
        if (nowPlaying != null) nowPlaying.start();
        updateWeather();
        Weather.refresh(this, false, this::updateWeather);
        Updater.check(this, false, this::updateHint); // no máximo 1x por dia
        // Primeiro uso (ou permissões perdidas): oferece o assistente de configuração.
        if (!Setup.asked && !Setup.ready(this)) {
            content.postDelayed(() -> { if (resumed && !Setup.ready(this)) Setup.intro(this, this::updateHint); }, 800);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        resumed = false;
        if (nowPlaying != null) nowPlaying.stop();
        if (moving != null) endMove();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Home apertado já estando aqui: volta para o topo, como no tvOS.
        if (resumed && folderDialog == null && moving == null && !items.isEmpty()) {
            focusKey = items.get(0).key;
            View v = content.findViewWithTag(focusKey);
            if (v != null) v.requestFocus();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(pkgRx); } catch (Exception ignored) { }
        try { unregisterReceiver(tickRx); } catch (Exception ignored) { }
    }

    void updateClock() {
        if (clock == null) return;
        String fmt = DateFormat.is24HourFormat(this) ? "HH:mm" : "h:mm";
        clock.setText(DateFormat.format(fmt, System.currentTimeMillis()));
        if (greeting != null) {
            int h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
            String g = h >= 5 && h < 12 ? "Bom dia" : h >= 12 && h < 18 ? "Boa tarde" : "Boa noite";
            if (userName == null) userName = Device.firstName(this);
            greeting.setText(userName.isEmpty() ? g : g + ", " + userName);
        }
        if (dateLine != null) {
            String ds = new SimpleDateFormat("EEEE, d 'de' MMMM", new Locale("pt", "BR")).format(new Date());
            dateLine.setText(ds.substring(0, 1).toUpperCase() + ds.substring(1));
        }
    }

    @Override
    public void onBackPressed() {
        // Tela inicial: voltar não sai.
    }

    void registerPackageReceiver() {
        pkgRx = new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) {
                Uri u = i.getData();
                if (u != null) cache.remove(u.getSchemeSpecificPart());
                gridSig = null; // o banner de um app pode ter mudado
                if (resumed) reload();
            }
        };
        IntentFilter pf = new IntentFilter();
        pf.addAction(Intent.ACTION_PACKAGE_ADDED);
        pf.addAction(Intent.ACTION_PACKAGE_REMOVED);
        pf.addAction(Intent.ACTION_PACKAGE_CHANGED);
        pf.addAction(Intent.ACTION_PACKAGE_REPLACED);
        pf.addDataScheme("package");
        registerReceiver(pkgRx, pf);

        tickRx = new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) {
                updateClock();
                Weather.refresh(MainActivity.this, false, MainActivity.this::updateWeather);
            }
        };
        IntentFilter tf = new IntentFilter(Intent.ACTION_TIME_TICK);
        tf.addAction(Intent.ACTION_TIME_CHANGED);
        tf.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(tickRx, tf);
    }

    // ------------------------------------------------------------------ UI base

    /** ScrollView que só rola quando mandamos (rolagem mínima e animada). */
    static class GridScroll extends ScrollView {
        GridScroll(Context c) { super(c); }
        @Override protected int computeScrollDeltaToGetChildRectOnScreen(Rect r) { return 0; }
        // As setas nunca rolam sozinhas (na última fileira, "para baixo" não faz nada).
        @Override public boolean executeKeyEvent(android.view.KeyEvent e) { return false; }
    }

    void buildUi() {
        dark = Ui.isDark(this);
        heroKey = null;
        settingsBackdrop = null;

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Ui.base(dark));
        root.setClipChildren(false);

        shelf = new ShelfView(this, dark);
        root.addView(shelf, new FrameLayout.LayoutParams(MATCH, MATCH));

        // Grade de apps (6 por linha)
        scroll = new GridScroll(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        root.addView(scroll, new FrameLayout.LayoutParams(MATCH, MATCH));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setClipChildren(false);
        content.setClipToPadding(false);
        content.setPadding(0, 0, 0, bottomPad);
        scroll.addView(content, new FrameLayout.LayoutParams(MATCH, WRAP));

        // Engrenagem discreta no canto superior esquerdo
        FrameLayout.LayoutParams gl = new FrameLayout.LayoutParams((int) px(60), (int) px(60), Gravity.TOP | Gravity.START);
        gl.topMargin = (int) px(48);
        gl.leftMargin = side;
        header = new FrameLayout(this);
        header.setClipChildren(false);
        root.addView(header, new FrameLayout.LayoutParams(MATCH, MATCH));
        appsOnly = false;
        header.addView(makeGearButton(), gl);

        // Saudação com o nome da conta, ao lado da engrenagem
        greeting = Ui.text(this, px(30), Ui.medium, Ui.text(dark));
        if (dark) greeting.setShadowLayer(px(16), 0, px(2), 0x55000000);
        FrameLayout.LayoutParams grl = new FrameLayout.LayoutParams(WRAP, (int) px(60), Gravity.TOP | Gravity.START);
        grl.topMargin = (int) px(48);
        grl.leftMargin = side + (int) px(84);
        greeting.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(greeting, grl);

        // Tocando agora (Spotify etc.), discreto no topo central
        if (nowPlaying != null) nowPlaying.stop();
        nowPlaying = new NowPlayingView(this, dark);
        FrameLayout.LayoutParams npl = new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        npl.topMargin = (int) px(28);
        header.addView(nowPlaying, npl);
        if (resumed) nowPlaying.start();


        // Relógio no canto superior direito
        LinearLayout clockBox = new LinearLayout(this);
        clockBox.setOrientation(LinearLayout.VERTICAL);
        clockBox.setGravity(Gravity.END);
        clock = Ui.text(this, px(40), Ui.medium, Ui.text(dark));
        if (dark) clock.setShadowLayer(px(16), 0, px(2), 0x55000000);
        clockBox.addView(clock, new LinearLayout.LayoutParams(WRAP, WRAP));
        dateLine = Ui.text(this, px(22), Ui.medium, Ui.text2(dark));
        LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(WRAP, WRAP);
        dl.topMargin = (int) px(8);
        clockBox.addView(dateLine, dl);

        // Clima abaixo da data
        weatherRow = new LinearLayout(this);
        weatherRow.setOrientation(LinearLayout.HORIZONTAL);
        weatherRow.setGravity(Gravity.CENTER_VERTICAL);
        weatherGlyph = new Glyph(Glyph.Kind.SUN, Ui.text2(dark));
        ImageView wi = new ImageView(this);
        wi.setImageDrawable(weatherGlyph);
        int wis = (int) px(28);
        weatherRow.addView(wi, new LinearLayout.LayoutParams(wis, wis));
        weatherText = Ui.text(this, px(22), Ui.medium, Ui.text2(dark));
        LinearLayout.LayoutParams wtl = new LinearLayout.LayoutParams(WRAP, WRAP);
        wtl.leftMargin = (int) px(10);
        weatherRow.addView(weatherText, wtl);
        LinearLayout.LayoutParams wl = new LinearLayout.LayoutParams(WRAP, WRAP);
        wl.topMargin = (int) px(8);
        clockBox.addView(weatherRow, wl);
        updateWeather();

        FrameLayout.LayoutParams cl = new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.TOP | Gravity.END);
        cl.topMargin = (int) px(52);
        cl.rightMargin = side;
        header.addView(clockBox, cl);
        updateClock();

        hint = Ui.text(this, px(24), Ui.regular, 0xFFFF9F0A);
        FrameLayout.LayoutParams hl = new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.BOTTOM | Gravity.START);
        hl.bottomMargin = (int) px(44);
        hl.leftMargin = side;
        root.addView(hint, hl);

        setContentView(root);
    }

    void updateHint() {
        if (moving != null) {
            hint.setTextColor(Ui.text2(dark));
            hint.setText("Mova com as setas  ·  OK para concluir  ·  ☰ ou ⏯ para mais opções");
        } else if (!Setup.ready(this)) {
            hint.setTextColor(0xFFFF9F0A);
            hint.setText("Configuração pendente — aperte ☰ e escolha \"Configurar o RinoStick\"");
        } else if (Updater.available(this) != null) {
            hint.setTextColor(Ui.text2(dark));
            hint.setText("Nova versão " + Updater.available(this) + " do RinoStick — aperte ☰ e escolha \"Atualização disponível\"");
        } else {
            hint.setText("");
        }
    }

    // ------------------------------------------------------------------ apps e pastas

    /** Assinatura do que a grade mostra; se não mudou, não precisa redesenhar. */
    String gridSig;

    String signature() {
        StringBuilder b = new StringBuilder(dark ? "d" : "l");
        for (Item it : items) {
            b.append(';').append(it.key).append('|').append(it.label());
            if (it.folder != null) for (App a : it.folder.apps) b.append(',').append(a.key);
        }
        return b.toString();
    }

    void reload() {
        // Voltar à tela inicial sem nada ter mudado não reconstrói a grade (evita o
        // engasgo ao apertar Home). Os objetos antigos são mantidos porque a grade,
        // o modo mover e as pastas comparam por identidade.
        List<Item> oldItems = new ArrayList<>(items);
        Map<String, Folder> oldFolders = new LinkedHashMap<>(folders);
        loadAll();
        if (signature().equals(gridSig) && content.getChildCount() > 0) {
            items.clear();
            items.addAll(oldItems);
            folders.clear();
            folders.putAll(oldFolders);
        } else {
            buildGrid(focusKey);
        }
        if (openFolderId != null && folderDialog != null) {
            Folder f = folders.get(openFolderId);
            folderDialog.dismiss();
            if (f != null && !f.apps.isEmpty()) openFolder(f);
        }
    }

    void loadAll() {
        PackageManager pm = getPackageManager();
        Set<String> hidden = new HashSet<>(prefs.getStringSet("hidden", DEFAULT_HIDDEN));
        visibleApps.clear();

        String[] cats = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
        for (String cat : cats) {
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(cat);
            for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
                ActivityInfo ai = ri.activityInfo;
                String pkg = ai.packageName;
                if (pkg.equals(getPackageName()) || visibleApps.containsKey(pkg) || hidden.contains(pkg)) continue;
                if (cat.equals(Intent.CATEGORY_LAUNCHER) && pkg.startsWith("com.amazon.")) continue;
                App a = cache.get(pkg);
                if (a == null) {
                    a = new App();
                    a.key = pkg;
                    a.label = String.valueOf(ri.loadLabel(pm));
                    a.launch = new Intent(Intent.ACTION_MAIN).addCategory(cat)
                            .setClassName(pkg, ai.name)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    try { a.banner = ai.loadBanner(pm); } catch (Exception ignored) { }
                    if (a.banner == null) {
                        try { a.banner = ai.applicationInfo.loadBanner(pm); } catch (Exception ignored) { }
                    }
                    try { a.icon = ri.loadIcon(pm); } catch (Exception ignored) { }
                    a.color = Images.dominant(this, a.banner != null ? a.banner : a.icon);
                    cache.put(pkg, a);
                }
                visibleApps.put(pkg, a);
            }
        }

        // Pastas
        folders.clear();
        Set<String> inFolder = new HashSet<>();
        try {
            JSONObject jo = new JSONObject(prefs.getString("folders", "{}"));
            Iterator<String> it = jo.keys();
            while (it.hasNext()) {
                String id = it.next();
                JSONObject f = jo.getJSONObject(id);
                Folder fo = new Folder();
                fo.id = id;
                fo.name = f.optString("name", "Pasta");
                JSONArray arr = f.optJSONArray("apps");
                if (arr != null) for (int i = 0; i < arr.length(); i++) {
                    String pkg = arr.getString(i);
                    if (!fo.pkgs.contains(pkg)) fo.pkgs.add(pkg);
                }
                for (String pkg : fo.pkgs) {
                    App a = visibleApps.get(pkg);
                    if (a != null) { fo.apps.add(a); inFolder.add(pkg); }
                }
                folders.put(id, fo);
            }
        } catch (Exception ignored) { }

        List<String> saved = savedOrder();
        items.clear();
        for (App a : visibleApps.values()) {
            if (inFolder.contains(a.key)) continue;
            Item it = new Item();
            it.key = a.key;
            it.app = a;
            items.add(it);
        }
        for (Folder f : folders.values()) {
            if (f.apps.isEmpty()) continue;
            Item it = new Item();
            it.key = FOLDER + f.id;
            it.folder = f;
            items.add(it);
        }
        items.sort((x, y) -> {
            int c = Integer.compare(rank(saved, x), rank(saved, y));
            return c != 0 ? c : x.label().compareToIgnoreCase(y.label());
        });
    }

    List<String> savedOrder() {
        List<String> saved = new ArrayList<>();
        String o = prefs.getString("order", "");
        if (!o.isEmpty()) saved.addAll(Arrays.asList(o.split(",")));
        return saved;
    }

    static int rank(List<String> saved, Item it) {
        int i = saved.indexOf(it.key);
        if (i >= 0) return i;
        if (it.app != null) {
            int d = DEFAULT_ORDER.indexOf(it.key);
            if (d >= 0) return 10000 + d;
            return 20000;
        }
        if (it.folder != null) return 25000;
        return 30000; // Ajustes por último
    }

    void saveOrder() {
        StringBuilder sb = new StringBuilder();
        for (Item it : items) {
            if (sb.length() > 0) sb.append(',');
            sb.append(it.key);
        }
        prefs.edit().putString("order", sb.toString()).apply();
    }

    void saveFolders() {
        try {
            JSONObject jo = new JSONObject();
            for (Folder f : folders.values()) {
                if (f.pkgs.isEmpty()) continue;
                JSONObject o = new JSONObject();
                o.put("name", f.name);
                o.put("apps", new JSONArray(f.pkgs));
                jo.put(f.id, o);
            }
            prefs.edit().putString("folders", jo.toString()).apply();
        } catch (Exception ignored) { }
    }

    void setHidden(String key, boolean hide) {
        Set<String> h = new HashSet<>(prefs.getStringSet("hidden", DEFAULT_HIDDEN));
        if (hide) h.add(key); else h.remove(key);
        prefs.edit().putStringSet("hidden", h).apply();
    }

    // ------------------------------------------------------------------ grade

    void buildGrid(String wantFocus) {
        if (wiggle != null) { wiggle.cancel(); wiggle = null; }
        quiet = true;
        content.removeAllViews();
        heroKey = null;

        // Como no tvOS: a tela inicial mostra as 2 primeiras fileiras; as demais
        // aparecem ao descer (o topo some e a tela fica só com apps).
        int rows = Math.max(1, (items.size() + COLS - 1) / COLS);
        final int dockPad = (int) px(24); // folga da moldura da 1ª fileira
        int needed2 = 2 * tileH + rowGap + 2 * dockPad;
        int top = Math.min(shelfH, Ui.H - bottomPad - needed2) - dockPad;
        content.setPadding(0, 0, 0, rows > 2 ? Ui.H : bottomPad);
        View spacer = new View(this);
        content.addView(spacer, new LinearLayout.LayoutParams(MATCH, top));

        LinearLayout row = null;
        for (int i = 0; i < items.size(); i++) {
            if (i % COLS == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setClipChildren(false);
                row.setClipToPadding(false);
                if (i == 0) {
                    // 1ª fileira dentro de uma moldura de vidro (como o "dock" do tvOS).
                    FrameLayout dock = new FrameLayout(this);
                    dock.setClipChildren(false);
                    dock.setClipToPadding(false);
                    GradientDrawable db = Ui.round(dark ? 0x17FFFFFF : 0x0F000000, px(42));
                    db.setStroke((int) Math.max(1, px(1.5f)), dark ? 0x26FFFFFF : 0x17000000);
                    dock.setBackground(db);
                    row.setPadding(dockPad, dockPad, dockPad, dockPad);
                    dock.addView(row, new FrameLayout.LayoutParams(MATCH, MATCH));
                    LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(MATCH, tileH + 2 * dockPad);
                    dl.leftMargin = side - dockPad;
                    dl.rightMargin = side - dockPad;
                    content.addView(dock, dl);
                } else {
                    row.setPadding(side, 0, side, 0);
                    LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(MATCH, tileH);
                    rl.topMargin = rowGap;
                    content.addView(row, rl);
                }
            }
            LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(tileW, tileH);
            if (i % COLS != COLS - 1) cl.rightMargin = gap;
            row.addView(makeTile(items.get(i), false, i / COLS), cl);
        }

        fadeRows(false);

        View target = wantFocus != null ? content.findViewWithTag(wantFocus) : null;
        if (target == null && !items.isEmpty()) target = content.findViewWithTag(items.get(0).key);
        final View t = target;
        if (t != null) {
            t.post(() -> {
                t.requestFocus();
                if (moving != null && moving.key.equals(t.getTag())) startWiggle(t);
                quiet = false;
            });
        } else {
            quiet = false;
        }
        gridSig = signature();
    }

    View makeTile(final Item it, final boolean inFolder, final int rowIndex) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setClipChildren(false);
        col.setClipToPadding(false);

        final float radius = px(14);
        final FrameLayout card;
        if (it.settings) {
            card = SettingsActivity.settingsIcon(this, tileW, tileH);
        } else {
            card = new FrameLayout(this);
            card.setOutlineProvider(new ViewOutlineProvider() {
                @Override public void getOutline(View v, Outline o) {
                    o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius);
                }
            });
            card.setClipToOutline(true);
            if (it.folder != null) fillFolderTile(card, it.folder);
            else fillAppTile(card, it.app);
        }
        card.setTag(it.key);
        card.setFocusable(true);
        card.setClickable(true);
        card.setCameraDistance(px(7000));

        // Reflexo de luz que atravessa o ícone ao receber foco.
        final View sheen = new View(this);
        sheen.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0x00FFFFFF, 0x47FFFFFF, 0x00FFFFFF}));
        sheen.setRotation(18f);
        sheen.setAlpha(0f);
        card.addView(sheen, new FrameLayout.LayoutParams((int) (tileW * 0.45f), tileH * 2, Gravity.CENTER_VERTICAL));

        col.addView(card, new LinearLayout.LayoutParams(tileW, tileH));
        if (inFolder) col.setPadding(0, 0, 0, (int) px(64)); // espaço entre linhas da pasta

        final DecelerateInterpolator ease = new DecelerateInterpolator(2f);
        card.setOnFocusChangeListener((v, has) -> {
            float sc = has ? 1.07f : 1f;
            v.animate().scaleX(sc).scaleY(sc).translationZ(has ? px(22) : 0)
                    .rotationX(0).rotationY(0)
                    .setDuration(has ? 260 : 200).setInterpolator(ease).start();
            if (!has) {
                sheen.animate().cancel();
                sheen.setAlpha(0f);
                return;
            }
            // Inclinação (paralaxe) no sentido do movimento + reflexo.
            v.setRotationY(dirX * 7f);
            v.setRotationX(-dirY * 6f);
            sheen.setTranslationX(-tileW * 0.6f);
            sheen.setAlpha(1f);
            sheen.animate().translationX(tileW * 1.15f).setDuration(750)
                    .setInterpolator(new DecelerateInterpolator(1.2f))
                    .withEndAction(() -> sheen.setAlpha(0f)).start();
            if (!quiet) Ui.tick(this);
            if (!inFolder) onGridFocus(it, rowIndex);
        });
        card.setOnClickListener(v -> {
            if (moving != null) return;
            Ui.select(this);
            v.postDelayed(() -> activate(it, v), 90);
        });
        card.setOnLongClickListener(v -> {
            if (moving != null) return true;
            Ui.select(this);
            if (inFolder) folderAppOptions(it.app);
            else startMove(it);
            return true;
        });
        return col;
    }

    /** Engrenagem discreta no canto superior direito (abre os Ajustes). */
    View makeGearButton() {
        final FrameLayout b = new FrameLayout(this);
        b.setFocusable(true);
        b.setClickable(true);
        final GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        final int idle = dark ? 0x1FFFFFFF : 0x14000000;
        bg.setColor(idle);
        b.setBackground(bg);
        final Glyph g = new Glyph(Glyph.Kind.GEAR, Ui.text2(dark));
        ImageView iv = new ImageView(this);
        iv.setImageDrawable(g);
        int is = (int) px(30);
        b.addView(iv, new FrameLayout.LayoutParams(is, is, Gravity.CENTER));
        b.setOnFocusChangeListener((v, has) -> {
            bg.setColor(has ? Ui.focusBg(dark) : idle);
            Ui.lift(v, has, dark);
            g.setColor(has ? Ui.focusText(dark) : Ui.text2(dark));
            v.animate().scaleX(has ? 1.18f : 1f).scaleY(has ? 1.18f : 1f).setDuration(180).start();
            if (has && !quiet) Ui.tick(this);
        });
        b.setOnClickListener(v -> openSettings());
        return b;
    }

    void fillAppTile(FrameLayout card, App a) {
        if (a.banner != null) {
            card.setBackgroundColor(a.color);
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageDrawable(a.banner);
            card.addView(iv, new FrameLayout.LayoutParams(MATCH, MATCH));
        } else {
            card.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[]{a.color, Images.darker(a.color, 0.6f)}));
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iv.setImageDrawable(a.icon);
            int sz = (int) (tileH * 0.52f);
            card.addView(iv, new FrameLayout.LayoutParams(sz, sz, Gravity.CENTER));
        }
    }

    /** Pasta: miniaturas dos apps numa grade 3×2 sobre fundo translúcido. */
    void fillFolderTile(FrameLayout card, Folder f) {
        card.setBackgroundColor(dark ? 0x33FFFFFF : 0x1F000000);
        int pad = (int) px(14), g = (int) px(8);
        int mw = (tileW - 2 * pad - 2 * g) / 3, mh = mw * 9 / 16;
        int totalH = 2 * mh + g;
        int top = (tileH - totalH) / 2;
        for (int i = 0; i < Math.min(6, f.apps.size()); i++) {
            App a = f.apps.get(i);
            FrameLayout mini = new FrameLayout(this);
            final float r = px(6);
            mini.setOutlineProvider(new ViewOutlineProvider() {
                @Override public void getOutline(View v, Outline o) {
                    o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), r);
                }
            });
            mini.setClipToOutline(true);
            fillMini(mini, a, mh);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(mw, mh);
            lp.leftMargin = pad + (i % 3) * (mw + g);
            lp.topMargin = top + (i / 3) * (mh + g);
            card.addView(mini, lp);
        }
    }

    void fillMini(FrameLayout mini, App a, int mh) {
        mini.setBackgroundColor(a.color);
        ImageView iv = new ImageView(this);
        if (a.banner != null) {
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageDrawable(a.banner);
            mini.addView(iv, new FrameLayout.LayoutParams(MATCH, MATCH));
        } else {
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iv.setImageDrawable(a.icon);
            int sz = (int) (mh * 0.6f);
            mini.addView(iv, new FrameLayout.LayoutParams(sz, sz, Gravity.CENTER));
        }
    }

    /**
     * Foco na grade: atualiza o fundo e rola só o mínimo necessário para a
     * linha em foco ficar inteira na tela.
     */
    void onGridFocus(Item it, int rowIndex) {
        focusKey = it.key;
        showHero(it);
        // 1ª e 2ª fileiras: tela inicial normal (topo + fundo), sem rolagem.
        if (rowIndex <= 1) {
            setAppsOnly(false);
            scrollToY(0);
            return;
        }
        // Da 3ª em diante: tudo sobe e a tela fica só com apps (como no tvOS).
        View row = content.getChildAt(1 + rowIndex);
        View second = content.getChildAt(2);
        if (row == null || second == null) return;
        setAppsOnly(true);
        int topApps = (int) px(90);
        int extra = (int) (tileH * 0.08f) + (int) px(10); // folga para o ícone ampliado
        int rowTop = row.getTop() - extra, rowBottom = row.getBottom() + extra;
        int base = second.getTop() - extra - topApps;    // 2ª fileira encostada no topo
        int y = Math.max(scroll.getScrollY(), base);
        if (rowTop < y + topApps) y = Math.max(base, rowTop - topApps);
        if (rowBottom > y + Ui.H - bottomPad) y = rowBottom - (Ui.H - bottomPad);
        int max = Math.max(0, content.getHeight() - scroll.getHeight());
        scrollToY(Math.max(0, Math.min(y, max)));
    }

    ValueAnimator scrollAnim;

    void scrollToY(int yTarget) {
        if (scroll.getScrollY() == yTarget) return;
        if (scrollAnim != null) scrollAnim.cancel();
        scrollAnim = ValueAnimator.ofInt(scroll.getScrollY(), yTarget);
        scrollAnim.setDuration(380);
        scrollAnim.setInterpolator(new DecelerateInterpolator(2f));
        scrollAnim.addUpdateListener(a -> scroll.scrollTo(0, (Integer) a.getAnimatedValue()));
        scrollAnim.start();
    }

    void showHero(Item it) {
        if (it.key.equals(heroKey)) return;
        heroKey = it.key;
        shelf.setHero(backdropFor(it));
    }

    Bitmap backdropFor(Item it) {
        if (it.app != null) {
            if (it.app.backdrop == null) it.app.backdrop = Images.backdrop(this, it.app.banner, it.app.icon, it.app.color, dark);
            return it.app.backdrop;
        }
        if (it.folder != null && !it.folder.apps.isEmpty()) return backdropFor(itemFor(it.folder.apps.get(0)));
        if (settingsBackdrop == null) settingsBackdrop = Images.backdrop(this, null, null, 0xFF8E8E93, dark);
        return settingsBackdrop;
    }

    Item itemFor(App a) {
        Item i = new Item();
        i.key = a.key;
        i.app = a;
        return i;
    }

    void activate(Item it, View from) {
        if (it.settings) {
            openSettings();
        } else if (it.folder != null) {
            openFolder(it.folder);
        } else {
            try {
                startActivity(it.app.launch, zoom(from));
            } catch (Exception e) {
                Toast.makeText(this, "Não foi possível abrir " + it.app.label, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /** Ajustes abrem por cima, translúcidos (o fade é feito pela própria tela). */
    void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
        overridePendingTransition(0, 0);
    }

    /** Animação de abertura a partir do ícone (zoom, como no tvOS). */
    static Bundle zoom(View v) {
        try {
            return ActivityOptions.makeScaleUpAnimation(v, 0, 0, v.getWidth(), v.getHeight()).toBundle();
        } catch (Exception e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ teclas

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int k = e.getKeyCode();
        boolean down = e.getAction() == KeyEvent.ACTION_DOWN;

        switch (k) {
            case KeyEvent.KEYCODE_DPAD_LEFT: if (down) { dirX = -1; dirY = 0; } break;
            case KeyEvent.KEYCODE_DPAD_RIGHT: if (down) { dirX = 1; dirY = 0; } break;
            case KeyEvent.KEYCODE_DPAD_UP: if (down) { dirX = 0; dirY = -1; } break;
            case KeyEvent.KEYCODE_DPAD_DOWN: if (down) { dirX = 0; dirY = 1; } break;
        }

        if (moving != null) {
            if (down) {
                int idx = items.indexOf(moving);
                int n = idx;
                switch (k) {
                    case KeyEvent.KEYCODE_DPAD_LEFT: n = idx - 1; break;
                    case KeyEvent.KEYCODE_DPAD_RIGHT: n = idx + 1; break;
                    case KeyEvent.KEYCODE_DPAD_UP: n = idx - COLS; break;
                    case KeyEvent.KEYCODE_DPAD_DOWN: n = idx + COLS; break;
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                    case KeyEvent.KEYCODE_ENTER:
                    case KeyEvent.KEYCODE_BACK:
                        if (e.getRepeatCount() == 0) endMove();
                        return true;
                    case KeyEvent.KEYCODE_MENU:
                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        if (e.getRepeatCount() == 0) itemOptions(moving);
                        return true;
                    default:
                        return super.dispatchKeyEvent(e);
                }
                if (n >= items.size()) n = items.size() - 1;
                if (n >= 0 && n != idx) {
                    items.remove(idx);
                    items.add(n, moving);
                    saveOrder();
                    Ui.tick(this);
                    buildGrid(moving.key);
                }
            }
            return true;
        }

        if (k == KeyEvent.KEYCODE_MENU) {
            if (e.getAction() == KeyEvent.ACTION_UP) openSettings();
            return true;
        }

        // "Clique": o ícone afunda ao pressionar e volta ao soltar.
        if (k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER) {
            View f = getCurrentFocus();
            if (f != null && f.getTag() instanceof String) {
                if (down && e.getRepeatCount() == 0) {
                    f.animate().scaleX(0.99f).scaleY(0.99f).translationZ(px(6)).setDuration(90).start();
                } else if (!down) {
                    f.animate().scaleX(1.07f).scaleY(1.07f).translationZ(px(22)).setDuration(160).start();
                }
            }
        }
        return super.dispatchKeyEvent(e);
    }

    // ------------------------------------------------------------------ edição (ícones tremendo)

    void startMove(Item it) {
        moving = it;
        updateHint();
        buildGrid(it.key);
    }

    void endMove() {
        if (moving == null) return;
        String key = moving.key;
        moving = null;
        if (wiggle != null) { wiggle.cancel(); wiggle = null; }
        saveOrder();
        updateHint();
        buildGrid(key);
    }

    void startWiggle(View v) {
        wiggle = ObjectAnimator.ofFloat(v, "rotation", -1.5f, 1.5f);
        wiggle.setDuration(120);
        wiggle.setRepeatCount(ValueAnimator.INFINITE);
        wiggle.setRepeatMode(ValueAnimator.REVERSE);
        wiggle.start();
    }

    void itemOptions(final Item it) {
        final List<String> labels = new ArrayList<>();
        final List<Runnable> acts = new ArrayList<>();
        if (it.app != null) {
            labels.add("Nova pasta");
            acts.add(() -> newFolderWith(it));
            for (final Folder f : folders.values()) {
                if (f.apps.isEmpty()) continue;
                labels.add("Mover para \"" + f.name + "\"");
                acts.add(() -> moveToFolder(it, f));
            }
            labels.add("Ocultar app");
            acts.add(() -> hideItem(it));
            labels.add("Informações do app");
            acts.add(() -> Device.appInfo(this, it.app.key));
        } else if (it.folder != null) {
            labels.add("Renomear pasta");
            acts.add(() -> Ui.prompt(this, "Nome da pasta", it.folder.name, name -> {
                it.folder.name = name;
                saveFolders();
                reload();
            }));
            labels.add("!Desfazer pasta");
            acts.add(() -> dissolveFolder(it));
        }
        labels.add("Concluir edição");
        acts.add(() -> { });
        Ui.menu(this, it.label(), labels.toArray(new String[0]), w -> {
            endMove();
            acts.get(w).run();
        });
    }

    void hideItem(Item it) {
        int idx = items.indexOf(it);
        setHidden(it.key, true);
        loadAll();
        String next = items.get(Math.max(0, Math.min(idx, items.size() - 1))).key;
        buildGrid(next);
    }

    void newFolderWith(Item it) {
        Folder f = new Folder();
        f.id = Long.toString(System.currentTimeMillis(), 36);
        f.name = "Nova pasta";
        f.pkgs.add(it.app.key);
        folders.put(f.id, f);
        int idx = items.indexOf(it);
        Item fi = new Item();
        fi.key = FOLDER + f.id;
        fi.folder = f;
        items.set(idx, fi);
        saveFolders();
        saveOrder();
        reload();
        focusKey = fi.key;
        buildGrid(fi.key);
        Ui.prompt(this, "Nome da pasta", f.name, name -> {
            Folder ff = folders.get(f.id);
            if (ff != null) { ff.name = name; saveFolders(); reload(); }
        });
    }

    void moveToFolder(Item it, Folder f) {
        if (!f.pkgs.contains(it.app.key)) f.pkgs.add(it.app.key);
        saveFolders();
        items.remove(it);
        saveOrder();
        focusKey = FOLDER + f.id;
        reload();
    }

    void dissolveFolder(Item it) {
        int idx = items.indexOf(it);
        items.remove(idx);
        for (App a : it.folder.apps) {
            items.add(Math.min(idx++, items.size()), itemFor(a));
        }
        folders.remove(it.folder.id);
        saveFolders();
        saveOrder();
        focusKey = it.folder.apps.isEmpty() ? null : it.folder.apps.get(0).key;
        reload();
    }

    // ------------------------------------------------------------------ pasta aberta

    void openFolder(final Folder f) {
        final Dialog d = Ui.fullDialog(this);
        folderDialog = d;
        openFolderId = f.id;

        FrameLayout over = new FrameLayout(this);
        over.setBackgroundColor(dark ? 0xEB141416 : 0xEBE9E9EE);
        over.setClipChildren(false);

        TextView name = Ui.text(this, px(52), Ui.bold, Ui.text(dark));
        name.setText(f.name);
        FrameLayout.LayoutParams nl = new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        nl.topMargin = (int) px(110);
        over.addView(name, nl);

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.setClipChildren(false);
        sv.setClipToPadding(false);
        sv.setPadding(0, (int) px(40), 0, (int) px(120));
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setClipChildren(false);
        box.setClipToPadding(false);
        sv.addView(box);

        quiet = true;
        LinearLayout row = null;
        View first = null;
        for (int i = 0; i < f.apps.size(); i++) {
            if (i % COLS == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setClipChildren(false);
                row.setClipToPadding(false);
                row.setPadding(side, 0, side, 0);
                box.addView(row, new LinearLayout.LayoutParams(MATCH, WRAP));
            }
            LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(tileW, WRAP);
            if (i % COLS != COLS - 1) cl.rightMargin = gap;
            View tile = makeTile(itemFor(f.apps.get(i)), true, 0);
            row.addView(tile, cl);
            if (first == null) first = tile.findViewWithTag(f.apps.get(i).key);
        }
        FrameLayout.LayoutParams sl = new FrameLayout.LayoutParams(MATCH, MATCH);
        sl.topMargin = (int) px(230);
        over.addView(sv, sl);

        d.setContentView(over);
        d.setOnKeyListener((di, k, e) -> {
            if (e.getAction() == KeyEvent.ACTION_DOWN) {
                if (k == KeyEvent.KEYCODE_DPAD_LEFT) { dirX = -1; dirY = 0; }
                else if (k == KeyEvent.KEYCODE_DPAD_RIGHT) { dirX = 1; dirY = 0; }
                else if (k == KeyEvent.KEYCODE_DPAD_UP) { dirX = 0; dirY = -1; }
                else if (k == KeyEvent.KEYCODE_DPAD_DOWN) { dirX = 0; dirY = 1; }
            }
            if (k == KeyEvent.KEYCODE_BACK && e.getAction() == KeyEvent.ACTION_UP) Ui.back(this);
            return false;
        });
        d.setOnDismissListener(di -> {
            if (folderDialog == d) { folderDialog = null; openFolderId = null; }
            View t = content.findViewWithTag(FOLDER + f.id);
            if (t != null) t.requestFocus();
        });
        d.show();
        over.setAlpha(0f);
        over.animate().alpha(1f).setDuration(220).start();
        final View ff = first;
        if (ff != null) ff.post(() -> { ff.requestFocus(); quiet = false; });
        else quiet = false;
    }

    void folderAppOptions(final App a) {
        Ui.menu(this, a.label, new String[]{"Remover da pasta", "Ocultar app", "Informações do app"}, w -> {
            Folder f = openFolderId != null ? folders.get(openFolderId) : null;
            if (w == 0 && f != null) {
                f.pkgs.remove(a.key);
                Item fi = null;
                for (Item i : items) if (i.folder == f) fi = i;
                int pos = fi != null ? items.indexOf(fi) + 1 : items.size() - 1;
                items.add(Math.min(pos, items.size()), itemFor(a));
                saveFolders();
                saveOrder();
                reload();
            } else if (w == 1) {
                setHidden(a.key, true);
                reload();
            } else if (w == 2) {
                Device.appInfo(this, a.key);
            }
        });
    }
}
