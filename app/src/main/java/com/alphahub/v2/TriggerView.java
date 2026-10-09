package com.alphahub.v2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

/**
 * Compact edge handle. The collapsed state is a slim transparent touch target;
 * opening it reveals the utility rail. Any tap on the rail opens the dashboard.
 */
public class TriggerView extends View {
    interface Listener {
        void onOpenRail();
        void onExpand();
    }

    private final Listener listener;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private float downX;
    private float downY;
    private long downTime;
    private boolean rail;

    TriggerView(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        density = getResources().getDisplayMetrics().density;
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setContentDescription("Alpha Hub edge trigger");
        setClickable(true);
    }

    private void rounded(Canvas canvas, float left, float top, float right,
                         float bottom, float radius, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();

        if (!rail) {
            // Transparent handle with a very subtle outline; deliberately no arrow.
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1f, density * 0.7f));
            paint.setColor(Color.argb(155, 225, 239, 255));
            float inset = Math.max(1f, density * 0.5f);
            canvas.drawRoundRect(inset, height * 0.08f,
                    Math.max(inset + 1f, width - inset), height * 0.92f,
                    width / 2f, width / 2f, paint);
            paint.setStyle(Paint.Style.FILL);
            return;
        }

        rounded(canvas, 0, 0, width, height, 28 * density,
                Color.rgb(17, 20, 31));
        rounded(canvas, 14 * density, 20 * density,
                width - 14 * density, 126 * density, 20 * density,
                Color.rgb(17, 31, 60));
        rounded(canvas, 25 * density, 30 * density,
                81 * density, 86 * density, 12 * density,
                Color.rgb(25, 125, 255));
        text(canvas, "A", 53 * density, 70 * density,
                28 * density, Color.WHITE);
        text(canvas, "Screen", width / 2, 100 * density,
                14 * density, Color.WHITE);
        text(canvas, "translation", width / 2, 118 * density,
                13 * density, Color.WHITE);

        for (int i = 0; i < 5; i++) {
            rounded(canvas, 25 * density, (145 + i * 68) * density,
                    81 * density, (195 + i * 68) * density, 12 * density,
                    Color.rgb(24, 43, 78));
        }
        rounded(canvas, 22 * density, height - 112 * density,
                84 * density, height - 58 * density, 27 * density,
                Color.rgb(28, 43, 68));
        text(canvas, "✎", 53 * density, height - 76 * density,
                27 * density, Color.WHITE);
        text(canvas, "Edit", 53 * density, height - 38 * density,
                13 * density, Color.WHITE);
        rounded(canvas, 22 * density, height - 50 * density,
                84 * density, height + 2 * density, 26 * density,
                Color.rgb(25, 50, 90));
        text(canvas, "‹", 53 * density, height - 16 * density,
                30 * density, Color.WHITE);
    }

    private void text(Canvas canvas, String value, float x, float y,
                      float size, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextSize(size);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(value, x, y, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            downX = event.getX();
            downY = event.getY();
            downTime = System.currentTimeMillis();
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float dx = event.getX() - downX;
            float dy = event.getY() - downY;
            if (!rail) {
                // A simple tap or inward edge swipe reveals the rail.
                if (Math.abs(dx) < 24 * density || dx > 0) {
                    rail = true;
                    invalidate();
                    listener.onOpenRail();
                }
            } else {
                // Tapping the rail or swiping inward opens the complete dashboard.
                if (Math.abs(dx) < 24 * density || dx > 0
                        || System.currentTimeMillis() - downTime < 900
                        || Math.abs(dy) > 24 * density) {
                    listener.onExpand();
                }
            }
            performClick();
            return true;
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    void setRailMode(boolean value) {
        rail = value;
        invalidate();
    }
}
