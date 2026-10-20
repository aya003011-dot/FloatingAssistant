package com.example.floatingassistant;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
 @Override public void onCreate(Bundle b) {
  super.onCreate(b);
  LinearLayout p=new LinearLayout(this); p.setOrientation(1); p.setPadding(28,40,28,28); p.setGravity(Gravity.CENTER_HORIZONTAL); p.setBackgroundColor(0xFFF4F5F7);
  TextView t=new TextView(this); t.setText("دستیار شناور"); t.setTextSize(27); t.setTextColor(0xFF20232B); t.setGravity(Gravity.CENTER); p.addView(t,new LinearLayout.LayoutParams(-1,-2));
  TextView h=new TextView(this); h.setText("چت شناور روی برنامه‌های دیگر. ابتدا اجازهٔ نمایش روی برنامه‌ها را فعال کن، بعد پنجره را باز کن."); h.setTextSize(16); h.setTextColor(0xFF333333); h.setPadding(0,24,0,20); p.addView(h);
  Button g=new Button(this); g.setText("۱) اجازهٔ پنجرهٔ شناور"); g.setOnClickListener(v->{ if(Build.VERSION.SDK_INT>=23) startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName()))); }); p.addView(g,new LinearLayout.LayoutParams(-1,-2));
  Button o=new Button(this); o.setText("۲) باز کردن دستیار"); o.setOnClickListener(v->{ if(Build.VERSION.SDK_INT<23||Settings.canDrawOverlays(this)){startService(new Intent(this,FloatingService.class));finish();}else h.setText("اول اجازهٔ شناور را فعال کن و دوباره بزن."); }); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.topMargin=12; p.addView(o,lp);
  setContentView(p);
 }
}
