/*
 * Depth First + Local Optimization Crawler
 *
 * Android UI flow is a graph with cycles, not a tree, we never know where will we go before clicking a button.
 * Same as our life - Eaway 2015/10/21
 */

package com.eaway.appcrawler.strategy;

import static com.eaway.appcrawler.Config.sOutputDir;

import android.graphics.Rect;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.util.Log;

import com.eaway.appcrawler.Config;
import com.eaway.appcrawler.FileLog;
import com.eaway.appcrawler.common.UiHelper;
import com.eaway.appcrawler.common.UiScreen;
import com.eaway.appcrawler.common.UiWidget;
import com.eaway.appcrawler.performance.PerformanceMonitor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import pt.mastersilvapt.expere.Tree;

/**
 * AppCrawler test using Android UiAutomator 2.0
 */
public class DepthFirstCrawler extends Crawler {
    private static final String TAG = Config.TAG;
    private static final String TAG_MAIN = Config.TAG_MAIN;
    private static final String TAG_DEBUG = Config.TAG_DEBUG;

    public static final String PERMISSION_MANAGER_PKG = "com.android.packageinstaller";

    private static int sDepth = 0; // root screen depth = 0
    private static int sSteps = 0;
    private static int sDepthPeak = 0;
    private static int sLoop = 0;
    private static Date sStartTime;
    private static List<UiScreen> sScannedScreenList = new ArrayList<UiScreen>();
    private static UiScreen sRootScreen = null;
    private static UiScreen sLastScreen = null;
    private static UiWidget sLastActionWidget = null;
    private static String sLastActionMessage = new String("");
    private static boolean sFinished = false;
    private UiDevice mDevice;
    private Tree<String> tree;

