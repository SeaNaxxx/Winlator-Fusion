package com.winlator.fusion.core;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class CrashLogger implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "CrashLogger";
    private static final String DIR_NAME = "crash_logs";

    private final Context appContext;
    private final Thread.UncaughtExceptionHandler defaultHandler;

    private CrashLogger(Context context, Thread.UncaughtExceptionHandler defaultHandler) {
        this.appContext = context.getApplicationContext();
        this.defaultHandler = defaultHandler;
    }

    public static void install(Context context) {
        Thread.UncaughtExceptionHandler existing = Thread.getDefaultUncaughtExceptionHandler();
        if (existing instanceof CrashLogger) return;
        Thread.setDefaultUncaughtExceptionHandler(new CrashLogger(context, existing));
        Log.i(TAG, "CrashLogger installed");
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        try {
            writeCrashFile(thread, throwable);
        } catch (Throwable t) {
            Log.e(TAG, "Failed to write crash log", t);
        }
        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, throwable);
        } else {
            System.exit(2);
        }
    }

    private void writeCrashFile(Thread thread, Throwable throwable) throws Exception {
        File baseDir = appContext.getExternalFilesDir(null);
        if (baseDir == null) baseDir = appContext.getFilesDir();
        File dir = new File(baseDir, DIR_NAME);
        if (!dir.exists() && !dir.mkdirs()) {
            Log.e(TAG, "Could not create crash dir: " + dir);
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
        File file = new File(dir, "crash-" + timestamp + ".txt");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("=== Winlator Fusion Crash Report ===");
        pw.println("Time:        " + new Date());
        pw.println("Thread:      " + thread.getName());
        pw.println("App version: " + getAppVersion());
        pw.println("Android:     " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")");
        pw.println("Device:      " + Build.MANUFACTURER + " " + Build.MODEL);
        pw.println("ABI:         " + java.util.Arrays.toString(Build.SUPPORTED_ABIS));
        pw.println();
        pw.println("--- Stack trace ---");
        throwable.printStackTrace(pw);
        Throwable cause = throwable.getCause();
        while (cause != null) {
            pw.println();
            pw.println("--- Caused by ---");
            cause.printStackTrace(pw);
            cause = cause.getCause();
        }
        pw.flush();

        try (FileWriter fw = new FileWriter(file, false)) {
            fw.write(sw.toString());
            fw.flush();
        }
        Log.e(TAG, "Crash written to " + file.getAbsolutePath());
    }

    private String getAppVersion() {
        try {
            return appContext.getPackageManager()
                    .getPackageInfo(appContext.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "unknown";
        }
    }
}
