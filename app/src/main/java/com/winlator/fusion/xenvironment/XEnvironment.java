package com.winlator.fusion.xenvironment;

import android.content.Context;

import com.winlator.fusion.core.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

public class XEnvironment implements Iterable<EnvironmentComponent> {
    private final Context context;
    private final FusionFS fusionFS;
    private final RootFS rootFS;
    private com.winlator.fusion.xserver.XServer xServer;
    private final ArrayList<EnvironmentComponent> components = new ArrayList<>();

    public XEnvironment(Context context, FusionFS fusionFS) {
        this.context = context;
        this.fusionFS = fusionFS;
        this.rootFS = RootFS.fromDir(fusionFS.getGlibcDir());
    }

    public XEnvironment(Context context, FusionFS fusionFS, com.winlator.fusion.xserver.XServer xServer) {
        this.context = context;
        this.fusionFS = fusionFS;
        this.rootFS = RootFS.fromDir(fusionFS.getGlibcDir());
        this.xServer = xServer;
    }

    public XEnvironment(Context context, RootFS rootFS) {
        this.context = context;
        this.fusionFS = FusionFS.fromDir(rootFS.getRootDir().getParentFile());
        this.rootFS = rootFS;
    }

    public XEnvironment(Context context, RootFS rootFS, com.winlator.fusion.xserver.XServer xServer) {
        this.context = context;
        this.fusionFS = FusionFS.fromDir(rootFS.getRootDir().getParentFile());
        this.rootFS = rootFS;
        this.xServer = xServer;
    }

    public Context getContext() {
        return context;
    }

    public FusionFS getFusionFS() {
        return fusionFS;
    }

    public RootFS getRootFS() {
        return rootFS;
    }

    public ImageFs getImageFs() {
        return ImageFs.find(fusionFS.getBionicDir());
    }

    public com.winlator.fusion.xserver.XServer getXServer() {
        return xServer;
    }

    public void setXServer(com.winlator.fusion.xserver.XServer xServer) {
        this.xServer = xServer;
    }

    public void addComponent(EnvironmentComponent environmentComponent) {
        environmentComponent.environment = this;
        components.add(environmentComponent);
    }

    public <T extends EnvironmentComponent> T getComponent(Class<T> componentClass) {
        for (EnvironmentComponent component : components) {
            if (component.getClass() == componentClass) return (T)component;
        }
        return null;
    }

    @Override
    public Iterator<EnvironmentComponent> iterator() {
        return components.iterator();
    }

    public File getTmpDir() {
        File tmpDir = new File(context.getFilesDir(), "tmp");
        if (!tmpDir.isDirectory()) {
            tmpDir.mkdirs();
            FileUtils.chmod(tmpDir, 0771);
        }
        return tmpDir;
    }

    public void startEnvironmentComponents() {
        FileUtils.clear(getTmpDir());
        for (EnvironmentComponent environmentComponent : this) environmentComponent.start();
    }

    public void stopEnvironmentComponents() {
        for (EnvironmentComponent environmentComponent : this) environmentComponent.stop();
    }

    public void onPause() {
        for (EnvironmentComponent environmentComponent : this) environmentComponent.onPause();
    }

    public void onResume() {
        for (EnvironmentComponent environmentComponent : this) environmentComponent.onResume();
    }
}
