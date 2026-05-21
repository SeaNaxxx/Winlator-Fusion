package com.winlator.fusion.runtime;

import com.winlator.fusion.xenvironment.FusionFS;
import com.winlator.fusion.xenvironment.RootFS;

public class RootFSAdapter {
    private final RootFS rootFS;

    private RootFSAdapter(RootFS rootFS) {
        this.rootFS = rootFS;
    }

    public RootFS getRootFS() {
        return rootFS;
    }

    public static RootFSAdapter forGlibc(android.content.Context context) {
        FusionFS fusionFS = FusionFS.find(context);
        return new RootFSAdapter(RootFS.fromDir(fusionFS.getGlibcDir()));
    }

    public static RootFSAdapter forBionic(android.content.Context context) {
        FusionFS fusionFS = FusionFS.find(context);
        return new RootFSAdapter(RootFS.fromDir(fusionFS.getBionicDir()));
    }

    public static RootFSAdapter forProfile(RuntimeProfile profile, android.content.Context context) {
        if (profile.isBionic()) {
            return forBionic(context);
        }
        return forGlibc(context);
    }
}
