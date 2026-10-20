package com.example.floatingassistant;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A small, private, offline-first Persian intent/FAQ engine. It does not call a server. */
public final class OfflineBrain {
    private OfflineBrain() { }

    public static String reply(String input) {
        if (input == null) return null;
        String q = normalize(input);
        if (q.isEmpty()) return null;

        // Simple arithmetic is evaluated locally; only a standalone expression (or one preceded by "حاصل/حساب") is accepted.
        String mathText = toAsciiDigits(q).replaceFirst("^(حاصل|حساب کن|حساب)\\s*", "").trim();
        Matcher math = Pattern.compile("(-?\\d+(?:\\.\\d+)?)\\s*([+\\-*/×÷])\\s*(-?\\d+(?:\\.\\d+)?)").matcher(mathText);
        if (math.matches()) {
            try {
                double a = Double.parseDouble(math.group(1));
                double b = Double.parseDouble(math.group(3));
                double value;
                switch (math.group(2)) {
                    case "+": value = a + b; break;
                    case "-": value = a - b; break;
                    case "*": case "×": value = a * b; break;
                    case "/": case "÷":
                        if (b == 0) return "تقسیم بر صفر تعریف نشده.";
                        value = a / b; break;
                    default: return null;
                }
                if (Double.isNaN(value) || Double.isInfinite(value)) return "نتیجه قابل محاسبه نیست.";
                DecimalFormat f = new DecimalFormat("0.####", DecimalFormatSymbols.getInstance(Locale.US));
                return "نتیجه: " + f.format(value);
            } catch (Exception ignored) { }
        }

        if (matches(q, "سلام", "سلام دستیار", "درود", "صبح بخیر", "شب بخیر", "عصر بخیر", "سلام خوبی"))
            return "سلام رفیق! ✦ من آماده‌ام. فرمان‌های گوشی و چند پاسخ ساده رو آفلاین انجام می‌دم؛ برای اطلاعات جدید هم می‌تونم گوگل رو باز کنم.";
        if (containsAny(q, "خوبی", "حالت چطوره", "چطوری", "چه خبر"))
            return "خوبم، ممنون که پرسیدی! 😄 بگو چه کاری انجام بدم.";
        if (containsAny(q, "ممنون", "مرسی", "دمت گرم", "تشکر", "سپاس"))
            return "خواهش می‌کنم رفیق ❤️";
        if (containsAny(q, "خداحافظ", "فعلا", "فعلاً", "بای", "شب خوش"))
            return "فعلاً! هر وقت خواستی روی ✦ بزن تا برگردم.";
        if (containsAny(q, "تو کی هستی", "اسمت چیه", "اسم تو چیه", "تو چی هستی", "کی هستی"))
            return "من دستیار شناور گوشی‌اتم. این نسخه یک موتور فرمان و پاسخ آفلاین سبک داره؛ مدل زبانی بزرگ مثل ChatGPT داخلش نصب نشده.";
        if (containsAny(q, "چه کارهایی بلدی", "چیکار میتونی بکنی", "چیکار می تونی بکنی", "کمک کن", "راهنما", "چه فرمان هایی", "فرمان ها"))
            return "نمونه فرمان‌ها: «برو تلگرام»، «صدا رو زیاد کن»، «صدا رو کم کن»، «وای‌فای»، «بلوتوث»، «برگرد»، «خانه»، «مخفی کن»، «اسکرین‌شات بگیر» و «سرچ کن آخرین اخبار». بعضی کارها نیاز به اجازهٔ اندروید دارن.";
        if (containsAny(q, "آفلاین کار میکنی", "بدون اینترنت کار میکنی", "اینترنت ندارم", "آفلاین هستی", "بدون نت"))
            return "بله، فرمان‌های محلی و پاسخ‌های پایه بدون اینترنت کار می‌کنن. تشخیص صدای فارسی آفلاین به سرویس گفتار و دانلود بودن بستهٔ زبان فارسی در گوشی بستگی داره؛ تایپ فرمان همیشه جایگزینشه.";
        if (containsAny(q, "گوگل چیست", "گوگل چیه", "google چیست", "گوگل رو میشناسی"))
            return "گوگل یک موتور جست‌وجو و مجموعه‌ای از سرویس‌های اینترنتیه. بگو «سرچ کن ...» تا جست‌وجو رو در مرورگر باز کنم.";
        if (containsAny(q, "هوش مصنوعی چیست", "هوش مصنوعی چیه", "ai چیست", "ai چیه"))
            return "هوش مصنوعی به نرم‌افزاری گفته می‌شه که کارهایی مثل فهم زبان، تشخیص الگو و تولید پاسخ رو انجام می‌ده. این نسخه برای حفظ کارکرد آفلاین از فرمان‌های محلی و پاسخ‌های ازپیش‌تعریف‌شده استفاده می‌کنه؛ برای گفت‌وگوی آزاد شبیه ChatGPT باید یک مدل زبانی محلی جداگانه نصب بشه.";
        if (containsAny(q, "اینترنت چیست", "اینترنت چیه"))
            return "اینترنت شبکه‌ای جهانی از دستگاه‌ها و سرویس‌های متصله که امکان تبادل اطلاعات و استفاده از وب رو می‌ده.";
        if (containsAny(q, "وی پی ان چیست", "vpn چیست", "وی پی ان چیه", "vpn چیه"))
            return "VPN ارتباط اینترنتی رو از مسیر یک سرور واسطه عبور می‌ده. فقط از سرویس قابل اعتماد استفاده کن؛ VPN به‌تنهایی ناشناس‌بودن کامل رو تضمین نمی‌کنه.";
        if (containsAny(q, "ساعت چنده", "الان ساعت چنده", "زمان چنده")) {
            return "ساعت گوشی: " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        }
        if (containsAny(q, "روز بخیر", "خسته نباشی"))
            return "ممنون رفیق! بگو چه کمکی ازم برمیاد.";
        return null;
    }

    private static String normalize(String input) {
        return input.toLowerCase(Locale.ROOT).replace('ي','ی').replace('ك','ک').replace('‌',' ')
                .replaceAll("[!?.؟،,]+$", "").replaceAll("\\s+", " ").trim();
    }

    private static String toAsciiDigits(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '۰' && c <= '۹') b.append((char) ('0' + (c - '۰')));
            else if (c >= '٠' && c <= '٩') b.append((char) ('0' + (c - '٠')));
            else b.append(c);
        }
        return b.toString();
    }

    private static boolean matches(String q, String... values) {
        for (String v : values) if (q.equals(v)) return true;
        return false;
    }
    private static boolean containsAny(String q, String... values) {
        for (String v : values) if (q.contains(v)) return true;
        return false;
    }
}
