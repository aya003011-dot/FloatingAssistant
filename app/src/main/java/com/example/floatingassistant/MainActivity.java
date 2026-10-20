package com.example.floatingassistant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
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
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(28, 40, 28, 28);
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setBackgroundColor(0xFFF4F5F7);

        TextView title = new TextView(this);
        title.setText("دستیار شناور");
        title.setTextSize(27);
        title.setTextColor(0xFF20232B);
        title.setGravity(Gravity.CENTER);
        page.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView help = new TextView(this);
        help.setText("نسخهٔ آفلاین‌محور: فرمان‌های گوشی، پاسخ‌های پایه و محاسبه‌های ساده روی خود گوشی انجام می‌شن و برای همین به سرور یا API پولی نیاز ندارن. با اینترنت می‌تونی سؤال‌های تازه رو در جستجوی گوگل باز کنی. این نسخه مدل زبانی بزرگ مثل ChatGPT رو داخل خودش نداره؛ تشخیص صدای فارسی بدون اینترنت هم به پشتیبانی سرویس گفتار گوشی و دانلود بودن بستهٔ فارسی بستگی داره. اگر صدا کار نکرد، فرمان رو تایپ کن.");
        help.setTextSize(15);
        help.setTextColor(0xFF333333);
        help.setPadding(0, 24, 0, 20);
        page.addView(help);

        Button overlay = new Button(this);
        overlay.setText("۱) اجازهٔ پنجرهٔ شناور");
        overlay.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 23) {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
            }
        });
        page.addView(overlay, new LinearLayout.LayoutParams(-1, -2));

        Button access = new Button(this);
        access.setText("۲) فعال‌کردن کنترل صفحه و اسکرین‌شات (اختیاری)");
        access.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, -2);
        ap.topMargin = 10;
        page.addView(access, ap);

        Button mic = new Button(this);
        mic.setText("۳) اجازهٔ میکروفون");
        mic.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 502);
            } else {
                help.setText("اجازهٔ میکروفون قبلاً داده شده است. هنگام ضبط، برنامه وضعیت ضبط را نشان می‌دهد و با زدن دکمهٔ پایان، فرمان را اجرا می‌کند.");
            }
        });
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(-1, -2);
        mp.topMargin = 10;
        page.addView(mic, mp);

        Button start = new Button(this);
        start.setText("۴) باز کردن دستیار شناور");
        start.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this)) {
                startService(new Intent(this, FloatingService.class));
                finish();
            } else {
                help.setText("اول اجازهٔ پنجرهٔ شناور رو فعال کن و بعد دوباره بزن.");
            }
        });
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = 10;
        page.addView(start, sp);
        setContentView(page);
    }
}
