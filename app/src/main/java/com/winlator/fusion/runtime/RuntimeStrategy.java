package com.winlator.fusion.runtime;

import com.winlator.fusion.core.EnvVars;
import com.winlator.fusion.core.WineInfo;
import com.winlator.fusion.xenvironment.ImageFs;
import com.winlator.fusion.xenvironment.RootFS;

import java.io.File;
import java.util.List;

public interface RuntimeStrategy {

    String getVariant();

    String getDisplayName();

    File getRootDir();

    String getHomePath();

    File getTmpDir();

    File getLibDir();

    File getBinDir();

    String getWinePath();

    String getWinePrefix();

    String getBox64Path();

    String getBox64Version();

    String getBox64VersionPrefKey();

    String getBox64AssetPath(String version);

    boolean hasBox64LdLibraryPath();

    String getBox64LdLibraryPath();

    String getDefaultEnvVars();

    EnvVars buildBaseEnvVars();

    String buildLdPreload();

    String getLdLibraryPath();

    boolean supportsVortek();

    boolean supportsAdrenotools();

    boolean supportsFEXCore();

    boolean supportsZink();

    boolean supportsWrapper();

    boolean needsEvshimPatching();

    boolean needsFEXCoreConfig();

    String getWineBinaryPath(WineInfo wineInfo);

    String getPathEnvVar(WineInfo wineInfo);

    RootFS getRootFS();

    String buildLaunchCommand(String guestExecutable, WineInfo wineInfo);

    void addRuntimeSpecificEnvVars(EnvVars envVars, WineInfo wineInfo);

    void extractBinaries();

    List<String> getSupportedDrivers();
}
