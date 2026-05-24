package com.winlator.fusion.container;

import com.winlator.fusion.box64.Box64Preset;
import com.winlator.fusion.core.AppUtils;
import com.winlator.fusion.core.DefaultVersion;
import com.winlator.fusion.core.EnvVars;
import com.winlator.fusion.core.FileUtils;
import com.winlator.fusion.core.KeyValueSet;
import com.winlator.fusion.core.WineInfo;
import com.winlator.fusion.core.WineThemeManager;
import com.winlator.fusion.fexcore.FEXCorePreset;
import com.winlator.fusion.fexcore.FEXCorePresetManager;
import com.winlator.fusion.runtime.BionicRuntimeStrategy;
import com.winlator.fusion.runtime.GlibcRuntimeStrategy;
import com.winlator.fusion.runtime.RuntimeStrategy;
import com.winlator.fusion.widget.FrameRating;
import com.winlator.fusion.winhandler.WinHandler;
import com.winlator.fusion.xenvironment.ImageFs;
import com.winlator.fusion.xenvironment.RootFS;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class Container {
    public enum ContainerType {
        GLIBC_WINE_X8664,
        BIONIC_PROTON_X8664,
        BIONIC_PROTON_ARM64EC,
        BIONIC_WINE_ARM64EC
    }

    public enum XrControllerMapping {
        BUTTON_A, BUTTON_B, BUTTON_X, BUTTON_Y, BUTTON_GRIP, BUTTON_TRIGGER,
        THUMBSTICK_UP, THUMBSTICK_DOWN, THUMBSTICK_LEFT, THUMBSTICK_RIGHT
    }

    public static final String DEFAULT_ENV_VARS_GLIBC = "ZINK_DESCRIPTORS=lazy ZINK_DEBUG=compact MESA_SHADER_CACHE_DISABLE=false MESA_SHADER_CACHE_MAX_SIZE=512MB mesa_glthread=true WINEESYNC=1";
    public static final String DEFAULT_ENV_VARS_BIONIC = "WRAPPER_MAX_IMAGE_COUNT=0 RENDERER_SWAPCHAIN=0 VKD3D_SHADER_MODEL=6_6 ZINK_DESCRIPTORS=lazy ZINK_DEBUG=compact MESA_SHADER_CACHE_DISABLE=false MESA_SHADER_CACHE_MAX_SIZE=512MB mesa_glthread=true WINEESYNC=1 TU_DEBUG=noconform,sysmem DXVK_HUD=/0";
    public static final String DEFAULT_ENV_VARS = DEFAULT_ENV_VARS_BIONIC;
    public static final String DEFAULT_SCREEN_SIZE = "1280x720";
    public static final String DEFAULT_AUDIO_DRIVER = AudioDrivers.ALSA;
    public static final String DEFAULT_DXWRAPPER = DXWrappers.DXVK;
    public static final String DEFAULT_DXWRAPPER_BIONIC = "dxvk+vkd3d";
    public static final String DEFAULT_DDRAWRAPPER = "none";
    public static final String DEFAULT_WINCOMPONENTS = "direct3d=1,directsound=0,directmusic=0,directshow=0,directplay=0,xaudio=0,vcrun2005=0,vcrun2010=1,wmdecoder=1";
    public static final String FALLBACK_WINCOMPONENTS = "direct3d=1,directsound=1,directmusic=1,directshow=1,directplay=1,xaudio=1,vcrun2005=1,vcrun2010=1,wmdecoder=1";
    public static final String DEFAULT_DRIVES = "D:"+AppUtils.DIRECTORY_DOWNLOADS +"E:"+AppUtils.INTERNAL_STORAGE;
    public static final String DEFAULT_GRAPHICSDRIVERCONFIG =
            "vulkanVersion=1.3;version=;blacklistedExtensions=;maxDeviceMemory=0;presentMode=mailbox;syncFrame=0;disablePresentWait=0;resourceType=auto;bcnEmulation=auto;bcnEmulationType=compute;bcnEmulationCache=0;gpuName=Device";
    public static final byte STARTUP_SELECTION_NORMAL = 0;
    public static final byte STARTUP_SELECTION_ESSENTIAL = 1;
    public static final byte STARTUP_SELECTION_AGGRESSIVE = 2;
    public static final byte MAX_DRIVE_LETTERS = 26;
    public static final String GLIBC = "glibc";
    public static final String BIONIC = "bionic";
    public static final String DEFAULT_VARIANT = BIONIC;

    public static final byte RENDERER_GL = 0;
    public static final byte RENDERER_VULKAN = 1;
    public static final byte PRESENT_MODE_FIFO = 0;
    public static final byte PRESENT_MODE_MAILBOX = 1;
    public static final byte PRESENT_MODE_IMMEDIATE = 2;
    public static final byte FILTER_MODE_NEAREST = 0;
    public static final byte FILTER_MODE_LINEAR = 1;

    public final int id;
    private String name;
    private String screenSize = DEFAULT_SCREEN_SIZE;
    private String envVars = DEFAULT_ENV_VARS_BIONIC;
    private String graphicsDriver = GraphicsDrivers.WRAPPER + "," + GraphicsDrivers.DEFAULT_OPENGL_DRIVER_BIONIC;
    private String graphicsDriverConfig = DEFAULT_GRAPHICSDRIVERCONFIG;
    private String dxwrapper = DEFAULT_DXWRAPPER_BIONIC;
    private String dxwrapperConfig = "";
    private String audioDriverConfig = "";
    private String wincomponents = DEFAULT_WINCOMPONENTS;
    private String audioDriver = DEFAULT_AUDIO_DRIVER;
    private String drives = DEFAULT_DRIVES;
    private String wineVersion = WineInfo.MAIN_WINE_INFO.identifier();
    private boolean showFPS = false;
    private byte hudMode = (byte)FrameRating.Mode.DISABLED.ordinal();
    private byte startupSelection = STARTUP_SELECTION_ESSENTIAL;
    private String cpuList;
    private String cpuListWoW64;
    private String desktopTheme = WineThemeManager.DEFAULT_DESKTOP_THEME;
    private String fexcoreVersion = DefaultVersion.FEXCORE;
    private String fexcorePreset = FEXCorePreset.DEFAULT;
    private String box64Preset = Box64Preset.DEFAULT;
    private String box64Version = DefaultVersion.BOX64_BIONIC;
    private String emulator = "Box64";
    private File rootDir;
    private JSONObject extraData;

    private String containerVariant = DEFAULT_VARIANT;
    private byte rendererType = RENDERER_VULKAN;
    private boolean rendererNative = false;
    private String rendererPresentMode = "fifo";
    private String rendererDriverId = "";
    private int rendererFilterMode = 0;
    private int rendererRefreshRateLimit = 60;
    private boolean rendererSwapRB = false;
    private boolean fullscreenStretched;
    private String midiSoundFont = "";
    private int inputType = WinHandler.DEFAULT_INPUT_TYPE;
    private String lc_all = "";
    private int primaryController = 1;
    private String controllerMapping = new String(new char[XrControllerMapping.values().length]);
    private boolean exclusiveXInput = true;

    public Container(int id) {
        this.id = id;
        this.name = "Container-"+id;
    }

    public ContainerType getContainerType() {
        if (!isBionic()) {
            return ContainerType.GLIBC_WINE_X8664;
        }
        if (isArm64EC()) {
            if (isProton()) {
                return ContainerType.BIONIC_PROTON_ARM64EC;
            } else {
                return ContainerType.BIONIC_WINE_ARM64EC;
            }
        } else {
            return ContainerType.BIONIC_PROTON_X8664;
        }
    }

    public boolean isArm64EC() {
        return wineVersion != null && wineVersion.contains("arm64ec");
    }

    public boolean isProton() {
        return wineVersion != null && wineVersion.contains("proton");
    }

    public boolean isGlibc() {
        return !isBionic();
    }

    public boolean isBionic() {
        return BIONIC.equals(containerVariant);
    }

    public boolean usesFEXCore() {
        return isArm64EC();
    }

    public boolean usesWOWBox64() {
        return isArm64EC();
    }

    public boolean usesBox64Only() {
        return isBionic() && !isArm64EC();
    }

    public String getDefaultEnvVarsForType() {
        return isBionic() ? DEFAULT_ENV_VARS_BIONIC : DEFAULT_ENV_VARS_GLIBC;
    }

    public String getDefaultGraphicsDriverForType() {
        if (isBionic()) {
            return GraphicsDrivers.WRAPPER + "," + GraphicsDrivers.DEFAULT_OPENGL_DRIVER_BIONIC;
        } else {
            return GraphicsDrivers.TURNIP + "," + GraphicsDrivers.DEFAULT_OPENGL_DRIVER;
        }
    }

    public String getDefaultDXWrapperForType() {
        return isBionic() ? DEFAULT_DXWRAPPER_BIONIC : DEFAULT_DXWRAPPER;
    }

    public String getDefaultEmulatorForType() {
        if (isArm64EC()) return "FEXCore";
        if (isBionic()) return "Box64";
        return "Box64";
    }

    public String getDefaultBox64VersionForType() {
        if (isArm64EC()) return DefaultVersion.WOWBOX64;
        if (isBionic()) return DefaultVersion.BOX64_BIONIC;
        return DefaultVersion.BOX64;
    }

    public RuntimeStrategy getRuntimeStrategy(android.content.Context context) {
        if (isBionic()) {
            BionicRuntimeStrategy strategy = new BionicRuntimeStrategy(context);
            strategy.setContainer(this);
            return strategy;
        } else {
            GlibcRuntimeStrategy strategy = new GlibcRuntimeStrategy(context);
            strategy.setContainer(this);
            return strategy;
        }
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getScreenSize() { return screenSize; }
    public void setScreenSize(String screenSize) { this.screenSize = screenSize; }

    public String getEnvVars() { return envVars; }
    public void setEnvVars(String envVars) { this.envVars = envVars != null ? envVars : ""; }

    public String getGraphicsDriver() { return graphicsDriver; }
    public void setGraphicsDriver(String graphicsDriver) { this.graphicsDriver = graphicsDriver; }

    public String getGraphicsDriverConfig() { return graphicsDriverConfig; }
    public void setGraphicsDriverConfig(String graphicsDriverConfig) { this.graphicsDriverConfig = graphicsDriverConfig != null ? graphicsDriverConfig : ""; }

    public String getDXWrapper() { return dxwrapper; }
    public void setDXWrapper(String dxwrapper) { this.dxwrapper = dxwrapper; }

    public String getDXWrapperConfig() { return dxwrapperConfig; }
    public void setDXWrapperConfig(String dxwrapperConfig) { this.dxwrapperConfig = dxwrapperConfig != null ? dxwrapperConfig : ""; }

    public String getAudioDriverConfig() { return audioDriverConfig; }
    public void setAudioDriverConfig(String audioDriverConfig) { this.audioDriverConfig = audioDriverConfig != null ? audioDriverConfig : ""; }

    public String getAudioDriver() { return audioDriver; }
    public void setAudioDriver(String audioDriver) { this.audioDriver = audioDriver; }

    public String getWinComponents() { return wincomponents; }
    public void setWinComponents(String wincomponents) { this.wincomponents = wincomponents; }

    public String getDrives() { return drives; }
    public void setDrives(String drives) { this.drives = drives; }

    public boolean isShowFPS() { return showFPS; }
    public void setShowFPS(boolean showFPS) { this.showFPS = showFPS; }

    public byte getHUDMode() { return hudMode; }
    public void setHUDMode(byte hudMode) { this.hudMode = hudMode; }

    public byte getStartupSelection() { return startupSelection; }
    public void setStartupSelection(byte startupSelection) { this.startupSelection = startupSelection; }

    public String getCPUList() { return getCPUList(false); }
    public String getCPUList(boolean allowFallback) { return cpuList != null ? cpuList : (allowFallback ? getFallbackCPUList() : null); }
    public void setCPUList(String cpuList) { this.cpuList = cpuList != null && !cpuList.isEmpty() ? cpuList : null; }

    public String getCPUListWoW64() { return getCPUListWoW64(false); }
    public String getCPUListWoW64(boolean allowFallback) { return cpuListWoW64 != null ? cpuListWoW64 : (allowFallback ? getFallbackCPUListWoW64() : null); }
    public void setCPUListWoW64(String cpuListWoW64) { this.cpuListWoW64 = cpuListWoW64 != null && !cpuListWoW64.isEmpty() ? cpuListWoW64 : null; }

    public String getBox64Preset() { return box64Preset; }
    public void setBox64Preset(String box64Preset) { this.box64Preset = box64Preset; }

    public File getRootDir() { return rootDir; }
    public void setRootDir(File rootDir) { this.rootDir = rootDir; }

    public void setExtraData(JSONObject extraData) { this.extraData = extraData; }
    public String getExtraData() { return extraData != null ? extraData.toString() : ""; }

    public String getExtra(String name) { return getExtra(name, ""); }
    public String getExtra(String name, String fallback) {
        try { return extraData != null && extraData.has(name) ? extraData.getString(name) : fallback; }
        catch (JSONException e) { return fallback; }
    }
    public void putExtra(String name, Object value) {
        if (extraData == null) extraData = new JSONObject();
        try { if (value != null) extraData.put(name, value); else extraData.remove(name); }
        catch (JSONException e) {}
    }

    public String getWineVersion() { return wineVersion; }
    public void setWineVersion(String wineVersion) { this.wineVersion = wineVersion; }

    public File getConfigFile() { return rootDir != null ? new File(rootDir, ".container") : null; }

    public File getUserDir() {
        if (rootDir == null) return null;
        String user = isBionic() ? ImageFs.USER : RootFS.USER;
        return new File(rootDir, ".wine/drive_c/users/" + user + "/");
    }

    public File getDesktopDir() {
        if (rootDir == null) return null;
        String user = isBionic() ? ImageFs.USER : RootFS.USER;
        return new File(rootDir, ".wine/drive_c/users/" + user + "/Desktop/");
    }

    public File getStartMenuDir() {
        return rootDir != null ? new File(rootDir, ".wine/drive_c/ProgramData/Microsoft/Windows/Start Menu/") : null;
    }

    public File getIconsDir(int size) {
        return rootDir != null ? new File(rootDir, ".local/share/icons/hicolor/"+size+"x"+size+"/apps/") : null;
    }

    public String getDesktopTheme() { return desktopTheme; }
    public void setDesktopTheme(String desktopTheme) { this.desktopTheme = desktopTheme; }

    public String getContainerVariant() { return containerVariant; }
    public void setContainerVariant(String containerVariant) { this.containerVariant = containerVariant; }

    public String getFEXCoreVersion() { return fexcoreVersion; }
    public void setFEXCoreVersion(String version) { this.fexcoreVersion = version; }

    public String getBox64Version() { return box64Version; }
    public void setBox64Version(String version) { this.box64Version = version; }

    public String getEmulator() { return emulator; }
    public void setEmulator(String emulator) { this.emulator = emulator; }

    public String getFEXCorePreset() { return fexcorePreset; }
    public void setFEXCorePreset(String v) { this.fexcorePreset = v; }

    public byte getRendererType() { return rendererType; }
    public void setRendererType(byte rendererType) { this.rendererType = rendererType; }

    public boolean isRendererNative() { return rendererNative; }
    public void setRendererNative(boolean rendererNative) { this.rendererNative = rendererNative; }

    public String getRendererPresentMode() { return rendererPresentMode; }
    public void setRendererPresentMode(String rendererPresentMode) { this.rendererPresentMode = rendererPresentMode != null ? rendererPresentMode : "fifo"; }

    public String getRendererDriverId() { return rendererDriverId; }
    public void setRendererDriverId(String rendererDriverId) { this.rendererDriverId = rendererDriverId != null ? rendererDriverId : ""; }

    public int getRendererFilterMode() { return rendererFilterMode; }
    public void setRendererFilterMode(int rendererFilterMode) { this.rendererFilterMode = rendererFilterMode; }

    public int getRendererRefreshRateLimit() { return rendererRefreshRateLimit; }
    public void setRendererRefreshRateLimit(int rendererRefreshRateLimit) { this.rendererRefreshRateLimit = rendererRefreshRateLimit > 0 ? rendererRefreshRateLimit : 0; }

    public boolean getRendererSwapRB() { return rendererSwapRB; }
    public void setRendererSwapRB(boolean rendererSwapRB) { this.rendererSwapRB = rendererSwapRB; }

    public boolean isVulkanRenderer() { return rendererType == RENDERER_VULKAN; }
    public boolean isFullscreenStretched() { return fullscreenStretched; }
    public void setFullscreenStretched(boolean fullscreenStretched) { this.fullscreenStretched = fullscreenStretched; }

    public String getMIDISoundFont() { return midiSoundFont; }
    public void setMIDISoundFont(String fileName) { this.midiSoundFont = fileName; }

    public int getInputType() { return inputType; }
    public void setInputType(int inputType) { this.inputType = inputType; }

    public String getLC_ALL() { return lc_all; }
    public void setLC_ALL(String lc_all) { this.lc_all = lc_all; }

    public int getPrimaryController() { return primaryController; }
    public void setPrimaryController(int primaryController) { this.primaryController = primaryController; }

    public byte getControllerMapping(XrControllerMapping input) { return (byte) controllerMapping.charAt(input.ordinal()); }
    public void setControllerMapping(String controllerMapping) { this.controllerMapping = controllerMapping; }

    public boolean isExclusiveXInput() { return exclusiveXInput; }
    public void setExclusiveXInput(boolean exclusiveXInput) { this.exclusiveXInput = exclusiveXInput; }

    public boolean hasEnvVar(String keyValue) {
        if (envVars == null || envVars.isEmpty()) return false;
        EnvVars vars = new EnvVars(envVars);
        String[] parts = keyValue.split("=", 2);
        if (parts.length == 2) return vars.has(parts[0]) && vars.get(parts[0]).equals(parts[1]);
        return vars.has(keyValue);
    }

    public Iterable<Drive> drivesIterator() { return drivesIterator(drives); }

    public static Iterable<Drive> drivesIterator(final String drives) {
        final int[] index = {drives.indexOf(":")};
        return () -> new Iterator<Drive>() {
            @Override
            public boolean hasNext() { return index[0] != -1; }

            @Override
            public Drive next() {
                if (index[0] <= 0 || index[0] >= drives.length()) throw new NoSuchElementException();
                String letter = String.valueOf(drives.charAt(index[0]-1));
                int nextIndex = drives.indexOf(":", index[0]+1);
                String path = drives.substring(index[0]+1, nextIndex != -1 ? nextIndex-1 : drives.length());
                index[0] = nextIndex;
                return new Drive(letter, path);
            }
        };
    }

    public void saveData() {
        File configFile = getConfigFile();
        if (configFile == null) return;
        try {
            JSONObject data = new JSONObject();
            data.put("id", id);
            data.put("name", name);
            data.put("screenSize", screenSize);
            data.put("envVars", envVars);
            data.put("cpuList", cpuList);
            data.put("cpuListWoW64", cpuListWoW64);
            data.put("graphicsDriver", graphicsDriver);
            data.put("graphicsDriverConfig", graphicsDriverConfig);
            data.put("dxwrapper", dxwrapper);
            if (!dxwrapperConfig.isEmpty()) data.put("dxwrapperConfig", dxwrapperConfig);
            if (!audioDriverConfig.isEmpty()) data.put("audioDriverConfig", audioDriverConfig);
            data.put("audioDriver", audioDriver);
            data.put("wincomponents", wincomponents);
            data.put("drives", drives);
            data.put("showFPS", showFPS);
            data.put("hudMode", hudMode);
            data.put("startupSelection", startupSelection);
            data.put("box64Preset", box64Preset);
            data.put("box64Version", box64Version);
            data.put("fexcoreVersion", fexcoreVersion);
            data.put("fexcorePreset", fexcorePreset);
            data.put("emulator", emulator);
            data.put("desktopTheme", desktopTheme);
            data.put("extraData", extraData);
            data.put("containerVariant", containerVariant);
            data.put("primaryController", primaryController);
            data.put("controllerMapping", controllerMapping);
            data.put("inputType", inputType);
            data.put("exclusiveXInput", exclusiveXInput);

            if (rendererType != RENDERER_GL) data.put("rendererType", rendererType);
            data.put("rendererNative", rendererNative);
            data.put("rendererPresentMode", rendererPresentMode);
            if (!rendererDriverId.isEmpty()) data.put("rendererDriverId", rendererDriverId);
            if (rendererFilterMode != 0) data.put("rendererFilterMode", rendererFilterMode);
            if (rendererRefreshRateLimit != 60) data.put("rendererRefreshRateLimit", rendererRefreshRateLimit);
            if (rendererSwapRB) data.put("rendererSwapRB", true);

            if (fullscreenStretched) data.put("fullscreenStretched", fullscreenStretched);
            if (!midiSoundFont.isEmpty()) data.put("midiSoundFont", midiSoundFont);
            if (!lc_all.isEmpty()) data.put("lc_all", lc_all);

            if (!WineInfo.isMainWineVersion(wineVersion)) data.put("wineVersion", wineVersion);
            FileUtils.writeString(configFile, data.toString());
        }
        catch (JSONException e) {}
    }

    public void loadData(JSONObject data) throws JSONException {
        wineVersion = WineInfo.MAIN_WINE_INFO.identifier();
        dxwrapperConfig = "";
        graphicsDriverConfig = DEFAULT_GRAPHICSDRIVERCONFIG;
        audioDriverConfig = "";

        checkObsoleteOrMissingProperties(data);

        for (Iterator<String> it = data.keys(); it.hasNext(); ) {
            String key = it.next();
            switch (key) {
                case "name": setName(data.getString(key)); break;
                case "screenSize": setScreenSize(data.getString(key)); break;
                case "envVars": setEnvVars(data.getString(key)); break;
                case "cpuList": setCPUList(data.getString(key)); break;
                case "cpuListWoW64": setCPUListWoW64(data.getString(key)); break;
                case "graphicsDriver": setGraphicsDriver(data.getString(key)); break;
                case "graphicsDriverConfig": setGraphicsDriverConfig(data.getString(key)); break;
                case "wincomponents": setWinComponents(data.getString(key)); break;
                case "dxwrapper": {
                    String dxw = data.getString(key);
                    if ("dxvk+vkd3d".equals(dxw)) dxw = DEFAULT_DXWRAPPER_BIONIC;
                    setDXWrapper(dxw);
                    break;
                }
                case "dxwrapperConfig": {
                    String cfg = data.getString(key);
                    if (cfg.contains("vkd3dVersion=") && !cfg.contains("|")) {
                        String[] parts = migrateLudashiDXWrapperConfig(cfg);
                        setDXWrapperConfig(parts[0] + "|" + parts[1]);
                    } else {
                        setDXWrapperConfig(cfg);
                    }
                    break;
                }
                case "audioDriverConfig": setAudioDriverConfig(data.getString(key)); break;
                case "drives": setDrives(data.getString(key)); break;
                case "showFPS": setShowFPS(data.getBoolean(key)); break;
                case "hudMode": setHUDMode((byte)data.getInt(key)); break;
                case "startupSelection": setStartupSelection((byte)data.getInt(key)); break;
                case "extraData": {
                    JSONObject extraDataObj = data.getJSONObject(key);
                    checkObsoleteOrMissingProperties(extraDataObj);
                    setExtraData(extraDataObj);
                    break;
                }
                case "wineVersion": setWineVersion(data.getString(key)); break;
                case "box64Preset": setBox64Preset(data.getString(key)); break;
                case "box64Version": setBox64Version(data.getString(key)); break;
                case "fexcoreVersion": setFEXCoreVersion(data.getString(key)); break;
                case "fexcorePreset": setFEXCorePreset(data.getString(key)); break;
                case "audioDriver": setAudioDriver(data.getString(key)); break;
                case "emulator": setEmulator(data.getString(key)); break;
                case "containerVariant": setContainerVariant(data.getString(key)); break;
                case "rendererType": setRendererType((byte)data.getInt(key)); break;
                case "rendererNative": setRendererNative(data.getBoolean(key)); break;
                case "rendererPresentMode": {
                    try {
                        int mode = data.getInt(key);
                        switch (mode) {
                            case 1: setRendererPresentMode("mailbox"); break;
                            case 2: setRendererPresentMode("immediate"); break;
                            default: setRendererPresentMode("fifo"); break;
                        }
                    }
                    catch (Exception e) { setRendererPresentMode(data.optString(key, "fifo")); }
                    break;
                }
                case "rendererDriverId": setRendererDriverId(data.getString(key)); break;
                case "rendererFilterMode": setRendererFilterMode(data.getInt(key)); break;
                case "rendererRefreshRateLimit": setRendererRefreshRateLimit(data.optInt(key, 60)); break;
                case "rendererRefreshRate": {
                    try { setRendererRefreshRateLimit(data.getInt(key)); }
                    catch (Exception e) { setRendererRefreshRateLimit(60); }
                    break;
                }
                case "rendererSwapRB": setRendererSwapRB(data.getBoolean(key)); break;
                case "desktopTheme": setDesktopTheme(data.getString(key)); break;
                case "fullscreenStretched": fullscreenStretched = data.getBoolean(key); break;
                case "inputType": inputType = data.getInt(key); break;
                case "midiSoundFont": midiSoundFont = data.getString(key); break;
                case "lc_all": lc_all = data.getString(key); break;
                case "primaryController": primaryController = data.getInt(key); break;
                case "controllerMapping": controllerMapping = data.getString(key); break;
                case "exclusiveXInput": exclusiveXInput = data.getBoolean(key); break;
                case "fexcoreTSOPreset": {
                    if (!data.has("fexcorePreset")) {
                        String legacyTSO = data.getString(key);
                        String legacyX87 = data.optString("fexcoreX87Mode", "Fast");
                        String legacyMB = data.optString("fexcoreMultiblock", "Disabled");
                        if (legacyTSO.equals(FEXCorePreset.STABILITY) || legacyTSO.equals(FEXCorePreset.COMPATIBILITY) ||
                            legacyTSO.equals(FEXCorePreset.INTERMEDIATE) || legacyTSO.equals(FEXCorePreset.PERFORMANCE) ||
                            legacyTSO.equals(FEXCorePreset.CUSTOM)) {
                            setFEXCorePreset(legacyTSO);
                        } else {
                            setFEXCorePreset(FEXCorePresetManager.migrateFromLegacyValues(legacyTSO, legacyX87, legacyMB));
                        }
                    }
                    break;
                }
                case "fexcoreX87Mode": break;
                case "fexcoreMultiblock": break;
                case "rcfileId": break;
            }
        }

        validateVariantDefaults();
    }

    private void validateVariantDefaults() {
        if (isBionic()) {
            String[] driverIds = GraphicsDrivers.parseIdentifiers(graphicsDriver);
            if (driverIds[0].equals(GraphicsDrivers.VORTEK) || driverIds[0].equals(GraphicsDrivers.TURNIP)) {
                graphicsDriver = GraphicsDrivers.WRAPPER + "," + driverIds[1];
            }
            if (box64Version == null || box64Version.isEmpty() || box64Version.equals(DefaultVersion.BOX64)) {
                box64Version = isArm64EC() ? DefaultVersion.WOWBOX64 : DefaultVersion.BOX64_BIONIC;
            }
            if (WineInfo.isMainWineVersion(wineVersion)) {
                wineVersion = WineInfo.BIONIC_WINE_IDENTIFIER;
            }
            if (!isArm64EC()) {
                if (emulator == null || emulator.equals("FEXCore")) emulator = "Box64";
            }
            if ("dxvk".equals(dxwrapper)) dxwrapper = DEFAULT_DXWRAPPER_BIONIC;
        } else {
            String[] driverIds = GraphicsDrivers.parseIdentifiers(graphicsDriver);
            if (driverIds[0].equals(GraphicsDrivers.WRAPPER)) {
                graphicsDriver = GraphicsDrivers.TURNIP + "," + driverIds[1];
            }
            if (box64Version == null || box64Version.isEmpty() || box64Version.equals(DefaultVersion.BOX64_BIONIC)) {
                box64Version = DefaultVersion.BOX64;
            }
            if (emulator == null || emulator.equals("FEXCore")) emulator = "Box64";
            if (DEFAULT_DXWRAPPER_BIONIC.equals(dxwrapper)) dxwrapper = DEFAULT_DXWRAPPER;
        }
    }

    private static String[] migrateLudashiDXWrapperConfig(String flatConfig) {
        String dxvkPart = "";
        String vkd3dPart = "";
        String[] pairs = flatConfig.split(",");
        for (String pair : pairs) {
            if (pair.isEmpty()) continue;
            String[] kv = pair.split("=", 2);
            if (kv.length != 2) continue;
            String key = kv[0].trim();
            String val = kv[1].trim();
            switch (key) {
                case "version": case "framerate": case "async": case "asyncCache":
                case "ddrawrapper": case "csmt": case "gpuName": case "videoMemorySize":
                case "strict_shader_math": case "OffscreenRenderingMode": case "renderer":
                    dxvkPart += (dxvkPart.isEmpty() ? "" : ",") + key + "=" + val;
                    break;
                case "vkd3dVersion":
                    vkd3dPart += (vkd3dPart.isEmpty() ? "" : ",") + "version=" + val;
                    break;
                case "vkd3dLevel":
                    String featureLevel = val.replace("_", ".");
                    vkd3dPart += (vkd3dPart.isEmpty() ? "" : ",") + "featureLevel=" + featureLevel;
                    break;
            }
        }
        return new String[]{dxvkPart, vkd3dPart};
    }

    public static KeyValueSet[] migrateLudashiDXWrapperConfigToKeyValueSet(String flatConfig) {
        String[] parts = migrateLudashiDXWrapperConfig(flatConfig);
        return new KeyValueSet[]{new KeyValueSet(parts[0]), new KeyValueSet(parts[1])};
    }

    public static void checkObsoleteOrMissingProperties(JSONObject data) {
        try {
            if (data.has("dxcomponents")) {
                data.put("wincomponents", data.getString("dxcomponents"));
                data.remove("dxcomponents");
            }
            if (data.has("dxwrapper")) {
                String dxwrapper = data.getString("dxwrapper");
                if (dxwrapper.equals("original-wined3d")) data.put("dxwrapper", DEFAULT_DXWRAPPER_BIONIC);
            }
            if (data.has("graphicsDriver")) {
                String gd = data.getString("graphicsDriver");
                if (gd.equals("turnip-zink") || gd.equals("turnip") || gd.equals("llvmpipe")) data.put("graphicsDriver", "wrapper");
            }
            if (data.has("envVars")) {
                boolean isBionic = data.has("containerVariant") && data.getString("containerVariant").equals(BIONIC);
                boolean shouldPatch = true;
                if (data.has("extraData")) {
                    JSONObject extraData = data.getJSONObject("extraData");
                    int appVersion = Integer.parseInt(extraData.optString("appVersion", "0"));
                    shouldPatch = appVersion < 16;
                }
                if (shouldPatch) {
                    EnvVars defaultEnvVars = new EnvVars(isBionic ? DEFAULT_ENV_VARS_BIONIC : DEFAULT_ENV_VARS_GLIBC);
                    EnvVars envVars = new EnvVars(data.getString("envVars"));
                    for (String name : defaultEnvVars) if (!envVars.has(name)) envVars.put(name, defaultEnvVars.get(name));
                    data.put("envVars", envVars.toString());
                }
            }
            if (data.has("wincomponents")) {
                KeyValueSet wincomponents1 = new KeyValueSet(DEFAULT_WINCOMPONENTS);
                KeyValueSet wincomponents2 = new KeyValueSet(data.getString("wincomponents"));
                String result = "";
                for (String[] wincomponent1 : wincomponents1) {
                    String value = wincomponent1[1];
                    for (String[] wincomponent2 : wincomponents2) {
                        if (wincomponent1[0].equals(wincomponent2[0])) { value = wincomponent2[1]; break; }
                    }
                    result += (!result.isEmpty() ? "," : "")+wincomponent1[0]+"="+value;
                }
                data.put("wincomponents", result);
            }
        }
        catch (JSONException e) {}
    }

    public static String getFallbackCPUList() {
        String cpuList = "";
        int numProcessors = Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < numProcessors; i++) cpuList += (!cpuList.isEmpty() ? "," : "")+i;
        return cpuList;
    }

    public static String getFallbackCPUListWoW64() {
        String cpuList = "";
        int numProcessors = Runtime.getRuntime().availableProcessors();
        for (int i = numProcessors / 2; i < numProcessors; i++) cpuList += (!cpuList.isEmpty() ? "," : "")+i;
        return cpuList;
    }
}
