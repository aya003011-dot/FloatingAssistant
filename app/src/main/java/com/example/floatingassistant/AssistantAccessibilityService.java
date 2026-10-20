package com.example.floatingassistant;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.view.accessibility.AccessibilityEvent;
import android.os.Build;
import java.util.List;

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
        AssistantAccessibilityService s=instance;
        if (s == null || Build.VERSION.SDK_INT < 28) return false;
        return s.performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT);
    }
    private AccessibilityNodeInfo externalRoot() {
        try {
            for (AccessibilityWindowInfo window : getWindows()) {
                AccessibilityNodeInfo root = window.getRoot();
                if (root == null) continue;
                CharSequence pkg = root.getPackageName();
                if (pkg != null && !getPackageName().contentEquals(pkg)) return root;
                root.recycle();
            }
        } catch (Exception ignored) { }
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null && getPackageName().contentEquals(root.getPackageName())) {
            root.recycle();
            return null;
        }
        return root;
    }
    public static boolean clickVisibleText(String wanted) {
        AssistantAccessibilityService s = instance;
        if (s == null) return false;
        AccessibilityNodeInfo root = s.externalRoot();
        if (root == null) return false;
        try {
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(wanted);
            if (nodes == null) return false;
            for (AccessibilityNodeInfo node : nodes) {
                AccessibilityNodeInfo cur = node;
                for (int i=0; cur != null && i<8; i++) {
                    if (cur.isClickable() && cur.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
                    cur = cur.getParent();
                }
            }
            return false;
        } finally { root.recycle(); }
    }
    public static boolean setFocusedText(String text) {
        AssistantAccessibilityService s=instance;
        if(s==null) return false;
        AccessibilityNodeInfo root=s.externalRoot();
        if(root==null) return false;
        try {
            AccessibilityNodeInfo focused=root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            if(focused==null) return false;
            Bundle args=new Bundle(); args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
            return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args);
        } finally { root.recycle(); }
    }
}
