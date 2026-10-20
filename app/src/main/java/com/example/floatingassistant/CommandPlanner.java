package com.example.floatingassistant;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Small on-device Persian command planner. It converts a sentence or a sequence
 * of instructions into explicit UI actions. No text is sent to a remote server.
 * It deliberately asks for human confirmation before sending an outgoing message.
 */
public final class CommandPlanner {
    public enum Type { LOCAL, OPEN_APP, SEARCH_APP, SEND_TELEGRAM, VPN_ON, VPN_OFF, CLOSE_CURRENT, HOME, BACK }

    public static final class Step {
        public final Type type;
        public final String original;
        public final String app;
        public final String query;
        public final String recipient;
        public final String message;

        private Step(Type type, String original, String app, String query, String recipient, String message) {
            this.type = type;
            this.original = original;
            this.app = app == null ? "" : app.trim();
            this.query = query == null ? "" : query.trim();
            this.recipient = recipient == null ? "" : recipient.trim();
            this.message = message == null ? "" : message.trim();
        }
        static Step local(String text) { return new Step(Type.LOCAL, text, "", "", "", ""); }
        static Step of(Type type, String original, String app, String query, String recipient, String message) {
            return new Step(type, original, app, query, recipient, message);
        }
    }

    private CommandPlanner() { }

    public static List<Step> plan(String input) {
        List<Step> steps = new ArrayList<>();
        if (input == null) return steps;
        String normalized = normalize(input)
                .replace("تیم تاک", "تیک تاک")
                .replace("تیگ تاک", "تیک تاک")
                .replace("تیکتاک", "تیک تاک")
                .replace("جو جوتسو", "جوجوتسو")
                .replace("جوجیتسو", "جوجوتسو");
        if (normalized.isEmpty()) return steps;
        List<String> parts = splitSequence(normalized);
        for (String part : parts) {
            Step step = parseOne(part.trim());
            if (step != null) steps.add(step);
        }
        if (steps.isEmpty()) steps.add(Step.local(normalized));
        return steps;
    }

    private static List<String> splitSequence(String text) {
        // Split only when the connector is followed by a likely action, not arbitrary prose.
        String action = "(?=(?:برو|باز\\s*کن|بازکن|وارد\\s+|خاموش\\s+کن|روشن\\s+کن|فعالش\\s+کن|غیرفعالش\\s+کن|وی.?پی.?ان|فیلترشکن|از\\s+.+?\\s+(?:بیرون\\s+برو|خارج\\s+شو)|خانه|برگرد|بازگشت))";
        String regex = "\\s+(?:و\\s+بعد(?:ش)?|بعد(?:ش)?|بعد\\s+از\\s+اون|سپس|سریعاً)\\s+" + action;
        String[] split = text.split(regex);
        List<String> result = new ArrayList<>();
        for (String item : split) if (!item.trim().isEmpty()) result.add(item.trim());
        // A second action can be spoken without a connector ("... بگو سلام، برو دوباره تیک‌تاک ...").
        if (result.size() == 1) {
            Matcher m = Pattern.compile("(?i)\\s+(?=برو\\s+(?:دوباره\\s+)?(?:(?:تو|توی|داخل|به)\\s+)?(?:تلگرام|telegram|تیک.?تاک|tiktok|یوتیوب|youtube|تنظیمات|خانه))").matcher(text);
            List<String> implicit = new ArrayList<>();
            int last = 0;
            while (m.find()) { if (m.start() > last) { implicit.add(text.substring(last, m.start()).trim()); last = m.start(); } }
            if (last > 0) { implicit.add(text.substring(last).trim()); return nonEmpty(implicit); }
        }
        return result;
    }

    private static List<String> nonEmpty(List<String> input) {
        List<String> out = new ArrayList<>();
        for (String s : input) if (s != null && !s.trim().isEmpty()) out.add(s.trim());
        return out;
    }

