package com.winlator.fusion.runtime;

import android.app.Service;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;

import com.winlator.fusion.container.Container;
import com.winlator.fusion.contents.AdrenotoolsManager;
import com.winlator.fusion.core.DefaultVersion;
import com.winlator.fusion.core.EnvVars;
import com.winlator.fusion.core.LocaleHelper;
import com.winlator.fusion.core.WineInfo;
import com.winlator.fusion.fexcore.FEXCorePreset;
import com.winlator.fusion.fexcore.FEXCorePresetManager;
import com.winlator.fusion.xconnector.UnixSocketConfig;
import com.winlator.fusion.xenvironment.FusionFS;
import com.winlator.fusion.xenvironment.ImageFs;
import com.winlator.fusion.xenvironment.RootFS;
import com.winlator.fusion.xenvironment.RuntimeFS;

import java.io.File;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

public class BionicRuntimeStrategy implements RuntimeStrategy {

    private final Context context;
    private final FusionFS fusionFS;
    private final ImageFs imageFs;
    private final RuntimeFS runtimeFS;
    private Container container;

    public BionicRuntimeStrategy(Context context) {
        this.context = context;
        this.fusionFS = FusionFS.find(context);
        this.imageFs = ImageFs.find(context);
        this.runtimeFS = RuntimeFS.find(context);
    }

    public void setContainer(Container container) {
        this.container = container;
    }

    @Override
    public String getVariant() {
        return Container.BIONIC;
    }

    @Override
    public String getDisplayName() {
        return "BIONIC";
    }

    @Override
    public File getRootDir() {
        return imageFs.getRootDir();
    }

    @Override
    public String getHomePath() {
        return imageFs.home_path;
    }

    @Override
    public File getTmpDir() {
        return imageFs.getTmpDir();
    }

    @Override
    public File getLibDir() {
        return imageFs.getLibDir();
    }

    @Override
    public File getBinDir() {
        return imageFs.getBinDir();
    }

    @Override
    public String getWinePath() {
        return imageFs.getWinePath();
    }

    @Override
    public String getWinePrefix() {
        return imageFs.wineprefix;
    }

    @Override
    public String getBox64Path() {
        return getRootDir() + "/usr/bin/box64";
    }

    @Override
    public String getBox64Version() {
        return container != null ? container.getBox64Version() : DefaultVersion.BOX64_BIONIC;
    }

    @Override
    public String getBox64VersionPrefKey() {
        return "current_box64_bionic_version";
    }

    @Override
    public String getBox64AssetPath(String version) {
        return "box64/box64-" + version + ".tzst";
    }

    @Override
    public boolean hasBox64LdLibraryPath() {
        return false;
    }

    @Override
    public String getBox64LdLibraryPath() {
        return "";
    }

    @Override
    public String getDefaultEnvVars() {
        return Container.DEFAULT_ENV_VARS_BIONIC;
    }

