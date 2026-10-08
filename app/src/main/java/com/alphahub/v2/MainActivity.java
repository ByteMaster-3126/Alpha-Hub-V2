package com.alphahub.v2;
import android.app.*;import android.content.*;import android.net.Uri;import android.os.*;import android.provider.Settings;import android.widget.Toast;
public class MainActivity extends Activity{
 @Override public void onCreate(Bundle b){super.onCreate(b);if(!Settings.canDrawOverlays(this)){Toast.makeText(this,"Allow Alpha Hub to display over other apps.",Toast.LENGTH_LONG).show();startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName())));}}
 @Override protected void onResume(){super.onResume();if(Settings.canDrawOverlays(this))startForegroundService(new Intent(this,AlphaTriggerService.class));}
}