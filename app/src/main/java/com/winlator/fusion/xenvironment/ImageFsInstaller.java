package com.winlator.fusion.xenvironment;

import android.content.Context;

import com.winlator.fusion.MainActivity;

public abstract class ImageFsInstaller {
    public static final byte LATEST_VERSION = 2;

    public static void installWineFromAssets(final MainActivity activity) {
        FusionFS fusionFS = FusionFS.find(activity);
        FusionFSInstaller.installWineFromAssets(activity, fusionFS);
    }

    public static void installDriversFromAssets(final MainActivity activity) {
        FusionFSInstaller.installDriversFromAssets(activity);
    }

    public static boolean isBionicAvailable(Context context) {
        return FusionFSInstaller.isBionicAvailable(context);
    }

    public static boolean isBionicAvailable(Context context, boolean requireProtonBinaries) {
        return FusionFSInstaller.isBionicAvailable(context, requireProtonBinaries);
    }

    public static boolean hasProtonInstalled(Context context) {
        return FusionFSInstaller.hasProtonInstalled(context);
    }

    public static boolean installFromAssets(final MainActivity activity) {
        FusionFSInstaller.install(activity);
        return true;
    }

    public static void installIfNeeded(final MainActivity activity) {
        FusionFSInstaller.installIfNeeded(activity);
    }

    public static void ensureMinimalImageFsStructure(Context context) {
        FusionFSInstaller.ensureMinimalFusionFSStructure(context);
    }
}