    @Override
    public EnvVars buildBaseEnvVars() {
        EnvVars envVars = new EnvVars();
        LocaleHelper.setEnvVars(envVars);

        File rootDir = getRootDir();

        envVars.put("HOME", imageFs.home_path);
        envVars.put("USER", ImageFs.USER);
        envVars.put("TMPDIR", rootDir.getPath() + "/usr/tmp");
        envVars.put("DISPLAY", ":0");
        envVars.put("XDG_DATA_DIRS", rootDir.getPath() + "/usr/share");
        envVars.put("XDG_CONFIG_DIRS", rootDir.getPath() + "/usr/etc/xdg");
        envVars.put("GST_PLUGIN_PATH", rootDir.getPath() + "/usr/lib/gstreamer-1.0");
        envVars.put("FONTCONFIG_PATH", rootDir.getPath() + "/usr/etc/fonts");

        envVars.put("LD_LIBRARY_PATH", getLdLibraryPath());

        envVars.put("VK_LAYER_PATH", runtimeFS.getVulkanLayerPath(getRootDir()));
        envVars.put("WRAPPER_LAYER_PATH", rootDir.getPath() + "/usr/lib");
        envVars.put("WRAPPER_CACHE_PATH", rootDir.getPath() + "/usr/var/cache");
        envVars.put("WINE_NO_DUPLICATE_EXPLORER", "1");
        envVars.put("PREFIX", rootDir.getPath() + "/usr");
        envVars.put("WINE_DISABLE_FULLSCREEN_HACK", "1");
        envVars.put("GST_PLUGIN_FEATURE_RANK", "ximagesink:3000");
        envVars.put("ALSA_CONFIG_PATH", rootDir.getPath() + "/usr/share/alsa/alsa.conf" + ":" + rootDir.getPath() + "/usr/etc/alsa/conf.d/android_aserver.conf");
        envVars.put("ALSA_PLUGIN_DIR", rootDir.getPath() + "/usr/lib/alsa-lib");
        envVars.put("WINE_X11FORCEGLX", "1");
        envVars.put("WINE_GST_NO_GL", "1");
        envVars.put("OPENSSL_CONF", rootDir.getPath() + "/usr/etc/tls/openssl.cnf");
        envVars.put("SSL_CERT_FILE", rootDir.getPath() + "/usr/etc/tls/cert.pem");
        envVars.put("SSL_CERT_DIR", rootDir.getPath() + "/usr/etc/tls/certs");
        envVars.put("SteamGameId", "0");
        envVars.put("PROTON_AUDIO_CONVERT", "0");
        envVars.put("PROTON_VIDEO_CONVERT", "0");
        envVars.put("PROTON_DEMUX", "0");
        envVars.put("WINE_NEW_NDIS", "1");

        envVars.put("ANDROID_SYSVSHM_SERVER", rootDir + UnixSocketConfig.SYSVSHM_SERVER_PATH);

        try {
            String primaryDNS = "8.8.4.4";
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Service.CONNECTIVITY_SERVICE);
            if (connectivityManager.getActiveNetwork() != null) {
                LinkProperties lp = connectivityManager.getLinkProperties(connectivityManager.getActiveNetwork());
                if (lp != null) {
                    List<InetAddress> dnsServers = lp.getDnsServers();
                    if (!dnsServers.isEmpty()) {
                        primaryDNS = dnsServers.get(0).toString().substring(1);
                    }
                }
            }
            envVars.put("ANDROID_RESOLV_DNS", primaryDNS);
        } catch (SecurityException e) {
            envVars.put("ANDROID_RESOLV_DNS", "8.8.4.4");
        }

