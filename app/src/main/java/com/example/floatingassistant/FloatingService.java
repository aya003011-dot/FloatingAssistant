package com.example.floatingassistant;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class FloatingService extends Service {
 private WindowManager wm; private View panel; private WindowManager.LayoutParams params; private LinearLayout messages;
 private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
 @Override public void onCreate(){super.onCreate();wm=(WindowManager)getSystemService(WINDOW_SERVICE);if(Build.VERSION.SDK_INT>=23&&!Settings.canDrawOverlays(this)){stopSelf();return;}createWindow();}
 private void createWindow(){
  LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(10),dp(8),dp(10),dp(10));root.setBackgroundColor(0xFF20232B);
  LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);
  TextView title=new TextView(this);title.setText("✦ دستیار شناور");title.setTextColor(0xFFFFFFFF);title.setTextSize(16);head.addView(title,new LinearLayout.LayoutParams(0,dp(42),1));
  Button close=new Button(this);close.setText("×");close.setOnClickListener(v->stopSelf());head.addView(close,new LinearLayout.LayoutParams(dp(48),dp(42)));
  head.setOnTouchListener(new View.OnTouchListener(){float x,y;int ox,oy;public boolean onTouch(View v,MotionEvent e){if(e.getAction()==0){x=e.getRawX();y=e.getRawY();ox=params.x;oy=params.y;return true;}if(e.getAction()==2){params.x=ox+(int)(e.getRawX()-x);params.y=oy+(int)(e.getRawY()-y);try{wm.updateViewLayout(panel,params);}catch(Exception ignored){}return true;}return true;}});
  root.addView(head);
  ScrollView sc=new ScrollView(this);messages=new LinearLayout(this);messages.setOrientation(1);sc.addView(messages);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
  addMessage("دستیار","سلام! پنجره شناوره. می‌تونی این پنجره رو از نوار بالا جابه‌جا کنی.");
  LinearLayout bar=new LinearLayout(this);EditText input=new EditText(this);input.setSingleLine(true);input.setTextSize(14);input.setHint("پیامت رو بنویس...");input.setTextColor(0xFF111111);input.setBackgroundColor(0xFFFFFFFF);bar.addView(input,new LinearLayout.LayoutParams(0,dp(48),1));
  Button send=new Button(this);send.setText("ارسال");send.setOnClickListener(v->{String s=input.getText().toString().trim();if(s.isEmpty())return;addMessage("تو",s);input.setText("");handle(s);});bar.addView(send,new LinearLayout.LayoutParams(dp(72),dp(48)));root.addView(bar);
  panel=root;int type=Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE;
  params=new WindowManager.LayoutParams(dp(320),dp(420),type,WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);params.gravity=Gravity.TOP|Gravity.START;params.x=dp(16);params.y=dp(100);
  try{wm.addView(panel,params);}catch(Exception e){Toast.makeText(this,"اجازهٔ شناور را فعال کن.",Toast.LENGTH_LONG).show();stopSelf();}
 }
 private void addMessage(String who,String msg){if(messages==null)return;TextView t=new TextView(this);t.setText(who+": "+msg);t.setTextColor(0xFFFFFFFF);t.setTextSize(14);t.setPadding(dp(8),dp(8),dp(8),dp(8));messages.addView(t);}
 private void handle(String s){String q=s.toLowerCase();
  if(q.contains("گوگل")||q.contains("google")||q.contains("جستجو"))open(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com")),"گوگل را باز کردم.");
  else if(q.contains("یوتیوب")||q.contains("youtube"))open(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.youtube.com")),"یوتیوب را باز کردم.");
  else if(q.contains("وای‌فای")||q.contains("وای فای")||q.contains("wifi")||q.contains("wi-fi"))open(new Intent(Settings.ACTION_WIFI_SETTINGS),"تنظیمات وای‌فای را باز کردم.");
  else if(q.contains("بلوتوث"))open(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS),"تنظیمات بلوتوث را باز کردم.");
  else if(q.contains("تنظیمات"))open(new Intent(Settings.ACTION_SETTINGS),"تنظیمات گوشی را باز کردم.");
  else if(q.contains("vpn")||q.contains("وی پی ان")||q.contains("وی‌پی‌ان"))open(new Intent(Settings.ACTION_SETTINGS),"تنظیمات گوشی را باز کردم؛ خاموش‌کردن مستقیم VPN در این نسخه پشتیبانی نمی‌شود.");
  else if(q.startsWith("جستجو "))open(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/search?q="+Uri.encode(s.substring(6)))),"جستجو را باز کردم.");
  else addMessage("دستیار","پیامت ثبت شد، ولی این نسخه هنوز به مدل هوش مصنوعی متصل نیست. فعلاً دستورهای گوگل، یوتیوب، تنظیمات، وای‌فای و بلوتوث را امتحان کن.");
 }
 private void open(Intent i,String msg){try{i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);addMessage("دستیار",msg);}catch(Exception e){addMessage("دستیار","نتوانستم باز کنم.");}}
 @Override public int onStartCommand(Intent i,int f,int id){return START_NOT_STICKY;}
 @Override public void onDestroy(){if(wm!=null&&panel!=null)try{wm.removeView(panel);}catch(Exception ignored){}super.onDestroy();}
 @Override public IBinder onBind(Intent i){return null;}
}
