package com.alphahub.v2;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeView extends View {
    private static final int BG = Color.rgb(2, 5, 15);
    private static final int SURFACE = Color.rgb(5, 15, 36);
    private static final int TILE = Color.rgb(6, 24, 56);
    private static final int CYAN = Color.rgb(0, 229, 255);
    private static final int BLUE = Color.rgb(21, 145, 255);
    private static final int PURPLE = Color.rgb(139, 92, 255);
    private static final int MAGENTA = Color.rgb(179, 57, 255);
    private static final int MUTED = Color.rgb(158, 176, 211);
    private static final int GOLD = Color.rgb(255, 205, 75);
    private static final int WHITE = Color.WHITE;

    private final Runnable close;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private final int touchSlop;
    private final Map<String, Drawable> iconCache = new HashMap<>();
    private final Map<String, Boolean> missingIcons = new HashMap<>();
    private final List<HitTarget> hitTargets = new ArrayList<>();

    private float unit;
    private float railWidth;
    private float contentLeft;
    private float contentRight;
    private float headerTop;
    private float searchTop;
    private float bodyTop;
    private float navTop;
    private float maxScroll;
    private float scrollY;
    private float downX;
    private float downY;
    private float lastY;
    private boolean dragging;

    private static final class HitTarget {
        final RectF bounds;
        final String action;
        final String label;

        HitTarget(RectF bounds, String action, String label) {
            this.bounds = bounds;
            this.action = action;
            this.label = label;
        }
    }

    public HomeView(Context context, Runnable closeAction) {
        super(context);
        close = closeAction;
        density = getResources().getDisplayMetrics().density;
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setFocusable(true);
        setContentDescription("Alpha Hub home panel");
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final float width = getWidth();
        final float height = getHeight();
        if (width <= 0 || height <= 0) return;

        // Scale the page to the available overlay height while respecting Android density.
        unit = Math.max(density * 0.72f, Math.min(density, height / 760.0f));
        railWidth = Math.max(48 * unit, Math.min(102 * unit, width * 0.145f));
        contentLeft = railWidth + 13 * unit;
        contentRight = width - 15 * unit;
        headerTop = 12 * unit;
        searchTop = headerTop + 70 * unit;
        bodyTop = searchTop + 49 * unit + 9 * unit;
        navTop = height - 62 * unit;

        canvas.drawColor(BG);
        drawMainFrame(canvas, width, height);
        drawSideRail(canvas, width, height);
        drawHeader(canvas, width);
        drawSearch(canvas);

        float favY = bodyTop + 7 * unit;
        float favH = 124 * unit;
        float webY = favY + favH + 9 * unit;
        float webH = 108 * unit;
        float recentY = webY + webH + 9 * unit;
        float recentH = 160 * unit;
        float settingsY = recentY + recentH + 9 * unit;
        float settingsH = 98 * unit;
        float contentEnd = settingsY + settingsH + 6 * unit;
        float bodyBottom = navTop - 7 * unit;

        maxScroll = Math.max(0, contentEnd - bodyBottom);
        scrollY = Math.max(0, Math.min(scrollY, maxScroll));

        hitTargets.clear();
        int save = canvas.save();
        canvas.clipRect(contentLeft, bodyTop, contentRight, bodyBottom);
        canvas.translate(0, -scrollY);

        drawSection(canvas, favY, favH, "FAVORITE APPS", "star",
                new String[]{"Chrome", "YouTube", "Play Store", "Settings"}, "apps");
        drawSection(canvas, webY, webH, "FAVORITE WEBSITES", "web",
                new String[]{"Google", "YouTube", "ChatGPT"}, "websites");
        drawSection(canvas, recentY, recentH, "RECENT", "history",
                new String[]{"Chrome", "YouTube", "Google", "ChatGPT",
                        "Play Store", "Settings", "Gmail", "Facebook"}, "recent");
        drawSection(canvas, settingsY, settingsH, "Phone Settings", "tools",
                new String[]{"Wi-Fi", "Bluetooth", "Mobile Data"}, "phone");
        canvas.restoreToCount(save);

        drawBottomNav(canvas, width, height);
        drawScrollIndicator(canvas, bodyBottom, contentEnd);
    }

    private void drawMainFrame(Canvas c, float w, float h) {
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(railWidth + 4 * unit, 0, w, h,
                Color.rgb(5, 14, 37), Color.rgb(10, 5, 31), Shader.TileMode.CLAMP));
        c.drawRoundRect(railWidth + 4 * unit, 7 * unit, w - 7 * unit,
                h - 7 * unit, 26 * unit, 26 * unit, paint);
        paint.setShader(null);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f * unit);
        paint.setShader(new LinearGradient(railWidth, 0, w, h,
                PURPLE, BLUE, Shader.TileMode.CLAMP));
        c.drawRoundRect(railWidth + 4 * unit, 7 * unit, w - 7 * unit,
                h - 7 * unit, 26 * unit, 26 * unit, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawSideRail(Canvas c, float w, float h) {
        float left = 8 * unit;
        float right = railWidth - 7 * unit;
        drawCard(c, left, 9 * unit, right, h - 9 * unit,
                25 * unit, Color.rgb(3, 9, 28), WHITE, 1.3f * unit);

        float center = (left + right) / 2f;
        float toolSize = 43 * unit;
        drawCard(c, center - toolSize / 2, 17 * unit,
                center + toolSize / 2, 17 * unit + toolSize,
                12 * unit, Color.rgb(5, 20, 51), BLUE, 1.5f * unit);
        drawSmallSquareIcon(c, center, 17 * unit + toolSize / 2, 20 * unit);
        drawText(c, "Tools", center, 17 * unit + toolSize + 19 * unit,
                13.5f * unit, WHITE, true, Paint.Align.CENTER);

        float translateTop = 91 * unit;
        float translateSize = 38 * unit;
        drawCard(c, center - translateSize / 2, translateTop,
                center + translateSize / 2, translateTop + translateSize,
                11 * unit, Color.rgb(5, 22, 49), BLUE, 1.3f * unit);
        drawText(c, "文A", center, translateTop + 26 * unit,
                21 * unit, CYAN, false, Paint.Align.CENTER);
        drawText(c, "Screen", center, translateTop + translateSize + 15 * unit,
                11.5f * unit, WHITE, true, Paint.Align.CENTER);
        drawText(c, "translation", center, translateTop + translateSize + 29 * unit,
                10.5f * unit, WHITE, true, Paint.Align.CENTER);

        float editTop = h - 131 * unit;
        float placeholderTop = translateTop + translateSize + 56 * unit;
        float placeholderH = 40 * unit;
        float availableStep = (editTop - placeholderTop - placeholderH) / 4f;
        float step = Math.max(42 * unit, Math.min(61 * unit, availableStep));
        for (int i = 0; i < 5; i++) {
            float top = placeholderTop + i * step;
            if (top + placeholderH > editTop - 5 * unit) break;
            drawCard(c, left + 16 * unit, top, right - 16 * unit,
                    top + placeholderH, 10 * unit,
                    Color.rgb(7, 24, 55), Color.rgb(35, 77, 135), 1 * unit);
        }

        float editCenterY = h - 108 * unit;
        drawCircleButton(c, center, editCenterY, 24 * unit, Color.rgb(4, 21, 50), BLUE);
        drawText(c, "✎", center, editCenterY + 8 * unit,
                27 * unit, WHITE, false, Paint.Align.CENTER);
        drawText(c, "Edit", center, editCenterY + 39 * unit,
                13 * unit, WHITE, true, Paint.Align.CENTER);

        float backY = h - 42 * unit;
        drawCircleButton(c, center, backY, 22 * unit, Color.rgb(8, 27, 61), BLUE);
        drawText(c, "‹", center, backY + 10 * unit,
                32 * unit, WHITE, false, Paint.Align.CENTER);
    }

    private void drawSmallSquareIcon(Canvas c, float cx, float cy, float size) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2 * unit);
        paint.setColor(CYAN);
        c.drawRoundRect(cx - size * .34f, cy - size * .34f,
                cx + size * .34f, cy + size * .34f,
                4 * unit, 4 * unit, paint);
        c.drawRoundRect(cx - size * .22f, cy - size * .22f,
                cx + size * .22f, cy + size * .22f,
                3 * unit, 3 * unit, paint);
        drawText(c, "A", cx, cy + size * .16f, size * .48f,
                WHITE, true, Paint.Align.CENTER);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawHeader(Canvas c, float width) {
        float logoCx = contentLeft + 22 * unit;
        float logoCy = headerTop + 24 * unit;
        float logoR = 20 * unit;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(13, 12, 26));
        c.drawCircle(logoCx, logoCy, logoR, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.8f * unit);
        paint.setColor(GOLD);
        c.drawCircle(logoCx, logoCy, logoR - unit, paint);
        paint.setStrokeWidth(.8f * unit);
        c.drawCircle(logoCx, logoCy, logoR - 4 * unit, paint);
        paint.setStyle(Paint.Style.FILL);
        drawText(c, "AI", logoCx, logoCy + 5.5f * unit,
                14 * unit, GOLD, true, Paint.Align.CENTER);

        float titleX = contentLeft + 51 * unit;
        drawText(c, "Alpha Hub", titleX, headerTop + 25 * unit,
                25 * unit, CYAN, true, Paint.Align.LEFT);
        drawText(c, "Your All-in-One", titleX, headerTop + 45 * unit,
                15.5f * unit, Color.rgb(60, 190, 255), true, Paint.Align.LEFT);
        drawText(c, "Companion", titleX, headerTop + 63 * unit,
                15.5f * unit, Color.rgb(60, 190, 255), true, Paint.Align.LEFT);

        float settingsCx = contentRight - 20 * unit;
        float starCx = contentRight - 64 * unit;
        float buttonCy = headerTop + 23 * unit;
        drawCircleButton(c, starCx, buttonCy, 18 * unit,
                Color.rgb(5, 13, 38), PURPLE);
        drawText(c, "★", starCx, buttonCy + 6 * unit,
                20 * unit, Color.rgb(220, 232, 255), true, Paint.Align.CENTER);
        drawCircleButton(c, settingsCx, buttonCy, 18 * unit,
                Color.rgb(5, 13, 38), PURPLE);
        drawText(c, "⚙", settingsCx, buttonCy + 6 * unit,
                19 * unit, Color.rgb(220, 232, 255), false, Paint.Align.CENTER);
    }

    private void drawSearch(Canvas c) {
        float left = contentLeft;
        float right = contentRight;
        float top = searchTop;
        float bottom = top + 49 * unit;
        drawCard(c, left, top, right, bottom, 20 * unit,
                Color.rgb(5, 25, 61), BLUE, 1.5f * unit);

        float cx = left + 22 * unit;
        float cy = top + 24.5f * unit;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.6f * unit);
        paint.setColor(CYAN);
        c.drawCircle(cx - 2 * unit, cy - 2 * unit, 7 * unit, paint);
        c.drawLine(cx + 3 * unit, cy + 3 * unit,
                cx + 10 * unit, cy + 10 * unit, paint);
        paint.setStyle(Paint.Style.FILL);

        drawText(c, "Universal Search", left + 45 * unit,
                top + 30 * unit, 16.5f * unit,
                MUTED, false, Paint.Align.LEFT);
        drawText(c, "●", right - 20 * unit, top + 29 * unit,
                11 * unit, MAGENTA, true, Paint.Align.CENTER);
        drawText(c, "⌕", right - 20 * unit, top + 30 * unit,
                18 * unit, MAGENTA, true, Paint.Align.CENTER);
    }

    private void drawSection(Canvas c, float top, float height, String title,
                             String icon, String[] labels, String kind) {
        float left = contentLeft;
        float right = contentRight;
        drawCard(c, left, top, right, top + height, 19 * unit,
                SURFACE, BLUE, 1.15f * unit);

        drawSectionGlyph(c, left + 20 * unit, top + 22 * unit, icon);
        drawText(c, title, left + 39 * unit, top + 26 * unit,
                13.5f * unit, CYAN, true, Paint.Align.LEFT);
        drawText(c, "+ Add", right - 12 * unit, top + 26 * unit,
                13.5f * unit, MAGENTA, true, Paint.Align.RIGHT);

        if ("apps".equals(kind)) {
            drawTileRow(c, labels, 4, left, right, top + 34 * unit,
                    50 * unit, "app", false);
            float cx = (left + right) / 2f;
            drawCard(c, cx - 31 * unit, top + 91 * unit,
                    cx + 31 * unit, top + 114 * unit, 13 * unit,
                    Color.rgb(5, 21, 50), BLUE, 1 * unit);
            drawText(c, "View all", cx, top + 106.5f * unit,
                    11.5f * unit, CYAN, true, Paint.Align.CENTER);
        } else if ("websites".equals(kind)) {
            drawTileRow(c, labels, 3, left, right, top + 36 * unit,
                    56 * unit, "web", false);
        } else if ("recent".equals(kind)) {
            String[] first = new String[]{labels[0], labels[1], labels[2], labels[3]};
            String[] second = new String[]{labels[4], labels[5], labels[6], labels[7]};
            drawTileRow(c, first, 4, left, right, top + 34 * unit,
                    51 * unit, "recent", false);
            drawTileRow(c, second, 4, left, right, top + 91 * unit,
                    51 * unit, "recent", false);
        } else if ("phone".equals(kind)) {
            drawTileRow(c, labels, 3, left, right, top + 34 * unit,
                    51 * unit, "setting", false);
        }
    }

    private void drawSectionGlyph(Canvas c, float cx, float cy, String icon) {
        if ("star".equals(icon)) {
            drawText(c, "★", cx, cy + 8 * unit, 25 * unit, CYAN, true, Paint.Align.CENTER);
        } else if ("web".equals(icon)) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.1f * unit);
            paint.setColor(CYAN);
            c.drawCircle(cx, cy, 8 * unit, paint);
            c.drawOval(cx - 4 * unit, cy - 8 * unit,
                    cx + 4 * unit, cy + 8 * unit, paint);
            c.drawLine(cx - 7 * unit, cy, cx + 7 * unit, cy, paint);
            paint.setStyle(Paint.Style.FILL);
        } else if ("history".equals(icon)) {
            drawText(c, "◷", cx, cy + 8 * unit, 25 * unit, CYAN, true, Paint.Align.CENTER);
        } else {
            drawText(c, "⚒", cx, cy + 7 * unit, 23 * unit, CYAN, true, Paint.Align.CENTER);
        }
    }

    private void drawTileRow(Canvas c, String[] labels, int columns,
                             float left, float right, float top, float height,
                             String type, boolean unused) {
        float sidePadding = 11 * unit;
        float gap = 7 * unit;
        float totalWidth = right - left - sidePadding * 2 - gap * (columns - 1);
        float tileWidth = totalWidth / columns;

        for (int i = 0; i < labels.length; i++) {
            int col = i % columns;
            int row = i / columns;
            float x = left + sidePadding + col * (tileWidth + gap);
            float y = top + row * (height + 5 * unit);
            drawTile(c, x, y, x + tileWidth, y + height, labels[i], type);
        }
    }

    private void drawTile(Canvas c, float left, float top, float right,
                          float bottom, String label, String type) {
        drawCard(c, left, top, right, bottom, 12 * unit,
                TILE, Color.rgb(16, 117, 255), 1.1f * unit);

        float cx = (left + right) / 2f;
        float iconSize = "web".equals(type) ? 31 * unit : 27 * unit;
        if ("setting".equals(type)) iconSize = 25 * unit;

        if ("setting".equals(type)) {
            drawSettingsIcon(c, label, cx, top + 7 * unit, iconSize);
        } else {
            drawAppIcon(c, label, cx, top + 5 * unit, iconSize);
        }

        float baseline = bottom - 6 * unit;
        drawText(c, label, cx, baseline, 10.5f * unit,
                WHITE, true, Paint.Align.CENTER);

        String action;
        if ("web".equals(type)) {
            action = "WEB";
        } else if ("setting".equals(type)) {
            action = "SETTING";
        } else {
            action = "APP";
        }
        hitTargets.add(new HitTarget(new RectF(left, top, right, bottom), action, label));
    }

    private void drawAppIcon(Canvas c, String label, float cx, float top, float size) {
        Drawable drawable = getAppIcon(label);
        if (drawable != null) {
            int save = c.save();
            RectF bounds = new RectF(cx - size / 2f, top, cx + size / 2f, top + size);
            drawable.setBounds(Math.round(bounds.left), Math.round(bounds.top),
                    Math.round(bounds.right), Math.round(bounds.bottom));
            drawable.draw(c);
            c.restoreToCount(save);
            return;
        }
        drawFallbackIcon(c, label, cx, top, size);
    }

    private Drawable getAppIcon(String label) {
        if (Boolean.TRUE.equals(missingIcons.get(label))) return null;
        if (iconCache.containsKey(label)) return iconCache.get(label);

        String packageName = packageFor(label);
        if (packageName == null) return null;
        try {
            Drawable d = getContext().getPackageManager().getApplicationIcon(packageName);
            iconCache.put(label, d);
            return d;
        } catch (PackageManager.NameNotFoundException | RuntimeException ignored) {
            missingIcons.put(label, true);
            return null;
        }
    }

    private String packageFor(String label) {
        switch (label) {
            case "Chrome": return "com.android.chrome";
            case "YouTube": return "com.google.android.youtube";
            case "Play Store": return "com.android.vending";
            case "Settings": return "com.android.settings";
            case "Google": return "com.google.android.googlequicksearchbox";
            case "ChatGPT": return "com.openai.chatgpt";
            case "Gmail": return "com.google.android.gm";
            case "Facebook": return "com.facebook.katana";
            default: return null;
        }
    }

    private void drawFallbackIcon(Canvas c, String label, float cx, float top, float size) {
        String key = label.toLowerCase(Locale.US);
        float cy = top + size / 2f;
        float radius = size * .48f;

        if ("chrome".equals(key)) {
            RectF ring = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(size * .28f);
            paint.setColor(Color.rgb(234, 67, 53));
            c.drawArc(ring, -90, 118, false, paint);
            paint.setColor(Color.rgb(251, 188, 5));
            c.drawArc(ring, 28, 118, false, paint);
            paint.setColor(Color.rgb(52, 168, 83));
            c.drawArc(ring, 146, 124, false, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(66, 133, 244));
            c.drawCircle(cx, cy, size * .22f, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(size * .03f);
            paint.setColor(WHITE);
            c.drawCircle(cx, cy, size * .22f, paint);
            paint.setStyle(Paint.Style.FILL);
            return;
        }

        if ("youtube".equals(key)) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(255, 0, 0));
            c.drawRoundRect(cx - size * .5f, top + size * .12f,
                    cx + size * .5f, top + size * .88f,
                    size * .2f, size * .2f, paint);
            Path play = new Path();
            play.moveTo(cx - size * .12f, cy - size * .20f);
            play.lineTo(cx + size * .24f, cy);
            play.lineTo(cx - size * .12f, cy + size * .20f);
            play.close();
            paint.setColor(WHITE);
            c.drawPath(play, paint);
            return;
        }

        if ("play store".equals(key)) {
            paint.setColor(WHITE);
            c.drawCircle(cx, cy, size * .5f, paint);
            Path triangle = new Path();
            triangle.moveTo(cx - size * .22f, cy - size * .32f);
            triangle.lineTo(cx + size * .30f, cy);
            triangle.lineTo(cx - size * .22f, cy + size * .32f);
            triangle.close();
            paint.setColor(Color.rgb(52, 168, 83));
            c.drawPath(triangle, paint);
            paint.setColor(Color.rgb(66, 133, 244));
            Path blue = new Path();
            blue.moveTo(cx - size * .22f, cy - size * .32f);
            blue.lineTo(cx + size * .06f, cy - size * .07f);
            blue.lineTo(cx + size * .30f, cy);
            blue.close();
            c.drawPath(blue, paint);
            paint.setColor(Color.rgb(251, 188, 5));
            Path yellow = new Path();
            yellow.moveTo(cx + size * .06f, cy + size * .07f);
            yellow.lineTo(cx + size * .30f, cy);
            yellow.lineTo(cx - size * .22f, cy + size * .32f);
            yellow.close();
            c.drawPath(yellow, paint);
            paint.setColor(Color.rgb(234, 67, 53));
            Path red = new Path();
            red.moveTo(cx + size * .06f, cy - size * .07f);
            red.lineTo(cx + size * .30f, cy);
            red.lineTo(cx + size * .06f, cy + size * .07f);
            red.close();
            c.drawPath(red, paint);
            return;
        }

        int back;
        if ("google".equals(key)) back = WHITE;
        else if ("chatgpt".equals(key)) back = Color.rgb(18, 23, 32);
        else if ("settings".equals(key)) back = Color.rgb(126, 139, 157);
        else if ("gmail".equals(key)) back = WHITE;
        else if ("facebook".equals(key)) back = Color.rgb(24, 119, 242);
        else back = Color.rgb(34, 104, 208);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(back);
        c.drawRoundRect(cx - size * .48f, top, cx + size * .48f,
                top + size, size * .22f, size * .22f, paint);

        String mark;
        int markColor;
        if ("google".equals(key)) { mark = "G"; markColor = Color.rgb(66, 133, 244); }
        else if ("chatgpt".equals(key)) { mark = "✳"; markColor = WHITE; }
        else if ("settings".equals(key)) { mark = "⚙"; markColor = WHITE; }
        else if ("gmail".equals(key)) { mark = "M"; markColor = Color.rgb(234, 67, 53); }
        else if ("facebook".equals(key)) { mark = "f"; markColor = WHITE; }
        else { mark = label.length() > 1 ? label.substring(0, 1) : label; markColor = WHITE; }

        drawText(c, mark, cx, cy + size * .18f, size * .62f,
                markColor, true, Paint.Align.CENTER);
    }

    private void drawSettingsIcon(Canvas c, String label, float cx, float top, float size) {
        float cy = top + size / 2f;
        paint.setStyle(Paint.Style.FILL);
        if ("Wi-Fi".equals(label)) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.5f * unit);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(CYAN);
            RectF arc = new RectF(cx - size * .46f, cy - size * .3f,
                    cx + size * .46f, cy + size * .48f);
            c.drawArc(arc, 215, 110, false, paint);
            RectF arc2 = new RectF(cx - size * .28f, cy - size * .12f,
                    cx + size * .28f, cy + size * .4f);
            c.drawArc(arc2, 220, 100, false, paint);
            paint.setStyle(Paint.Style.FILL);
            c.drawCircle(cx, cy + size * .26f, 2.7f * unit, paint);
        } else if ("Bluetooth".equals(label)) {
            drawText(c, "ᛒ", cx, cy + size * .30f, size * .95f,
                    Color.rgb(92, 168, 255), true, Paint.Align.CENTER);
        } else {
            paint.setColor(Color.rgb(39, 181, 143));
            c.drawRoundRect(cx - size * .15f, cy - size * .46f,
                    cx + size * .15f, cy + size * .46f,
                    3 * unit, 3 * unit, paint);
            paint.setColor(Color.rgb(132, 240, 206));
            c.drawRect(cx - size * .34f, cy - size * .20f,
                    cx + size * .34f, cy + size * .18f, paint);
            paint.setColor(Color.rgb(39, 181, 143));
            c.drawCircle(cx, cy + size * .30f, 2.5f * unit, paint);
        }
    }

    private void drawBottomNav(Canvas c, float width, float height) {
        float left = contentLeft;
        float right = contentRight;
        float top = navTop + 4 * unit;
        float bottom = height - 8 * unit;
        drawCard(c, left, top, right, bottom, 26 * unit,
                Color.rgb(3, 12, 34), BLUE, 1.3f * unit);

        String[] labels = {"Home", "Tools", "Apps", "Shortcuts"};
        String[] icons = {"⌂", "⚒", "▦", "◎"};
        float itemWidth = (right - left) / 4f;
        for (int i = 0; i < labels.length; i++) {
            float cx = left + itemWidth * (i + .5f);
            int color = i == 0 ? CYAN : MUTED;
            drawText(c, icons[i], cx, top + 25 * unit, 24 * unit,
                    color, true, Paint.Align.CENTER);
            drawText(c, labels[i], cx, top + 43 * unit,
                    11.5f * unit, color, true, Paint.Align.CENTER);
            if (i == 0) {
                paint.setColor(CYAN);
                c.drawRoundRect(cx - 20 * unit, bottom - 4 * unit,
                        cx + 20 * unit, bottom - 1.5f * unit,
                        2 * unit, 2 * unit, paint);
            }
        }
    }

    private void drawScrollIndicator(Canvas c, float bodyBottom, float contentEnd) {
        if (maxScroll <= 0) return;
        float x = contentRight - 2 * unit;
        float trackTop = bodyTop + 4 * unit;
        float trackBottom = bodyBottom - 4 * unit;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2 * unit);
        paint.setColor(Color.argb(75, 150, 175, 220));
        c.drawLine(x, trackTop, x, trackBottom, paint);
        float fraction = (bodyBottom - bodyTop) / (contentEnd - bodyTop);
        float thumbH = Math.max(24 * unit, (trackBottom - trackTop) * fraction);
        float movable = (trackBottom - trackTop) - thumbH;
        float thumbTop = trackTop + (maxScroll == 0 ? 0 : movable * scrollY / maxScroll);
        paint.setColor(CYAN);
        paint.setStrokeWidth(2.4f * unit);
        c.drawLine(x, thumbTop, x, thumbTop + thumbH, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawCard(Canvas c, float left, float top, float right, float bottom,
                          float radius, int fill, int stroke, float strokeWidth) {
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(fill);
        paint.setShadowLayer(4 * unit, 0, 0, Color.argb(45, 23, 120, 255));
        c.drawRoundRect(left, top, right, bottom, radius, radius, paint);
        paint.clearShadowLayer();

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        paint.setShader(new LinearGradient(left, top, right, bottom,
                stroke, stroke == WHITE ? Color.rgb(36, 98, 166) : PURPLE,
                Shader.TileMode.CLAMP));
        c.drawRoundRect(left, top, right, bottom, radius, radius, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawCircleButton(Canvas c, float cx, float cy, float radius,
                                  int fill, int stroke) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(fill);
        paint.setShadowLayer(5 * unit, 0, 0, Color.argb(75, 18, 132, 255));
        c.drawCircle(cx, cy, radius, paint);
        paint.clearShadowLayer();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.3f * unit);
        paint.setShader(new LinearGradient(cx - radius, cy - radius,
                cx + radius, cy + radius, CYAN, stroke, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, radius, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawText(Canvas c, String text, float x, float baseline,
                          float size, int color, boolean bold, Paint.Align align) {
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextSize(size);
        paint.setTextAlign(align);
        paint.setTypeface(Typeface.create("sans-serif",
                bold ? Typeface.BOLD : Typeface.NORMAL));
        c.drawText(text, x, baseline, paint);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                lastY = downY;
                dragging = false;
                return true;

            case MotionEvent.ACTION_MOVE:
                float deltaY = event.getY() - lastY;
                if (!dragging
                        && Math.abs(event.getY() - downY) > touchSlop
                        && downX >= railWidth
                        && downY >= bodyTop
                        && downY < navTop - 5 * unit) {
                    dragging = true;
                }
                if (dragging) {
                    scrollY = Math.max(0, Math.min(maxScroll, scrollY - deltaY));
                    invalidate();
                }
                lastY = event.getY();
                return true;

            case MotionEvent.ACTION_UP:
                if (dragging) {
                    dragging = false;
                    performClick();
                    return true;
                }

                float x = event.getX();
                float y = event.getY();

                // The back control and settings control both provide a reliable way to close the panel.
                if ((x < railWidth && y > getHeight() - 72 * unit)
                        || (x > getWidth() - 47 * unit && y < headerTop + 50 * unit)) {
                    if (close != null) close.run();
                    performClick();
                    return true;
                }

                if (x >= railWidth && y >= bodyTop && y < navTop - 5 * unit) {
                    float contentY = y + scrollY;
                    for (int i = hitTargets.size() - 1; i >= 0; i--) {
                        HitTarget target = hitTargets.get(i);
                        if (target.bounds.contains(x, contentY)) {
                            launchTarget(target);
                            performClick();
                            return true;
                        }
                    }
                }

                performClick();
                return true;
        }
        return true;
    }

    private void launchTarget(HitTarget target) {
        try {
            if ("WEB".equals(target.action)) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl(target.label)));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(intent);
                return;
            }

            if ("SETTING".equals(target.action)) {
                String action;
                if ("Wi-Fi".equals(target.label)) action = Settings.ACTION_WIFI_SETTINGS;
                else if ("Bluetooth".equals(target.label)) action = Settings.ACTION_BLUETOOTH_SETTINGS;
                else action = Settings.ACTION_DATA_ROAMING_SETTINGS;
                Intent intent = new Intent(action);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(intent);
                return;
            }

            String packageName = packageFor(target.label);
            Intent appIntent = packageName == null ? null
                    : getContext().getPackageManager().getLaunchIntentForPackage(packageName);
            if (appIntent != null) {
                appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(appIntent);
            } else {
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl(target.label)));
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(webIntent);
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Unable to open " + target.label,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private String webUrl(String label) {
        switch (label) {
            case "Google": return "https://www.google.com";
            case "YouTube": return "https://www.youtube.com";
            case "ChatGPT": return "https://chatgpt.com";
            case "Facebook": return "https://www.facebook.com";
            case "Gmail": return "https://mail.google.com";
            case "Chrome": return "https://www.google.com/chrome/";
            case "Play Store": return "https://play.google.com/store";
            case "Settings": return "https://support.google.com/android/";
            default: return "https://www.google.com";
        }
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }
}
