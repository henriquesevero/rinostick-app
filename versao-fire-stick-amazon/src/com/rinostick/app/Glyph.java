package com.rinostick.app;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** Ícones simples desenhados em código (sem depender de imagens). */
class Glyph extends Drawable {

    enum Kind { GEAR, POWER, APPEARANCE, SPEAKER, WIFI, BAG, TV, EYE, SORT, RESTORE, CLOSE, PLAY, PAUSE, NEXT, XMARK, SUN, MOON, CLOUD, PARTLY, RAIN, STORM, FOG }

    Kind kind;

    void kindOverride(Kind k) {
        if (k == kind) return;
        kind = k;
        invalidateSelf();
    }
    final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    boolean muted;

    Glyph(Kind kind, int color) {
        this.kind = kind;
        fill.setStyle(Paint.Style.FILL);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        setColor(color);
    }

    void setColor(int c) {
        fill.setColor(c);
        stroke.setColor(c);
        invalidateSelf();
    }

    void setMuted(boolean m) {
        muted = m;
        invalidateSelf();
    }

    @Override
    public void draw(Canvas c) {
        Rect b = getBounds();
        float s = Math.min(b.width(), b.height());
        float cx = b.exactCenterX(), cy = b.exactCenterY();
        float sw = s * 0.085f;
        stroke.setStrokeWidth(sw);
        switch (kind) {
            case GEAR: drawGear(c, cx, cy, s * 0.5f); break;
            case POWER: {
                float r = s * 0.36f;
                RectF o = new RectF(cx - r, cy - r + s * 0.04f, cx + r, cy + r + s * 0.04f);
                c.drawArc(o, -60, 300, false, stroke);
                c.drawLine(cx, cy - s * 0.42f, cx, cy - s * 0.02f, stroke);
                break;
            }
            case APPEARANCE: {
                float r = s * 0.38f;
                c.drawCircle(cx, cy, r, stroke);
                RectF o = new RectF(cx - r, cy - r, cx + r, cy + r);
                c.drawArc(o, 90, 180, true, fill);
                break;
            }
            case SPEAKER: {
                Path p = new Path();
                float l = cx - s * 0.40f;
                p.moveTo(l, cy - s * 0.12f);
                p.lineTo(l + s * 0.16f, cy - s * 0.12f);
                p.lineTo(l + s * 0.38f, cy - s * 0.34f);
                p.lineTo(l + s * 0.38f, cy + s * 0.34f);
                p.lineTo(l + s * 0.16f, cy + s * 0.12f);
                p.lineTo(l, cy + s * 0.12f);
                p.close();
                c.drawPath(p, fill);
                float ax = l + s * 0.40f;
                if (muted) {
                    c.drawLine(ax + s * 0.12f, cy - s * 0.14f, ax + s * 0.38f, cy + s * 0.14f, stroke);
                    c.drawLine(ax + s * 0.12f, cy + s * 0.14f, ax + s * 0.38f, cy - s * 0.14f, stroke);
                } else {
                    for (int i = 1; i <= 2; i++) {
                        float r = s * 0.16f * i;
                        c.drawArc(new RectF(ax - r, cy - r, ax + r, cy + r), -45, 90, false, stroke);
                    }
                }
                break;
            }
            case WIFI: {
                float by = cy + s * 0.30f;
                for (int i = 1; i <= 3; i++) {
                    float r = s * 0.20f * i;
                    c.drawArc(new RectF(cx - r, by - r, cx + r, by + r), -135, 90, false, stroke);
                }
                c.drawCircle(cx, by - s * 0.02f, s * 0.07f, fill);
                break;
            }
            case PLAY: {
                Path p = new Path();
                p.moveTo(cx - s * 0.24f, cy - s * 0.36f);
                p.lineTo(cx + s * 0.38f, cy);
                p.lineTo(cx - s * 0.24f, cy + s * 0.36f);
                p.close();
                c.drawPath(p, fill);
                break;
            }
            case PAUSE: {
                float w = s * 0.20f, h = s * 0.36f, r = s * 0.04f;
                c.drawRoundRect(new RectF(cx - s * 0.30f, cy - h, cx - s * 0.30f + w, cy + h), r, r, fill);
                c.drawRoundRect(new RectF(cx + s * 0.10f, cy - h, cx + s * 0.10f + w, cy + h), r, r, fill);
                break;
            }
            case NEXT: {
                float h = s * 0.28f;
                for (int i = 0; i < 2; i++) {
                    float x0 = cx - s * 0.42f + i * s * 0.36f;
                    Path p = new Path();
                    p.moveTo(x0, cy - h);
                    p.lineTo(x0 + s * 0.36f, cy);
                    p.lineTo(x0, cy + h);
                    p.close();
                    c.drawPath(p, fill);
                }
                c.drawRoundRect(new RectF(cx + s * 0.32f, cy - h, cx + s * 0.42f, cy + h), s * 0.03f, s * 0.03f, fill);
                break;
            }
            case SUN: drawSun(c, cx, cy, s * 0.5f); break;
            case MOON: {
                Path m = new Path();
                m.addCircle(cx, cy, s * 0.36f, Path.Direction.CW);
                Path cut = new Path();
                cut.addCircle(cx + s * 0.20f, cy - s * 0.14f, s * 0.30f, Path.Direction.CW);
                m.op(cut, Path.Op.DIFFERENCE);
                c.drawPath(m, fill);
                break;
            }
            case CLOUD: drawCloud(c, cx, cy + s * 0.04f, s); break;
            case PARTLY:
                drawSun(c, cx - s * 0.16f, cy - s * 0.14f, s * 0.34f);
                drawCloud(c, cx + s * 0.08f, cy + s * 0.12f, s * 0.80f);
                break;
            case RAIN: {
                drawCloud(c, cx, cy - s * 0.14f, s * 0.92f);
                stroke.setStrokeWidth(s * 0.07f);
                for (int i = -1; i <= 1; i++) {
                    float x = cx + i * s * 0.20f;
                    c.drawLine(x + s * 0.04f, cy + s * 0.16f, x - s * 0.04f, cy + s * 0.36f, stroke);
                }
                break;
            }
            case STORM: {
                drawCloud(c, cx, cy - s * 0.14f, s * 0.92f);
                Path bolt = new Path();
                bolt.moveTo(cx + s * 0.04f, cy + s * 0.08f);
                bolt.lineTo(cx - s * 0.12f, cy + s * 0.28f);
                bolt.lineTo(cx + s * 0.01f, cy + s * 0.28f);
                bolt.lineTo(cx - s * 0.06f, cy + s * 0.46f);
                bolt.lineTo(cx + s * 0.14f, cy + s * 0.22f);
                bolt.lineTo(cx + s * 0.01f, cy + s * 0.22f);
                bolt.close();
                c.drawPath(bolt, fill);
                break;
            }
            case FOG: {
                for (int i = 0; i < 3; i++) {
                    float y = cy - s * 0.20f + i * s * 0.20f;
                    float inset = i == 1 ? s * 0.06f : 0;
                    c.drawLine(cx - s * 0.38f + inset, y, cx + s * 0.38f - inset, y, stroke);
                }
                break;
            }
            case XMARK: {
                float d = s * 0.26f;
                c.drawLine(cx - d, cy - d, cx + d, cy + d, stroke);
                c.drawLine(cx - d, cy + d, cx + d, cy - d, stroke);
                break;
            }
            case CLOSE: {
                float r = s * 0.38f, d = s * 0.14f;
                c.drawCircle(cx, cy, r, stroke);
                c.drawLine(cx - d, cy - d, cx + d, cy + d, stroke);
                c.drawLine(cx - d, cy + d, cx + d, cy - d, stroke);
                break;
            }
            case TV: {
                RectF scr = new RectF(cx - s * 0.42f, cy - s * 0.32f, cx + s * 0.42f, cy + s * 0.20f);
                c.drawRoundRect(scr, s * 0.06f, s * 0.06f, stroke);
                c.drawLine(cx - s * 0.18f, cy + s * 0.36f, cx + s * 0.18f, cy + s * 0.36f, stroke);
                break;
            }
            case EYE: {
                Path p = new Path();
                float hw = s * 0.44f, hh = s * 0.26f;
                p.moveTo(cx - hw, cy);
                p.quadTo(cx, cy - hh * 2f, cx + hw, cy);
                p.quadTo(cx, cy + hh * 2f, cx - hw, cy);
                p.close();
                c.drawPath(p, stroke);
                c.drawCircle(cx, cy, s * 0.11f, fill);
                break;
            }
            case SORT: {
                float l = cx - s * 0.36f;
                c.drawLine(l, cy - s * 0.24f, cx + s * 0.36f, cy - s * 0.24f, stroke);
                c.drawLine(l, cy, cx + s * 0.16f, cy, stroke);
                c.drawLine(l, cy + s * 0.24f, cx - s * 0.04f, cy + s * 0.24f, stroke);
                break;
            }
            case RESTORE: {
                float r = s * 0.34f;
                c.drawArc(new RectF(cx - r, cy - r, cx + r, cy + r), -60, 290, false, stroke);
                // ponta da seta no início do arco
                float ax = cx + r * (float) Math.cos(Math.toRadians(-60));
                float ay = cy + r * (float) Math.sin(Math.toRadians(-60));
                Path h = new Path();
                h.moveTo(ax + s * 0.02f, ay - s * 0.17f);
                h.lineTo(ax + s * 0.05f, ay + s * 0.03f);
                h.lineTo(ax - s * 0.15f, ay + s * 0.05f);
                c.drawPath(h, stroke);
                break;
            }
            case BAG: {
                float w = s * 0.70f, h = s * 0.54f, top = cy - s * 0.14f;
                RectF body = new RectF(cx - w / 2, top, cx + w / 2, top + h);
                c.drawRoundRect(body, s * 0.08f, s * 0.08f, stroke);
                float hr = s * 0.17f;
                c.drawArc(new RectF(cx - hr, top - hr * 1.3f, cx + hr, top + hr * 0.9f), 180, 180, false, stroke);
                break;
            }
        }
    }

