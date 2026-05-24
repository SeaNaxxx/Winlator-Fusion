package com.winlator.fusion.xenvironment.components;

import android.content.Context;
import android.os.Process;

import com.winlator.fusion.contentdialog.AudioDriverConfigDialog;
import com.winlator.fusion.core.EnvVars;
import com.winlator.fusion.core.FileUtils;
import com.winlator.fusion.core.ProcessHelper;
import com.winlator.fusion.xconnector.UnixSocketConfig;
import com.winlator.fusion.xenvironment.EnvironmentComponent;

import java.io.File;

public class PulseAudioComponent extends EnvironmentComponent {
    public static final String PROFILE_MINIMAL = "minimal";
    public static final String PROFILE_DESKTOP = "desktop";

    private final UnixSocketConfig socketConfig;
    private String audioProfile = PROFILE_MINIMAL;
    private int pid = -1;
    private float volume = AudioDriverConfigDialog.DEFAULT_VOLUME;
    private byte performanceMode = AudioDriverConfigDialog.DEFAULT_PERFORMANCE_MODE;
    private static final Object lock = new Object();

    public PulseAudioComponent(UnixSocketConfig socketConfig) {
        this.socketConfig = socketConfig;
    }

    public void setAudioProfile(String audioProfile) {
        this.audioProfile = audioProfile;
    }

    @Override
    public void start() {
        synchronized (lock) {
            stop();
            pid = execPulseAudio();
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

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public int getPerformanceMode() {
        return performanceMode;
    }

    public void setPerformanceMode(int performanceMode) {
        this.performanceMode = (byte)performanceMode;
    }

    private int execPulseAudio() {
        Context context = environment.getContext();
        String nativeLibraryDir = context.getApplicationInfo().nativeLibraryDir;
        String pulseSubdir = PROFILE_DESKTOP.equals(audioProfile) ? "bionic" : "glibc";
        File workingDir = new File(context.getFilesDir(), "/pulseaudio/" + pulseSubdir);
        if (!workingDir.isDirectory()) {
            workingDir.mkdirs();
            FileUtils.chmod(workingDir, 0771);
        }

        File configFile = new File(workingDir, "default.pa");
        String config;
        if (PROFILE_DESKTOP.equals(audioProfile)) {
            config = String.join("\n",
                "load-module module-augment-properties",
                "load-module module-native-protocol-unix auth-anonymous=1 auth-cookie-enabled=0 socket=\""+socketConfig.path+"\"",
                "load-module module-aaudio-sink volume="+volume+" performance_mode="+performanceMode,
                "load-module module-null-sink sink_name=null sink_properties=device.description=\"NullSink\"",
                "load-module module-remap-sink remix=no",
                "load-module module-always-sink",
                "set-default-sink AAudioSink"
            );
        } else {
            config = String.join("\n",
                "load-module module-native-protocol-unix auth-anonymous=1 auth-cookie-enabled=0 socket=\""+socketConfig.path+"\"",
                "load-module module-aaudio-sink volume="+volume+" performance_mode="+performanceMode,
                "set-default-sink AAudioSink"
            );
        }
        FileUtils.writeString(configFile, config);

        File modulesDir = new File(workingDir, "modules");
        EnvVars envVars = new EnvVars();
        StringBuilder ldPath = new StringBuilder();
        ldPath.append("/system/lib64").append(":").append(nativeLibraryDir).append(":").append(modulesDir);
        if (PROFILE_DESKTOP.equals(audioProfile)) {
            File arm64Dir = new File(modulesDir, "arm64");
            File armhfDir = new File(modulesDir, "armhf");
            if (arm64Dir.isDirectory()) ldPath.append(":").append(arm64Dir);
            if (armhfDir.isDirectory()) ldPath.append(":").append(armhfDir);
        }
        envVars.put("LD_LIBRARY_PATH", ldPath.toString());
        envVars.put("HOME", workingDir);
        envVars.put("TMPDIR", environment.getTmpDir());

        String command = nativeLibraryDir+"/libpulseaudio.so";
        command += " --system=false";
        command += " --disable-shm=true";
        command += " --fail=false";
        command += " -n --file=default.pa";
        command += " --daemonize=false";
        command += " --use-pid-file=false";
        command += " --exit-idle-time=-1";

        return ProcessHelper.exec(command, envVars, workingDir);
    }
}
