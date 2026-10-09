package com.alphahub.v2;

import android.app.*;
import android.content.*;
import android.graphics.PixelFormat;
import android.os.*;
import android.view.*;

public class AlphaTriggerService extends Service {
    private WindowManager wm;
    private TriggerView trigger;
    private HomeView home;

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override public void onCreate() {
        super.onCreate();
        channel();
        startForeground(1001, notification());
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        showTrigger();
    }

    private void channel() {
        if (Build.VERSION.SDK_INT >= 26) {
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                    .createNotificationChannel(new NotificationChannel(
                            "alpha", "Alpha Hub", NotificationManager.IMPORTANCE_LOW));
        }
    }

    private Notification notification() {
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, "alpha")
                : new Notification.Builder(this);
        return builder.setContentTitle("Alpha Hub")
                .setContentText("Edge trigger is active")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setOngoing(true)
                .build();
    }

    private WindowManager.LayoutParams params(int width, int height) {
        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        return new WindowManager.LayoutParams(width, height, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
    }

    /** Stage 1: the transparent slim handle at the left edge, vertically centered at about 48%. */
    private void showTrigger() {
        if (trigger != null) {
            try { wm.removeView(trigger); } catch (RuntimeException ignored) { }
        }

        trigger = new TriggerView(this, new TriggerView.Listener() {
            @Override public void onOpenRail() { showRail(); }
            @Override public void onExpand() { showHome(); }
            @Override public void onCollapse() { collapseToEdge(); }
        });

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int width = Math.max(dp(8), Math.round(screenWidth * 0.02f));
        int height = Math.max(dp(56), Math.round(screenHeight * 0.10f));
        WindowManager.LayoutParams p = params(width, height);
        p.gravity = Gravity.START | Gravity.TOP;
        p.x = 0;
        p.y = Math.round(screenHeight * 0.43f);
        wm.addView(trigger, p);
    }

    /** Stage 2: compact floating panel, sized to match the supplied recording. */
    private void showRail() {
        if (trigger == null) return;
        trigger.setRailMode(true);
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        WindowManager.LayoutParams p = params(dp(84), dp(215));
        p.gravity = Gravity.START | Gravity.TOP;
        p.x = 0;
        // The recording shows this compact preview appearing in the lower-left area.
        p.y = Math.round(screenHeight * 0.59f);
        try {
            wm.updateViewLayout(trigger, p);
        } catch (RuntimeException ignored) {
            try { wm.removeView(trigger); } catch (RuntimeException ignoredAgain) { }
            trigger = null;
            showTrigger();
        }
    }

    /** The bottom chevron collapses the compact panel back to the edge handle. */
    private void collapseToEdge() {
        if (trigger == null) return;
        trigger.setRailMode(false);
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int width = Math.max(dp(8), Math.round(screenWidth * 0.02f));
        int height = Math.max(dp(56), Math.round(screenHeight * 0.10f));
        WindowManager.LayoutParams p = params(width, height);
        p.gravity = Gravity.START | Gravity.TOP;
        p.x = 0;
        p.y = Math.round(screenHeight * 0.43f);
        try {
            wm.updateViewLayout(trigger, p);
        } catch (RuntimeException ignored) {
            try { wm.removeView(trigger); } catch (RuntimeException ignoredAgain) { }
            trigger = null;
            showTrigger();
        }
    }

    /** Stage 3: the full reference dashboard with its own persistent left rail. */
    private void showHome() {
        try {
            if (trigger != null) wm.removeView(trigger);
        } catch (RuntimeException ignored) { }
        trigger = null;

        home = new HomeView(this, () -> {
            try {
                if (home != null) wm.removeView(home);
            } catch (RuntimeException ignored) { }
            home = null;
            // Return to the slim edge trigger only. Do not reopen the compact trigger rail.
            showTrigger();
        });

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        // Match the reference panel's breathing room above/below the system bars.
        WindowManager.LayoutParams p = params(
                Math.min(screenWidth - dp(4), dp(760)),
                Math.min(screenHeight - dp(70), dp(1160)));
        p.gravity = Gravity.CENTER;
        wm.addView(home, p);
    }

    @Override public IBinder onBind(Intent intent) {
        return null;
    }

    @Override public void onDestroy() {
        try { if (trigger != null) wm.removeView(trigger); }
        catch (RuntimeException ignored) { }
        try { if (home != null) wm.removeView(home); }
        catch (RuntimeException ignored) { }
        super.onDestroy();
    }
}
