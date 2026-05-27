package com.winlator.fusion.xenvironment;

import android.content.Context;

import androidx.annotation.NonNull;

import com.winlator.fusion.core.FileUtils;
import com.winlator.fusion.core.WineInfo;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class RootFS {
    public static final String USER = "xuser";
    public static final String HOME_PATH = "/home/"+USER;
    public static final String USER_CACHE_PATH = "/home/"+USER+"/.cache";
    public static final String USER_CONFIG_PATH = "/home/"+USER+"/.config";
    public static final String WINEPREFIX = "/home/"+USER+"/.wine";
    private final File rootDir;
    private String winePath = "/opt/wine";

    private RootFS(File rootDir) {
        this.rootDir = rootDir;
    }

    public static RootFS find(Context context) {
        FusionFS fusionFS = FusionFS.find(context);
        return new RootFS(fusionFS.getGlibcDir());
    }

    public static RootFS fromDir(File rootDir) {
        return new RootFS(rootDir);
    }

    public File getRootDir() {
        return rootDir;
    }

    public boolean isValid() {
        return rootDir.isDirectory() && (getRFSVersionFile().exists() || new File(rootDir, "usr/lib").isDirectory());
    }

    public int getVersion() {
        try {
            File rfsVersionFile = getRFSVersionFile();
            if (rfsVersionFile.exists()) return Integer.parseInt(FileUtils.readLines(rfsVersionFile).get(0));
            File imgVersionFile = new File(getImageInfoDir(), ".img_version");
            if (imgVersionFile.exists()) return Integer.parseInt(FileUtils.readLines(imgVersionFile).get(0));
            FusionFS fusionFS = FusionFS.fromDir(rootDir.getParentFile());
            if (fusionFS.isValid()) return fusionFS.getVersion();
        } catch (Exception e) {}
        return 0;
    }

    public String getFormattedVersion() {
        return String.format(Locale.ENGLISH, "%.1f", (float)getVersion());
    }

    public void createRFSVersionFile(int version) {
        getImageInfoDir().mkdirs();
        File file = getRFSVersionFile();
        try {
            file.createNewFile();
            FileUtils.writeString(file, String.valueOf(version));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getWinePath() {
        FusionFS fusionFS = FusionFS.fromDir(rootDir.getParentFile());
        if (fusionFS.getWineDir().isDirectory()) {
            return FileUtils.toRelativePath(rootDir.getPath(), fusionFS.getWineDir().getPath());
        }
        return winePath;
    }

    public void setWinePath(String winePath) {
        this.winePath = FileUtils.toRelativePath(rootDir.getPath(), winePath);
    }

    private File getImageInfoDir() {
        return new File(rootDir, ".winlator");
    }

    public File getRFSVersionFile() {
        return new File(getImageInfoDir(), ".rfs_version");
    }

    public File getInstalledWineDir() {
        FusionFS fusionFS = FusionFS.fromDir(rootDir.getParentFile());
        return fusionFS.getInstalledWineDir();
    }

    public File getTmpDir() {
        return new File(rootDir, "tmp");
    }

    public File getLibDir() {
        return new File(rootDir, "usr/lib");
    }

    public File getBinDir() {
        return new File(rootDir, "usr/bin");
    }

    public File getEtcDir() {
        File usrEtc = new File(rootDir, "usr/etc");
        if (usrEtc.isDirectory()) return usrEtc;
        return new File(rootDir, "etc");
    }

    public File getShareDir() {
        return new File(rootDir, "usr/share");
    }

    @NonNull
    @Override
    public String toString() {
        return rootDir.getPath();
    }

    public static String getDosUserCachePath() {
        return "Z:"+USER_CACHE_PATH.replace("/", "\\");
    }

    public static String getDosUserConfigPath() {
        return "Z:"+USER_CONFIG_PATH.replace("/", "\\");
    }
}
