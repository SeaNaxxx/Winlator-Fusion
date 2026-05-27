package com.winlator.fusion.xenvironment;

import android.content.Context;

import androidx.annotation.NonNull;

import com.winlator.fusion.core.FileUtils;
import com.winlator.fusion.core.WineInfo;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class ImageFs {
    public static final String USER = "xuser";
    public static final String HOME_PATH = "/home/"+USER;
    public static final String CACHE_PATH = HOME_PATH+"/.cache";
    public static final String CONFIG_PATH = HOME_PATH+"/.config";
    public static final String WINEPREFIX = HOME_PATH+"/.wine";
    private final File rootDir;
    private String winePath;
    public String home_path;
    public String cache_path;
    public String config_path;
    public String wineprefix;

    private ImageFs(File rootDir) {
        this.rootDir = rootDir;
        this.winePath = rootDir + "/opt/" + WineInfo.BIONIC_WINE_IDENTIFIER;
        this.home_path = rootDir + HOME_PATH;
        this.cache_path = rootDir + CACHE_PATH;
        this.config_path = rootDir + CONFIG_PATH;
        this.wineprefix = rootDir + WINEPREFIX;
    }

    public static ImageFs find(Context context) {
        FusionFS fusionFS = FusionFS.find(context);
        return new ImageFs(fusionFS.getBionicDir());
    }

    public static ImageFs find(File rootDir) {
        return new ImageFs(rootDir);
    }

    public File getRootDir() {
        return rootDir;
    }

    public boolean isValid() {
        return rootDir.isDirectory() && (getImgVersionFile().exists() || new File(rootDir, "usr/bin").isDirectory());
    }

    public int getVersion() {
        try {
            File imgVersionFile = getImgVersionFile();
            if (imgVersionFile.exists()) return Integer.parseInt(FileUtils.readLines(imgVersionFile).get(0));
            FusionFS fusionFS = FusionFS.fromDir(rootDir.getParentFile());
            if (fusionFS.isValid()) return fusionFS.getVersion();
        } catch (Exception e) {}
        return 0;
    }

    public String getFormattedVersion() {
        return String.format(Locale.ENGLISH, "%.1f", (float)getVersion());
    }

    public void createImgVersionFile(int version) {
        getConfigDir().mkdirs();
        File file = getImgVersionFile();
        try {
            file.createNewFile();
            FileUtils.writeString(file, String.valueOf(version));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getWinePath() {
        return winePath;
    }

    public void setWinePath(String winePath) {
        this.winePath = winePath;
    }

    public String getWinePathForVersion(String wineVersion) {
        File optDir = new File(rootDir, "opt/" + wineVersion);
        if (optDir.isDirectory()) return optDir.getPath();

        FusionFS fusionFS = FusionFS.fromDir(rootDir.getParentFile());
        if (fusionFS.isValid()) {
            return fusionFS.getWinePathForVersion(wineVersion);
        }
        return rootDir + "/opt/" + wineVersion;
    }

    public File getHomeDir() {
        return new File(home_path);
    }

    public File getCacheDir() {
        return new File(cache_path);
    }

    public File getConfigPathDir() {
        return new File(config_path);
    }

    public File getConfigDir() {
        return new File(rootDir, ".winlator");
    }

    public File getImgVersionFile() {
        return new File(getConfigDir(), ".img_version");
    }

    public File getVariantFile() {
        return new File(getConfigDir(), ".variant");
    }

    public String getVariant() {
        File variantFile = getVariantFile();
        if (variantFile.exists()) {
            try {
                return FileUtils.readLines(variantFile).get(0);
            } catch (Exception e) {
            }
        }
        if (new File(rootDir, "etc/ld.so.cache").exists() || new File(rootDir, "usr/local/bin").exists()) {
            return "glibc";
        }
        return "bionic";
    }

    public void createVariantFile(String variant) {
        getConfigDir().mkdirs();
        try {
            File file = getVariantFile();
            file.createNewFile();
            FileUtils.writeString(file, variant);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public File getInstalledWineDir() {
        FusionFS fusionFS = FusionFS.fromDir(rootDir.getParentFile());
        return fusionFS.getInstalledWineDir();
    }

    public File getTmpDir() {
        return new File(rootDir, "usr/tmp");
    }

    public File getLibDir() {
        return new File(rootDir, "usr/lib");
    }

    public File getBinDir() {
        return new File(rootDir, "usr/bin");
    }

    public File getEtcDir() {
        return new File(rootDir, "usr/etc");
    }

    public File getShareDir() {
        return new File(rootDir, "usr/share");
    }

    @NonNull
    @Override
    public String toString() {
        return rootDir.getPath();
    }
}
