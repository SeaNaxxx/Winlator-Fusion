package com.winlator.fusion.xenvironment;

import androidx.appcompat.app.AppCompatActivity;

import com.winlator.fusion.MainActivity;

public abstract class RootFSInstaller {
    public static final byte LATEST_VERSION = 2;
    public static final byte UPDATE_WINEPREFIX_VERSION = 16;

    public static void install(final MainActivity activity) {
        FusionFSInstaller.install(activity);
    }

    public static void installIfNeeded(final MainActivity activity) {
        FusionFSInstaller.installIfNeeded(activity);
    }

    public static void generateCompactContainerPattern(final AppCompatActivity activity) {
    }
}
