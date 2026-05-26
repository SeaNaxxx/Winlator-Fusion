package com.winlator.fusion.xenvironment;

import android.content.Context;

import androidx.annotation.NonNull;

import com.winlator.fusion.container.Container;
import com.winlator.fusion.core.FileUtils;
import com.winlator.fusion.core.WineInfo;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class RuntimeFS {
    public static final String USER = "xuser";
    public static final String HOME_PATH = "/home/" + USER;
    public static final String USER_CACHE_PATH = "/home/" + USER + "/.cache";
    public static final String USER_CONFIG_PATH = "/home/" + USER + "/.config";
    public static final String WINEPREFIX = "/home/" + USER + "/.wine";

    public static final String ASSET_CORE = "runtimefs_core.tzst";
    public static final String ASSET_GLIBC = "runtimefs_glibc.tzst";
    public static final String ASSET_BIONIC = "runtimefs_bionic.tzst";

    private final File coreDir;
    private final FusionFS fusionFS;
    private final RootFS rootFS;
    private final ImageFs imageFs;

    private RuntimeFS(File coreDir, FusionFS fusionFS) {
        this.coreDir = coreDir;
        this.fusionFS = fusionFS;
        this.rootFS = RootFS.fromDir(fusionFS.getGlibcDir());
        this.imageFs = ImageFs.find(fusionFS.getBionicDir());
    }

    public static RuntimeFS find(Context context) {
        File filesDir = context.getFilesDir();
        FusionFS fusionFS = FusionFS.find(context);
        return new RuntimeFS(
            new File(filesDir, "runtimefs_core"),
            fusionFS
        );
    }

    public File getCoreDir() {
        return coreDir;
    }

    public File getGlibcDir() {
        return fusionFS.getGlibcDir();
    }

    public File getBionicDir() {
        return fusionFS.getBionicDir();
    }

    public FusionFS getFusionFS() {
        return fusionFS;
    }

    public RootFS getRootFS() {
        return rootFS;
    }

    public ImageFs getImageFs() {
        return imageFs;
    }

    public File getDirForVariant(String variant) {
        return fusionFS.getDirForVariant(variant);
    }

    public File getCoreLibDir() {
        return new File(coreDir, "/usr/lib");
    }

    public File getCoreShareDir() {
        return new File(coreDir, "/usr/share");
    }

    public File getCoreEtcDir() {
        return new File(coreDir, "/usr/etc");
    }

    public boolean isCoreInstalled() {
        return coreDir.isDirectory() && new File(coreDir, ".winlator/.rfs_core_version").exists();
    }

    public int getCoreVersion() {
        try {
            File versionFile = new File(coreDir, ".winlator/.rfs_core_version");
            if (versionFile.exists()) return Integer.parseInt(FileUtils.readLines(versionFile).get(0));
        } catch (Exception e) {}
        return 0;
    }

    public void createCoreVersionFile(int version) {
        File dir = new File(coreDir, ".winlator");
        dir.mkdirs();
        File file = new File(dir, ".rfs_core_version");
        try {
            file.createNewFile();
            FileUtils.writeString(file, String.valueOf(version));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isGlibcInstalled() {
        return rootFS.isValid();
    }

    public int getGlibcVersion() {
        return rootFS.getVersion();
    }

    public boolean isBionicInstalled() {
        return imageFs.isValid();
    }

    public int getBionicVersion() {
        return imageFs.getVersion();
    }

    public boolean isFusionInstalled() {
        return fusionFS.isValid();
    }

    public int getFusionVersion() {
        return fusionFS.getVersion();
    }

    public File getHomeDirForVariant(String variant) {
        return new File(getDirForVariant(variant), HOME_PATH);
    }

    public String getHomePathForVariant(String variant) {
        return fusionFS.getHomePathForVariant(variant);
    }

    public String getWinePrefixForVariant(String variant) {
        return fusionFS.getWinePrefixForVariant(variant);
    }

    public String getTmpDirForVariant(String variant) {
        if (Container.BIONIC.equals(variant)) {
            return fusionFS.getBionicDir() + "/usr/tmp";
        }
        return fusionFS.getGlibcDir() + "/tmp";
    }

    public File getLibDirForVariant(String variant) {
        if (Container.BIONIC.equals(variant)) {
            return imageFs.getLibDir();
        }
        return rootFS.getLibDir();
    }

    public File getBinDirForVariant(String variant) {
        if (Container.BIONIC.equals(variant)) {
            return imageFs.getBinDir();
        }
        return new File(fusionFS.getGlibcDir(), "/usr/local/bin");
    }

    public String getWinePathForVariant(String variant) {
        if (Container.BIONIC.equals(variant)) {
            return imageFs.getWinePath();
        }
        return fusionFS.getWineDir().getPath();
    }

    public String getWinePathForVersion(String variant, String wineVersion) {
        if (Container.BIONIC.equals(variant)) {
            return fusionFS.getWinePathForVersion(wineVersion);
        }
        if (WineInfo.isMainWineVersion(wineVersion)) {
            return fusionFS.getWineDir().getPath();
        }
        File installedWineDir = fusionFS.getInstalledWineDir();
        File winePath = new File(installedWineDir, wineVersion);
        if (winePath.isDirectory()) return winePath.getPath();
        return fusionFS.getWineDir().getPath();
    }

    public String getBox64PathForVariant(String variant) {
        return fusionFS.getBox64PathForVariant(variant);
    }

    public String getBox64LdLibraryPathForVariant(String variant) {
        if (Container.BIONIC.equals(variant)) {
            return "";
        }
        return fusionFS.getBox64LdLibraryPathForGlibc();
    }

    public String getLdLibraryPathForGlibc() {
        return fusionFS.getLdLibraryPathForGlibc();
    }

    public String getLdLibraryPathForBionic() {
        return fusionFS.getLdLibraryPathForBionic();
    }

    public String getLdLibraryPathForVariant(String variant) {
        return fusionFS.getLdLibraryPathForVariant(variant);
    }

    public String getPathForGlibc(String winePath) {
        return fusionFS.getPathForGlibc();
    }

    public String getPathForBionic(String wineBinaryPath) {
        return fusionFS.getPathForBionic(wineBinaryPath);
    }

    public String getVulkanLayerPath(File variantDir) {
        return fusionFS.getVulkanLayerPath(variantDir);
    }

    public String getVulkanLayerPathForVariant(String variant) {
        return fusionFS.getVulkanLayerPathForVariant(variant);
    }

    @NonNull
    @Override
    public String toString() {
        return "RuntimeFS[core=" + coreDir + ", fusion=" + fusionFS + "]";
    }
}
