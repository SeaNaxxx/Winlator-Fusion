package com.winlator.fusion.xenvironment.components;

import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Process;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.winlator.fusion.box64.Box64Preset;
import com.winlator.fusion.box64.Box64PresetManager;
import com.winlator.fusion.container.Container;
import com.winlator.fusion.contents.AdrenotoolsManager;
import com.winlator.fusion.contents.ContentProfile;
import com.winlator.fusion.contents.ContentsManager;
import com.winlator.fusion.core.Callback;
import com.winlator.fusion.core.DefaultVersion;
import com.winlator.fusion.core.EnvVars;
import com.winlator.fusion.core.FileUtils;
import com.winlator.fusion.core.GPUHelper;
import com.winlator.fusion.core.GeneralComponents;
import com.winlator.fusion.core.ProcessHelper;
import com.winlator.fusion.core.TarCompressorUtils;
import com.winlator.fusion.core.WineInfo;
import com.winlator.fusion.fexcore.FEXCorePreset;
import com.winlator.fusion.fexcore.FEXCorePresetManager;
import com.winlator.fusion.runtime.RuntimeStrategy;
import com.winlator.fusion.xconnector.UnixSocketConfig;
import com.winlator.fusion.xenvironment.EnvironmentComponent;
import com.winlator.fusion.xenvironment.ImageFs;
import com.winlator.fusion.xenvironment.RuntimeFS;
import com.winlator.fusion.xenvironment.XEnvironment;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class BionicProgramLauncherComponent extends EnvironmentComponent {
    private String guestExecutable;
    private static int pid = -1;
    private EnvVars envVars;
    private WineInfo wineInfo;
    private String box64Preset = Box64Preset.CONSERVATIVE;
    private Callback<Integer> terminationCallback;
    private Container container;
    private ContentsManager contentsManager;
    private static final Object lock = new Object();

    public void setWineInfo(WineInfo wineInfo) {
        this.wineInfo = wineInfo;
    }

    public WineInfo getWineInfo() {
        return this.wineInfo;
    }

    public Container getContainer() {
        return this.container;
    }

    public void setContainer(Container container) {
        this.container = container;
    }

    public BionicProgramLauncherComponent() {
    }

    public BionicProgramLauncherComponent(ContentsManager contentsManager) {
        this.contentsManager = contentsManager;
    }

    @Override
    public void start() {
        synchronized (lock) {
            stop();
            if (container == null) {
                Log.e("BionicLauncher", "Container is null, cannot start");
                return;
            }
            if (wineInfo != null && wineInfo.isArm64EC()) {
                extractEmulatorsDlls();
            } else {
                extractBox64File();
                copyDefaultBox64RCFile();
            }
            pid = execGuestProgram();
        }
    }

    @Override
    public void stop() {
        synchronized (lock) {
            if (pid != -1) {
                Process.killProcess(pid);
                pid = -1;
            }
        }
    }

    public Callback<Integer> getTerminationCallback() {
        return terminationCallback;
    }

    public void setTerminationCallback(Callback<Integer> terminationCallback) {
        this.terminationCallback = terminationCallback;
    }

    public String getGuestExecutable() {
        return guestExecutable;
    }

    public void setGuestExecutable(String guestExecutable) {
        this.guestExecutable = guestExecutable;
    }

    public EnvVars getEnvVars() {
        return envVars;
    }

    public void setEnvVars(EnvVars envVars) {
        this.envVars = envVars;
    }

    public String getBox64Preset() {
        return box64Preset;
    }

    public void setBox64Preset(String box64Preset) {
        this.box64Preset = box64Preset;
    }

    private void extractBox64File() {
        if (wineInfo != null && wineInfo.isArm64EC()) return;

        Context context = environment.getContext();
        ImageFs imageFs = ImageFs.find(context);
        File rootDir = imageFs.getRootDir();
        String box64Version = container != null ? container.getBox64Version() : DefaultVersion.BOX64_BIONIC;

        if (contentsManager != null) {
            ContentProfile profile = contentsManager.getProfileByVersionName(ContentProfile.ContentType.CONTENT_TYPE_BOX64, box64Version);
            if (profile != null) {
                contentsManager.applyContent(profile);
            } else {
                try {
                    TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context, "box64/box64-" + box64Version + ".tzst", rootDir);
                } catch (Exception e) {
                    GeneralComponents.extractFile(GeneralComponents.Type.BOX64, context, DefaultVersion.BOX64_BIONIC, DefaultVersion.BOX64, rootDir, null);
                }
            }
        } else {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            String currentBox64Version = preferences.getString("current_box64_bionic_version", "");
            if (!box64Version.equals(currentBox64Version)) {
                GeneralComponents.extractFile(GeneralComponents.Type.BOX64, context, DefaultVersion.BOX64_BIONIC, DefaultVersion.BOX64, rootDir, null);
                preferences.edit().putString("current_box64_bionic_version", box64Version).apply();
            }
        }

        File box64File = new File(rootDir, "/usr/bin/box64");
        if (box64File.exists()) {
            FileUtils.chmod(box64File, 0755);
        }
    }

    private void copyDefaultBox64RCFile() {
        Context context = environment.getContext();
        ImageFs imageFs = ImageFs.find(context);
        File rcFile = new File(imageFs.getRootDir(), "/etc/config.box64rc");
        if (!rcFile.exists()) {
            FileUtils.copy(context, "box64/default.box64rc", rcFile);
        }
    }

    private void extractEmulatorsDlls() {
        if (container == null) {
            Log.e("BionicLauncher", "Container is null, cannot extract emulator DLLs");
            return;
        }

        Context context = environment.getContext();
        File rootDir = ImageFs.find(context).getRootDir();
        File system32dir = new File(rootDir + "/home/xuser/.wine/drive_c/windows/system32");
        boolean containerDataChanged = false;

        String wowbox64Version = container.getBox64Version();
        String fexcoreVersion = container.getFEXCoreVersion();

        if (!wowbox64Version.equals(container.getExtra("box64Version"))) {
            if (contentsManager != null) {
                ContentProfile profile = contentsManager.getProfileByVersionName(ContentProfile.ContentType.CONTENT_TYPE_WOWBOX64, wowbox64Version);
                if (profile != null)
                    contentsManager.applyContent(profile);
                else
                    TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context, "wowbox64/wowbox64-" + wowbox64Version + ".tzst", system32dir);
            }
            container.putExtra("box64Version", wowbox64Version);
            containerDataChanged = true;
        }

        if (!fexcoreVersion.equals(container.getExtra("fexcoreVersion"))) {
            if (contentsManager != null) {
                ContentProfile profile = contentsManager.getProfileByVersionName(ContentProfile.ContentType.CONTENT_TYPE_FEXCORE, fexcoreVersion);
                if (profile != null)
                    contentsManager.applyContent(profile);
                else
                    TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context, "fexcore/fexcore-" + fexcoreVersion + ".tzst", system32dir);
            }
            container.putExtra("fexcoreVersion", fexcoreVersion);
            containerDataChanged = true;
        }

        if (containerDataChanged) container.saveData();
    }

    private int execGuestProgram() {
        Context context = environment.getContext();
        ImageFs imageFs = ImageFs.find(context);
        File rootDir = imageFs.getRootDir();

        EnvVars envVars = new EnvVars();

        RuntimeStrategy strategy = container != null ? container.getRuntimeStrategy(context) : null;

        if (strategy != null) {
            envVars.putAll(strategy.buildBaseEnvVars());

            String ldPreload = strategy.buildLdPreload();
            if (!ldPreload.isEmpty()) {
                envVars.put("LD_PRELOAD", ldPreload);
            }

            strategy.addRuntimeSpecificEnvVars(envVars, wineInfo);

            if (!envVars.has("WINEPREFIX")) {
                envVars.put("WINEPREFIX", strategy.getWinePrefix());
            }
            envVars.put("MESA_DEBUG", "silent");
            envVars.put("MESA_NO_ERROR", "1");
            envVars.put("WINE_DO_NOT_CREATE_DXGI_DEVICE_MANAGER", "1");
            envVars.put("APP_CACHE_DIR", context.getCacheDir().getAbsolutePath());
        }

        if (wineInfo == null || !wineInfo.isArm64EC()) {
            addBox64EnvVars(envVars);
        }

        String fexcorePreset = container != null ? container.getFEXCorePreset() : FEXCorePreset.DEFAULT;
        boolean isArm64EC = wineInfo != null && wineInfo.isArm64EC();
        if (!isArm64EC && container != null) {
            WineInfo containerWineInfo = WineInfo.fromIdentifier(context, container.getWineVersion());
            isArm64EC = containerWineInfo.isArm64EC();
        }
        if (isArm64EC) {
            envVars.putAll(FEXCorePresetManager.getEnvVars(context, fexcorePreset));
        }

        String renderer = GPUHelper.glGetRenderer(context);
        if (renderer.contains("Mali"))
            envVars.put("BOX64_MMAP32", "0");
        if ("1".equals(envVars.get("BOX64_MMAP32")) && (wineInfo == null || !wineInfo.isArm64EC())) {
            envVars.put("WRAPPER_DISABLE_PLACED", "1");
        }

        String wineVersion = container != null ? container.getWineVersion() : WineInfo.BIONIC_WINE_IDENTIFIER;
        String winePath;
        if (strategy != null) {
            winePath = strategy.getWineBinaryPath(wineInfo != null ? wineInfo : WineInfo.fromIdentifier(context, wineVersion));
        } else if (WineInfo.isBionicDefaultWineVersion(wineVersion) || WineInfo.isMainWineVersion(wineVersion)) {
            winePath = imageFs.getWinePathForVersion(WineInfo.BIONIC_WINE_IDENTIFIER) + "/bin";
        } else {
            winePath = imageFs.getWinePathForVersion(wineVersion) + "/bin";
        }
        Log.d("BionicLauncher", "Wine version: " + wineVersion + ", WinePath: " + winePath);

        File wineBinDir = new File(winePath);
        if (!wineBinDir.isDirectory()) {
            Log.e("BionicLauncher", "Wine binary directory not found: " + winePath + " - Proton not installed");
            if (terminationCallback != null) terminationCallback.call(1);
            return -1;
        }

        File devInputDir = new File(rootDir, "dev/input");
        devInputDir.mkdirs();
        for (int i = 0; i < 4; i++) {
            File eventFile = new File(devInputDir, "event" + i);
            if (eventFile.exists()) eventFile.delete();
        }
        try { new File(devInputDir, "event0").createNewFile(); } catch (Exception e) {}
        envVars.put("FAKE_EVDEV_DIR", devInputDir.getAbsolutePath());
        envVars.put("FAKE_EVDEV_VIBRATION", "1");

        if (this.envVars != null) envVars.putAll(this.envVars);

        String command;
        String overriddenCommand = envVars.get("GUEST_PROGRAM_LAUNCHER_COMMAND");
        if (!overriddenCommand.isEmpty()) {
            command = overriddenCommand.replace(";", " ");
        } else if (strategy != null) {
            command = strategy.buildLaunchCommand(guestExecutable, wineInfo);
        } else {
            command = imageFs.getBinDir() + "/box64 " + guestExecutable;
        }

        File box64File = new File(rootDir, "/usr/bin/box64");
        if (box64File.exists()) {
            FileUtils.chmod(box64File, 0755);
        }

        return ProcessHelper.exec(command, envVars, rootDir, (status) -> {
            synchronized (lock) {
                pid = -1;
            }
            if (terminationCallback != null) terminationCallback.call(status);
        });
    }

    private void addBox64EnvVars(EnvVars envVars) {
        Context context = environment.getContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean enableBox64Logs = preferences.getBoolean("enable_box64_logs", false);

        envVars.put("BOX64_NOBANNER", enableBox64Logs ? "0" : "1");
        envVars.put("BOX64_DYNAREC", "1");
        envVars.put("BOX64_NORCFILES", "1");
        envVars.put("BOX64_X11GLX", "1");

        if (enableBox64Logs) {
            envVars.put("BOX64_LOG", "1");
            envVars.put("BOX64_DYNAREC_MISSING", "1");
        }

        envVars.putAll(Box64PresetManager.getEnvVars(context, box64Preset));

        ImageFs imageFs = ImageFs.find(context);
        File box64RCFile = new File(imageFs.getRootDir(), "/etc/config.box64rc");
        envVars.put("BOX64_RCFILE", box64RCFile.getPath());
    }

    @Override
    public void onPause() {
        synchronized (lock) {
            if (pid != -1) {
                List<ProcessHelper.PStat> processes = ProcessHelper.getChildProcesses();
                for (int i = processes.size()-1; i >= 0; i--) {
                    ProcessHelper.PStat process = processes.get(i);
                    if (process.guestProcess && process.state != ProcessHelper.PState.STOPPED) {
                        ProcessHelper.suspendProcess(process.pid);
                    }
                }
            }
        }
    }

    @Override
    public void onResume() {
        synchronized (lock) {
            if (pid != -1) {
                List<ProcessHelper.PStat> processes = ProcessHelper.getChildProcesses();
                for (int i = 0; i < processes.size(); i++) {
                    ProcessHelper.PStat process = processes.get(i);
                    if (process.guestProcess && process.state == ProcessHelper.PState.STOPPED) {
                        ProcessHelper.resumeProcess(process.pid);
                    }
                }
            }
        }
    }
}
