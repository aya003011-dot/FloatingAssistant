package com.example.floatingassistant;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class FloatingService extends Service {
    private WindowManager wm;
    private View panel;
    private Button bubble;
    private WindowManager.LayoutParams params;
    private WindowManager.LayoutParams bubbleParams;
    private LinearLayout messages;
    private EditText input;
    private boolean panelVisible = false;
    private int panelWidth = 320, panelHeight = 450;
    private static volatile FloatingService instance;

    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }

    @Override public void onCreate() {
        super.onCreate();
        instance = this;
        wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) { stopSelf(); return; }
        createWindow();
    }

    public static void submitCommandFromVoice(String text) {
        FloatingService s = instance;
        if (s != null && text != null && !text.trim().isEmpty()) {
            s.addMessage("تو 🎙", text.trim());
            s.handle(text.trim());
        }
    }

    private void createWindow() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(8), dp(7), dp(8), dp(8));
        root.setBackgroundColor(0xFF20232B);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText("✦ دستیار شناور"); title.setTextColor(0xFFFFFFFF); title.setTextSize(15);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(40), 1));
        Button hide = new Button(this); hide.setText("◉"); compactButton(hide);
        hide.setContentDescription("مخفی کردن پنجره، با امکان بازگرداندن از دکمهٔ شناور");
        hide.setOnClickListener(v -> hidePanel()); header.addView(hide, new LinearLayout.LayoutParams(dp(42), dp(40)));
        Button shrink = new Button(this); shrink.setText("−"); compactButton(shrink); shrink.setOnClickListener(v -> resize(-1));
        header.addView(shrink, new LinearLayout.LayoutParams(dp(42), dp(40)));
        Button grow = new Button(this); grow.setText("+"); compactButton(grow); grow.setOnClickListener(v -> resize(1));
        header.addView(grow, new LinearLayout.LayoutParams(dp(42), dp(40)));
        Button close = new Button(this); close.setText("×"); compactButton(close); close.setOnClickListener(v -> stopSelf());
        header.addView(close, new LinearLayout.LayoutParams(dp(42), dp(40)));
        title.setOnTouchListener(new View.OnTouchListener() {
            float x, y; int ox, oy;
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) { x=e.getRawX(); y=e.getRawY(); ox=params.x; oy=params.y; return true; }
                if (e.getAction() == MotionEvent.ACTION_MOVE) { params.x=ox+(int)(e.getRawX()-x); params.y=oy+(int)(e.getRawY()-y); updatePanel(); return true; }
                return e.getAction()==MotionEvent.ACTION_UP;
            }
        });
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        messages = new LinearLayout(this); messages.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(messages); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        addMessage("دستیار", "سلام! ✦ فرمان‌های ساده و پاسخ‌های پایه آفلاین کار می‌کنن. برای صحبت صوتی روی 🎙 بزن؛ یک بار شروع و بار دوم پایان رو بزن. نمونه: «برو تلگرام»، «صدا رو زیاد کن»، «حاصل ۲ + ۳»، «ساعت چنده»، «سرچ کن آخرین اخبار». برای پاسخ آزاد شبیه ChatGPT باید مدل جداگانه دانلود بشه. ◉ پنجره رو مخفی می‌کنه و ✦ برمی‌گردونه.");

        LinearLayout entry = new LinearLayout(this); entry.setOrientation(LinearLayout.HORIZONTAL);
        input = new EditText(this); input.setSingleLine(true); input.setTextSize(14); input.setHint("دستورت رو بنویس...");
        input.setTextColor(0xFF111111); input.setBackgroundColor(0xFFFFFFFF);
        entry.addView(input, new LinearLayout.LayoutParams(0, dp(48), 1));
        Button voice = new Button(this); voice.setText("🎙"); voice.setMinWidth(0); voice.setPadding(0,0,0,0);
        voice.setOnClickListener(v -> { try { Intent i = new Intent(this, VoiceCommandActivity.class); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); } catch(Exception e) { addMessage("دستیار", "ورودی صوتی باز نشد. می‌تونی دستور رو تایپ کنی."); } });
        entry.addView(voice, new LinearLayout.LayoutParams(dp(50), dp(48)));
        Button send = new Button(this); send.setText("ارسال"); send.setOnClickListener(v -> { String s=input.getText().toString().trim(); if(s.isEmpty())return; input.setText(""); addMessage("تو",s); handle(s); });
        entry.addView(send, new LinearLayout.LayoutParams(dp(68), dp(48))); root.addView(entry);

        LinearLayout quick = new LinearLayout(this); quick.setOrientation(LinearLayout.HORIZONTAL);
        addQuick(quick,"گالری","برو گالری"); addQuick(quick,"تلگرام","برو تلگرام"); addQuick(quick,"تیک‌تاک","برو تیک‌تاک"); addQuick(quick,"گوگل","گوگل");
        root.addView(quick);
        panel = root;
        int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        params = new WindowManager.LayoutParams(dp(panelWidth), dp(panelHeight), type, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP|Gravity.START; params.x=dp(14); params.y=dp(90);
        try { wm.addView(panel, params); panelVisible=true; }
        catch(Exception e) { Toast.makeText(this,"اجازهٔ پنجرهٔ شناور رو فعال کن.",Toast.LENGTH_LONG).show(); stopSelf(); }
        createBubble(type);
    }

    private void createBubble(int type) {
        bubble = new Button(this); bubble.setText("✦"); bubble.setTextSize(19); bubble.setPadding(0,0,0,0);
        bubble.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF3949AB)); bubble.setTextColor(0xFFFFFFFF);
        bubble.setContentDescription("نمایش دوبارهٔ دستیار شناور");
        bubbleParams = new WindowManager.LayoutParams(dp(56), dp(56), type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        bubbleParams.gravity=Gravity.TOP|Gravity.START; bubbleParams.x=dp(14); bubbleParams.y=dp(160);
        bubble.setOnTouchListener(new View.OnTouchListener() {
            float sx, sy; int ox, oy; boolean moved;
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction()==MotionEvent.ACTION_DOWN) { sx=e.getRawX(); sy=e.getRawY(); ox=bubbleParams.x; oy=bubbleParams.y; moved=false; return true; }
                if (e.getAction()==MotionEvent.ACTION_MOVE) { int dx=(int)(e.getRawX()-sx), dy=(int)(e.getRawY()-sy); if(Math.abs(dx)>dp(5)||Math.abs(dy)>dp(5))moved=true; bubbleParams.x=ox+dx; bubbleParams.y=oy+dy; try{wm.updateViewLayout(bubble,bubbleParams);}catch(Exception ignored){} return true; }
                if (e.getAction()==MotionEvent.ACTION_UP) { if(!moved)showPanel(); return true; }
                return true;
            }
        });
    }

    private void compactButton(Button b) { b.setMinWidth(0); b.setPadding(0,0,0,0); }
    private void addQuick(LinearLayout row,String label,String command) { Button b=new Button(this); b.setText(label); b.setTextSize(10); b.setPadding(0,0,0,0); b.setOnClickListener(v->{ addMessage("تو",command); handle(command); }); row.addView(b,new LinearLayout.LayoutParams(0,dp(38),1)); }
    private void resize(int dir) { panelWidth=Math.max(260,Math.min(390,panelWidth+dir*30)); panelHeight=Math.max(320,Math.min(640,panelHeight+dir*45)); params.width=dp(panelWidth); params.height=dp(panelHeight); updatePanel(); }
    private void updatePanel() { try { if(panelVisible&&panel!=null)wm.updateViewLayout(panel,params); } catch(Exception ignored){} }
    private void hidePanel() {
        if (!panelVisible || wm==null || panel==null || bubble==null) return;
        try { wm.removeView(panel); panelVisible=false; wm.addView(bubble,bubbleParams); }
        catch(Exception e) { Toast.makeText(this,"پنجره رو نتونستم مخفی کنم.",Toast.LENGTH_SHORT).show(); }
    }
    private void showPanel() {
        if (panelVisible || wm==null || panel==null) return;
        try { if(bubble!=null&&bubble.getParent()!=null)wm.removeView(bubble); wm.addView(panel,params); panelVisible=true; }
        catch(Exception e) { Toast.makeText(this,"پنجره رو نتونستم باز کنم.",Toast.LENGTH_SHORT).show(); }
    }

    private void addMessage(String who,String msg) {
        if(messages==null)return; TextView t=new TextView(this); t.setText(who+": "+msg); t.setTextColor(0xFFFFFFFF); t.setTextSize(13); t.setPadding(dp(7),dp(7),dp(7),dp(7)); messages.addView(t);
    }
    private String normalize(String value) { String s=value.toLowerCase(Locale.ROOT).replace('ي','ی').replace('ك','ک').replace('‌',' ').trim(); return s.replaceAll("\\s+"," "); }
    private String correctCommonTypos(String s) { String[][] fixes={{"aneim","anime"},{"animi","anime"},{"animé","anime"},{"anme","anime"},{"animie","anime"},{"joojo","jojo"},{"jojutsu","jujutsu"},{"jujtsu","jujutsu"},{"you tube","youtube"},{"tik tok","tiktok"},{"تیک تاک","تیک‌تاک"}}; for(String[] f:fixes)s=s.replace(f[0],f[1]); return s; }

    private void handle(String raw) {
        String q=correctCommonTypos(normalize(raw));
        if(q.isEmpty())return;
        if (isVolumeUpCommand(q)) { adjustMediaVolume(AudioManager.ADJUST_RAISE, "صدای رسانه رو زیاد کردم."); return; }
        if (isVolumeDownCommand(q)) { adjustMediaVolume(AudioManager.ADJUST_LOWER, "صدای رسانه رو کم کردم."); return; }
        if (isVolumeMuteCommand(q)) { adjustMediaVolume(AudioManager.ADJUST_MUTE, "صدای رسانه رو بی‌صدا کردم."); return; }
        if (isVolumeUnmuteCommand(q)) { adjustMediaVolume(AudioManager.ADJUST_UNMUTE, "صدای رسانه رو از حالت بی‌صدا خارج کردم."); return; }
        if (isScreenshotCommand(q)) {
            if (AssistantAccessibilityService.takeScreenshot()) addMessage("دستیار", "درخواست اسکرین‌شات ارسال شد. بسته به نسخهٔ اندروید ممکنه پنل یا تأیید سیستمی نمایش داده بشه.");
            else addMessage("دستیار", "اسکرین‌شات در دسترس نیست. اندروید ۹ یا جدیدتر و سرویس دسترس‌پذیری فعال لازم است.");
            return;
        }
        if(q.equals("مخفی")||q.contains("مخفی کن")||q.contains("پنجره رو مخفی")){ hidePanel(); addMessage("دستیار","مخفی شدم؛ روی ✦ بزن تا برگردم."); return; }
        if(q.equals("نمایش")||q.contains("برگرد به دستیار")){ showPanel(); return; }
        if(q.equals("برگرد")||q.equals("برگرد عقب")||q.equals("بازگشت")){ if(AssistantAccessibilityService.goBack())addMessage("دستیار","برگشتم عقب.");else addMessage("دستیار","سرویس دسترس‌پذیری رو از تنظیمات فعال کن."); return; }
        if(q.equals("خانه")||q.equals("صفحه اصلی")||q.equals("برو خانه")){ if(AssistantAccessibilityService.goHome())addMessage("دستیار","به صفحهٔ اصلی برگشتم.");else addMessage("دستیار","برای این کار سرویس دسترس‌پذیری رو فعال کن."); return; }
        if(q.startsWith("روی ")&&q.endsWith(" کلیک کن")){ String target=q.substring(4,q.length()-" کلیک کن".length()).trim(); if(AssistantAccessibilityService.clickVisibleText(target))addMessage("دستیار","روی «"+target+"» زدم.");else addMessage("دستیار","اون نوشته پیدا نشد یا سرویس دسترس‌پذیری فعاله نیست."); return; }
        if(q.startsWith("تایپ کن ")||q.startsWith("بنویس ")){ String text=q.startsWith("تایپ کن ")?q.substring(8).trim():q.substring(6).trim(); if(AssistantAccessibilityService.setFocusedText(text))addMessage("دستیار","متن رو در کادر فعلی وارد کردم.");else addMessage("دستیار","کادر متنی پیدا نشد یا سرویس دسترس‌پذیری فعال نیست."); return; }
        if(q.contains("وای فای")||q.contains("وای‌فای")||q.contains("wifi")){ openSettings(new Intent(Settings.ACTION_WIFI_SETTINGS),"تنظیمات وای‌فای رو باز کردم."); return; }
        if(q.contains("بلوتوث")){ openSettings(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS),"تنظیمات بلوتوث رو باز کردم."); return; }
        // Offline-first answers are checked before generic app-name matching.
        String localReply = OfflineBrain.reply(q);
        if (localReply != null) { addMessage("دستیار", localReply); return; }
        if (shouldSearchOnline(q)) {
            if (hasInternetConnection()) {
                openUrl("https://www.google.com/search?q=" + Uri.encode(raw.trim()), "جواب آفلاین آماده‌ای برای این سؤال ندارم؛ جستجوی گوگل رو باز کردم.");
            } else {
                addMessage("دستیار", "این سؤال به اطلاعات آنلاین نیاز داره. فعلاً آفلاین هستی؛ وقتی اینترنت وصل شد بگو «سرچ کن " + raw.trim() + "».");
            }
            return;
        }
        if((q.contains("تیک‌تاک")||q.contains("tiktok"))&&(q.contains("سرچ")||q.contains("جستجو")||q.contains("search"))) { String term=extractSearch(q); if(!term.isEmpty()){openUrl("https://www.tiktok.com/search?q="+Uri.encode(term),"جستجوی تیک‌تاک برای «"+term+"» رو باز کردم.");return;} }
        if((q.contains("یوتیوب")||q.contains("youtube"))&&(q.contains("سرچ")||q.contains("جستجو")||q.contains("search"))) { String term=extractSearch(q); if(!term.isEmpty()){openUrl("https://www.youtube.com/results?search_query="+Uri.encode(term),"جستجوی یوتیوب برای «"+term+"» رو باز کردم.");return;} }
        if(q.contains("vpn")||q.contains("وی پی ان")||q.contains("وی‌پی‌ان")||q.contains("وی پی‌ان")||q.contains("وی‌پی ان")) {
            if(q.contains("روشن")||q.contains("وصل")||q.contains("فعال")) {
                if(tryLaunchByAliases(new String[]{"vpn star","vpnstar","star vpn","وی پی ان استار","وی پی ان","vpn"})) addMessage("دستیار","برنامهٔ VPN رو باز کردم. اگر دکمهٔ اتصال یا تأیید سیستمی اومد، خودت بررسی و تأییدش کن؛ نمی‌تونم تأیید امنیتی اندروید رو دور بزنم.");
                else openVpn("برنامهٔ VPN پیدا نشد؛ تنظیمات VPN رو باز کردم. اگر VPN Star نصبه، اسم دقیقش رو بگو.");
            } else if(tryLaunchByAliases(new String[]{"vpn star","vpnstar","star vpn","وی پی ان استار"})) addMessage("دستیار","برنامهٔ VPN رو باز کردم.");
            else openVpn("تنظیمات VPN باز شد.");
            return;
        }
        if(isMessageSendCommand(q)) {
            if(tryLaunchByAliases(new String[]{"telegram","تلگرام"})) addMessage("دستیار","تلگرام باز شد. برای جلوگیری از ارسال اشتباهی، مخاطب و متن پیام رو داخل تلگرام بررسی کن و خودت دکمهٔ ارسال رو بزن.");
            else addMessage("دستیار","تلگرام رو پیدا نکردم. مطمئن شو نصبه.");
            return;
        }
        if(isAppOpenCommand(q)) {
            if(tryLaunchFromCommand(q))return;
        }
        if(q.contains("یوتیوب")||q.contains("youtube")||q.contains("you tube")) {
            String term=extractSearch(q);
            if(term.isEmpty()) openAppOrUrl("com.google.android.youtube","https://www.youtube.com","یوتیوب رو باز کردم.");
            else openUrl("https://www.youtube.com/results?search_query="+Uri.encode(term),"جستجوی یوتیوب برای «"+term+"» رو باز کردم."); return;
        }
        if (q.equals("گوگل") || q.equals("google") || q.equals("برو گوگل") || q.equals("باز کن گوگل") || q.equals("open google")) { openUrl("https://www.google.com", "گوگل رو باز کردم."); return; }
        if (q.startsWith("جستجو ") || q.startsWith("سرچ ") || q.contains("جستجو کن") || q.contains("سرچ کن") || q.startsWith("گوگل سرچ") || q.startsWith("در گوگل دنبال")) {
            String term = extractSearch(q);
            if (term.isEmpty()) openUrl("https://www.google.com", "گوگل رو باز کردم.");
            else openUrl("https://www.google.com/search?q=" + Uri.encode(term), "جستجوی گوگل برای «" + term + "» رو باز کردم.");
            return;
        }
        if(q.contains("تیک‌تاک")||q.contains("tiktok")) { openAppOrUrl("com.zhiliaoapp.musically","https://www.tiktok.com","تیک‌تاک رو باز کردم."); return; }
        if(q.contains("گالری")||q.contains("عکس هام")||q.contains("عکس‌ها")||q.contains("عکس ها")) { if(tryLaunchByAliases(new String[]{"gallery","گالری","photos","تصاویر"}))addMessage("دستیار","گالری رو باز کردم.");else openAppOrUrl("com.google.android.apps.photos","https://photos.google.com","برنامهٔ عکس‌ها رو باز کردم."); return; }
        if(q.contains("تلگرام")||q.contains("telegram")) { if(tryLaunchByAliases(new String[]{"telegram","تلگرام"}))addMessage("دستیار","تلگرام رو باز کردم.");else openUrl("https://telegram.org","تلگرام نصب نیست یا پیدا نشد."); return; }
        addMessage("دستیار", "این فرمان رو دقیق نفهمیدم. بگو «راهنما» تا نمونه فرمان‌ها رو ببینی، یا «سرچ کن ...» تا گوگل باز بشه.");
    }

    private boolean shouldSearchOnline(String q) {
        if (q == null || q.trim().isEmpty()) return false;
        String t = normalize(q);
        // Do not turn an explicit app-launch command written with a question mark into a web search.
        if (isAppOpenCommand(t) || t.startsWith("برو ") || t.startsWith("باز کن ")) return false;
        if (t.endsWith("؟") || t.endsWith("?")) return true;
        String[] starts = {"چی ", "چیست", "چیه", "چرا ", "چطور ", "چگونه ", "آیا ", "کجا ", "کی ", "کیه ", "چه ", "چند ", "کدوم ", "معنی ", "آخرین ", "جدیدترین ", "قیمت ", "اخبار ", "اطلاعات درباره ", "دربارهٔ ", "درباره ", "تاریخ پخش", "اسم واقعی", "چند قسمت", "چه سالی", "چه زمانی", "فصل ", "تاریخ انتشار"};
        for (String prefix : starts) if (t.startsWith(prefix)) return true;
        return t.contains("کی اومد") || t.contains("کی میاد") || t.contains("پخش شده") || t.contains("تاریخ انتشار") || t.contains("اومده") || t.contains("منتشر شده");
    }

    private boolean isVolumeUpCommand(String q) {
        return q.contains("صدا رو زیاد") || q.contains("صدا را زیاد") || q.contains("صدای گوشی رو زیاد") ||
                q.contains("صدای گوشی را زیاد") || q.contains("ولوم رو ببر بالا") || q.contains("ولوم را ببر بالا") ||
                q.equals("بلندتر") || q.equals("volume up") || q.equals("increase volume") || q.equals("زیاد کردن صدا");
    }
    private boolean isVolumeDownCommand(String q) {
        return q.contains("صدا رو کم") || q.contains("صدا را کم") || q.contains("صدای گوشی رو کم") ||
                q.contains("صدای گوشی را کم") || q.contains("ولوم رو بیار پایین") || q.contains("ولوم را بیار پایین") ||
                q.equals("آروم‌تر") || q.equals("volume down") || q.equals("decrease volume") || q.equals("کم کردن صدا");
    }
    private boolean isVolumeMuteCommand(String q) {
        return q.equals("صدای رسانه رو قطع کن") || q.equals("صدای رسانه را قطع کن") || q.equals("mute media") || q.equals("بی‌صدا کردن رسانه");
    }
    private boolean isVolumeUnmuteCommand(String q) {
        return q.equals("صدای رسانه رو وصل کن") || q.equals("صدای رسانه را وصل کن") || q.equals("unmute media");
    }
    private boolean isScreenshotCommand(String q) {
        return q.contains("اسکرین شات") || q.contains("screenshot") || q.contains("عکس از صفحه") || q.contains("از صفحه عکس بگیر");
    }
    private void adjustMediaVolume(int direction, String successMessage) {
        try {
            AudioManager audio = (AudioManager)getSystemService(AUDIO_SERVICE);
            if (audio == null) { addMessage("دستیار", "کنترل صدا در دسترس نیست."); return; }
            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI);
            addMessage("دستیار", successMessage);
        } catch (Exception e) { addMessage("دستیار", "نتونستم صدای رسانه رو تغییر بدم."); }
    }

    private boolean isMessageSendCommand(String q) { return (q.contains("پیام بده")||q.contains("پیام بفرست")||q.contains("ارسال پیام")||q.contains("send message")) && (q.contains("تلگرام")||q.contains("telegram")); }
    private boolean isAppOpenCommand(String q) { return q.startsWith("برو ")||q.startsWith("باز کن")||q.startsWith("بازکن")||q.startsWith("برو تو")||q.startsWith("وارد ")||q.startsWith("open ")||q.startsWith("launch ")||q.startsWith("برو داخل")||q.startsWith("برنامه "); }

    private boolean tryLaunchFromCommand(String q) {
        String target=q;
        String[] prefixes={"برو داخل ","برو توی ","برو تو ","برو به ","برو ","باز کن ","بازکن ","وارد ","برنامه ","open ","launch ","داخل ","توی ","به ","لطفا ","لطفاً "};
        for(String p:prefixes)if(target.startsWith(p)){target=target.substring(p.length()).trim();break;}
        target=target.replaceAll("(رو باز کن|را باز کن|بازش کن|باز کن|روشن کن|فعال کن|اجرا کن)$","").trim();
        if(target.isEmpty())return false;
        String norm=normalize(target);
        if(norm.contains("تیک")||norm.contains("tiktok")){ boolean ok=tryLaunchByAliases(new String[]{"tiktok","تیک‌تاک","تیک تاک"}); if(ok)addMessage("دستیار","تیک‌تاک رو باز کردم."); return ok; }
        if(norm.contains("تلگرام")||norm.contains("telegram")){ boolean ok=tryLaunchByAliases(new String[]{"telegram","تلگرام"}); if(ok)addMessage("دستیار","تلگرام رو باز کردم."); return ok; }
        if(norm.contains("گالری")||norm.contains("gallery")||norm.contains("عکس")){ boolean ok=tryLaunchByAliases(new String[]{"gallery","گالری","photos","تصاویر","عکس‌ها"}); if(ok)addMessage("دستیار","گالری رو باز کردم."); return ok; }
        if(norm.contains("vpn star")||norm.contains("vpnstar")||norm.contains("وی پی ان استار")){ boolean ok=tryLaunchByAliases(new String[]{"vpn star","vpnstar","star vpn","وی پی ان استار"}); if(ok)addMessage("دستیار","VPN Star رو باز کردم."); return ok; }
        if(norm.contains("یوتیوب")||norm.contains("youtube")){ openAppOrUrl("com.google.android.youtube","https://www.youtube.com","یوتیوب رو باز کردم."); return true; }
        if(norm.contains("تنظیمات"))return openSettings(new Intent(Settings.ACTION_SETTINGS),"تنظیمات رو باز کردم.");
        if(tryLaunchByAliases(new String[]{target})) { addMessage("دستیار","برنامهٔ «"+target+"» رو باز کردم."); return true; }
        return false;
    }

    private boolean tryLaunchByAliases(String[] aliases) {
        List<ResolveInfo> apps=getLaunchableApps();
        for(String alias:aliases) {
            String wanted=normalize(alias).replace(" ","");
            for(ResolveInfo ri:apps) {
                String label=normalize(String.valueOf(ri.loadLabel(getPackageManager()))).replace(" ","");
                if(label.equals(wanted)||label.contains(wanted)||wanted.contains(label)) {
                    try { Intent launch=new Intent(Intent.ACTION_MAIN); launch.addCategory(Intent.CATEGORY_LAUNCHER); launch.setClassName(ri.activityInfo.packageName,ri.activityInfo.name); launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(launch); return true; } catch(Exception ignored) {}
                }
            }
        }
        // Fall back to known packages for apps that have non-standard launcher visibility.
        for(String alias:aliases) {
            String a=normalize(alias);
            if(a.contains("telegram")||a.contains("تلگرام")) { if(launchPackage("org.telegram.messenger"))return true; }
            if(a.contains("tiktok")||a.contains("تیک")) { if(launchPackage("com.zhiliaoapp.musically"))return true; if(launchPackage("com.ss.android.ugc.trill"))return true; }
            if(a.contains("photos")||a.contains("عکس")) { if(launchPackage("com.google.android.apps.photos"))return true; }
        }
        return false;
    }

    private boolean launchPackage(String packageName) {
        try { Intent i=getPackageManager().getLaunchIntentForPackage(packageName); if(i==null)return false; i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); return true; } catch(Exception e) { return false; }
    }
    private List<ResolveInfo> getLaunchableApps() {
        Intent intent=new Intent(Intent.ACTION_MAIN); intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps;
        try { apps=getPackageManager().queryIntentActivities(intent,0); } catch(Exception e) { return new ArrayList<>(); }
        Collections.sort(apps,new Comparator<ResolveInfo>() { public int compare(ResolveInfo a,ResolveInfo b){return String.valueOf(a.loadLabel(getPackageManager())).compareToIgnoreCase(String.valueOf(b.loadLabel(getPackageManager())));} });
        return apps;
    }
    private boolean openSettings(Intent i,String msg) { try { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); addMessage("دستیار",msg); return true; } catch(Exception e) { addMessage("دستیار","نتونستم تنظیمات رو باز کنم."); return false; } }
    private void openVpn(String msg) { try { Intent i=new Intent("android.settings.VPN_SETTINGS"); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); addMessage("دستیار",msg); } catch(Exception e) { openSettings(new Intent(Settings.ACTION_SETTINGS),"صفحهٔ اصلی تنظیمات باز شد؛ بخش VPN رو پیدا کن."); } }
    private void openAppOrUrl(String packageName,String url,String msg) { try { Intent i=getPackageManager().getLaunchIntentForPackage(packageName); if(i!=null){i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);addMessage("دستیار",msg);}else openUrl(url,msg);}catch(Exception e){openUrl(url,msg);} }
    private void openUrl(String url,String msg) { try { Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse(url)); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); addMessage("دستیار",msg); } catch(Exception e) { addMessage("دستیار","نتونستم اون صفحه رو باز کنم."); } }
    private String extractSearch(String q) {
        String s = q;
        String[] phrases = {
                "برو توی یوتیوب", "برو تو یوتیوب", "برو یوتیوب", "داخل یوتیوب", "توی یوتیوب", "تو یوتیوب",
                "برو توی تیک تاک", "برو تو تیک تاک", "در یوتیوب", "تیک‌تاک", "تیک تاک", "یوتیوب", "youtube", "you tube", "tiktok",
                "در گوگل دنبال", "تو گوگل", "در گوگل", "گوگل سرچ", "جستجو کن", "سرچ کن", "search for",
                "جستجو", "سرچ", "search", "گوگل", "google", "درباره ی", "دربارهٔ", "درباره",
                "لطفا", "لطفاً", "برام", "برای من", "برو تو", "برو"
        };
        for (String phrase : phrases) s = s.replace(phrase, " ");
        return s.replaceAll("\\s+", " ").trim();
    }

    private boolean hasInternetConnection() {
        try {
            ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network n=cm.getActiveNetwork();
                if (n == null) return false;
                NetworkCapabilities caps=cm.getNetworkCapabilities(n);
                return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            }
            NetworkInfo info=cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } catch (Exception e) { return false; }
    }

    @Override public int onStartCommand(Intent i,int flags,int startId) { return START_NOT_STICKY; }
    @Override public void onDestroy() {
        instance=null;
        if(wm!=null) { try{if(panel!=null&&panel.getParent()!=null)wm.removeView(panel);}catch(Exception ignored){} try{if(bubble!=null&&bubble.getParent()!=null)wm.removeView(bubble);}catch(Exception ignored){} }
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent i) { return null; }
}
