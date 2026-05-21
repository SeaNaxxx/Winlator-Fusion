package com.winlator.fusion.runtime;

import com.winlator.fusion.container.Container;
import com.winlator.fusion.core.Callback;
import com.winlator.fusion.core.EnvVars;
import com.winlator.fusion.core.WineInfo;
import com.winlator.fusion.xenvironment.XEnvironment;

public interface RuntimeEnvironment {
    RuntimeProfile getProfile();
    void prepare();
    void setupBaseEnvVars(EnvVars envVars, WineInfo wineInfo);
    void setupGraphics(XEnvironment environment, EnvVars envVars, String[] graphicsDriver, com.winlator.fusion.core.KeyValueSet[] graphicsDriverConfig);
    void setupAudio(XEnvironment environment, EnvVars envVars, String audioDriver, com.winlator.fusion.core.KeyValueSet audioDriverConfig);
    void setupLauncher(XEnvironment environment, Container container, WineInfo wineInfo, String guestExecutable, EnvVars envVars, String box64Preset, Callback<Integer> terminationCallback);
    void postSetup(XEnvironment environment, Container container);
    String getSocketPath(String relativeSocketPath);
    RootFSAdapter getRootFSAdapter();
}
