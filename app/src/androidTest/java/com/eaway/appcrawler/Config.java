package com.eaway.appcrawler;

import java.io.File;

/**
 * Configurations
 */
public class Config {
    public static final String VERSION = "1.0.0.0";
    public static final String TAG = "AppCrawler";
    public static final String TAG_MAIN = TAG + "Main";
    public static final String TAG_DEBUG = TAG + "Debug";

    public static int sLaunchTimeout = 5000;
    public static int sWaitIdleTimeout = 100;
    public static int sMaxDepth = 30;
    public static int sMaxSteps = 999;
    public static int sMaxRuntime = 3600;
    public static int sMaxScreenshot = 999;
    public static int sMaxScreenLoop = 20;
    public static int sScreenSignatueLength = 160;

    public static boolean sDebug = true;
    public static boolean sCaptureSteps = false;
    public static boolean sRandomText = true;

    public static File sOutputDir;
    public static String sFileLog;
    public static String sPerformanceLog;

    public static String sCrawlerPackage = "com.eaway.appcrawler";
    // public static String sTargetPackage = "com.google.android.youtube";
    /// public static String sTargetPackage = "pt.luxcorp.expere.apptest5";
    public static String sTargetPackage = "com.video.playnow";

    public static final String COMMAND_GET_ACTIVITY = String.format("su -c dumpsys activity %s | grep ACTIVITY\n", sTargetPackage);

    // Activities to be ignored
    public static final String[] IGNORED_ACTIVITY = {
            "Feedback & Help", "意見與協助"
    };

    // Common buttons that we can handle.
    public static final String[] COMMON_BUTTONS = {
            "OK", "Cancel", "Yes", "No",
            "確定", "取消", "是", "否"
    };

    // Text for Deny Button on permission request
    public static final String[] DENY_TEXT = {
            "Deny", "Rejeitar"
    };

    // Text for EditText testing
    public static final String[] RANDOM_TEXT = {
            "LOVE", "Latte", "Coffee", "Beer",
            "Taiwan", "Taipei", "Saturday", "Morning", "December",
            "Steve", "Jordan", "Michael",
    };

}
