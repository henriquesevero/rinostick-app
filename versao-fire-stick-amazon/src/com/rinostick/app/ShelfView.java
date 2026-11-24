package com.rinostick.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * Fundo da tela inicial no estilo tvOS: cor de base (clara ou escura) e, no
 * topo, a "prateleira" com a imagem desfocada do app em foco na primeira
 * linha. Tudo é composto num bitmap pequeno e ampliado — leve para a GPU — e
 * só é redesenhado quando algo muda (sem animação contínua).
 */
public class ShelfView extends View {

    static final int BW = 192, BH = 108;

    final Bitmap buf = Bitmap.createBitmap(BW, BH, Bitmap.Config.ARGB_8888);
    final Canvas bc = new Canvas(buf);
    final Paint blit = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    final Paint heroPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    final Paint fade = new Paint(Paint.DITHER_FLAG);
    final Paint vignette = new Paint(Paint.DITHER_FLAG);
    final Rect dst = new Rect();
    final Rect heroDst = new Rect(0, 0, BW, Math.round(BH * 0.78f));

    final boolean dark;
    final int base;
    final float strength;

    Bitmap heroFrom, heroTo;
    float mix = 1f, vis = 1f;
    ValueAnimator mixAnim, visAnim;

    public ShelfView(Context c, boolean dark) {
        super(c);
        this.dark = dark;
        this.base = Ui.base(dark);
        this.strength = dark ? 0.95f : 0.80f;
        int b = base & 0x00FFFFFF;
        fade.setShader(new LinearGradient(0, BH * 0.30f, 0, BH * 0.78f,
                new int[]{b, b | 0x8C000000, base}, new float[]{0f, 0.55f, 1f},
                Shader.TileMode.CLAMP));
        vignette.setShader(new LinearGradient(0, 0, 0, BH,
                new int[]{dark ? 0x26000000 : 0x00000000, 0x00000000, dark ? 0x40000000 : 0x0D000000},
                new float[]{0f, 0.4f, 1f}, Shader.TileMode.CLAMP));
        render();
    }

    void render() {
        bc.drawColor(base);
        float a = strength * vis;
        if (heroFrom != null && mix < 1f) {
            heroPaint.setAlpha(Math.round(255 * a * (1f - mix)));
            bc.drawBitmap(heroFrom, null, heroDst, heroPaint);
        }
        if (heroTo != null) {
            heroPaint.setAlpha(Math.round(255 * a * mix));
            bc.drawBitmap(heroTo, null, heroDst, heroPaint);
        }
        bc.drawRect(0, 0, BW, BH, fade);
        bc.drawRect(0, 0, BW, BH, vignette);
        invalidate();
    }

    /** Troca a imagem da prateleira com cross-fade. */
    public void setHero(Bitmap b) {
        if (b == heroTo) return;
        if (mixAnim != null) mixAnim.cancel();
        heroFrom = heroTo;
        heroTo = b;
        if (heroFrom == null) {
            mix = 1f;
            render();
            return;
        }
        mix = 0f;
        mixAnim = ValueAnimator.ofFloat(0f, 1f);
        mixAnim.setDuration(420);
        mixAnim.addUpdateListener(v -> {
            mix = (Float) v.getAnimatedValue();
            render();
        });
        mixAnim.start();
    }

    /** Mostra/esconde a prateleira (some quando o foco desce para outras linhas). */
    public void setHeroVisible(boolean show) {
        float target = show ? 1f : 0f;
        if (visAnim != null) visAnim.cancel();
        if (vis == target) return;
        visAnim = ValueAnimator.ofFloat(vis, target);
        visAnim.setDuration(380);
        visAnim.setInterpolator(new DecelerateInterpolator(1.5f));
        visAnim.addUpdateListener(v -> {
            vis = (Float) v.getAnimatedValue();
            render();
        });
        visAnim.start();
    }

    @Override
    protected void onDraw(Canvas c) {
        dst.set(0, 0, getWidth(), getHeight());
        c.drawBitmap(buf, null, dst, blit);
    }
}
