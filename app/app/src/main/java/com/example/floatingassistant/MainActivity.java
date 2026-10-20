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
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(28, 40, 28, 28);
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setBackgroundColor(0xFFF4F5F7);
        TextView title = new TextView(this);
        title.setText("دستیار شناور"); title.setTextSize(27); title.setTextColor(0xFF20232B); title.setGravity(Gravity.CENTER);
        page.addView(title, new LinearLayout.LayoutParams(-1, -2));
        TextView help = new TextView(this);
        help.setText("این برنامه یک پنجرهٔ چت شناور با کادر تایپ دارد. برای بازکردن پنجره اجازهٔ شناور را فعال کن. برای فرمان‌هایی مثل کلیک روی متن مشخص، می‌توانی سرویس دسترس‌پذیری را هم خودت فعال کنی.");
        help.setTextSize(16); help.setTextColor(0xFF333333); help.setPadding(0, 24, 0, 20); page.addView(help);
        Button overlay = new Button(this); overlay.setText("۱) اجازهٔ پنجرهٔ شناور");
        overlay.setOnClickListener(v -> { if (Build.VERSION.SDK_INT >= 23) startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))); });
        page.addView(overlay, new LinearLayout.LayoutParams(-1, -2));
        Button access = new Button(this); access.setText("۲) تنظیمات کنترل صفحه (اختیاری)");
        access.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, -2); ap.topMargin=10; page.addView(access, ap);
        Button start = new Button(this); start.setText("۳) باز کردن دستیار شناور");
        start.setOnClickListener(v -> { if (Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this)) { startService(new Intent(this, FloatingService.class)); finish(); } else help.setText("اول اجازهٔ شناور را فعال کن و دوباره بزن."); });
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2); sp.topMargin=10; page.addView(start, sp);
        setContentView(page);
    }
}
