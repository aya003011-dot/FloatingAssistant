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
    private TextView helpText;
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
        helpText = help;
        help.setText("دستیار شناور پیشرفته: فرمان فارسی رو تحلیل می‌کنه، کارهای پشت‌سرهم رو صف می‌کنه، برنامه‌ها رو باز می‌کنه و با اجازهٔ دسترس‌پذیری می‌تونه داخل برنامهٔ باز جست‌وجو و متن وارد کنه. میکروفون مستقیماً داخل پنجرهٔ شناوره و صفحهٔ ضبط جدا باز نمی‌شه. برای حفظ امنیت، پیش از ارسال پیام باید مخاطب و متن رو بررسی و تأیید کنی. این نسخه موتور محلی فرمان داره، نه مدل زبانی نامحدود مثل ChatGPT؛ رابط برنامه‌ها هم ممکنه با نسخهٔ گوشی فرق کنه.");
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
        access.setText("۲) فعال‌کردن کنترل داخل برنامه‌ها (لازم برای جست‌وجو و تایپ خودکار)");
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
                help.setText("اجازهٔ میکروفون قبلاً داده شده است. صدای فرمان از همان پنجرهٔ شناور دریافت می‌شود.");
            }
        });
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(-1, -2);
        mp.topMargin = 10;
        page.addView(mic, mp);

        Button start = new Button(this);
        start.setText("۴) باز کردن دستیار شناور");
        start.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                help.setText("برای اینکه میکروفون مستقیماً داخل پنجرهٔ شناور کار کنه، اول اجازهٔ میکروفون رو بده.");
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 503);
                return;
            }
            launchAssistantWhenAllowed();
        });
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = 10;
        page.addView(start, sp);
        setContentView(page);
    }

    private void launchAssistantWhenAllowed() {
        if (Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this)) {
            Intent service = new Intent(this, FloatingService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(service); else startService(service);
            finish();
        } else if (helpText != null) {
            helpText.setText("اول اجازهٔ پنجرهٔ شناور رو از دکمهٔ اول فعال کن، بعد دوباره «باز کردن دستیار شناور» رو بزن.");
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 503) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchAssistantWhenAllowed();
            } else if (helpText != null) {
                helpText.setText("بدون اجازهٔ میکروفون، فرمان صوتی داخل پنجره فعال نمی‌شه. می‌تونی مجوز رو از تنظیمات برنامه فعال کنی.");
            }
        } else if (requestCode == 502 && helpText != null) {
            helpText.setText(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    ? "اجازهٔ میکروفون داده شد. حالا دستیار شناور رو باز کن؛ ضبط صوتی همان‌جا در پنجره انجام می‌شه."
                    : "برای فرمان صوتی باید مجوز میکروفون رو بدی.");
        }
    }
}
