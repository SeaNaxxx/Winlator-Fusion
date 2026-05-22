package com.winlator.fusion.runtime;

import android.content.Context;

import com.winlator.fusion.box64.Box64Preset;
import com.winlator.fusion.container.Container;
import com.winlator.fusion.container.GraphicsDrivers;
import com.winlator.fusion.core.Callback;
import com.winlator.fusion.core.EnvVars;
import com.winlator.fusion.core.FileUtils;
import com.winlator.fusion.core.LocaleHelper;
import com.winlator.fusion.core.WineInfo;
import com.winlator.fusion.xconnector.UnixSocketConfig;
import com.winlator.fusion.xenvironment.RootFS;
import com.winlator.fusion.xenvironment.RuntimeFS;
import com.winlator.fusion.xenvironment.XEnvironment;
import com.winlator.fusion.xenvironment.components.ALSAServerComponent;
import com.winlator.fusion.xenvironment.components.GuestProgramLauncherComponent;
import com.winlator.fusion.xenvironment.components.PulseAudioComponent;
import com.winlator.fusion.xenvironment.components.VirGLRendererComponent;
import com.winlator.fusion.xenvironment.components.VortekRendererComponent;
import com.winlator.fusion.alsaserver.ALSAClient;
import com.winlator.fusion.contentdialog.AudioDriverConfigDialog;

import java.io.File;

public class GlibcRuntimeEnvironment implements RuntimeEnvironment {
    private final RuntimeProfile profile;
    private final RootFSAdapter rootFSAdapter;
    private final Context context;
    private final GlibcRuntimeStrategy strategy;

    public GlibcRuntimeEnvironment(Context context) {
        this.context = context;
        this.profile = RuntimeProfile.forGlibc(context);
        this.rootFSAdapter = RootFSAdapter.forGlibc(context);
        this.strategy = new GlibcRuntimeStrategy(context);
    }

    @Override
    public RuntimeProfile getProfile() {
        return profile;
    }

    @Override
    public void prepare() {
        FileUtils.clear(profile.getTmpDir());
        File shmDir = new File(profile.getRootDir(), "/tmp/shm");
        if (!shmDir.isDirectory()) shmDir.mkdirs();
    }

    @Override
    public void setupBaseEnvVars(EnvVars envVars, WineInfo wineInfo) {
        if (wineInfo != null) {
            strategy.setWineInfo(wineInfo);
        }
        EnvVars baseVars = strategy.buildBaseEnvVars();
        envVars.putAll(baseVars);

        envVars.put("MESA_DEBUG", "silent");
        envVars.put("MESA_NO_ERROR", "1");
        envVars.put("WINE_DO_NOT_CREATE_DXGI_DEVICE_MANAGER", "1");
        envVars.put("WINEPREFIX", profile.getWinePrefix());
        envVars.put("APP_CACHE_DIR", context.getCacheDir().getAbsolutePath());
    }

    @Override
    public void setupGraphics(XEnvironment environment, EnvVars envVars, String[] graphicsDriver, com.winlator.fusion.core.KeyValueSet[] graphicsDriverConfig) {
        String rootPath = profile.getRootDir().getPath();

        envVars.put("X11_SERVER_PATH", rootPath + UnixSocketConfig.XSERVER_PATH);
        envVars.put("VORTEK_SERVER_PATH", rootPath + UnixSocketConfig.VORTEK_SERVER_PATH);

        if (graphicsDriver[0].equals(GraphicsDrivers.VORTEK)) {
            VortekRendererComponent.Options options = VortekRendererComponent.Options.fromKeyValueSet(context, graphicsDriverConfig[0]);
            environment.addComponent(new VortekRendererComponent(
                environment.getXServer(),
                UnixSocketConfig.create(rootPath, UnixSocketConfig.VORTEK_SERVER_PATH),
                options
            ));
        }

        if (graphicsDriver[1].equals(GraphicsDrivers.VIRGL)) {
            environment.addComponent(new VirGLRendererComponent(
                environment.getXServer(),
                UnixSocketConfig.create(rootPath, UnixSocketConfig.VIRGL_SERVER_PATH)
            ));
        }
    }

    @Override
    public void setupAudio(XEnvironment environment, EnvVars envVars, String audioDriver, com.winlator.fusion.core.KeyValueSet audioDriverConfig) {
        String rootPath = profile.getRootDir().getPath();

        if (audioDriver.equals(com.winlator.fusion.container.AudioDrivers.ALSA)) {
            envVars.put("ANDROID_ALSA_SERVER", rootPath + UnixSocketConfig.ALSA_SERVER_PATH);
            envVars.put("ANDROID_ASERVER_USE_SHM", ALSAClient.USE_SHARED_MEMORY ? "true" : "false");
            ALSAClient.Options options = ALSAClient.Options.fromKeyValueSet(audioDriverConfig);
            environment.addComponent(new ALSAServerComponent(
                UnixSocketConfig.create(rootPath, UnixSocketConfig.ALSA_SERVER_PATH), options
            ));
        } else if (audioDriver.equals(com.winlator.fusion.container.AudioDrivers.PULSEAUDIO)) {
            PulseAudioComponent pulseAudioComponent = new PulseAudioComponent(
                UnixSocketConfig.create(rootPath, UnixSocketConfig.PULSE_SERVER_PATH)
            );
            envVars.put("PULSE_SERVER", rootPath + UnixSocketConfig.PULSE_SERVER_PATH);

            if (!audioDriverConfig.isEmpty()) {
                envVars.put("PULSE_LATENCY_MSEC", audioDriverConfig.getInt("latencyMillis", AudioDriverConfigDialog.DEFAULT_LATENCY_MILLIS));
                pulseAudioComponent.setVolume(audioDriverConfig.getFloat("volume", AudioDriverConfigDialog.DEFAULT_VOLUME));
                pulseAudioComponent.setPerformanceMode(audioDriverConfig.getInt("performanceMode", AudioDriverConfigDialog.DEFAULT_PERFORMANCE_MODE));
            } else {
                envVars.put("PULSE_LATENCY_MSEC", AudioDriverConfigDialog.DEFAULT_LATENCY_MILLIS);
            }
            environment.addComponent(pulseAudioComponent);
        }
    }

    @Override
    public void setupLauncher(XEnvironment environment, Container container, WineInfo wineInfo, String guestExecutable, EnvVars envVars, String box64Preset, Callback<Integer> terminationCallback) {
        GuestProgramLauncherComponent launcher = new GuestProgramLauncherComponent();
        launcher.setGuestExecutable(guestExecutable);
        launcher.setContainer(container);
        launcher.setEnvVars(envVars);
        launcher.setBox64Preset(box64Preset != null ? box64Preset : Box64Preset.CONSERVATIVE);
        launcher.setTerminationCallback(terminationCallback);
        environment.addComponent(launcher);
    }

    @Override
    public void postSetup(XEnvironment environment, Container container) {
    }

    @Override
    public String getSocketPath(String relativeSocketPath) {
        return profile.getRootDir().getPath() + relativeSocketPath;
    }

    @Override
    public RootFSAdapter getRootFSAdapter() {
        return rootFSAdapter;
    }
}
