package com.rinostick.app;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

/**
 * Fundo "aurora": manchas de luz suaves que derivam devagar sobre um fundo
 * quase preto. Uma das manchas assume a cor do app em foco.
 *
 * Desempenho: tudo é desenhado num bitmap minúsculo (160x90) por software e
 * só ampliado na tela com filtro — como o fundo é totalmente desfocado, o
 * resultado é idêntico e a GPU da TV fica livre. A animação também
 * pausa enquanto o usuário navega.
 */
public class AuroraView extends View {

    static final int BASE = 0xFF07070B;
    static final int BW = 160, BH = 90;
    static final long FRAME_MS = 66;      // ~15 fps: o movimento é bem lento
    static final long QUIET_MS = 900;     // pausa após cada tecla

    static class Blob {
        int color;
        float cx, cy, r, ax, ay, fx, fy, phase;
        RadialGradient shader;
        final Matrix m = new Matrix();
    }

    final Bitmap buf = Bitmap.createBitmap(BW, BH, Bitmap.Config.ARGB_8888);
    final Canvas bc = new Canvas(buf);
    final Paint blobPaint = new Paint(Paint.DITHER_FLAG);
    final Paint blit = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    final Rect dst = new Rect();
    final Blob[] blobs = new Blob[5];
    final long start = SystemClock.uptimeMillis();
    boolean running = true;
    long quietUntil = 0;
    ValueAnimator tintAnim, heroAnim;

    // Fundo desfocado do app em foco (cross-fade entre o anterior e o novo)
    Bitmap heroFrom, heroTo;
    float heroMix = 1f;
    final Rect heroDst = new Rect(0, 0, BW, Math.round(BH * 0.62f));
    final Paint heroPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    static final float HERO_ALPHA = 0.55f;

    // Sombras estáticas para legibilidade (lateral esquerda e base)
    final Paint sideShade = new Paint();
    final Paint bottomShade = new Paint();

    final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            if (SystemClock.uptimeMillis() >= quietUntil) {
                renderBuffer();
                invalidate();
            }
            postDelayed(this, FRAME_MS);
        }
    };

    public AuroraView(Context c) {
        super(c);
        //            cor            cx     cy    raio  amplitude x/y   frequência x/y  fase
        blobs[0] = blob(0xB0302A8C, 0.18f, 0.25f, 0.55f, 0.10f, 0.08f, 0.050f, 0.043f, 0.0f);
        blobs[1] = blob(0x9A5A1F7A, 0.82f, 0.18f, 0.50f, 0.09f, 0.10f, 0.037f, 0.055f, 1.7f);
        blobs[2] = blob(0x8C0D5A6E, 0.70f, 0.85f, 0.60f, 0.12f, 0.07f, 0.045f, 0.031f, 3.1f);
        blobs[3] = blob(0x8C14306E, 0.22f, 0.90f, 0.55f, 0.08f, 0.09f, 0.029f, 0.047f, 4.4f);
        blobs[4] = blob(0x00000000, 0.55f, 0.30f, 0.45f, 0.10f, 0.06f, 0.041f, 0.036f, 2.3f); // cor do app
        for (Blob b : blobs) makeShader(b);
        int base = BASE & 0x00FFFFFF;
        sideShade.setShader(new android.graphics.LinearGradient(0, 0, BW * 0.6f, 0,
                new int[]{base | 0x80000000, base | 0x20000000, base},
                null, Shader.TileMode.CLAMP));
        bottomShade.setShader(new android.graphics.LinearGradient(0, BH * 0.5f, 0, BH,
                new int[]{base, base | 0x59000000, base | 0xA6000000},
                null, Shader.TileMode.CLAMP));
        renderBuffer();
    }

    /** Troca o fundo do app em foco com um cross-fade suave. */
    public void setHero(Bitmap b) {
        if (b == heroTo) return;
        if (heroAnim != null) heroAnim.cancel();
        heroFrom = heroTo;
        heroTo = b;
        heroMix = heroFrom == null ? 1f : 0f;
        if (heroFrom == null) {
            renderBuffer();
            invalidate();
            return;
        }
        heroAnim = ValueAnimator.ofFloat(0f, 1f);
        heroAnim.setDuration(380);
        heroAnim.addUpdateListener(a -> {
            heroMix = (Float) a.getAnimatedValue();
            renderBuffer();
            invalidate();
        });
        heroAnim.start();
    }

    static Blob blob(int color, float cx, float cy, float r, float ax, float ay,
                     float fx, float fy, float phase) {
        Blob b = new Blob();
        b.color = color; b.cx = cx; b.cy = cy; b.r = r;
        b.ax = ax; b.ay = ay; b.fx = fx; b.fy = fy; b.phase = phase;
        return b;
    }

    void makeShader(Blob b) {
        float r = b.r * BW;
        int a = b.color >>> 24;
        int rgb = b.color & 0x00FFFFFF;
        b.shader = new RadialGradient(0, 0, r,
                new int[]{b.color, rgb | (Math.round(a * 0.45f) << 24), rgb},
                new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP);
    }

    void renderBuffer() {
        bc.drawColor(BASE);
        float t = (SystemClock.uptimeMillis() - start) / 1000f;
        for (Blob b : blobs) {
            if ((b.color >>> 24) == 0) continue;
            float x = (b.cx + (float) Math.sin(t * b.fx * 6.283f + b.phase) * b.ax) * BW;
            float y = (b.cy + (float) Math.cos(t * b.fy * 6.283f + b.phase) * b.ay) * BH;
            b.m.setTranslate(x, y);
            b.shader.setLocalMatrix(b.m);
            blobPaint.setShader(b.shader);
            bc.drawRect(0, 0, BW, BH, blobPaint);
        }
        if (heroFrom != null && heroMix < 1f) {
            heroPaint.setAlpha(Math.round(255 * HERO_ALPHA * (1f - heroMix)));
            bc.drawBitmap(heroFrom, null, heroDst, heroPaint);
        }
        if (heroTo != null) {
            heroPaint.setAlpha(Math.round(255 * HERO_ALPHA * heroMix));
            bc.drawBitmap(heroTo, null, heroDst, heroPaint);
        }
        bc.drawRect(0, 0, BW, BH, sideShade);
        bc.drawRect(0, 0, BW, BH, bottomShade);
    }

    /** Faz a mancha de destaque ir suavemente para a cor do app em foco. */
    public void tint(int color) {
        final Blob b = blobs[4];
        int target = (color & 0x00FFFFFF) | 0xA6000000;
        if (tintAnim != null) tintAnim.cancel();
        tintAnim = ValueAnimator.ofObject(new ArgbEvaluator(), b.color, target);
        tintAnim.setDuration(700);
        tintAnim.setStartDelay(QUIET_MS - 150);
        tintAnim.addUpdateListener(a -> {
            b.color = (Integer) a.getAnimatedValue();
            makeShader(b);
        });
        tintAnim.start();
    }

    /** Chamado a cada tecla: segura a animação para a navegação ficar fluida. */
    public void poke() {
        quietUntil = SystemClock.uptimeMillis() + QUIET_MS;
    }

    public void setRunning(boolean r) {
        if (r == running) return;
        running = r;
        removeCallbacks(tick);
        if (r) post(tick);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        removeCallbacks(tick);
        if (running) post(tick);
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(tick);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas c) {
        dst.set(0, 0, getWidth(), getHeight());
        c.drawBitmap(buf, null, dst, blit);
    }
}
