package com.winlator.fusion;

import android.app.Application;

import com.winlator.fusion.core.CrashLogger;

/**
 * Application entry point. Installs the global crash logger as early as
 * possible so we capture failures that happen before any Activity is created
 * (e.g. during the FusionFS install step).
 */
public class FusionApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CrashLogger.install(this);
    }
}