    @Override
    public void run() {
        sDepth = 0;
        sSteps = 0;
        sLoop = 0;
        sDepthPeak = 0;
        sStartTime = new Date();
        sRootScreen = null;
        sLastScreen = null;
        sLastActionWidget = null;
        sLastActionMessage = new String("");
        sFinished = false;
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        tree = new Tree<>();
        // Start from main activity
        if (!UiHelper.launchTargetApp())
            return;

//        Thread t = new Thread(() -> {
//            try{
//                Thread.sleep(5000);
//                String command = String.format("logcat -c --pid=$(pidof -s %s)", Config.sTargetPackage);
//                // String command = String.format("pidof -s %s", Config.sTargetPackage);
//                // FileLog.v(TAG, command);
//                Process p = Runtime.getRuntime().exec(command);
//                BufferedReader bis = new BufferedReader(new InputStreamReader(p.getInputStream()));
//                BufferedReader bisErr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
//                String line;
//                while((line = bis.readLine()) != null){
//                    FileLog.i(TAG, String.format("%s: %s", Config.sTargetPackage, line));
//                }
//                bis.close();
//                while((line = bisErr.readLine()) != null){
//                    FileLog.i(TAG, String.format("%s ERR: %s", Config.sTargetPackage, line));
//                }
//                bisErr.close();
//                FileLog.i(TAG, "ExitValue: " + p.exitValue());
//            }catch (Exception e){
//                FileLog.e(TAG, "handleCrash4");
//            }
//        });
//        t.setName("Thread Minha");
//        t.start();

        while (!sFinished) {
            sSteps++;

            // Get current screen
            UiScreen currentScreen = new UiScreen(sLastScreen, sLastActionWidget);
            currentScreen.id = sScannedScreenList.size() + 1;

            try {
                currentScreen.device.dumpWindowHierarchy(new File(sOutputDir + "/" + currentScreen.id));
            }catch (IOException e){
                e.printStackTrace();
            }

            // In other package
            if (currentScreen.pkg.compareTo(Config.sTargetPackage) != 0) {
                FileLog.i(TAG_MAIN, "{Inspect} screen, in other package: " + currentScreen.pkg);
                handleOtherPackage(currentScreen);
                continue;
            }

            // In target package, check where are we now.
            boolean newScreen = true;
            for (UiScreen screen : sScannedScreenList) {
                if (screen.equals(currentScreen)) {
                    newScreen = false;
                    currentScreen = screen;
                    sDepth = currentScreen.depth;
                    break;
                }
            }

            if (sDepth == 0) {
                if (sRootScreen != null) {
                    Log.i(TAG, "Root screen changed, may be due to app's coachmarks");
                }
                sRootScreen = currentScreen;
            }

            // New screen?
            if (newScreen) {
                tree.addNode(currentScreen.signature, currentScreen.id, currentScreen.depth);
                String action = "";
                try{
                    if(currentScreen.parentWidget != null && currentScreen.parentWidget.uiObject.exists())
                        action = currentScreen.parentWidget.uiObject.getText().replaceAll("\\r\\n|\\r|\\n", " ");
                }catch (UiObjectNotFoundException e){
                    FileLog.e(TAG, e.getMessage());
                }
                if(currentScreen.parentScreen == null)
                    tree.addChild(null, currentScreen.signature, action);
                else
                    tree.addChild(currentScreen.parentScreen.signature, currentScreen.signature, action);
                handleNewScreen(currentScreen);
                sLoop = 0;
            } else {
                handleOldScreen(currentScreen);
                if (++sLoop > Config.sMaxScreenLoop) {
                    Log.i(TAG, "Reached max old screen loop, re-launch target app");
                    sLoop = 0;
                    UiHelper.launchTargetApp();
                    continue;
                }
            }

            // Record CPU and Memory info
            PerformanceMonitor.record(currentScreen.name);

            // If there have unfinished widgets, mark all ascendant screens unfinished, so we have chance to go back.
            if (!currentScreen.isFinished()) {
                UiScreen screen = currentScreen;
                do {
                    Log.d(TAG_DEBUG, String.format("Marking Screen %s with depth %d and signature %s as not finished", screen.name, screen.depth, screen.signature));
                    if (screen.parentWidget != null)
                        screen.parentWidget.setFinished(false);
                    if (screen.parentScreen != null)
                        screen.parentScreen.setFinished(false);
                    screen = screen.parentScreen;
                } while (screen != null);
            }

            // Input some random text to EditText if any
            if (Config.sRandomText) {
                UiHelper.inputRandomTextToEditText();
            }

            // Handle  next unfinished widget
            handleNextWidget(currentScreen);

            // Check finish
            if (currentScreen.isFinished()) {
                Log.d(TAG, "Screen[" + currentScreen.id + "] finished");

                // Update parent screens
                UiScreen screen = currentScreen;
                do {
                    if (screen.parentWidget != null)
                        screen.parentWidget.setFinished(true);
                    if (screen.parentScreen != null) {
                        if (!screen.parentScreen.isFinished())
                            break;
                    }
                    screen = screen.parentScreen;
                } while (screen != null);

                if (currentScreen == sRootScreen) {
                    // Root screen is finished.
                    // We should check the last widget we clicked brings us to some where new?
                    // For example, a first-run-wizard that will lead us to the real root screen.
                    if (isNewTargetPkgScreen()) {
                        // The root screen may be a first-run-wizard, Tips-of-the-day
                        if (sLastActionWidget != null)
                            sLastActionWidget.setFinished(false);
                        sRootScreen.setFinished(false);
                    } else {
                        FileLog.i(TAG_MAIN, "{Stop} root screen finished, id:" + sRootScreen.id);
                        sFinished = true;
                    }
                } else {
                    if (UiHelper.isInTheSameScreen(currentScreen)) {
                        FileLog.i(TAG_MAIN, "{Click} Back");
                        mDevice.pressBack();
                    }
                }
            }

            // Max run time
            if ((new Date().getTime() - sStartTime.getTime()) / 1000 > Config.sMaxRuntime) {
                FileLog.i(TAG_MAIN, "{Stop} reached max run-time second: " + Config.sMaxRuntime);
                sFinished = true;
            }

            //  Max screen files
            if (UiHelper.sScreenshotIndex >= Config.sMaxScreenshot - 1) {
                FileLog.i(TAG_MAIN, "{Stop} reached max screenshot files.");
                sFinished = true;
            }

            // Max test steps
            if (sSteps >= Config.sMaxSteps) {
                FileLog.i(TAG_MAIN, "{Stop} reached max screenshot files.");
                sFinished = true;
            }

            // Avoid infinite loop
            if (++currentScreen.loop > Config.sMaxScreenLoop) {
                Log.i(TAG, "Reached max screen loop, set screen finished");
                currentScreen.setFinished(true);
            }

            // Debug
            if (Config.sDebug) {
                logAllScreenInfo();
            }
        }

        try {
            tree.serialize(new File(Config.sOutputDir, "tree.ser").getAbsolutePath());
            tree.serializeToJson(new File(Config.sOutputDir, "tree.json").getAbsolutePath());
        }catch (Exception e){
            e.printStackTrace();
            FileLog.e(TAG, e.getMessage());
        }
        // Done
        FileLog.i(TAG_MAIN, "Total executed steps:" + sSteps +
                ", peak depth:" + sDepthPeak +
                ", detected screens:" + sScannedScreenList.size() +
                ", screenshot:" + UiHelper.sScreenshotIndex);

        String log = String.format("CPU average:%.1f%%, CPU peak:%.1f%%, " +
                        "Memory average (KB):%d, Memory peak (KB):%d",
                PerformanceMonitor.getAverageCpu(), PerformanceMonitor.cpuPeak,
                PerformanceMonitor.getAverageMemory(), PerformanceMonitor.memPeak);
        FileLog.i(TAG_MAIN, log);

    }

