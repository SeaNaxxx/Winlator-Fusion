package com.winlator.fusion;

import android.app.Application;

import com.winlator.fusion.core.CrashLogger;

public class FusionApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CrashLogger.install(this);
    }
}
