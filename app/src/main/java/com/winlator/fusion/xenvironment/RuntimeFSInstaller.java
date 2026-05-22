package com.winlator.fusion.xenvironment;

import android.content.Context;

import com.winlator.fusion.MainActivity;

public abstract class RuntimeFSInstaller {
    public static final byte LATEST_CORE_VERSION = 2;
    public static final byte LATEST_GLIBC_VERSION = 2;
    public static final byte LATEST_BIONIC_VERSION = 2;

    public static void installAllIfNeeded(final MainActivity activity) {
        FusionFSInstaller.installIfNeeded(activity);
    }

    public static void installCoreIfNeeded(final MainActivity activity) {
    }

    public static void installGlibcIfNeeded(final MainActivity activity) {
        FusionFSInstaller.installIfNeeded(activity);
    }

    public static void installBionicIfNeeded(final MainActivity activity) {
        FusionFSInstaller.installIfNeeded(activity);
    }

    public static void installBionic(final MainActivity activity) {
        FusionFSInstaller.install(activity);
    }

    public static void installGlibc(final MainActivity activity) {
        FusionFSInstaller.install(activity);
    }

    public static boolean isModularAssetsAvailable(Context context) {
        return FusionFSInstaller.isFusionAssetAvailable(context);
    }
}
