package com.alphahub.v2;
import android.content.*;
import android.graphics.*;
import android.view.*;

public class TriggerView extends View {
    interface Listener { void onOpenRail(); void onExpand(); }
    Listener l;
    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    float d, downX, downY;
    long downT;
    boolean rail;

    TriggerView(Context c, Listener x) {
        super(c);
        l = x;
        d = getResources().getDisplayMetrics().density;
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    void box(Canvas c, float a, float b, float x, float y, float r, int col) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(col);
        c.drawRoundRect(a, b, x, y, r, r, p);
    }

    void t(Canvas c, String s, float x, float y, float z, int col) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(col);
        p.setTextSize(z);
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText(s, x, y, p);
    }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        if (!rail) {
            // Slim cyan-edged handle; the rest of the overlay stays fully transparent.
            float pillW = Math.max(3 * d, w * 0.34f);
            float pillH = h * 0.96f;
            float left = (w - pillW) / 2f;
            float top = (h - pillH) / 2f;
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Math.max(1.2f, 1.2f * d));
            p.setColor(Color.argb(175, 190, 205, 220));
            c.drawRoundRect(left, top, left + pillW, top + pillH, pillW / 2f, pillW / 2f, p);
            return;
        }

        box(c, 0, 0, w, h, 28*d, Color.rgb(24,25,32));
        box(c, 14*d, 20*d, w-14*d, 126*d, 20*d, Color.rgb(17,31,60));
        box(c, 25*d, 30*d, 81*d, 86*d, 12*d, Color.rgb(25,125,255));
        t(c, "A", 53*d, 70*d, 28*d, Color.WHITE);
        t(c, "Screen", w/2, 100*d, 14*d, Color.WHITE);
        t(c, "translation", w/2, 118*d, 13*d, Color.WHITE);
        for (int i=0; i<5; i++) box(c, 25*d, (145+i*68)*d, 81*d, (195+i*68)*d, 12*d, Color.rgb(24,43,78));
        box(c, 22*d, h-112*d, 84*d, h-58*d, 27*d, Color.rgb(28,43,68));
        t(c, "✎", 53*d, h-76*d, 27*d, Color.WHITE);
        t(c, "Edit", 53*d, h-38*d, 13*d, Color.WHITE);
        box(c, 22*d, h-50*d, 84*d, h+2*d, 26*d, Color.rgb(25,50,90));
        t(c, "‹", 53*d, h-16*d, 30*d, Color.WHITE);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            downX = e.getX();
            downY = e.getY();
            downT = System.currentTimeMillis();
            return true;
        }
        if (e.getAction() == MotionEvent.ACTION_UP) {
            long dt = System.currentTimeMillis() - downT;
            float dx = e.getX() - downX;
            if (!rail) {
                if (dx > 8*d || dt < 700) l.onOpenRail();
            } else if (e.getY() > getHeight()-90*d || dx > 25*d) {
                l.onExpand();
            }
            performClick();
            return true;
        }
        return true;
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }

    void setRailMode(boolean v) { rail = v; invalidate(); }
}