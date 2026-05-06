package com.winlator.nova.xenvironment.components;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Process;

import androidx.preference.PreferenceManager;

import com.winlator.nova.box64.Box64Preset;
import com.winlator.nova.box64.Box64PresetManager;
import com.winlator.nova.container.Container;
import com.winlator.nova.core.Callback;
import com.winlator.nova.core.DefaultVersion;
import com.winlator.nova.core.EnvVars;
import com.winlator.nova.core.FileUtils;
import com.winlator.nova.core.GeneralComponents;
import com.winlator.nova.core.LocaleHelper;
import com.winlator.nova.core.ProcessHelper;
import com.winlator.nova.core.RootFSPatcher;
import com.winlator.nova.fexcore.FEXCorePreset;
import com.winlator.nova.fexcore.FEXCorePresetManager;
import com.winlator.nova.widget.LogView;
import com.winlator.nova.xconnector.UnixSocketConfig;
import com.winlator.nova.xenvironment.EnvironmentComponent;
import com.winlator.nova.xenvironment.RootFS;

import java.io.File;
import java.util.List;

public class GuestProgramLauncherComponent extends EnvironmentComponent {
    private String guestExecutable;
    private static int pid = -1;
    private EnvVars envVars;
    private String box64Preset = Box64Preset.CONSERVATIVE;
    private String fexcorePreset = FEXCorePreset.DEFAULT;
    private String emulator = Container.EMULATOR_BOX64;
    private Callback<Integer> terminationCallback;
    private static final Object lock = new Object();

