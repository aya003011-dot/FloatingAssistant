package com.example.floatingassistant;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout page=new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL); page.setPadding(28,40,28,28); page.setGravity(Gravity.CENTER_HORIZONTAL); page.setBackgroundColor(0xFFF4F5F7);
        TextView title=new TextView(this); title.setText("دستیار شناور"); title.setTextSize(27); title.setTextColor(0xFF20232B); title.setGravity(Gravity.CENTER); page.addView(title,new LinearLayout.LayoutParams(-1,-2));
        TextView help=new TextView(this); help.setText("فرمان تایپی و صوتی، بازکردن برنامه‌های نصب‌شده و پنجرهٔ شناور قابل مخفی/بازگردانی. کنترل بعضی صفحه‌ها به سرویس دسترس‌پذیری نیاز دارد. اتصال هوش مصنوعی آنلاین اختیاری است و به سرور جداگانه نیاز دارد."); help.setTextSize(15); help.setTextColor(0xFF333333); help.setPadding(0,24,0,20); page.addView(help);
        Button overlay=new Button(this); overlay.setText("۱) اجازهٔ پنجرهٔ شناور"); overlay.setOnClickListener(v->{if(Build.VERSION.SDK_INT>=23)startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName())));}); page.addView(overlay,new LinearLayout.LayoutParams(-1,-2));
        Button access=new Button(this); access.setText("۲) تنظیمات کنترل صفحه (اختیاری)"); access.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))); LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2);ap.topMargin=10;page.addView(access,ap);
        Button ai=new Button(this); ai.setText("۳) تنظیم اتصال هوش مصنوعی (اختیاری)"); ai.setOnClickListener(v->showAiSettings()); LinearLayout.LayoutParams aip=new LinearLayout.LayoutParams(-1,-2);aip.topMargin=10;page.addView(ai,aip);
        Button start=new Button(this); start.setText("۴) باز کردن دستیار شناور"); start.setOnClickListener(v->{if(Build.VERSION.SDK_INT<23||Settings.canDrawOverlays(this)){startService(new Intent(this,FloatingService.class));finish();}else help.setText("اول اجازهٔ شناور رو فعال کن و دوباره بزن.");}); LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=10;page.addView(start,sp);
        setContentView(page);
    }
    private void showAiSettings() {
        SharedPreferences prefs=getSharedPreferences("assistant_settings",MODE_PRIVATE);
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(12,4,12,4);
        EditText url=new EditText(this);url.setSingleLine(true);url.setHint("آدرس HTTPS سرور، بدون /command");url.setText(prefs.getString("backend_url",""));box.addView(url);
        EditText token=new EditText(this);token.setSingleLine(true);token.setHint("توکن دسترسی سرور (اختیاری اگر سرور تنظیم نشده)");token.setText(prefs.getString("backend_token",""));box.addView(token);
        new AlertDialog.Builder(this).setTitle("اتصال امن به سرور هوش مصنوعی").setMessage("این قسمت تا وقتی سرور رو راه‌اندازی نکرده‌ای کار نمی‌کنه. وقتی فرمانی به هوش مصنوعی فرستاده بشه، متن فرمان و نام برنامه‌های قابل‌اجرا به سروری که خودت تنظیم می‌کنی ارسال می‌شن. کلید OpenAI رو اینجا وارد نکن؛ اون باید فقط روی سرور نگه داشته بشه.").setView(box).setNegativeButton("انصراف",(d,w)->{}).setNeutralButton("پاک‌کردن",(d,w)->prefs.edit().remove("backend_url").remove("backend_token").apply()).setPositiveButton("ذخیره",(d,w)->prefs.edit().putString("backend_url",url.getText().toString().trim()).putString("backend_token",token.getText().toString().trim()).apply()).show();
    }
}
