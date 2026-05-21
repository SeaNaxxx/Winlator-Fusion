package com.winlator.fusion.fexcore;

import androidx.annotation.NonNull;

public class FEXCorePreset {
    public static final String STABILITY = "STABILITY";
    public static final String COMPATIBILITY = "COMPATIBILITY";
    public static final String INTERMEDIATE = "INTERMEDIATE";
    public static final String PERFORMANCE = "PERFORMANCE";
    public static final String CUSTOM = "CUSTOM";
    public static final String DEFAULT = COMPATIBILITY;

    public final String id;
    public final String name;

    public FEXCorePreset(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public boolean isCustom() {
        return id.startsWith(CUSTOM);
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }

    public static String[] getAll() {
        return new String[]{STABILITY, COMPATIBILITY, INTERMEDIATE, PERFORMANCE, CUSTOM};
    }

    public static boolean isBuiltIn(String preset) {
        return preset.equals(STABILITY) || preset.equals(COMPATIBILITY) ||
               preset.equals(INTERMEDIATE) || preset.equals(PERFORMANCE);
    }
}