    private static Step parseOne(String q) {
        if (q.isEmpty()) return null;
        String n = normalize(q);

        if (containsAny(n, "خونه", "خانه", "صفحه اصلی", "صفحهٔ اصلی") && (n.equals("خانه") || n.contains("برو خانه") || n.contains("صفحه اصلی")))
            return Step.of(Type.HOME, q, "", "", "", "");
        if (containsAny(n, "برگرد عقب", "بازگشت", "برگرد به عقب", "دکمه برگشت"))
            return Step.of(Type.BACK, q, "", "", "", "");
        if (isCloseCommand(n)) return Step.of(Type.CLOSE_CURRENT, q, "", "", "", "");

        if (isVpn(n)) {
            if (containsAny(n, "خاموش", "قطع", "غیرفعال", "disconnect", "turn off", "off vpn"))
                return Step.of(Type.VPN_OFF, q, vpnAppName(n), "", "", "");
            if (containsAny(n, "روشن", "وصل", "فعال", "connect", "turn on", "on vpn"))
                return Step.of(Type.VPN_ON, q, vpnAppName(n), "", "", "");
        }

        // Outgoing messages are always represented as a distinct step; the service asks before tapping Send.
        if (containsAny(n, "تلگرام", "telegram") && containsAny(n, "بگو", "پیام بده", "پیام بفرست", "بنویس برای", "send message")) {
            String afterTelegram = n;
            int ix = Math.max(afterTelegram.indexOf("تلگرام"), afterTelegram.indexOf("telegram"));
            if (ix >= 0) afterTelegram = afterTelegram.substring(ix + (afterTelegram.startsWith("telegram", ix) ? "telegram".length() : "تلگرام".length())).trim();
            Matcher m = Pattern.compile("(?:به|برای)\\s+(.+?)\\s+(?:بگو|پیام\\s+بده|پیام\\s+بفرست|بنویس\\s+برای)\\s+(.+)$").matcher(afterTelegram.trim());
            if (m.find()) {
                String recipient = cleanupRecipient(m.group(1));
                String message = cleanupTrailingPunctuation(m.group(2));
                if (!recipient.isEmpty() && !message.isEmpty())
                    return Step.of(Type.SEND_TELEGRAM, q, "تلگرام", "", recipient, message);
            }
        }

        String app = detectSearchApp(n);
        String term = extractSearchTerm(n);
        if (!app.isEmpty() && !term.isEmpty() && isSearchRequest(n))
            return Step.of(Type.SEARCH_APP, q, app, cleanQuery(term, app), "", "");

        if (isSearchRequest(n)) return Step.local(q);

        if (startsOpenCommand(n) || isStandaloneApp(n)) {
            String appName = detectAppName(n);
            if (!appName.isEmpty()) return Step.of(Type.OPEN_APP, q, appName, "", "", "");
        }
        return Step.local(q);
    }

