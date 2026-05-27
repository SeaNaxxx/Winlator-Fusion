package com.winlator.fusion.runtime;

import com.winlator.fusion.container.Container;
import com.winlator.fusion.xenvironment.ImageFs;
import com.winlator.fusion.xenvironment.RootFS;
import com.winlator.fusion.xenvironment.RuntimeFS;

import java.io.File;

public final class RuntimeProfile {
    private final RuntimeType type;
    private final File rootDir;
    private final File wineDir;
    private final File libDir;
    private final File tmpDir;
    private final File socketDir;
    private final File binDir;
    private final File etcDir;
    private final File shareDir;
    private final boolean usesImageFs;
    private final String homePath;
    private final String winePrefix;
    private final String variant;

    private RuntimeProfile(Builder builder) {
        this.type = builder.type;
        this.rootDir = builder.rootDir;
        this.wineDir = builder.wineDir;
        this.libDir = builder.libDir;
        this.tmpDir = builder.tmpDir;
        this.socketDir = builder.socketDir;
        this.binDir = builder.binDir;
        this.etcDir = builder.etcDir;
        this.shareDir = builder.shareDir;
        this.usesImageFs = builder.usesImageFs;
        this.homePath = builder.homePath;
        this.winePrefix = builder.winePrefix;
        this.variant = builder.variant;
    }

    public RuntimeType getType() { return type; }
    public File getRootDir() { return rootDir; }
    public File getWineDir() { return wineDir; }
    public File getLibDir() { return libDir; }
    public File getTmpDir() { return tmpDir; }
    public File getSocketDir() { return socketDir; }
    public File getBinDir() { return binDir; }
    public File getEtcDir() { return etcDir; }
    public File getShareDir() { return shareDir; }
    public boolean usesImageFs() { return usesImageFs; }
    public String getHomePath() { return homePath; }
    public String getWinePrefix() { return winePrefix; }
    public String getVariant() { return variant; }

    public boolean isBionic() { return type == RuntimeType.BIONIC; }

    public static RuntimeProfile forContainer(android.content.Context context, Container container) {
        if (container.isBionic()) {
            return forBionic(context);
        }
        return forGlibc(context);
    }

    public static RuntimeProfile forGlibc(android.content.Context context) {
        RootFS rootFS = RootFS.find(context);
        RuntimeFS runtimeFS = RuntimeFS.find(context);
        File rootDir = rootFS.getRootDir();

        return new Builder()
            .type(RuntimeType.GLIBC)
            .rootDir(rootDir)
            .wineDir(new File(rootDir, rootFS.getWinePath()))
            .libDir(rootFS.getLibDir())
            .tmpDir(rootFS.getTmpDir())
            .socketDir(new File(rootDir, "tmp"))
            .binDir(new File(rootDir, "usr/local/bin"))
            .etcDir(rootFS.getEtcDir())
            .shareDir(rootFS.getShareDir())
            .usesImageFs(false)
            .homePath(rootDir + RootFS.HOME_PATH)
            .winePrefix(rootDir + RootFS.WINEPREFIX)
            .variant(Container.GLIBC)
            .build();
    }

    public static RuntimeProfile forBionic(android.content.Context context) {
        ImageFs imageFs = ImageFs.find(context);
        RuntimeFS runtimeFS = RuntimeFS.find(context);
        File rootDir = imageFs.getRootDir();

        return new Builder()
            .type(RuntimeType.BIONIC)
            .rootDir(rootDir)
            .wineDir(new File(imageFs.getWinePath()))
            .libDir(imageFs.getLibDir())
            .tmpDir(imageFs.getTmpDir())
            .socketDir(new File(rootDir, "tmp"))
            .binDir(imageFs.getBinDir())
            .etcDir(imageFs.getEtcDir())
            .shareDir(imageFs.getShareDir())
            .usesImageFs(true)
            .homePath(imageFs.home_path)
            .winePrefix(imageFs.wineprefix)
            .variant(Container.BIONIC)
            .build();
    }

    public static final class Builder {
        private RuntimeType type;
        private File rootDir;
        private File wineDir;
        private File libDir;
        private File tmpDir;
        private File socketDir;
        private File binDir;
        private File etcDir;
        private File shareDir;
        private boolean usesImageFs;
        private String homePath;
        private String winePrefix;
        private String variant;

        public Builder type(RuntimeType type) { this.type = type; return this; }
        public Builder rootDir(File rootDir) { this.rootDir = rootDir; return this; }
        public Builder wineDir(File wineDir) { this.wineDir = wineDir; return this; }
        public Builder libDir(File libDir) { this.libDir = libDir; return this; }
        public Builder tmpDir(File tmpDir) { this.tmpDir = tmpDir; return this; }
        public Builder socketDir(File socketDir) { this.socketDir = socketDir; return this; }
        public Builder binDir(File binDir) { this.binDir = binDir; return this; }
        public Builder etcDir(File etcDir) { this.etcDir = etcDir; return this; }
        public Builder shareDir(File shareDir) { this.shareDir = shareDir; return this; }
        public Builder usesImageFs(boolean usesImageFs) { this.usesImageFs = usesImageFs; return this; }
        public Builder homePath(String homePath) { this.homePath = homePath; return this; }
        public Builder winePrefix(String winePrefix) { this.winePrefix = winePrefix; return this; }
        public Builder variant(String variant) { this.variant = variant; return this; }

        public RuntimeProfile build() { return new RuntimeProfile(this); }
    }
}
