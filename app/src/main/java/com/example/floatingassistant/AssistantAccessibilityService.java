package com.example.floatingassistant;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Accessibility bridge for user-requested actions in the currently visible app. */
public class AssistantAccessibilityService extends AccessibilityService {
    private static volatile AssistantAccessibilityService instance;

    @Override public void onServiceConnected() { super.onServiceConnected(); instance = this; }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }
    @Override public void onInterrupt() { }
    @Override public void onDestroy() { if (instance == this) instance = null; super.onDestroy(); }

    public static boolean isEnabled() { return instance != null; }
    public static boolean goBack() { return instance != null && instance.performGlobalAction(GLOBAL_ACTION_BACK); }
    public static boolean goHome() { return instance != null && instance.performGlobalAction(GLOBAL_ACTION_HOME); }
    public static boolean takeScreenshot() {
        AssistantAccessibilityService s = instance;
        return s != null && Build.VERSION.SDK_INT >= 28 && s.performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT);
    }

    public static boolean clickVisibleText(String wanted) {
        AssistantAccessibilityService s = instance;
        return s != null && s.clickMatching(new String[]{wanted}, false, false);
    }
    public static boolean clickAnyVisibleText(String... wanted) {
        AssistantAccessibilityService s = instance;
        return s != null && s.clickMatching(wanted, false, false);
    }
    public static boolean clickAnyContentDescription(String... wanted) {
        AssistantAccessibilityService s = instance;
        return s != null && s.clickMatching(wanted, true, false);
    }
    public static boolean clickExactVisibleText(String wanted) {
        AssistantAccessibilityService s = instance;
        return s != null && s.clickMatching(new String[]{wanted}, false, true);
    }
    public static boolean hasVisibleText(String... wanted) {
        AssistantAccessibilityService s = instance;
        return s != null && s.hasMatching(wanted);
    }
    public static boolean setFocusedText(String text) {
        AssistantAccessibilityService s = instance;
        if (s == null) return false;
        for (AccessibilityNodeInfo root : s.externalRoots()) {
            try {
                AccessibilityNodeInfo focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
                if (focused != null) {
                    try {
                        Bundle args = new Bundle();
                        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
                        boolean ok = focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
                        if (ok) return true;
                    } finally { focused.recycle(); }
                }
            } finally { root.recycle(); }
        }
        return false;
    }
    public static boolean focusFirstEditable() {
        AssistantAccessibilityService s = instance;
        if (s == null) return false;
        for (AccessibilityNodeInfo root : s.externalRoots()) {
            try { if (s.focusEditableInTree(root)) return true; }
            finally { root.recycle(); }
        }
        return false;
    }
    public static boolean performSearchImeAction() {
        AssistantAccessibilityService s = instance;
        if (s == null) return false;
        for (AccessibilityNodeInfo root : s.externalRoots()) {
            try {
                AccessibilityNodeInfo focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
                if (focused == null) continue;
                try {
                    if (Build.VERSION.SDK_INT >= 30 && focused.performAction(android.R.id.accessibilityActionImeEnter)) return true;
                    return false;
                } finally { focused.recycle(); }
            } finally { root.recycle(); }
        }
        return false;
    }

    private boolean focusEditableInTree(AccessibilityNodeInfo node) {
        if (node == null) return false;
        try {
            CharSequence cls = node.getClassName();
            if (node.isVisibleToUser() && node.isEnabled() && (node.isEditable() || (cls != null && cls.toString().contains("EditText")))) {
                if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
                if (node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)) return true;
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child == null) continue;
                try { if (focusEditableInTree(child)) return true; }
                finally { child.recycle(); }
            }
            return false;
        } catch (Exception ignored) { return false; }
    }

    private boolean clickMatching(String[] wanted, boolean descriptionsOnly, boolean exact) {
        if (wanted == null || wanted.length == 0) return false;
        for (AccessibilityNodeInfo root : externalRoots()) {
            try { if (clickInTree(root, wanted, descriptionsOnly, exact)) return true; }
            finally { root.recycle(); }
        }
        return false;
    }

    private boolean clickInTree(AccessibilityNodeInfo node, String[] wanted, boolean descriptionsOnly, boolean exact) {
        if (node == null) return false;
        try {
            if (node.isVisibleToUser() && matchesNode(node, wanted, descriptionsOnly, exact)) {
                AccessibilityNodeInfo current = node;
                boolean ownsCurrent = false;
                for (int i = 0; current != null && i < 8; i++) {
                    try {
                        if (current.isEnabled() && current.isClickable() && current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
                        AccessibilityNodeInfo parent = current.getParent();
                        if (ownsCurrent) current.recycle();
                        current = parent;
                        ownsCurrent = true;
                    } catch (Exception ignored) { break; }
                }
                if (ownsCurrent && current != null) current.recycle();
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child == null) continue;
                try { if (clickInTree(child, wanted, descriptionsOnly, exact)) return true; }
                finally { child.recycle(); }
            }
        } catch (Exception ignored) { }
        return false;
    }

    private boolean hasMatching(String[] wanted) {
        if (wanted == null) return false;
        for (AccessibilityNodeInfo root : externalRoots()) {
            try { if (containsInTree(root, wanted)) return true; }
            finally { root.recycle(); }
        }
        return false;
    }
    private boolean containsInTree(AccessibilityNodeInfo node, String[] wanted) {
        if (node == null) return false;
        try {
            if (node.isVisibleToUser() && matchesNode(node, wanted, false, false)) return true;
            if (node.isVisibleToUser() && matchesNode(node, wanted, true, false)) return true;
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child == null) continue;
                try { if (containsInTree(child, wanted)) return true; }
                finally { child.recycle(); }
            }
        } catch (Exception ignored) { }
        return false;
    }

    private boolean matchesNode(AccessibilityNodeInfo node, String[] wanted, boolean descriptionsOnly, boolean exact) {
        String text = descriptionsOnly ? "" : safe(node.getText());
        String description = safe(node.getContentDescription());
        for (String w : wanted) {
            String n = norm(w);
            if (n.isEmpty()) continue;
            // Never try to click the typed search/query text itself; prefer a result row with that label.
            if (!descriptionsOnly && !node.isEditable() && matchText(norm(text), n, exact)) return true;
            if (matchText(norm(description), n, exact)) return true;
        }
        return false;
    }
    private boolean matchText(String value, String wanted, boolean exact) {
        if (value.isEmpty()) return false;
        return exact ? value.equals(wanted) : value.contains(wanted) || wanted.contains(value) && value.length() > 2;
    }

    private List<AccessibilityNodeInfo> externalRoots() {
        List<AccessibilityNodeInfo> roots = new ArrayList<>();
        try {
            for (AccessibilityWindowInfo window : getWindows()) {
                AccessibilityNodeInfo root = window.getRoot();
                if (root == null) continue;
                CharSequence pkg = root.getPackageName();
                if (pkg != null && getPackageName().contentEquals(pkg)) { root.recycle(); continue; }
                roots.add(root);
            }
        } catch (Exception ignored) { }
        if (roots.isEmpty()) {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                CharSequence pkg = root.getPackageName();
                if (pkg == null || !getPackageName().contentEquals(pkg)) roots.add(root);
                else root.recycle();
            }
        }
        return roots;
    }
    private static String safe(CharSequence c) { return c == null ? "" : c.toString(); }
    private static String norm(String value) { return value == null ? "" : value.toLowerCase(Locale.ROOT).replace('ي','ی').replace('ك','ک').replace('‌',' ').replaceAll("\\s+", " ").trim(); }
}
