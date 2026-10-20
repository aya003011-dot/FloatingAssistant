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
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

public class FloatingService extends Service {
    private WindowManager wm; private View panel; private WindowManager.LayoutParams params; private LinearLayout messages; private int panelWidth=320, panelHeight=430;
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    @Override public void onCreate(){super.onCreate();wm=(WindowManager)getSystemService(WINDOW_SERVICE);if(Build.VERSION.SDK_INT>=23&&!Settings.canDrawOverlays(this)){stopSelf();return;}createWindow();}
    private void createWindow(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(8),dp(7),dp(8),dp(8));root.setBackgroundColor(0xFF20232B);
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=new TextView(this);title.setText("✦ دستیار شناور");title.setTextColor(0xFFFFFFFF);title.setTextSize(15);header.addView(title,new LinearLayout.LayoutParams(0,dp(40),1));
        Button shrink=new Button(this);shrink.setText("−");shrink.setMinWidth(0);shrink.setPadding(0,0,0,0);shrink.setOnClickListener(v->resize(-1));header.addView(shrink,new LinearLayout.LayoutParams(dp(42),dp(40)));
        Button grow=new Button(this);grow.setText("+");grow.setMinWidth(0);grow.setPadding(0,0,0,0);grow.setOnClickListener(v->resize(1));header.addView(grow,new LinearLayout.LayoutParams(dp(42),dp(40)));
        Button close=new Button(this);close.setText("×");close.setMinWidth(0);close.setPadding(0,0,0,0);close.setOnClickListener(v->stopSelf());header.addView(close,new LinearLayout.LayoutParams(dp(42),dp(40)));
        title.setOnTouchListener(new View.OnTouchListener(){float x,y;int ox,oy;public boolean onTouch(View v,MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN){x=e.getRawX();y=e.getRawY();ox=params.x;oy=params.y;return true;}if(e.getAction()==MotionEvent.ACTION_MOVE){params.x=ox+(int)(e.getRawX()-x);params.y=oy+(int)(e.getRawY()-y);try{wm.updateViewLayout(panel,params);}catch(Exception ignored){}return true;}return e.getAction()==MotionEvent.ACTION_UP;}});
        root.addView(header);
        ScrollView scroll=new ScrollView(this);messages=new LinearLayout(this);messages.setOrientation(1);scroll.addView(messages);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        addMessage("دستیار","سلام! می‌تونی پنجره رو از عنوان جابه‌جا کنی، با + بزرگ و با − کوچک کنی. فرمان نمونه: «برو تو یوتیوب سرچ کن aneim».");
        LinearLayout entry=new LinearLayout(this);EditText input=new EditText(this);input.setSingleLine(true);input.setTextSize(14);input.setHint("دستورت رو بنویس...");input.setTextColor(0xFF111111);input.setBackgroundColor(0xFFFFFFFF);entry.addView(input,new LinearLayout.LayoutParams(0,dp(48),1));
        Button send=new Button(this);send.setText("ارسال");send.setOnClickListener(v->{String s=input.getText().toString().trim();if(s.isEmpty())return;addMessage("تو",s);input.setText("");handle(s);});entry.addView(send,new LinearLayout.LayoutParams(dp(70),dp(48)));root.addView(entry);
        LinearLayout quick=new LinearLayout(this);quick.setOrientation(LinearLayout.HORIZONTAL);
        addQuick(quick,"تیک‌تاک","برو تیک‌تاک");addQuick(quick,"یوتیوب","برو یوتیوب");addQuick(quick,"VPN","تنظیمات VPN");root.addView(quick);
        panel=root;int type=Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE;params=new WindowManager.LayoutParams(dp(panelWidth),dp(panelHeight),type,WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);params.gravity=Gravity.TOP|Gravity.START;params.x=dp(14);params.y=dp(90);
        try{wm.addView(panel,params);}catch(Exception e){Toast.makeText(this,"اجازهٔ پنجرهٔ شناور را فعال کن.",Toast.LENGTH_LONG).show();stopSelf();}
    }
    private void addQuick(LinearLayout row,String label,String command){Button b=new Button(this);b.setText(label);b.setTextSize(10);b.setPadding(0,0,0,0);b.setOnClickListener(v->{addMessage("تو",command);handle(command);});row.addView(b,new LinearLayout.LayoutParams(0,dp(38),1));}
    private void resize(int dir){panelWidth=Math.max(260,Math.min(390,panelWidth+dir*30));panelHeight=Math.max(320,Math.min(640,panelHeight+dir*45));params.width=dp(panelWidth);params.height=dp(panelHeight);try{wm.updateViewLayout(panel,params);}catch(Exception ignored){}}
    private void addMessage(String who,String msg){if(messages==null)return;TextView t=new TextView(this);t.setText(who+": "+msg);t.setTextColor(0xFFFFFFFF);t.setTextSize(13);t.setPadding(dp(7),dp(7),dp(7),dp(7));messages.addView(t);}
    private String normalize(String value){String s=value.toLowerCase(Locale.ROOT).replace('ي','ی').replace('ك','ک').replace('‌',' ').trim();s=s.replaceAll("\\s+"," ");return s;}
    private String correctCommonTypos(String s){String[][] fixes={{"aneim","anime"},{"animi","anime"},{"animé","anime"},{"anme","anime"},{"animie","anime"},{"joojo","jojo"},{"jojutsu","jujutsu"},{"jujtsu","jujutsu"},{"you tube","youtube"},{"tik tok","tiktok"}};for(String[] f:fixes)s=s.replace(f[0],f[1]);return s;}
    private void handle(String raw){String q=correctCommonTypos(normalize(raw));
        if(q.matches(".*(وی ?پی ?ان|vpn).*(روشن|وصل|فعال).*") && q.matches(".*(تیک.?تاک|tiktok).*") ) { openVpn("اول صفحهٔ تنظیمات VPN رو باز کردم. خودت VPN رو روشن کن؛ بعد بنویس «حالا تیک‌تاک» تا بازش کنم."); return; }
        if(q.contains("حالا تیک تاک")||q.contains("حالا تیک‌تاک")||q.equals("تیک تاک")||q.equals("تیک‌تاک")||q.contains("برو تیک تاک")||q.contains("برو تیک‌تاک")||q.contains("tiktok")) { String query=extractSearch(q); if(!query.isEmpty() && (q.contains("سرچ")||q.contains("جستجو")||q.contains("search"))) openUrl("https://www.tiktok.com/search?q="+Uri.encode(query),"جستجوی تیک‌تاک رو باز کردم: "+query); else if(q.contains("حالا تیک")) openAppOrUrl("com.zhiliaoapp.musically","https://www.tiktok.com","تیک‌تاک رو باز کردم."); else openAppOrUrl("com.zhiliaoapp.musically","https://www.tiktok.com","تیک‌تاک رو باز کردم."); return; }
        if(q.contains("یوتیوب")||q.contains("youtube")||q.contains("you tube")){String term=extractSearch(q);if(term.isEmpty())openAppOrUrl("com.google.android.youtube","https://www.youtube.com","یوتیوب رو باز کردم.");else openUrl("https://www.youtube.com/results?search_query="+Uri.encode(term),"جستجوی یوتیوب برای «"+term+"» رو باز کردم.");return;}
        if(q.contains("گوگل")||q.contains("google")||q.startsWith("جستجو ")||q.startsWith("سرچ ")){String term=extractSearch(q);if(term.isEmpty())openUrl("https://www.google.com","گوگل رو باز کردم.");else openUrl("https://www.google.com/search?q="+Uri.encode(term),"جستجوی گوگل برای «"+term+"» رو باز کردم.");return;}
        if(q.contains("تنظیمات vpn")||q.contains("وی پی ان")||q.contains("وی‌پی‌ان")||q.contains(" vpn")||q.equals("vpn")){openVpn("تنظیمات VPN باز شد. روشن/خاموش‌کردن مستقیم VPN از این برنامه پشتیبانی نمی‌شه؛ باید داخل تنظیمات خودت انجام بدی.");return;}
        if(q.contains("وای فای")||q.contains("وای‌فای")||q.contains("wifi")){openSettings(new Intent(Settings.ACTION_WIFI_SETTINGS),"تنظیمات وای‌فای رو باز کردم.");return;}
        if(q.contains("بلوتوث")){openSettings(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS),"تنظیمات بلوتوث رو باز کردم.");return;}
        if(q.equals("برگرد")||q.equals("برگرد عقب")||q.equals("بازگشت")){if(AssistantAccessibilityService.goBack())addMessage("دستیار","برگشتم عقب.");else addMessage("دستیار","برای کنترل صفحهٔ برنامه‌های دیگر، سرویس دسترس‌پذیری رو در تنظیمات فعال کن.");return;}
        if(q.startsWith("روی ")&&q.endsWith(" کلیک کن")){String target=q.substring(4,q.length()-" کلیک کن".length()).trim();if(AssistantAccessibilityService.clickVisibleText(target))addMessage("دستیار","روی «"+target+"» کلیک کردم.");else addMessage("دستیار","متن پیدا نشد یا اجازهٔ کنترل صفحه فعال نیست. عنوان دقیق روی صفحه رو بنویس یا سرویس دسترس‌پذیری رو فعال کن.");return;}
        if(q.startsWith("تایپ کن ")||q.startsWith("بنویس ")){String text=q.startsWith("تایپ کن ")?q.substring(8).trim():q.substring(6).trim();if(AssistantAccessibilityService.setFocusedText(text))addMessage("دستیار","متن رو در کادر فعال وارد کردم.");else addMessage("دستیار","کادر متنی فعالی پیدا نشد یا سرویس دسترس‌پذیری فعال نیست.");return;}
        if(q.contains("روشن کن")||q.contains("خاموش کن")){addMessage("دستیار","اندروید بعضی کلیدهای سیستمی رو از برنامه‌های عادی محدود می‌کنه. می‌تونم صفحهٔ تنظیمات مربوط رو باز کنم، اما تغییر مستقیم همهٔ تنظیمات ممکن نیست.");return;}
        addMessage("دستیار","من این نسخه رو برای دستورهای مشخص آماده کردم و چند غلط رایج مثل aneim رو به anime اصلاح می‌کنم. برای فرمان‌های دیگر باید دستور تازه اضافه کنیم؛ هنوز به مدل ChatGPT وصل نیستم.");
    }
    private String extractSearch(String q){String s=q;String[] words={"برو توی یوتیوب","برو تو یوتیوب","برو یوتیوب","داخل یوتیوب","توی یوتیوب","تو یوتیوب","youtube","you tube","برو توی تیک تاک","برو تو تیک تاک","تیک تاک","تیک‌تاک","tiktok","در یوتیوب","یوتیوب","گوگل","google","سرچ کن","جستجو کن","search for","search","جستجو","سرچ","لطفا","لطفاً","برو تو","برو"};
        for(String w:words)s=s.replace(w," ");s=s.replaceAll("\\s+"," ").trim();return s;}
    private void openVpn(String msg){try{Intent i=new Intent("android.settings.VPN_SETTINGS");i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);addMessage("دستیار",msg);}catch(Exception e){openSettings(new Intent(Settings.ACTION_SETTINGS),"صفحهٔ اصلی تنظیمات باز شد؛ بخش VPN رو پیدا کن.");}}
    private void openSettings(Intent i,String msg){try{i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);addMessage("دستیار",msg);}catch(Exception e){addMessage("دستیار","نتونستم تنظیمات رو باز کنم.");}}
    private void openAppOrUrl(String packageName,String url,String msg){try{Intent i=getPackageManager().getLaunchIntentForPackage(packageName);if(i!=null){i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);addMessage("دستیار",msg);}else openUrl(url,msg);}catch(Exception e){openUrl(url,msg);}}
    private void openUrl(String url,String msg){try{Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse(url));i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);addMessage("دستیار",msg);}catch(Exception e){addMessage("دستیار","نتونستم صفحه رو باز کنم.");}}
    @Override public int onStartCommand(Intent i,int f,int id){return START_NOT_STICKY;}
    @Override public void onDestroy(){if(wm!=null&&panel!=null)try{wm.removeView(panel);}catch(Exception ignored){}super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
}
