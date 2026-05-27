package com.winlator.fusion.xenvironment;

import android.content.Context;

import androidx.annotation.NonNull;

import com.winlator.fusion.container.Container;
import com.winlator.fusion.core.FileUtils;
import com.winlator.fusion.core.WineInfo;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class FusionFS {
    public static final String USER = "xuser";
    public static final String HOME_PATH = "/home/" + USER;
    public static final String CACHE_PATH = HOME_PATH + "/.cache";
    public static final String CONFIG_PATH = HOME_PATH + "/.config";
    public static final String WINEPREFIX = HOME_PATH + "/.wine";

    public static final String ASSET_FUSIONFS = "fusionfs.tzst";
    public static final String ASSET_CONTAINER_PATTERN_COMMON = "container_pattern_common.tzst";
    public static final String ASSET_PULSEAUDIO = "pulseaudio.tzst";
    public static final String ASSET_PULSEAUDIO_FULL = "pulseaudio-full.tzst";

    private final File rootDir;
    private final File bionicDir;
    private final File glibcDir;
    private final File wineDir;
    private final File wineGlibcDir;
    private final File wineBionicDir;

    private FusionFS(File rootDir) {
        this.rootDir = rootDir;
        this.bionicDir = new File(rootDir, "bionic");
        this.glibcDir = new File(rootDir, "glibc");
        this.wineGlibcDir = new File(rootDir, "wine.glibc");
        this.wineBionicDir = new File(rootDir, "wine.bionic");
        File legacyWineDir = new File(rootDir, "wine");
        this.wineDir = (wineGlibcDir.isDirectory() && new File(wineGlibcDir, "bin").isDirectory()) ? wineGlibcDir : legacyWineDir;
    }

    public static FusionFS find(Context context) {
        return new FusionFS(new File(context.getFilesDir(), "fusionfs"));
    }

    public static FusionFS fromDir(File rootDir) {
        return new FusionFS(rootDir);
    }

    public File getRootDir() {
        return rootDir;
    }

    public File getBionicDir() {
        return bionicDir;
    }

    public File getGlibcDir() {
        return glibcDir;
    }

    public File getWineDir() {
        if (wineGlibcDir.isDirectory() && new File(wineGlibcDir, "bin").isDirectory()) return wineGlibcDir;
        if (wineBionicDir.isDirectory() && new File(wineBionicDir, "bin").isDirectory()) return wineBionicDir;
        return wineDir;
    }

    public File getWineGlibcDir() {
        return wineGlibcDir;
    }

    public File getWineBionicDir() {
        return wineBionicDir;
    }

    public boolean isValid() {
        return rootDir.isDirectory() && bionicDir.isDirectory() && (getVersionFile().exists() || getLegacyVersionFile().exists());
    }

    public boolean isFullyInstalled() {
        if (!isValid()) return false;
        return (isGlibcInstalled() || isBionicInstalled()) && isWineInstalled();
    }

    public int getVersion() {
        try {
            File versionFile = getVersionFile();
            if (versionFile.exists()) return Integer.parseInt(FileUtils.readLines(versionFile).get(0));
            File legacyFile = getLegacyVersionFile();
            if (legacyFile.exists()) return Integer.parseInt(FileUtils.readLines(legacyFile).get(0));
        } catch (Exception e) {}
        return 0;
    }

    public String getFormattedVersion() {
        return String.format(Locale.ENGLISH, "%.1f", (float) getVersion());
    }

    public void createVersionFile(int version) {
        getConfigDir().mkdirs();
        File file = getVersionFile();
        try {
            file.createNewFile();
            FileUtils.writeString(file, String.valueOf(version));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public File getConfigDir() {
        return new File(rootDir, ".winlator");
    }

    public File getVersionFile() {
        return new File(getConfigDir(), ".ffs_version");
    }

    public File getLegacyVersionFile() {
        return new File(getConfigDir(), ".fusionfs_version");
    }

    public boolean isBionicInstalled() {
        return bionicDir.isDirectory() && new File(bionicDir, "usr/bin").isDirectory();
    }

    public boolean isGlibcInstalled() {
        return glibcDir.isDirectory() && new File(glibcDir, "usr/lib").isDirectory();
    }

    public boolean isWineInstalled() {
        File dir = getWineDir();
        return dir.isDirectory() && new File(dir, "bin").isDirectory();
    }

    public boolean isBionicWineInstalled(String wineIdentifier) {
        File wineBin = new File(getBionicWinePathForVersion(wineIdentifier), "bin/wine");
        return wineBin.exists() && wineBin.canExecute();
    }

    private String getBionicWinePathForVersion(String wineVersion) {
        File installedWineDir = getInstalledWineDir();
        File optDir = new File(bionicDir, "opt/" + wineVersion);
        if (optDir.isDirectory()) return optDir.getPath();
        File versionDir = new File(installedWineDir, wineVersion);
        if (versionDir.isDirectory()) return versionDir.getPath();
        return wineBionicDir.getPath();
    }

    public String getWinePath() {
        return getWineDir().getPath();
    }

    public String getWinePathForVersion(String wineVersion) {
        if (WineInfo.isMainWineVersion(wineVersion)) {
            return getWineDir().getPath();
        }
        boolean isArm64EC = wineVersion != null && wineVersion.endsWith("-arm64ec");
        boolean isProton = wineVersion != null && wineVersion.startsWith("proton-");
        if (isProton || isArm64EC) {
            File optDir = new File(bionicDir, "opt/" + wineVersion);
            if (optDir.isDirectory()) return optDir.getPath();
        }
        File installedWineDir = getInstalledWineDir();
        File versionDir = new File(installedWineDir, wineVersion);
        if (versionDir.isDirectory()) return versionDir.getPath();
        if (!isProton && !isArm64EC) {
            return getWineDir().getPath();
        }
        return new File(bionicDir, "opt/" + wineVersion).getPath();
    }

    public File getInstalledWineDir() {
        return new File(rootDir, "installed-wine");
    }

    public File getHomeDir() {
        return new File(bionicDir, HOME_PATH);
    }

    public File getHomeDirForVariant(String variant) {
        return new File(getDirForVariant(variant), HOME_PATH);
    }

    public File getCacheDir() {
        return new File(bionicDir, CACHE_PATH);
    }

    public File getCacheDirForVariant(String variant) {
        return new File(getDirForVariant(variant), CACHE_PATH);
    }

    public File getConfigPathDir() {
        return new File(bionicDir, CONFIG_PATH);
    }

    public File getConfigPathDirForVariant(String variant) {
        return new File(getDirForVariant(variant), CONFIG_PATH);
    }

    public File getBionicLibDir() {
        return new File(bionicDir, "usr/lib");
    }

    public File getBionicBinDir() {
        return new File(bionicDir, "usr/bin");
    }

    public File getGlibcLibDir() {
        return new File(glibcDir, "usr/lib");
    }

    public File getGlibcBinDir() {
        return new File(glibcDir, "usr/bin");
    }

    public File getGlibcLocalBinDir() {
        return new File(glibcDir, "usr/local/bin");
    }

    public File getTmpDirForVariant(String variant) {
        if (Container.BIONIC.equals(variant)) {
            return new File(bionicDir, "usr/tmp");
        }
        return new File(glibcDir, "tmp");
    }

    public String getWinePrefixForVariant(String variant) {
        if (Container.BIONIC.equals(variant)) {
            return bionicDir + WINEPREFIX;
        }
        return glibcDir + WINEPREFIX;
    }

    public String getHomePathForVariant(String variant) {
        if (Container.BIONIC.equals(variant)) {
            return bionicDir + HOME_PATH;
        }
        return glibcDir + HOME_PATH;
    }

    public File getDirForVariant(String variant) {
        if (Container.BIONIC.equals(variant)) return bionicDir;
        return glibcDir;
    }

    public String getBox64PathForVariant(String variant) {
        if (Container.BIONIC.equals(variant)) {
            return bionicDir + "/usr/bin/box64";
        }
        return glibcDir + "/usr/local/bin/box64";
    }

    public String getLdLibraryPathForGlibc() {
        StringBuilder path = new StringBuilder();
        path.append(glibcDir).append("/usr/lib");
        path.append(":").append(glibcDir).append("/usr/lib/x86_64-linux-gnu");
        return path.toString();
    }

    public String getLdLibraryPathForBionic() {
        StringBuilder path = new StringBuilder();
        path.append(bionicDir).append("/usr/lib");
        path.append(":/system/lib64");
        return path.toString();
    }

    public String getLdLibraryPathForVariant(String variant) {
        if (Container.BIONIC.equals(variant)) {
            return getLdLibraryPathForBionic();
        }
        return getLdLibraryPathForGlibc();
    }

    public String getBox64LdLibraryPathForGlibc() {
        StringBuilder path = new StringBuilder();
        path.append(glibcDir).append("/usr/lib");
        path.append(":").append(glibcDir).append("/usr/lib/x86_64-linux-gnu");
        return path.toString();
    }

    public String getPathForGlibc() {
        StringBuilder path = new StringBuilder();
        path.append(getWineDir()).append("/bin");
        path.append(":").append(glibcDir).append("/usr/local/bin");
        path.append(":").append(glibcDir).append("/usr/bin");
        return path.toString();
    }

    public String getPathForBionic(String wineBinaryPath) {
        StringBuilder path = new StringBuilder();
        path.append(wineBinaryPath);
        path.append(":").append(bionicDir).append("/usr/bin");
        return path.toString();
    }

    public String getVulkanLayerPath(File variantDir) {
        StringBuilder path = new StringBuilder();
        path.append(variantDir).append("/usr/share/vulkan/implicit_layer.d");
        path.append(":").append(variantDir).append("/usr/share/vulkan/explicit_layer.d");
        return path.toString();
    }

    public String getVulkanLayerPathForVariant(String variant) {
        return getVulkanLayerPath(getDirForVariant(variant));
    }

    public File getEtcDirForVariant(String variant) {
        if (Container.BIONIC.equals(variant)) {
            return new File(bionicDir, "usr/etc");
        }
        return new File(glibcDir, "usr/etc");
    }

    public File getShareDirForVariant(String variant) {
        if (Container.BIONIC.equals(variant)) {
            return new File(bionicDir, "usr/share");
        }
        return new File(glibcDir, "usr/share");
    }

    @NonNull
    @Override
    public String toString() {
        return "FusionFS[root=" + rootDir + ", bionic=" + bionicDir + ", glibc=" + glibcDir + ", wine=" + wineDir + "]";
    }

}
