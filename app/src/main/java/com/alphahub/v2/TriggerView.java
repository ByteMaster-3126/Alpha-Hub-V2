package com.alphahub.v2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

/** Three-state edge launcher: slim edge handle, compact tool bubble, full dashboard. */
public class TriggerView extends View {
    interface Listener {
        void onOpenRail();
        void onExpand();
        void onCollapse();
    }

    private final Listener listener;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private float downX, downY;
    private boolean rail;

    TriggerView(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        density = getResources().getDisplayMetrics().density;
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setContentDescription("Alpha Hub edge trigger");
        setClickable(true);
    }

    private void rounded(Canvas c, float l, float t, float r, float b,
                         float radius, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        c.drawRoundRect(l, t, r, b, radius, radius, paint);
    }

    private void text(Canvas c, String s, float x, float y, float size, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextSize(size);
        paint.setTextAlign(Paint.Align.CENTER);
        c.drawText(s, x, y, paint);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        if (!rail) {
            // Reference collapsed trigger: narrow, transparent, left-edge pill, no arrow.
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1f, density * .7f));
            paint.setColor(Color.argb(155, 225, 239, 255));
            float inset = Math.max(1f, density * .5f);
            canvas.drawRoundRect(inset, h * .08f, Math.max(inset + 1, w - inset),
                    h * .92f, w / 2f, w / 2f, paint);
            paint.setStyle(Paint.Style.FILL);
            return;
        }

        // Compact launcher bubble as seen in the recording, not the full-height rail.
        rounded(canvas, 0, 0, w, h, 20 * density, Color.rgb(22, 25, 35));
        rounded(canvas, 10 * density, 12 * density, w - 10 * density,
                104 * density, 15 * density, Color.rgb(17, 36, 72));
        rounded(canvas, 22 * density, 23 * density, w - 22 * density,
                68 * density, 10 * density, Color.rgb(25, 125, 255));
        text(canvas, "A", w / 2f, 53 * density, 25 * density, Color.WHITE);
        text(canvas, "Screen", w / 2f, 83 * density, 13 * density, Color.WHITE);
        text(canvas, "translation", w / 2f, 99 * density, 12 * density, Color.WHITE);

        for (int i = 0; i < 3; i++) {
            rounded(canvas, 22 * density, (120 + i * 28) * density,
                    w - 22 * density, (139 + i * 28) * density,
                    7 * density, Color.rgb(24, 43, 78));
        }
        rounded(canvas, 17 * density, h - 47 * density,
                w - 17 * density, h - 8 * density, 19 * density,
                Color.rgb(28, 43, 68));
        text(canvas, "‹", w / 2f, h - 19 * density, 27 * density, Color.WHITE);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            downX = event.getX();
            downY = event.getY();
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float dx = event.getX() - downX;
            float dy = event.getY() - downY;
            if (!rail) {
                // Tap or swipe inward opens the compact floating launcher.
                if (Math.abs(dx) < 24 * density || dx > 0) {
                    rail = true;
                    invalidate();
                    listener.onOpenRail();
                }
            } else if (event.getY() >= getHeight() - 54 * density) {
                // Bottom chevron returns to the slim edge trigger.
                listener.onCollapse();
            } else if (Math.abs(dx) < 24 * density || dx > 0
                    || Math.abs(dy) > 24 * density) {
                // Tap the compact tool card to expand the dashboard.
                listener.onExpand();
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

    void setRailMode(boolean value) {
        rail = value;
        invalidate();
    }
}