    void drawSun(Canvas c, float cx, float cy, float R) {
        c.drawCircle(cx, cy, R * 0.42f, fill);
        stroke.setStrokeWidth(R * 0.14f);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI / 4 * i;
            float cs = (float) Math.cos(a), sn = (float) Math.sin(a);
            c.drawLine(cx + cs * R * 0.62f, cy + sn * R * 0.62f, cx + cs * R * 0.86f, cy + sn * R * 0.86f, stroke);
        }
    }

    /** Nuvem: três círculos sobre uma base arredondada. */
    void drawCloud(Canvas c, float cx, float cy, float s) {
        Path p = new Path();
        p.addRoundRect(new RectF(cx - s * 0.40f, cy - s * 0.02f, cx + s * 0.40f, cy + s * 0.22f),
                s * 0.12f, s * 0.12f, Path.Direction.CW);
        p.addCircle(cx - s * 0.18f, cy, s * 0.17f, Path.Direction.CW);
        p.addCircle(cx + s * 0.06f, cy - s * 0.08f, s * 0.24f, Path.Direction.CW);
        p.addCircle(cx + s * 0.26f, cy + s * 0.04f, s * 0.15f, Path.Direction.CW);
        p.setFillType(Path.FillType.WINDING);
        c.drawPath(p, fill);
    }

    void drawGear(Canvas c, float cx, float cy, float R) {
        Path gear = new Path();
        gear.addCircle(cx, cy, R * 0.72f, Path.Direction.CW);
        int teeth = 10;
        float tw = R * 0.30f, th = R * 0.34f;
        for (int i = 0; i < teeth; i++) {
            Path t = new Path();
            t.addRoundRect(new RectF(cx - tw / 2, cy - R, cx + tw / 2, cy - R + th),
                    tw * 0.2f, tw * 0.2f, Path.Direction.CW);
            Matrix m = new Matrix();
            m.setRotate(360f / teeth * i, cx, cy);
            t.transform(m);
            gear.op(t, Path.Op.UNION);
        }
        Path hole = new Path();
        hole.addCircle(cx, cy, R * 0.30f, Path.Direction.CW);
        gear.op(hole, Path.Op.DIFFERENCE);
        c.drawPath(gear, fill);
    }

    @Override public void setAlpha(int a) { fill.setAlpha(a); stroke.setAlpha(a); }
    @Override public void setColorFilter(ColorFilter cf) { fill.setColorFilter(cf); stroke.setColorFilter(cf); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
