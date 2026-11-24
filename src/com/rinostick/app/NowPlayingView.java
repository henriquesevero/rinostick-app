package com.rinostick.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * "Tocando agora" discreto no topo: capa, música, artista, ⏯ e ⏭.
 * Lê as sessões de mídia ativas (Spotify etc.) via NowPlayingListener.
 * Some sozinho quando nada está tocando.
 */
class NowPlayingView extends LinearLayout {

    final Activity act;
    final boolean dark;
    final MediaSessionManager msm;
    final ComponentName listener;
    final Handler main = new Handler(Looper.getMainLooper());

    final ImageView art;
    final TextView title, artist;
    final Glyph playGlyph;
    final LinearLayout info;

    MediaController controller;
    boolean started, shown;

    final MediaController.Callback cb = new MediaController.Callback() {
        @Override public void onMetadataChanged(MediaMetadata m) { refresh(); }
        @Override public void onSessionEvent(String e, android.os.Bundle b) { }
        @Override public void onPlaybackStateChanged(PlaybackState s) { pick(); }
        @Override public void onSessionDestroyed() { pick(); }
    };
    final MediaSessionManager.OnActiveSessionsChangedListener sessions = l -> main.post(this::pick);

    NowPlayingView(Activity a, boolean dark) {
        super(a);
        this.act = a;
        this.dark = dark;
        this.msm = (MediaSessionManager) a.getSystemService(Activity.MEDIA_SESSION_SERVICE);
        this.listener = new ComponentName(a, NowPlayingListener.class);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setClipChildren(false);
        setClipToPadding(false);
        int pad = (int) Ui.px(12);
        setPadding(pad, pad, pad, pad);
        setBackground(Ui.round(dark ? 0x1AFFFFFF : 0x12000000, Ui.px(34)));
        setVisibility(GONE);
        setAlpha(0f);

        // Capa + textos (OK abre o app que está tocando)
        info = new LinearLayout(a);
        info.setOrientation(HORIZONTAL);
        info.setGravity(Gravity.CENTER_VERTICAL);
        info.setFocusable(true);
        info.setClickable(true);
        info.setPadding(pad / 2, pad / 2, (int) Ui.px(20), pad / 2);
        final GradientDrawable infoBg = Ui.round(0x00000000, Ui.px(24));
        info.setBackground(infoBg);

        art = new ImageView(a);
        art.setScaleType(ImageView.ScaleType.CENTER_CROP);
        final float ar = Ui.px(12);
        art.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), ar);
            }
        });
        art.setClipToOutline(true);
        int as = (int) Ui.px(84);
        info.addView(art, new LayoutParams(as, as));

        LinearLayout texts = new LinearLayout(a);
        texts.setOrientation(VERTICAL);
        title = Ui.text(a, Ui.px(29), Ui.semibold, Ui.text(dark));
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setMaxWidth((int) Ui.px(460));
        texts.addView(title);
        artist = Ui.text(a, Ui.px(23), Ui.regular, Ui.text2(dark));
        artist.setSingleLine(true);
        artist.setEllipsize(TextUtils.TruncateAt.END);
        artist.setMaxWidth((int) Ui.px(460));
        artist.setPadding(0, (int) Ui.px(4), 0, 0);
        texts.addView(artist);
        LayoutParams tl = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        tl.leftMargin = (int) Ui.px(20);
        info.addView(texts, tl);

        info.setOnFocusChangeListener((v, has) -> {
            infoBg.setColor(has ? Ui.focusBg(dark) : 0x00000000);
            Ui.lift(v, has, dark);
            title.setTextColor(has ? Ui.focusText(dark) : Ui.text(dark));
            artist.setTextColor(has ? Ui.focusText2(dark) : Ui.text2(dark));
            if (has) Ui.tick(act);
        });
        info.setOnClickListener(v -> openPlayer());
        addView(info, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        playGlyph = new Glyph(Glyph.Kind.PAUSE, Ui.text(dark));
        addView(button(playGlyph, () -> {
            if (controller == null) return;
            PlaybackState s = controller.getPlaybackState();
            if (s != null && s.getState() == PlaybackState.STATE_PLAYING) controller.getTransportControls().pause();
            else controller.getTransportControls().play();
        }));
        addView(button(new Glyph(Glyph.Kind.NEXT, Ui.text(dark)), () -> {
            if (controller != null) controller.getTransportControls().skipToNext();
        }));
        // ✕ fecha o cartão; ele volta na próxima música (ou ao dar play, se estava pausado)
        addView(button(new Glyph(Glyph.Kind.XMARK, Ui.text2(dark)), () -> {
            dismissedKey = lastKey;
            dismissedPlaying = isPlaying();
            setShown(false);
        }));
    }

    /** Sobrevivem à reconstrução da tela (troca de aparência etc.). */
    static String dismissedKey;
    static boolean dismissedPlaying;
    final Runnable recheck = this::refresh;

    boolean isPlaying() {
        PlaybackState s = controller != null ? controller.getPlaybackState() : null;
        int st = s != null ? s.getState() : PlaybackState.STATE_NONE;
        return st == PlaybackState.STATE_PLAYING || st == PlaybackState.STATE_BUFFERING;
    }

    View button(final Glyph g, final Runnable action) {
        final FrameLayout b = new FrameLayout(act);
        b.setFocusable(true);
        b.setClickable(true);
        final GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(0x00000000);
        b.setBackground(bg);
        ImageView iv = new ImageView(act);
        iv.setImageDrawable(g);
        int is = (int) Ui.px(30);
        b.addView(iv, new FrameLayout.LayoutParams(is, is, Gravity.CENTER));
        b.setOnFocusChangeListener((v, has) -> {
            bg.setColor(has ? Ui.focusBg(dark) : 0x00000000);
            Ui.lift(v, has, dark);
            g.setColor(has ? Ui.focusText(dark) : Ui.text(dark));
            v.animate().scaleX(has ? 1.1f : 1f).scaleY(has ? 1.1f : 1f).setDuration(150).start();
            if (has) Ui.tick(act);
        });
        b.setOnClickListener(v -> {
            Ui.select(act);
            action.run();
        });
        int sz = (int) Ui.px(74);
        LayoutParams lp = new LayoutParams(sz, sz);
        lp.leftMargin = (int) Ui.px(4);
        b.setLayoutParams(lp);
        return b;
    }

    // ------------------------------------------------------------------ sessões

    void start() {
        if (started || msm == null) return;
        started = true;
        try {
            msm.addOnActiveSessionsChangedListener(sessions, listener);
        } catch (SecurityException e) {
            started = false; // acesso às notificações não liberado
            return;
        }
        pick();
    }

    void stop() {
        if (!started) return;
        started = false;
        try { msm.removeOnActiveSessionsChangedListener(sessions); } catch (Exception ignored) { }
        main.removeCallbacks(recheck);
        setController(null);
    }

    /** Escolhe a sessão: a que está tocando, senão a primeira com música carregada. */
    void pick() {
        if (!started) return;
        MediaController best = null;
        try {
            List<MediaController> list = msm.getActiveSessions(listener);
            for (MediaController c : list) {
                if (c.getPackageName().equals(act.getPackageName())) continue;
                MediaMetadata m = c.getMetadata();
                if (m == null || TextUtils.isEmpty(first(m, MediaMetadata.METADATA_KEY_DISPLAY_TITLE,
                        MediaMetadata.METADATA_KEY_TITLE))) continue; // sessões sem música (ex.: YouTube parado)
                PlaybackState s = c.getPlaybackState();
                if (s != null && s.getState() == PlaybackState.STATE_PLAYING) { best = c; break; }
                if (best == null) best = c;
            }
        } catch (SecurityException ignored) { }
        setController(best);
    }

    void setController(MediaController c) {
        if (controller != null && (c == null || !c.getSessionToken().equals(controller.getSessionToken()))) {
            controller.unregisterCallback(cb);
        }
        boolean same = controller != null && c != null && c.getSessionToken().equals(controller.getSessionToken());
        controller = c;
        if (c != null && !same) c.registerCallback(cb, main);
        refresh();
    }

    String lastKey;

    void refresh() {
        MediaController c = controller;
        MediaMetadata m = c != null ? c.getMetadata() : null;
        PlaybackState s = c != null ? c.getPlaybackState() : null;
        int st = s != null ? s.getState() : PlaybackState.STATE_NONE;
        boolean active = m != null && st != PlaybackState.STATE_NONE && st != PlaybackState.STATE_STOPPED
                && st != PlaybackState.STATE_ERROR;
        String t = active ? first(m, MediaMetadata.METADATA_KEY_DISPLAY_TITLE, MediaMetadata.METADATA_KEY_TITLE) : null;
        if (TextUtils.isEmpty(t)) active = false;
        if (!active) { main.removeCallbacks(recheck); setShown(false); return; }

        String ar = first(m, MediaMetadata.METADATA_KEY_ARTIST, MediaMetadata.METADATA_KEY_ALBUM_ARTIST,
                MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, MediaMetadata.METADATA_KEY_ALBUM);
        title.setText(t);
        artist.setText(ar != null ? ar : "");
        artist.setVisibility(TextUtils.isEmpty(ar) ? GONE : VISIBLE);

        String key = c.getPackageName() + "|" + t + "|" + ar;
        if (!key.equals(lastKey)) {
            lastKey = key;
            Bitmap b = m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
            if (b == null) b = m.getBitmap(MediaMetadata.METADATA_KEY_ART);
            if (b == null) b = m.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
            if (b != null) art.setImageBitmap(b);
            else {
                try { art.setImageDrawable(act.getPackageManager().getApplicationIcon(c.getPackageName())); }
                catch (Exception e) { art.setImageDrawable(null); }
            }
        }
        boolean playing = st == PlaybackState.STATE_PLAYING || st == PlaybackState.STATE_BUFFERING;
        playGlyph.kindOverride(playing ? Glyph.Kind.PAUSE : Glyph.Kind.PLAY);

        main.removeCallbacks(recheck);
        // Fechado no ✕: fica escondido até mudar a música ou voltar a tocar.
        if (key.equals(dismissedKey) && playing == dismissedPlaying) { setShown(false); return; }
        dismissedKey = null;
        // Pausado continua visível: só some no ✕ ou quando o app de música fecha a sessão.
        setShown(true);
    }

    static String first(MediaMetadata m, String... keys) {
        for (String k : keys) {
            CharSequence v = m.getText(k);
            if (v != null && v.length() > 0) return v.toString();
        }
        return null;
    }

    void setShown(boolean show) {
        if (show == shown) return;
        shown = show;
        animate().cancel();
        if (show) {
            setVisibility(VISIBLE);
            setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
            setTranslationY(-Ui.px(12));
            animate().alpha(1f).translationY(0).setDuration(320).start();
        } else {
            if (hasFocus()) {
                // devolve a seleção para a fileira de apps logo abaixo
                View f = findFocus();
                View next = f != null ? f.focusSearch(View.FOCUS_DOWN) : null;
                setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
                if (next == null || !next.requestFocus()) act.getWindow().getDecorView().requestFocus();
            }
            setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
            animate().alpha(0f).setDuration(220).withEndAction(() -> { if (!shown) setVisibility(GONE); }).start();
        }
    }

    void openPlayer() {
        if (controller == null) return;
        Ui.select(act);
        String pkg = controller.getPackageName();
        Intent i = act.getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = act.getPackageManager().getLaunchIntentForPackage(pkg);
        if (i != null) {
            try { act.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); } catch (Exception ignored) { }
        }
    }
}
