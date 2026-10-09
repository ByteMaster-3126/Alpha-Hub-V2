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

    @Override
    public void onCreate() {
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

    private void showTrigger() {
        trigger = new TriggerView(this, new TriggerView.Listener() {
            @Override public void onOpenRail() {
                showRail();
            }
            @Override public void onExpand() {
                showHome();
            }
            @Override public void onCollapse() {
                showTrigger();
            }
        });

        // Collapsed trigger: 2% of screen width, 10% of height, left edge,
        // vertically positioned around 48% of screen height.
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

    private void showRail() {
        if (trigger == null) return;
        trigger.setRailMode(true);
        // Compact floating tool card matching the supplied screen recording.
        WindowManager.LayoutParams p = params(dp(112), dp(236));
        p.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        p.x = 0;
        p.y = 0;
        try {
            wm.updateViewLayout(trigger, p);
        } catch (RuntimeException ignored) {
            // If Android has detached the overlay, restore the compact trigger safely.
            try { wm.removeView(trigger); } catch (RuntimeException ignoredAgain) { }
            showTrigger();
        }
    }

    private void showHome() {
        try {
            if (trigger != null) wm.removeView(trigger);
        } catch (RuntimeException ignored) { }

        home = new HomeView(this, () -> {
            try {
                if (home != null) wm.removeView(home);
            } catch (RuntimeException ignored) { }
            home = null;
            showRail();
        });

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        WindowManager.LayoutParams p = params(
                Math.min(screenWidth - dp(12), dp(760)),
                Math.min(screenHeight - dp(20), dp(1160)));
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
