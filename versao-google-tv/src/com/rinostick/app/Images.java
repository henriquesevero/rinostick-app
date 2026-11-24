package com.rinostick.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

/** Cor dominante e fundo desfocado ("prateleira") a partir do ícone/banner. */
final class Images {

    private Images() { }

    static Drawable copy(Context c, Drawable d) {
        Drawable.ConstantState cs = d.getConstantState();
        return cs != null ? cs.newDrawable(c.getResources()).mutate() : d;
    }

    static int dominant(Context ctx, Drawable d) {
        if (d == null) return 0xFF2A2A2E;
        int n = 24;
        Bitmap b = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888);
        Drawable dd = copy(ctx, d);
        dd.setBounds(0, 0, n, n);
        dd.draw(new Canvas(b));
        int[] p = new int[n * n];
        b.getPixels(p, 0, n, 0, 0, n, n);
        b.recycle();
        long r = 0, g = 0, bl = 0, cnt = 0, rs = 0, gs = 0, bs = 0, cs = 0;
        float[] hsv = new float[3];
        for (int c : p) {
            if (Color.alpha(c) < 160) continue;
            r += Color.red(c); g += Color.green(c); bl += Color.blue(c); cnt++;
            Color.colorToHSV(c, hsv);
            if (hsv[1] > 0.25f && hsv[2] > 0.2f) {
                rs += Color.red(c); gs += Color.green(c); bs += Color.blue(c); cs++;
            }
        }
        int avg;
        if (cs > 20) avg = Color.rgb((int) (rs / cs), (int) (gs / cs), (int) (bs / cs));
        else if (cnt > 0) avg = Color.rgb((int) (r / cnt), (int) (g / cnt), (int) (bl / cnt));
        else return 0xFF2A2A2E;
        Color.colorToHSV(avg, hsv);
        hsv[2] = Math.min(hsv[2], 0.45f);
        hsv[1] = Math.min(hsv[1], 0.85f);
        return Color.HSVToColor(hsv);
    }

    static int darker(int c, float f) {
        return Color.rgb((int) (Color.red(c) * f), (int) (Color.green(c) * f), (int) (Color.blue(c) * f));
    }

    /** Bitmap pequeno, desfocado e com bordas esmaecidas (ampliado pela ShelfView). */
    static Bitmap backdrop(Context ctx, Drawable banner, Drawable icon, int color, boolean dark) {
        int w = 64, h = 36;
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.drawColor(color);
        if (banner != null) {
            Drawable d = copy(ctx, banner);
            d.setBounds(0, 0, w, h);
            d.draw(c);
        } else if (icon != null) {
            Drawable d = copy(ctx, icon);
            int sz = 22;
            d.setBounds((w - sz) / 2, (h - sz) / 2, (w + sz) / 2, (h + sz) / 2);
            d.draw(c);
        }
        int[] p = new int[w * h];
        b.getPixels(p, 0, w, 0, 0, w, h);
        for (int i = 0; i < 3; i++) boxBlur(p, w, h, 3);
        // No escuro, limita o brilho (banners brancos viravam um topo cinza-claro)
        // e realça a cor; no claro, limita o escuro.
        float[] hsv = new float[3];
        for (int y = 0; y < h; y++) {
            float fy = y / (float) (h - 1);
            float ay = fy < 0.45f ? 1f : Math.max(0f, 1f - (fy - 0.45f) / 0.55f);
            for (int x = 0; x < w; x++) {
                Color.colorToHSV(p[y * w + x], hsv);
                if (dark) {
                    hsv[2] = Math.min(hsv[2] * 0.85f, 0.42f);
                    hsv[1] = Math.min(1f, hsv[1] * 1.15f);
                } else {
                    hsv[2] = Math.max(hsv[2], 0.70f);
                    hsv[1] = hsv[1] * 0.85f;
                }
                p[y * w + x] = Color.HSVToColor((int) (255 * ay), hsv);
            }
        }
        b.setPixels(p, 0, w, 0, 0, w, h);
        return b;
    }

    static void boxBlur(int[] p, int w, int h, int r) {
        int[] t = new int[p.length];
        blurPass(p, t, w, h, r, true);
        blurPass(t, p, w, h, r, false);
    }

    static void blurPass(int[] src, int[] dst, int w, int h, int r, boolean horiz) {
        int n = horiz ? w : h, lines = horiz ? h : w;
        for (int l = 0; l < lines; l++) {
            for (int i = 0; i < n; i++) {
                int sr = 0, sg = 0, sb = 0, cnt = 0;
                for (int k = -r; k <= r; k++) {
                    int j = Math.min(n - 1, Math.max(0, i + k));
                    int c = src[horiz ? l * w + j : j * w + l];
                    sr += (c >> 16) & 255;
                    sg += (c >> 8) & 255;
                    sb += c & 255;
                    cnt++;
                }
                dst[horiz ? l * w + i : i * w + l] =
                        0xFF000000 | ((sr / cnt) << 16) | ((sg / cnt) << 8) | (sb / cnt);
            }
        }
    }
}