        return envVars;
    }

    @Override
    public String buildLdPreload() {
        StringBuilder ldPreload = new StringBuilder();
        File libDir = imageFs.getLibDir();

        String redirectPath = findLibrary(libDir, "libredirect-bionic.so", "libhook_impl.so");
        if (redirectPath != null) {
            ldPreload.append(redirectPath);
        }

        String sysvPath = new File(libDir, "libandroid-sysvshm.so").getAbsolutePath();
        if (new File(sysvPath).exists()) {
            if (ldPreload.length() > 0) ldPreload.append(":");
            ldPreload.append(sysvPath);
        }

        String evshimPath = findLibrary(libDir, "libevshim.so", "libmain_hook.so");
        if (evshimPath != null) {
            if (ldPreload.length() > 0) ldPreload.append(":");
            ldPreload.append(evshimPath);
        }

        String fileRedirectPath = new File(libDir, "libfile_redirect_hook.so").getAbsolutePath();
        if (new File(fileRedirectPath).exists()) {
            if (ldPreload.length() > 0) ldPreload.append(":");
            ldPreload.append(fileRedirectPath);
        }

        return ldPreload.toString();
    }

    private String findLibrary(File libDir, String... names) {
        for (String name : names) {
            File f = new File(libDir, name);
            if (f.exists()) return f.getAbsolutePath();
        }
        return null;
    }

    @Override
    public String getLdLibraryPath() {
        return runtimeFS.getLdLibraryPathForBionic();
    }

    @Override
    public boolean supportsVortek() {
        return false;
    }

    @Override
    public boolean supportsAdrenotools() {
        return true;
    }

    @Override
    public boolean supportsFEXCore() {
        if (container == null) return false;
        String wineVersion = container.getWineVersion();
        WineInfo wineInfo = WineInfo.fromIdentifier(context, wineVersion);
        return wineInfo.isArm64EC();
    }

    @Override
    public boolean supportsZink() {
        return true;
    }

    @Override
    public boolean supportsWrapper() {
        return true;
    }

    @Override
    public boolean needsEvshimPatching() {
        return true;
    }

    @Override
    public boolean needsFEXCoreConfig() {
        return supportsFEXCore();
    }

    @Override
    public String getWineBinaryPath(WineInfo wineInfo) {
        String wineVersion = container != null ? container.getWineVersion() : WineInfo.MAIN_WINE_INFO.identifier();
        if (WineInfo.isMainWineVersion(wineVersion) || WineInfo.isBionicDefaultWineVersion(wineVersion)) {
            return fusionFS.getWinePathForVersion(WineInfo.BIONIC_WINE_IDENTIFIER) + "/bin";
        } else {
            return fusionFS.getWinePathForVersion(wineVersion) + "/bin";
        }
    }

    @Override
    public String getPathEnvVar(WineInfo wineInfo) {
        String winePath = getWineBinaryPath(wineInfo);
        return runtimeFS.getPathForBionic(winePath);
    }

    @Override
    public RootFS getRootFS() {
        return RootFS.fromDir(imageFs.getRootDir());
    }

    @Override
    public String buildLaunchCommand(String guestExecutable, WineInfo wineInfo) {
        if (wineInfo != null && wineInfo.isArm64EC()) {
            return getWineBinaryPath(wineInfo) + "/" + guestExecutable;
        } else {
            return imageFs.getBinDir() + "/box64 " + guestExecutable;
        }
    }

    @Override
    public void addRuntimeSpecificEnvVars(EnvVars envVars, WineInfo wineInfo) {
        boolean isArm64EC = wineInfo != null && wineInfo.isArm64EC();
        if (!isArm64EC && container != null) {
            WineInfo containerWineInfo = WineInfo.fromIdentifier(context, container.getWineVersion());
            isArm64EC = containerWineInfo.isArm64EC();
        }

        if (isArm64EC) {
            String fexcorePreset = container != null ? container.getFEXCorePreset() : FEXCorePreset.DEFAULT;
            envVars.putAll(FEXCorePresetManager.getEnvVars(context, fexcorePreset));

            String emulator = container != null ? container.getEmulator() : "FEXCore";
            if (emulator.toLowerCase().equals("fexcore")) {
                envVars.put("HODLL", "libwow64fex.dll");
            } else {
                envVars.put("HODLL", "wowbox64.dll");
            }
        }

        if (container != null) {
            String graphicsDriverConfig = container.getGraphicsDriverConfig();
            if (graphicsDriverConfig != null && !graphicsDriverConfig.isEmpty()) {
                String[] configParts = graphicsDriverConfig.split("\\|");
                if (configParts.length > 0) {
                    com.winlator.fusion.core.KeyValueSet config = new com.winlator.fusion.core.KeyValueSet(configParts[0]);
                    String adrenotoolsDriverId = config.get("adrenotoolsDriver");
                    if (adrenotoolsDriverId != null && !adrenotoolsDriverId.isEmpty()) {
                        AdrenotoolsManager adrenotoolsManager = new AdrenotoolsManager(context);
                        adrenotoolsManager.setDriverById(envVars, imageFs, adrenotoolsDriverId);
                    } else {
                        adrenotoolsDriverId = container.getExtra("adrenotoolsDriverId");
                        if (adrenotoolsDriverId != null && !adrenotoolsDriverId.isEmpty()) {
                            AdrenotoolsManager adrenotoolsManager = new AdrenotoolsManager(context);
                            adrenotoolsManager.setDriverById(envVars, imageFs, adrenotoolsDriverId);
                        }
                    }
                    String vkMaxVersion = config.get("vkMaxVersion");
                    if (vkMaxVersion != null && !vkMaxVersion.isEmpty() && !vkMaxVersion.equals("0")) {
                        envVars.put("WRAPPER_VK_VERSION", vkMaxVersion);
                    }
                    String maxDeviceMemory = config.get("maxDeviceMemory");
                    if (maxDeviceMemory != null && !maxDeviceMemory.isEmpty() && !maxDeviceMemory.equals("0")) {
                        envVars.put("WRAPPER_MAX_DEVICE_MEMORY", maxDeviceMemory);
                    }
                }
            }
        }

        envVars.put("PATH", getPathEnvVar(wineInfo));
    }

    @Override
    public void extractBinaries() {
    }

    @Override
    public List<String> getSupportedDrivers() {
        return Arrays.asList("Turnip", "Wrapper", "Zink", "Gladio", "VirGL");
    }
}
