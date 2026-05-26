package com.winlator.fusion.xenvironment;

import android.content.Context;

import com.winlator.fusion.MainActivity;
import com.winlator.fusion.R;
import com.winlator.fusion.container.Container;
import com.winlator.fusion.container.ContainerManager;
import com.winlator.fusion.contents.AdrenotoolsManager;
import com.winlator.fusion.core.AppUtils;
import com.winlator.fusion.core.DownloadProgressDialog;
import com.winlator.fusion.core.FileUtils;
import com.winlator.fusion.core.TarCompressorUtils;
import com.winlator.fusion.core.WineInfo;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public abstract class FusionFSInstaller {
    public static final byte LATEST_VERSION = 5;

    private static boolean assetExists(Context context, String assetName) {
        try {
            context.getAssets().open(assetName).close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static long getAssetSize(Context context, String assetName) {
        try {
            return FileUtils.getSize(context, assetName);
        } catch (Exception e) {
            return 0;
        }
    }

    public static boolean isFusionAssetAvailable(Context context) {
        if (assetExists(context, FusionFS.ASSET_FUSIONFS)) {
            long size = getAssetSize(context, FusionFS.ASSET_FUSIONFS);
            return size >= 100_000_000L;
        }
        return false;
    }

    public static boolean isAnyAssetsAvailable(Context context) {
        return isFusionAssetAvailable(context);
    }

    public static void install(final MainActivity activity) {
        if (isFusionAssetAvailable(activity)) {
            installFromFusionAsset(activity);
        } else {
            ensureMinimalFusionFSStructure(activity);
        }
    }

    public static void installIfNeeded(final MainActivity activity) {
        FusionFS fusionFS = FusionFS.find(activity);
        if (!fusionFS.isValid() || fusionFS.getVersion() < LATEST_VERSION) {
            install(activity);
        } else {
            ensureMinimalFusionFSStructure(activity);
        }
    }

    private static void installFromFusionAsset(final MainActivity activity) {
        AppUtils.keepScreenOn(activity);
        FusionFS fusionFS = FusionFS.find(activity);
        File rootDir = fusionFS.getRootDir();

        final DownloadProgressDialog dialog = new DownloadProgressDialog(activity);
        dialog.show(R.string.installing_system_files);
        dialog.setShowStatus(true);

        Executors.newSingleThreadExecutor().execute(() -> {
            clearFusionDir(rootDir);
            rootDir.mkdirs();

            long contentLength = 100000000L;
            try {
                long rawSize = getAssetSize(activity, FusionFS.ASSET_FUSIONFS);
                if (rawSize > 0) contentLength = (long)(rawSize * 4.0f);
            } catch (Exception e) {}
            if (contentLength <= 0) contentLength = 100000000L;
            final long totalContentLength = contentLength;
            AtomicLong totalSizeRef = new AtomicLong();

            boolean success = false;
            try {
                success = TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, activity, FusionFS.ASSET_FUSIONFS, rootDir, (file, size) -> {
                    if (size > 0) {
                        long totalSize = totalSizeRef.addAndGet(size);
                        final int progress = Math.min((int)(((float)totalSize / totalContentLength) * 70), 70);
                        if (!activity.isFinishing() && !activity.isDestroyed()) activity.runOnUiThread(() -> {
                            if (!activity.isFinishing() && !activity.isDestroyed()) dialog.setProgress(progress);
                        });
                    }
                    return file;
                });
            } catch (Exception e) { success = false; }

            if (success) {
                try {
                    renameExtractedDirs(rootDir);
                    applyPatches(activity, rootDir);
                    copySharedRuntimeLibraries(fusionFS);
                    if (!activity.isFinishing() && !activity.isDestroyed()) activity.runOnUiThread(() -> {
                        if (!activity.isFinishing() && !activity.isDestroyed()) dialog.setProgress(75);
                    });
                    createWineSymlink(fusionFS);
                    createCompatibilitySymlinks(activity, fusionFS);
                    installWineFromAssets(activity, fusionFS, dialog);
                    installContainerPatternsFromAssets(activity, fusionFS);
                    if (!activity.isFinishing() && !activity.isDestroyed()) activity.runOnUiThread(() -> {
                        if (!activity.isFinishing() && !activity.isDestroyed()) dialog.setProgress(95);
                    });
                    installDriversFromAssets(activity);
                    fusionFS.createVersionFile(LATEST_VERSION);
                    try {
                        resetContainerVersions(activity);
                    } catch (Exception e) {
                        android.util.Log.w("FusionFSInstaller", "Failed to reset container versions", e);
                    }
                    if (!activity.isFinishing() && !activity.isDestroyed()) activity.runOnUiThread(() -> {
                        if (!activity.isFinishing() && !activity.isDestroyed()) dialog.setProgress(100);
                    });
                } catch (Exception e) {
                    success = false;
                }
            }

            if (!success) {
                if (!activity.isFinishing() && !activity.isDestroyed()) {
                    activity.runOnUiThread(() -> AppUtils.showToast(activity, R.string.unable_to_install_system_files));
                }
            }
            if (!activity.isFinishing() && !activity.isDestroyed()) dialog.closeOnUiThread();
        });
    }

    public static void installWineFromAssets(final MainActivity activity, FusionFS fusionFS, DownloadProgressDialog dialog) {
        String[] versions = activity.getResources().getStringArray(R.array.wine_entries);
        int totalVersions = versions.length;

        for (int i = 0; i < versions.length; i++) {
            String version = versions[i];
            if (isWineVersionInstalled(fusionFS, version)) continue;

            boolean isProton = version.startsWith("proton-");
            boolean isArm64EC = version.endsWith("-arm64ec");
            boolean installed = false;

            int baseProgress = 75;
            int versionProgress = baseProgress + (int)(((float)(i + 1) / totalVersions) * 20);

            if (assetExists(activity, version + ".txz")) {
                File outFile = getWineInstallDir(fusionFS, version, isProton, isArm64EC);
                outFile.mkdirs();
                installed = TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, activity, version + ".txz", outFile);
            }
            if (!installed && assetExists(activity, version + ".tzst")) {
                File outFile = getWineInstallDir(fusionFS, version, isProton, isArm64EC);
                outFile.mkdirs();
                installed = TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, activity, version + ".tzst", outFile);
            }

            if (!installed) {
                File outFile = getWineInstallDir(fusionFS, version, isProton, isArm64EC);
                if (outFile.isDirectory() && outFile.list() != null && outFile.list().length == 0) {
                    FileUtils.delete(outFile);
                }
            }

            if (dialog != null && !activity.isFinishing() && !activity.isDestroyed()) {
                int progress = Math.min(versionProgress, 94);
                activity.runOnUiThread(() -> {
                    if (!activity.isFinishing() && !activity.isDestroyed()) dialog.setProgress(progress);
                });
            }
        }
    }

    public static void installWineFromAssets(final MainActivity activity, FusionFS fusionFS) {
        installWineFromAssets(activity, fusionFS, null);
    }

    private static File getWineInstallDir(FusionFS fusionFS, String version, boolean isProton, boolean isArm64EC) {
        if (isProton || isArm64EC) {
            return new File(fusionFS.getBionicDir(), "/opt/" + version);
        }
        if (WineInfo.isMainWineVersion(version)) {
            return fusionFS.getWineDir();
        }
        return new File(fusionFS.getInstalledWineDir(), version);
    }

    public static void installContainerPatternsFromAssets(final Context context, FusionFS fusionFS) {
        String[] versions = context.getResources().getStringArray(R.array.wine_entries);

        for (String version : versions) {
            String patternAsset = WineInfo.getContainerPatternAssetName(version);
            if (!assetExists(context, patternAsset)) continue;

            File installedWineDir = fusionFS.getInstalledWineDir();
            File patternFile = new File(installedWineDir, patternAsset);

            if (patternFile.exists()) continue;

            try {
                FileUtils.copy(context, patternAsset, patternFile);
            } catch (Exception e) {}
        }
    }

    private static boolean isWineVersionInstalled(FusionFS fusionFS, String version) {
        boolean isProton = version.startsWith("proton-");
        boolean isArm64EC = version.endsWith("-arm64ec");

        if (isProton || isArm64EC) {
            File outFile = new File(fusionFS.getBionicDir(), "/opt/" + version);
            return outFile.isDirectory() && new File(outFile, "bin").isDirectory();
        }
        if (WineInfo.isMainWineVersion(version)) {
            return fusionFS.isWineInstalled();
        }
        File outFile = new File(fusionFS.getInstalledWineDir(), version);
        return outFile.isDirectory() && new File(outFile, "bin").isDirectory();
    }

    public static void installDriversFromAssets(final MainActivity activity) {
        AdrenotoolsManager adrenotoolsManager = new AdrenotoolsManager(activity);
        String[] adrenotoolsAssetDrivers = activity.getResources().getStringArray(R.array.wrapper_graphics_driver_version_entries);
        for (String driver : adrenotoolsAssetDrivers)
            adrenotoolsManager.extractDriverFromResources(driver);
    }

    public static boolean hasProtonInstalled(Context context) {
        FusionFS fusionFS = FusionFS.find(context);
        File bionicOptDir = new File(fusionFS.getBionicDir(), "/opt");
        File[] optFiles = bionicOptDir.listFiles();
        if (optFiles != null) {
            for (File f : optFiles) {
                if (f.isDirectory() && f.getName().startsWith("proton") && new File(f, "bin").isDirectory()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isBionicAvailable(Context context) {
        return isBionicAvailable(context, false);
    }

    public static boolean isBionicAvailable(Context context, boolean requireProtonBinaries) {
        FusionFS fusionFS = FusionFS.find(context);

        if (fusionFS.isBionicInstalled()) {
            if (requireProtonBinaries) {
                return hasProtonInstalled(context);
            }
            return true;
        }

        if (isFusionAssetAvailable(context)) return true;

        return false;
    }

    public static void ensureMinimalFusionFSStructure(Context context) {
        FusionFS fusionFS = FusionFS.find(context);
        File bionicDir = fusionFS.getBionicDir();
        File glibcDir = fusionFS.getGlibcDir();
        File wineDir = fusionFS.getWineDir();

        new File(bionicDir, "/usr/bin").mkdirs();
        new File(bionicDir, "/usr/lib").mkdirs();
        new File(bionicDir, "/usr/tmp").mkdirs();
        new File(bionicDir, "/usr/etc/fonts").mkdirs();
        new File(bionicDir, "/usr/etc/xdg").mkdirs();
        new File(bionicDir, "/usr/etc/alsa/conf.d").mkdirs();
        new File(bionicDir, "/usr/etc/tls").mkdirs();
        new File(bionicDir, "/usr/share/alsa").mkdirs();
        new File(bionicDir, "/usr/share/vulkan/icd.d").mkdirs();
        new File(bionicDir, "/usr/share/vulkan/implicit_layer.d").mkdirs();
        new File(bionicDir, "/usr/share/vulkan/explicit_layer.d").mkdirs();
        new File(bionicDir, "/usr/lib/gstreamer-1.0").mkdirs();
        new File(bionicDir, "/usr/lib/alsa-lib").mkdirs();
        new File(bionicDir, "/home").mkdirs();
        new File(bionicDir, "/opt").mkdirs();
        new File(bionicDir, "/dev/input").mkdirs();
        new File(bionicDir, "/var/cache").mkdirs();
        new File(bionicDir, "/etc").mkdirs();
        new File(bionicDir, "/tmp").mkdirs();

        new File(glibcDir, "/usr/lib").mkdirs();
        new File(glibcDir, "/usr/lib/x86_64-linux-gnu").mkdirs();
        new File(glibcDir, "/usr/local/bin").mkdirs();
        new File(glibcDir, "/usr/bin").mkdirs();
        new File(glibcDir, "/usr/etc").mkdirs();
        new File(glibcDir, "/usr/share/fonts").mkdirs();
        new File(glibcDir, "/tmp").mkdirs();
        new File(glibcDir, "/home").mkdirs();
        new File(glibcDir, "/opt").mkdirs();
        new File(glibcDir, "/etc").mkdirs();

        createWineSymlink(fusionFS);
        createCompatibilitySymlinks(context, fusionFS);

        boolean hasProton = hasProtonInstalled(context);
        boolean hasWine = fusionFS.isWineInstalled();

        if (!fusionFS.isValid()) {
            fusionFS.createVersionFile((hasProton || hasWine) ? LATEST_VERSION : 0);
        }

        if (context instanceof MainActivity) {
            installWineFromAssets((MainActivity) context, fusionFS);
            installContainerPatternsFromAssets(context, fusionFS);
            installDriversFromAssets((MainActivity) context);
            if ((hasProtonInstalled(context) || fusionFS.isWineInstalled()) && fusionFS.getVersion() == 0) {
                fusionFS.createVersionFile(LATEST_VERSION);
            }
        }
    }

    private static void renameExtractedDirs(File rootDir) {
        File rootfsDir = new File(rootDir, "rootfs");
        File glibcDir = new File(rootDir, "glibc");
        if (rootfsDir.isDirectory() && !glibcDir.isDirectory()) {
            rootfsDir.renameTo(glibcDir);
        }

        File imagefsDir = new File(rootDir, "imagefs");
        File bionicDir = new File(rootDir, "bionic");
        if (imagefsDir.isDirectory() && !bionicDir.isDirectory()) {
            imagefsDir.renameTo(bionicDir);
        }

        populateEtcBionicIfNeeded(rootDir);
        ensureBionicSymlinks(rootDir);
    }

    private static void populateEtcBionicIfNeeded(File rootDir) {
        File etcBionic = new File(rootDir, "etc.bionic");
        File usrBionicEtc = new File(rootDir, "usr.bionic/etc");
        String[] etcBionicContents = etcBionic.list();
        if (etcBionic.isDirectory() && etcBionicContents != null && etcBionicContents.length == 0) {
            if (usrBionicEtc.isDirectory()) {
                try {
                    com.winlator.fusion.core.FileUtils.copy(usrBionicEtc, etcBionic);
                } catch (Exception e) {
                    android.util.Log.e("FusionFSInstaller", "Failed to copy etc.bionic from " + usrBionicEtc + " to " + etcBionic, e);
                }
            }
        }
    }

    private static void ensureBionicSymlinks(File rootDir) {
        File bionicDir = new File(rootDir, "bionic");
        if (!bionicDir.isDirectory()) return;

        String[][] symlinks = {
            {"usr", "../usr.bionic"},
            {"etc", "../etc.bionic"},
            {"bin", "usr/bin"},
            {"lib", "usr/lib"},
            {"share", "usr/share"},
            {"tmp", "usr/tmp"},
            {"var", "usr/var"}
        };

        for (String[] entry : symlinks) {
            File link = new File(bionicDir, entry[0]);
            if (!link.exists()) {
                FileUtils.symlink(entry[1], link.getAbsolutePath());
            }
        }

        ensureGlibcSymlinks(rootDir);
    }

    private static void ensureGlibcSymlinks(File rootDir) {
        File glibcDir = new File(rootDir, "glibc");
        if (!glibcDir.isDirectory()) return;

        String[][] symlinks = {
            {"usr", "../usr.glibc"},
            {"etc", "../etc.glibc"}
        };

        for (String[] entry : symlinks) {
            File link = new File(glibcDir, entry[0]);
            if (!link.exists()) {
                FileUtils.symlink(entry[1], link.getAbsolutePath());
            }
        }
    }

    private static void applyPatches(Context context, File rootDir) {
        try {
            if (assetExists(context, FusionFS.ASSET_CONTAINER_PATTERN_COMMON)) {
                TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context, FusionFS.ASSET_CONTAINER_PATTERN_COMMON, rootDir);
            }
        } catch (Exception e) {}
        try {
            if (assetExists(context, FusionFS.ASSET_FUSIONFS_PATCHES)) {
                TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context, FusionFS.ASSET_FUSIONFS_PATCHES, rootDir);
            }
        } catch (Exception e) {}
    }

    private static void copySharedRuntimeLibraries(FusionFS fusionFS) {
        File bionicLibDir = new File(fusionFS.getBionicDir(), "/usr/lib");
        File glibcLibDir = new File(fusionFS.getGlibcDir(), "/usr/lib");
        if (!glibcLibDir.isDirectory()) glibcLibDir.mkdirs();

        String[] sharedLibs = {"libandroid-sysvshm.so"};
        for (String libName : sharedLibs) {
            File src = new File(bionicLibDir, libName);
            File dst = new File(glibcLibDir, libName);
            if (src.exists() && !dst.exists()) {
                FileUtils.copy(src, dst);
            }
        }
    }

    private static void createCompatibilitySymlinks(Context context, FusionFS fusionFS) {
        File filesDir = context.getFilesDir();
        File imagefsLink = new File(filesDir, "imagefs");
        File rootfsLink = new File(filesDir, "rootfs");

        if (!imagefsLink.exists()) {
            FileUtils.symlink(fusionFS.getBionicDir().getAbsolutePath(), imagefsLink.getAbsolutePath());
        }
        if (!rootfsLink.exists()) {
            FileUtils.symlink(fusionFS.getGlibcDir().getAbsolutePath(), rootfsLink.getAbsolutePath());
        }
    }

    private static void createWineSymlink(FusionFS fusionFS) {
        File glibcOptWine = new File(fusionFS.getGlibcDir(), "/opt/wine");
        File wineDir = fusionFS.getWineDir();
        if (wineDir.isDirectory() && !glibcOptWine.exists()) {
            glibcOptWine.getParentFile().mkdirs();
            File wineGlibcDir = fusionFS.getWineGlibcDir();
            String target = wineGlibcDir.isDirectory() ? "../../wine.glibc" : "../../wine";
            FileUtils.symlink(target, glibcOptWine.getPath());
        }

        File bionicOptWine = new File(fusionFS.getBionicDir(), "/opt/wine");
        File wineBionicDir = fusionFS.getWineBionicDir();
        if (wineBionicDir.isDirectory() && !bionicOptWine.exists()) {
            bionicOptWine.getParentFile().mkdirs();
            FileUtils.symlink("../../wine.bionic", bionicOptWine.getPath());
        }
    }

    private static void resetContainerVersions(Context context) {
        ContainerManager manager = new ContainerManager(context);
        for (Container container : manager.getContainers()) {
            String imgVersion = container.getExtra("imgVersion");
            String rfsVersion = container.getExtra("rfsVersion");
            if (!imgVersion.isEmpty() || !rfsVersion.isEmpty()) {
                container.putExtra("imgVersion", null);
                container.putExtra("rfsVersion", null);
                container.putExtra("fusionfsVersion", String.valueOf(LATEST_VERSION));
                container.saveData();
            }
        }
    }

    private static void clearFusionDir(File rootDir) {
        if (rootDir.isDirectory()) {
            File[] files = rootDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        String name = file.getName();
                        if (name.equals("home") || name.equals("opt") || name.equals("installed-wine") || name.equals("bionic") || name.equals("glibc") || name.equals("wine") || name.equals("wine.glibc") || name.equals("wine.bionic") || name.equals("usr.glibc") || name.equals("usr.bionic") || name.equals("etc.glibc") || name.equals("etc.bionic")) continue;
                    }
                    FileUtils.delete(file);
                }
            }
        } else {
            rootDir.mkdirs();
        }
    }
}
