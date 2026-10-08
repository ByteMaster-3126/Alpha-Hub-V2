package com.alphahub.v2;
import android.app.*;import android.content.*;import android.graphics.PixelFormat;import android.os.*;import android.view.*;
public class AlphaTriggerService extends Service{
 WindowManager wm; TriggerView trigger; HomeView home; int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
 public void onCreate(){super.onCreate();channel();startForeground(1001,notification());wm=(WindowManager)getSystemService(WINDOW_SERVICE);showTrigger();}
 void channel(){if(Build.VERSION.SDK_INT>=26)((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel("alpha","Alpha Hub",NotificationManager.IMPORTANCE_LOW));}
 Notification notification(){return new Notification.Builder(this,"alpha").setContentTitle("Alpha Hub").setContentText("Edge trigger is active").setSmallIcon(android.R.drawable.ic_menu_view).setOngoing(true).build();}
 WindowManager.LayoutParams lp(int w,int h){int type=Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE;return new WindowManager.LayoutParams(w,h,type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);}
 void showTrigger(){trigger=new TriggerView(this,new TriggerView.Listener(){public void onOpenRail(){trigger.setRailMode(true);}public void onExpand(){showHome();}});WindowManager.LayoutParams p=lp(dp(94),dp(560));p.gravity=Gravity.START|Gravity.CENTER_VERTICAL;wm.addView(trigger,p);}
 void showHome(){try{wm.removeView(trigger);}catch(Exception e){}home=new HomeView(this,()->{try{wm.removeView(home);}catch(Exception e){}showTrigger();});WindowManager.LayoutParams p=lp(Math.min(getResources().getDisplayMetrics().widthPixels-dp(12),dp(760)),Math.min(getResources().getDisplayMetrics().heightPixels-dp(20),dp(1160)));p.gravity=Gravity.CENTER;wm.addView(home,p);}
 public IBinder onBind(Intent i){return null;}public void onDestroy(){try{if(trigger!=null)wm.removeView(trigger);}catch(Exception e){}try{if(home!=null)wm.removeView(home);}catch(Exception e){}super.onDestroy();}
}