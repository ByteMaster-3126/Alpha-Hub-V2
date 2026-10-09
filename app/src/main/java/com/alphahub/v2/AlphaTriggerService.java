package com.alphahub.v2;
import android.app.*;
import android.content.*;
import android.graphics.PixelFormat;
import android.os.*;
import android.view.*;

public class AlphaTriggerService extends Service {
    WindowManager wm;
    TriggerView trigger;
    HomeView home;
    WindowManager.LayoutParams triggerParams;

    int dp(int n) {
        return (int)(n * getResources().getDisplayMetrics().density + .5f);
    }

    int screenHeightDp() {
        return (int)(getResources().getDisplayMetrics().heightPixels /
                getResources().getDisplayMetrics().density);
    }

    @Override public void onCreate() {
        super.onCreate();
        channel();
        startForeground(1001, notification());
        wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        showTrigger();
    }

    void channel() {
        if (Build.VERSION.SDK_INT >= 26) {
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE))
                    .createNotificationChannel(new NotificationChannel(
                            "alpha", "Alpha Hub", NotificationManager.IMPORTANCE_LOW));
        }
    }

    Notification notification() {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, "alpha")
                : new Notification.Builder(this);
        return b.setContentTitle("Alpha Hub")
                .setContentText("Edge trigger is active")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setOngoing(true)
                .build();
    }

    WindowManager.LayoutParams lp(int w, int h) {
        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        return new WindowManager.LayoutParams(w, h, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
    }

    void showTrigger() {
        int height = Math.max(dp(48), Math.round(screenHeightDp() * 0.10f));
        // A larger transparent touch target surrounds the very slim visible handle.
        trigger = new TriggerView(this, new TriggerView.Listener() {
            @Override public void onOpenRail() { openRail(); }
            @Override public void onExpand() { showHome(); }
        });
        trigger.setRailMode(false);
        triggerParams = lp(dp(24), height);
        triggerParams.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        triggerParams.x = 0;
        wm.addView(trigger, triggerParams);
    }

    void openRail() {
        if (trigger == null || triggerParams == null) return;
        trigger.setRailMode(true);
        triggerParams.width = dp(94);
        triggerParams.height = Math.min(dp(560), Math.max(dp(400), screenHeightDp() * dp(1)));
        triggerParams.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        try {
            wm.updateViewLayout(trigger, triggerParams);
        } catch (RuntimeException ignored) {
            showHome();
        }
    }

    void showHome() {
        try {
            if (trigger != null) wm.removeView(trigger);
        } catch (Exception ignored) { }
        trigger = null;
        triggerParams = null;
        home = new HomeView(this, () -> {
            try { if (home != null) wm.removeView(home); } catch (Exception ignored) { }
            home = null;
            showTrigger();
        });
        int screenW = getResources().getDisplayMetrics().widthPixels;
        int screenH = getResources().getDisplayMetrics().heightPixels;
        WindowManager.LayoutParams p = lp(
                Math.min(screenW - dp(12), dp(760)),
                Math.min(screenH - dp(20), dp(1160)));
        p.gravity = Gravity.CENTER;
        wm.addView(home, p);
    }

    @Override public IBinder onBind(Intent i) { return null; }

    @Override public void onDestroy() {
        try { if (trigger != null) wm.removeView(trigger); } catch (Exception ignored) { }
        try { if (home != null) wm.removeView(home); } catch (Exception ignored) { }
        super.onDestroy();
    }
}