    @Override
    public void start() {
        synchronized (lock) {
            stop();
            if (emulator.equals(Container.EMULATOR_FEXCORE)) {
                extractFEXCoreFiles();
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

    public String getFEXCorePreset() {
        return fexcorePreset;
    }

    public void setFEXCorePreset(String fexcorePreset) {
        this.fexcorePreset = fexcorePreset;
    }

    public String getEmulator() {
        return emulator;
    }

    public void setEmulator(String emulator) {
        this.emulator = emulator;
    }

    private int execGuestProgram() {
        RootFS rootFS = environment.getRootFS();
        File rootDir = rootFS.getRootDir();

        EnvVars envVars = new EnvVars();
        boolean isFEXCore = emulator.equals(Container.EMULATOR_FEXCORE);

        if (isFEXCore) {
            addFEXCoreEnvVars(envVars);
        } else {
            addBox64EnvVars(envVars);
        }
        LocaleHelper.setEnvVars(envVars);

        envVars.put("HOME", rootDir+RootFS.HOME_PATH);
        envVars.put("USER", RootFS.USER);
        envVars.put("TMPDIR", rootDir+"/tmp");
        envVars.put("DISPLAY", ":0");
        envVars.put("PATH", rootDir+rootFS.getWinePath()+"/bin:"+rootDir+"/usr/local/bin:"+rootDir+"/usr/bin");

        // Set LD_LIBRARY_PATH to include all library directories
        // This is critical because ld.so.cache has been deleted and we need
        // the linker to find libraries by path
        envVars.put("LD_LIBRARY_PATH", rootDir+"/lib:"+rootDir+"/usr/lib:"+rootDir+"/usr/lib/aarch64-linux-gnu");

        if (!isFEXCore) {
            envVars.put("BOX64_LD_LIBRARY_PATH", rootDir+"/lib/x86_64-linux-gnu");
        }

        envVars.put("ANDROID_SYSVSHM_SERVER", rootDir+UnixSocketConfig.SYSVSHM_SERVER_PATH);

        if (this.envVars != null) envVars.putAll(this.envVars);

        File shmDir = new File(rootDir, "/tmp/shm");
        if (!shmDir.isDirectory()) shmDir.mkdirs();

        String command;
        if (isFEXCore) {
            command = guestExecutable;
        } else {
            // Use explicit dynamic linker invocation to bypass broken INTERP header
            // in box64 binary (which points to /data/data/com.winlator/... instead of
            // /data/data/com.winlator.nova/...). By invoking the linker directly with
            // --library-path, we ensure correct library resolution regardless of the
            // binary's embedded INTERP/RPATH values.
            String linker = rootDir+"/lib/ld-linux-aarch64.so.1";
            String libraryPath = rootDir+"/lib:"+rootDir+"/usr/lib";
            String box64Path = rootDir+"/usr/local/bin/box64";
            command = linker+" --library-path "+libraryPath+" "+box64Path+" "+guestExecutable;
        }

        return ProcessHelper.exec(command, envVars, rootDir, (status) -> {
            synchronized (lock) {
                pid = -1;
            }
            if (terminationCallback != null) terminationCallback.call(status);
        });
    }

    private void extractBox64File() {
        Context context = environment.getContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String box64Version = preferences.getString("box64_version", DefaultVersion.BOX64);
        String currentBox64Version = preferences.getString("current_box64_version", "");

        if (!box64Version.equals(currentBox64Version)) {
            GeneralComponents.extractFile(GeneralComponents.Type.BOX64, context, box64Version, DefaultVersion.BOX64);
            preferences.edit().putString("current_box64_version", box64Version).apply();

            // Patch the box64 binary to fix hardcoded RPATH/INTERP paths
            // The binary in the tarball was built for com.winlator but we use com.winlator.nova
            RootFS rootFS = environment.getRootFS();
            File box64Bin = new File(rootFS.getRootDir(), "/usr/local/bin/box64");
            if (box64Bin.exists()) {
                RootFSPatcher.patchElfRpath(box64Bin);
            }

            // Also patch any .so files extracted from wrapper.tzst or adrenotools drivers
            patchSharedLibraries(rootFS.getRootDir());
        }
    }

    /**
     * Patches shared library .so files in the rootfs that may contain
     * hardcoded package paths (e.g., from Ludashi/Cmod wrapper.tzst).
     */
    private void patchSharedLibraries(File rootDir) {
        File[] dirs = {new File(rootDir, "usr/lib"), new File(rootDir, "lib")};
        for (File dir : dirs) {
            if (!dir.isDirectory()) continue;
            File[] files = dir.listFiles();
            if (files == null) continue;
            for (File file : files) {
                String name = file.getName();
                if (name.endsWith(".so") || (name.startsWith("lib") && name.contains(".so."))) {
                    RootFSPatcher.patchElfRpath(file);
                }
            }
        }
    }

    private void copyDefaultBox64RCFile() {
        Context context = environment.getContext();
        RootFS rootFS = environment.getRootFS();
        FileUtils.copy(context, "box64/default.box64rc", new File(rootFS.getRootDir(), "/etc/config.box64rc"));
    }

    private void extractFEXCoreFiles() {
        // FEX-Core files are extracted via the contents system
        // The FEX-Core shared libraries (libwow64fex.dll, libarm64ecfex.dll)
        // are installed to the Windows system directory through ContentManager.applyContent()
    }

    private void addFEXCoreEnvVars(EnvVars envVars) {
        Context context = environment.getContext();

        // Set HODLL for ARM64EC Wine when using FEX-Core
        envVars.put("HODLL", "libwow64fex.dll");

        // Apply FEX-Core preset env vars
        envVars.putAll(FEXCorePresetManager.getEnvVars(context, fexcorePreset));
    }

    private void addBox64EnvVars(EnvVars envVars) {
        Context context = environment.getContext();
        RootFS rootFS = environment.getRootFS();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int box64Logs = preferences.getInt("box64_logs", 0);
        boolean saveToFile = preferences.getBoolean("save_logs_to_file", false);

        envVars.put("BOX64_NOBANNER", box64Logs >= 1 ? "0" : "1");
        envVars.put("BOX64_DYNAREC", "1");
        envVars.put("BOX64_UNITYPLAYER", "0");

        if (box64Logs >= 1) {
            envVars.put("BOX64_LOG", "1");
            envVars.put("BOX64_DYNAREC_MISSING", "1");

            if (box64Logs == 2) {
                envVars.put("BOX64_SHOWSEGV", "1");
                envVars.put("BOX64_DLSYM_ERROR", "1");
                envVars.put("BOX64_TRACE_FILE", "stderr");

                if (saveToFile) {
                    File parent = (new File(preferences.getString("log_file", LogView.getLogFile().getPath()))).getParentFile();
                    if (parent != null && parent.isDirectory()) {
                        File traceDir = new File(parent, "trace");
                        if (!traceDir.isDirectory()) traceDir.mkdirs();
                        FileUtils.clear(traceDir);

                        envVars.put("BOX64_TRACE_FILE", traceDir+"/box64-%pid.txt");
                    }
                }
            }
        }

        envVars.putAll(Box64PresetManager.getEnvVars(context, box64Preset));

        File box64RCFile = new File(rootFS.getRootDir(), "/etc/config.box64rc");
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