    private static boolean isCloseCommand(String q) {
        return containsAny(q, "از تیک تاک بیرون برو", "از تیک‌تاک بیرون برو", "از تلگرام بیرون برو", "از برنامه بیرون برو",
                "از برنامه خارج شو", "بیرون برو", "خارج شو", "برنامه رو ببند", "برنامه را ببند", "ببندش", "close app", "exit app");
    }
    private static boolean isVpn(String q) { return containsAny(q, "vpn", "وی پی ان", "وی‌پی‌ان", "وی پی‌ان", "وی‌پی ان", "فیلترشکن"); }
    private static String vpnAppName(String q) {
        Matcher m = Pattern.compile("(?:برو\\s+داخل|برو\\s+تو|داخل|توی|با)\\s+(.+?)(?:\\s+(?:روشن|خاموش|وصل|قطع|فعال|غیرفعال).*)?$").matcher(q);
        if (m.find() && isVpn(m.group(1))) return m.group(1).trim();
        return "VPN";
    }
    private static boolean isSearchRequest(String q) {
        return containsAny(q, "سرچ", "جستجو", "دنبال بگرد", "پیدا کن", "search", "look up");
    }
    private static String detectSearchApp(String q) {
        if (containsAny(q, "تیک تاک", "تیک‌تاک", "تیکتاک", "tiktok", "tik tok")) return "تیک‌تاک";
        if (containsAny(q, "یوتیوب", "youtube", "you tube")) return "یوتیوب";
        if (containsAny(q, "تلگرام", "telegram")) return "تلگرام";
        if (containsAny(q, "اینستاگرام", "instagram", "اینستا")) return "اینستاگرام";
        return "";
    }
    private static String extractSearchTerm(String q) {
        // First support the natural Persian word order: "topic رو سرچ کن".
        Matcher before = Pattern.compile("(.+?)\\s+(?:رو|را)?\\s*(?:سرچ|جستجو|search)\\s*(?:کن)?(?:\\s+(?:توی|تو|در|داخل)\\s+(?:تیک.?تاک|tik\\s*tok|tiktok|یوتیوب|youtube|you\\s*tube|تلگرام|telegram|اینستاگرام|instagram|اینستا))?$").matcher(q);
        if (before.find()) {
            String t = before.group(1).trim();
            if (!t.isEmpty()) return t;
        }
        // Then support "سرچ کن topic" / "search topic".
        String[] patterns = {
                "(?:سرچ|جستجو|search)\\s+کن\\s+(.+)$",
                "(?:دنبال\\s+بگرد|پیدا\\s+کن)\\s+(.+)$",
                "(?:سرچ|جستجو|search)\\s+(.+)$"
        };
        for (String p : patterns) {
            Matcher m = Pattern.compile(p).matcher(q);
            if (m.find()) {
                String t = m.group(1).trim();
                if (!t.isEmpty() && !t.equals("کن") && !t.equals("رو") && !t.equals("را")) return t;
            }
        }
        return "";
    }
    private static String cleanQuery(String term, String app) {
        String t = normalize(term);
        t = t.replaceAll("^(?:کن|بکن|برای من|برام)\\s+", "");
        // Remove the selected app from either side of the recognized query phrase.
        t = t.replaceAll("^(?:(?:در|توی|تو|داخل)\\s+)?(?:تیک.?تاک|tik\\s*tok|tiktok|یوتیوب|youtube|you\\s*tube|تلگرام|telegram|اینستاگرام|instagram|اینستا)\\s+", "");
        t = t.replaceAll("\\s+(?:(?:در|توی|تو|داخل)\\s+)?(?:تیک.?تاک|tik\\s*tok|tiktok|یوتیوب|youtube|you\\s*tube|تلگرام|telegram|اینستاگرام|instagram|اینستا)$", "");
        t = t.replaceAll("(?:توی|تو|در|داخل)\\s+(?:تیک.?تاک|tik\\s*tok|tiktok|یوتیوب|youtube|you\\s*tube|تلگرام|telegram|اینستا(?:گرام)?|instagram)", " ");
        t = t.replaceAll("\\s+(?:رو|را)$", "").replaceAll("^(?:رو|را)\\s+", "");
        t = t.replaceAll("^(?:سرچ|جستجو|کن|بکن)\\s+", "");
        return t.replaceAll("\\s+", " ").trim();
    }
    private static boolean startsOpenCommand(String q) {
        return q.startsWith("برو ") || q.startsWith("بروتو ") || q.startsWith("برو تو ") || q.startsWith("برو توی ") ||
                q.startsWith("برو داخل ") || q.startsWith("باز کن ") || q.startsWith("بازکن ") || q.startsWith("وارد ") ||
                q.startsWith("اجرا کن ") || q.startsWith("open ") || q.startsWith("launch ") || q.startsWith("go to ");
    }
    private static boolean isStandaloneApp(String q) {
        return containsAny(q, "تلگرام", "telegram", "تیک تاک", "تیک‌تاک", "tiktok", "یوتیوب", "youtube", "گالری", "اینستاگرام", "instagram", "واتساپ", "whatsapp", "کروم", "chrome", "تنظیمات", "دوربین", "camera", "گوشی", "ماشین حساب", "calculator", "پلی استور", "play store")
                && q.length() < 32 && !isSearchRequest(q) && !containsAny(q, "به ", "بگو", "پیام بده");
    }
    private static String detectAppName(String q) {
        String s = normalize(q);
        String[] prefixes = {"برو دوباره ", "برو داخل ", "برو توی ", "برو تو ", "برو به ", "برو ", "باز کن ", "بازکن ", "وارد ", "اجرا کن ", "open ", "launch ", "go to ", "لطفا ", "لطفاً "};
        for (String p : prefixes) if (s.startsWith(p)) { s = s.substring(p.length()).trim(); break; }
        s = s.replaceAll("(?:رو|را)?\\s*(?:باز کن|بازکن|بازش کن|اجرا کن|بکن)$", "").trim();
        if (containsAny(s, "تیک تاک", "تیک‌تاک", "تیکتاک", "tiktok", "tik tok")) return "تیک‌تاک";
        if (containsAny(s, "تلگرام", "telegram")) return "تلگرام";
        if (containsAny(s, "یوتیوب", "youtube", "you tube")) return "یوتیوب";
        if (containsAny(s, "گالری", "gallery", "تصاویر", "عکس ها", "عکس‌ها")) return "گالری";
        if (containsAny(s, "اینستاگرام", "instagram", "اینستا")) return "اینستاگرام";
        if (containsAny(s, "واتساپ", "whatsapp", "واتس اپ")) return "واتساپ";
        if (containsAny(s, "کروم", "chrome")) return "Chrome";
        if (containsAny(s, "تنظیمات", "settings")) return "تنظیمات";
        if (containsAny(s, "دوربین", "camera")) return "دوربین";
        if (containsAny(s, "ماشین حساب", "calculator", "حسابگر")) return "ماشین حساب";
        if (containsAny(s, "پلی استور", "play store", "فروشگاه پلی")) return "Play Store";
        if (containsAny(s, "وی پی ان استار", "vpn star", "vpnstar")) return "VPN Star";
        if (containsAny(s, "توییتر", "twitter", "ایکس", " x ")) return "X";
        return "";
    }
    private static String cleanupRecipient(String r) {
        String s = normalize(r).replaceAll("^(?:توی تلگرام|در تلگرام|داخل تلگرام|تلگرام)\\s+", "").trim();
        s = s.replaceAll("\\s+(?:رو|را)$", "").trim();
        return s;
    }
    private static String cleanupTrailingPunctuation(String s) { return s.trim().replaceAll("^[،,:;\\s]+|[،,:;\\s]+$", ""); }
    private static String normalize(String input) {
        return input.toLowerCase(Locale.ROOT).replace('ي','ی').replace('ك','ک').replace('‌',' ')
                .replaceAll("[!?.؟،,]+$", "").replaceAll("\\s+", " ").trim();
    }
    private static boolean containsAny(String q, String... values) {
        for (String v : values) if (q.contains(v)) return true;
        return false;
    }
}