    // Add info to node that permission was requested for post ExPeRe add permission name
    public void handlePermission(UiScreen currentScreen) {
        FileLog.i(TAG_MAIN, "{PM} requesting permissions!");
        tree.permissionRequest(currentScreen.parentScreen.signature);
        // mDevice.takeScreenshot(new File(Config.sOutputDir + "/" + currentScreen.name + ".png"));
        UiHelper.takeScreenshots(currentScreen.name);
        screenshotToTreeNode(currentScreen);
        for (int i = 0 ;; i++){
            UiObject uiObject = currentScreen.device.findObject(new UiSelector().clickable(true).instance(i));
            if(!uiObject.exists()) break;
            try {
                Log.d(TAG, String.format("{PM} Ui Name: %s", uiObject.getText()));
                if(isDeny(uiObject.getText())){
                    uiObject.click();
                    FileLog.i(TAG_MAIN, "{PM} Permission Denied!");
                    return;
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void screenshotToTreeNode(UiScreen currentScreen) {
        UiScreen screen = currentScreen;
        while (screen != null && !screen.pkg.equals(Config.sTargetPackage))
            screen = screen.parentScreen;
        if(screen != null)
            tree.addScreenshot(screen.signature, UiHelper.sLastFilename);
    }

    private boolean isDeny(String text){
        for(String s : Config.DENY_TEXT){
            if (s.equalsIgnoreCase(text))
                return true;
        }
        return false;
    }

    public void handleOtherPackage(UiScreen currentScreen) {
        if(PERMISSION_MANAGER_PKG.compareToIgnoreCase(currentScreen.pkg) == 0) {
            handlePermission(currentScreen);
            return;
        }
        FileLog.i(TAG, String.format("{Other package} %s", currentScreen.pkg));
        if (isNewScreen(currentScreen)) {
            UiHelper.takeScreenshots("(" + currentScreen.pkg + ")");
            screenshotToTreeNode(currentScreen);
            currentScreen.widgetList.clear();
            currentScreen.setFinished(true);
            sScannedScreenList.add(currentScreen);
        }

        sLastActionMessage = "";
        sLastActionWidget = null;

        if (UiHelper.handleAndroidUi()) {
            FileLog.i(TAG_MAIN, "Handle Android UI succeeded");
        } else if (UiHelper.handleCommonDialog()) {
            FileLog.i(TAG_MAIN, "Handle Common UI succeeded");
        } else {
            // Something we can not handle, try back
            FileLog.i(TAG_MAIN, "{Click} Back");
            mDevice.pressBack();
            if (!UiHelper.isInTargetApp()) {
                UiHelper.launchTargetApp();
                sDepth = 0;
            }
        }
    }

    public void handleNewScreen(UiScreen currentScreen) {
        FileLog.i(TAG_MAIN, "{Inspect} NEW screen, " + currentScreen.toString());
        sLastActionMessage = "";
        sLastActionWidget = null;
        UiHelper.takeScreenshots("");
        screenshotToTreeNode(currentScreen);

        currentScreen.depth = ++sDepth;
        if (sDepth > sDepthPeak)
            sDepthPeak = sDepth;

        boolean stop = false;
        if (UiHelper.isInIgnoredActivity(currentScreen.name)) {
            FileLog.i(TAG_MAIN, "{Inspect} screen, in ignored list: " + currentScreen.name);
            stop = true;
        }
        if (sDepth >= Config.sMaxDepth) {
            Log.i(TAG, "Has reached the MaxDepth: " + Config.sMaxDepth);
            stop = true;
        }

        if (stop) {
            Log.d(TAG_DEBUG, String.format("Stop in New Screen %s with signature %s", currentScreen.name, currentScreen.signature));
            currentScreen.widgetList.clear();
            currentScreen.setFinished(true);
            sScannedScreenList.add(currentScreen);
            FileLog.i(TAG_MAIN, "{Click} Back");
            // FIXME: Test if remove the pressback does something
            mDevice.pressBack(); // Not sure that we can always go back to previous page by back key
            mDevice.waitForIdle(Config.sWaitIdleTimeout);
        } else {
            sScannedScreenList.add(currentScreen);
        }

    }

    public void handleOldScreen(UiScreen currentScreen) {
        FileLog.i(TAG_MAIN, "{Inspect} OLD screen, " + currentScreen.toString());
        if (Config.sCaptureSteps) {
            UiHelper.takeScreenshots(sLastActionMessage);
            screenshotToTreeNode(currentScreen);
            sLastActionMessage = "";
            sLastActionWidget = null;
        }
    }

    public void handleNextWidget(UiScreen currentScreen) {
        UiWidget widget = getNextWidget(currentScreen);
        if (widget == null) {
            return;
        }

        String classname = "";
        String text = "";
        String desc = "";

        if (widget != null) {
            try {
                classname = widget.uiObject.getClassName();
                text = widget.uiObject.getText();
                if (text.length() > 25) {
                    text = text.substring(0, 21) + "...";
                }
            } catch (UiObjectNotFoundException e) {
                // Don't worry, it is ok without Text
            }
            try {
                desc = widget.uiObject.getContentDescription();
            } catch (UiObjectNotFoundException e) {
                // Never mind, it is ok without ContentDescription
            }
            try {
                Rect bounds = widget.uiObject.getBounds();
                String clazz = "";
                for (String tmp : classname.split("\\.")) {
                    clazz = tmp;
                }
                if (text.length() > 0)
                    sLastActionMessage = String.format("{Click} %s %s %s", text, clazz, bounds.toShortString());
                else if (desc.length() > 0)
                    sLastActionMessage = String.format("{Click} %s %s %s", desc, clazz, bounds.toShortString());
                else
                    sLastActionMessage = String.format("{Click} %s %s", clazz, bounds.toShortString());

                FileLog.i(TAG_MAIN, sLastActionMessage);
                sLastScreen = currentScreen;
                sLastActionWidget = widget;
                widget.setFinished(true);
                widget.uiObject.click();
            } catch (UiObjectNotFoundException e) {
                Log.e(TAG, "UiObjectNotFoundException, failed to test a widget");
            }
        }
    }

    public UiWidget getNextWidget(UiScreen currentScreen) {
        if (currentScreen.isFinished())
            return null;

        for (int i = 0; i < currentScreen.widgetList.size(); i++) {
            UiWidget widget = currentScreen.widgetList.get(i);
            if (!widget.uiObject.exists()) {
                widget.setFinished(true); // Maybe UI has changed
                continue;
            }
            if (!widget.isFinished())
                return widget;
        }

        return null;
    }

    public boolean isNewTargetPkgScreen() {
        UiScreen currentScreen = new UiScreen(null, null);
        if (0 != currentScreen.pkg.compareToIgnoreCase(Config.sTargetPackage))
            return false;
        return isNewScreen(currentScreen);
    }

    public boolean isNewScreen(UiScreen currentScreen) {
        for (UiScreen screen : sScannedScreenList) {
            if (screen.equals(currentScreen)) {
                return false;
            }
        }
        return true;
    }

    public boolean isAllScreenFinished() {
        for (int i = 0; i < sScannedScreenList.size(); i++) {
            UiScreen screen = sScannedScreenList.get(i);
            if (!screen.isFinished()) {
                return false;
            }
        }
        return true;
    }

    public void logAllScreenInfo() {
        for (int i = 0; i < sScannedScreenList.size(); i++) {
            UiScreen screen = sScannedScreenList.get(i);
            Log.d(TAG, "Screen[" + (i + 1) + "] " + screen.toString());
        }
        Log.d(TAG_DEBUG, "Root Screen id: " + sRootScreen.id);
    }
}

