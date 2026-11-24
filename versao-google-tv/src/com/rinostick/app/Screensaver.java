package com.rinostick.app;

import android.os.Handler;
import android.os.Looper;
import android.service.dreams.DreamService;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

/** Protetor de tela: aurora em movimento lento com a hora discreta. */
public class Screensaver extends DreamService {

    AuroraView aurora;
    TextView clock;
    final Handler h = new Handler(Looper.getMainLooper());
    final Runnable tick = new Runnable() {
        @Override public void run() {
            updateClock();
            h.postDelayed(this, 20000);
        }
    };

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        Ui.init(this);

        FrameLayout root = new FrameLayout(this);
        aurora = new AuroraView(this);
        aurora.tint(0xFF2E6FA8);
        root.addView(aurora, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));

        clock = Ui.text(this, Ui.px(46), Ui.medium, 0xD9FFFFFF);
        clock.setShadowLayer(Ui.px(20), 0, Ui.px(2), 0x66000000);
        FrameLayout.LayoutParams cl = new FrameLayout.LayoutParams(Ui.WRAP, Ui.WRAP, Gravity.TOP | Gravity.END);
        cl.topMargin = (int) Ui.px(60);
        cl.rightMargin = (int) Ui.px(90);
        root.addView(clock, cl);
        setContentView(root);
    }

    void updateClock() {
        String fmt = DateFormat.is24HourFormat(this) ? "HH:mm" : "h:mm";
        clock.setText(DateFormat.format(fmt, System.currentTimeMillis()));
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        aurora.setRunning(true);
        h.post(tick);
    }

    @Override
    public void onDreamingStopped() {
        super.onDreamingStopped();
        aurora.setRunning(false);
        h.removeCallbacks(tick);
    }
}
