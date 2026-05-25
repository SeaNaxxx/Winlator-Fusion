package com.winlator.fusion.runtime;

import android.content.Context;

import com.winlator.fusion.container.Container;
import com.winlator.fusion.core.EnvVars;
import com.winlator.fusion.core.FileUtils;
import com.winlator.fusion.core.LocaleHelper;
import com.winlator.fusion.core.WineInfo;
import com.winlator.fusion.xconnector.UnixSocketConfig;
import com.winlator.fusion.xenvironment.FusionFS;
import com.winlator.fusion.xenvironment.RootFS;
import com.winlator.fusion.xenvironment.RuntimeFS;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class GlibcRuntimeStrategy implements RuntimeStrategy {

    private final Context context;
    private final FusionFS fusionFS;
    private final RootFS rootFS;
    private final RuntimeFS runtimeFS;
    private Container container;
    private WineInfo wineInfo;

    public GlibcRuntimeStrategy(Context context) {
        this.context = context;
        this.fusionFS = FusionFS.find(context);
        this.rootFS = RootFS.find(context);
        this.runtimeFS = RuntimeFS.find(context);
    }

    public void setContainer(Container container) {
        this.container = container;
    }

    public void setWineInfo(WineInfo wineInfo) {
        this.wineInfo = wineInfo;
    }

    public Container getContainer() {
        return container;
    }

    @Override
    public String getVariant() {
        return Container.GLIBC;
    }

    @Override
    public String getDisplayName() {
        return "GLIBC";
    }

    @Override
    public File getRootDir() {
        return rootFS.getRootDir();
    }

    @Override
    public String getHomePath() {
        return getRootDir() + RootFS.HOME_PATH;
    }

    @Override
    public File getTmpDir() {
        return rootFS.getTmpDir();
    }

    @Override
    public File getLibDir() {
        return rootFS.getLibDir();
    }

    @Override
    public File getBinDir() {
        return new File(getRootDir(), "/usr/local/bin");
    }

    @Override
    public String getWinePath() {
        if (wineInfo != null && wineInfo != WineInfo.MAIN_WINE_INFO && wineInfo.path != null) {
            return FileUtils.toRelativePath(rootFS.getRootDir().getPath(), wineInfo.path);
        }
        if (container != null && !WineInfo.isMainWineVersion(container.getWineVersion())) {
            WineInfo info = WineInfo.fromIdentifier(context, container.getWineVersion());
            if (info.path != null) {
                return FileUtils.toRelativePath(rootFS.getRootDir().getPath(), info.path);
            }
        }
        File wineDir = fusionFS.getWineDir();
        if (wineDir.isDirectory()) {
            return FileUtils.toRelativePath(rootFS.getRootDir().getPath(), wineDir.getPath());
        }
        return rootFS.getWinePath();
    }

    @Override
    public String getWinePrefix() {
        return getRootDir() + RootFS.WINEPREFIX;
    }

    @Override
    public String getBox64Path() {
        return getRootDir() + "/usr/local/bin/box64";
    }

    @Override
    public String getBox64Version() {
        return com.winlator.fusion.core.DefaultVersion.BOX64;
    }

    @Override
    public String getBox64VersionPrefKey() {
        return "current_box64_version";
    }

    @Override
    public String getBox64AssetPath(String version) {
        return "box64/box64-" + version + ".tzst";
    }

    @Override
    public boolean hasBox64LdLibraryPath() {
        return true;
    }

    @Override
    public String getBox64LdLibraryPath() {
        return runtimeFS.getBox64LdLibraryPathForVariant(Container.GLIBC);
    }

    @Override
    public String getDefaultEnvVars() {
        return Container.DEFAULT_ENV_VARS_GLIBC;
    }

    @Override
    public EnvVars buildBaseEnvVars() {
        EnvVars envVars = new EnvVars();
        LocaleHelper.setEnvVars(envVars);

        envVars.put("HOME", getHomePath());
        envVars.put("USER", RootFS.USER);
        envVars.put("TMPDIR", getTmpDir().getPath());
        envVars.put("DISPLAY", ":0");
        envVars.put("PATH", runtimeFS.getPathForGlibc(getWinePath()));
        envVars.put("LD_LIBRARY_PATH", getLdLibraryPath());
        envVars.put("BOX64_LD_LIBRARY_PATH", getBox64LdLibraryPath());
        envVars.put("ANDROID_SYSVSHM_SERVER", getRootDir() + UnixSocketConfig.SYSVSHM_SERVER_PATH);

        envVars.put("VK_LAYER_PATH", runtimeFS.getVulkanLayerPath(getRootDir()));
        envVars.put("FONTCONFIG_PATH", getRootDir() + "/usr/etc/fonts");
        envVars.put("ALSA_CONFIG_PATH", getRootDir() + "/usr/share/alsa/alsa.conf" + ":" + getRootDir() + "/usr/etc/alsa/conf.d/android_aserver.conf");
        envVars.put("ALSA_PLUGIN_DIR", getRootDir() + "/usr/lib/alsa-lib");

        return envVars;
    }

    @Override
    public String buildLdPreload() {
        File libDir = getLibDir();
        String sysvPath = new File(libDir, "libandroid-sysvshm.so").getAbsolutePath();
        if (new File(sysvPath).exists()) {
            return sysvPath;
        }
        File bionicLibDir = fusionFS.getBionicLibDir();
        String bionicSysvPath = new File(bionicLibDir, "libandroid-sysvshm.so").getAbsolutePath();
        if (new File(bionicSysvPath).exists()) {
            return bionicSysvPath;
        }
        return "";
    }

    @Override
    public String getLdLibraryPath() {
        return runtimeFS.getLdLibraryPathForGlibc();
    }

    @Override
    public boolean supportsVortek() {
        return false;
    }

    @Override
    public boolean supportsAdrenotools() {
        return false;
    }

    @Override
    public boolean supportsFEXCore() {
        return false;
    }

    @Override
    public boolean supportsZink() {
        return true;
    }

    @Override
    public boolean supportsWrapper() {
        return false;
    }

    @Override
    public boolean needsEvshimPatching() {
        return false;
    }

    @Override
    public boolean needsFEXCoreConfig() {
        return false;
    }

    @Override
    public String getWineBinaryPath(WineInfo wineInfo) {
        if (WineInfo.isMainWineVersion(wineInfo != null ? wineInfo.identifier() : null)) {
            return fusionFS.getWineDir().getPath() + "/bin";
        }
        if (wineInfo != null && wineInfo.path != null) {
            return wineInfo.path + "/bin";
        }
        File wineDir = fusionFS.getWineDir();
        if (wineDir.isDirectory()) {
            return wineDir.getPath() + "/bin";
        }
        return getRootDir() + getWinePath() + "/bin";
    }

    @Override
    public String getPathEnvVar(WineInfo wineInfo) {
        return getWineBinaryPath(wineInfo) + ":" + getRootDir() + "/usr/local/bin:" + getRootDir() + "/usr/bin";
    }

    @Override
    public RootFS getRootFS() {
        return rootFS;
    }

    @Override
    public String buildLaunchCommand(String guestExecutable, WineInfo wineInfo) {
        return getBox64Path() + " " + guestExecutable;
    }

    @Override
    public void addRuntimeSpecificEnvVars(EnvVars envVars, WineInfo wineInfo) {
    }

    @Override
    public void extractBinaries() {
    }

    @Override
    public List<String> getSupportedDrivers() {
        return Arrays.asList("Turnip", "Zink", "VirGL", "Gladio");
    }
}
