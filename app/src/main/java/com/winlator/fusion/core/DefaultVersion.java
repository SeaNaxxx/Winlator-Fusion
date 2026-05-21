package com.winlator.fusion.core;

import com.winlator.fusion.container.GraphicsDrivers;

import java.util.Locale;

public abstract class DefaultVersion {
    public static final String BOX64 = "0.4.0";
    public static final String TURNIP = "26.1.0";
    public static final String VORTEK = "2.1";
    public static final String ZINK = "22.2.5";
    public static final String VIRGL = "23.1.9";
    public static final String GLADIO = "1.0";
    public static final String D8VK = "1.0";
    public static final String VKD3D = "2.14.1";
    public static final String WINED3D = WineInfo.MAIN_WINE_VERSION;
    public static final String CNC_DDRAW = "6.6";
    public static final String SOUNDFONT = "wt_210k_G";
    public static final String SOUNDFONT_LEGACY = "SONiVOX-EAS-GM-Wavetable";
    public static final String MINOR_DXVK = "1.10.3";
    public static final String MAJOR_DXVK = "2.3.1";
    public static final String SAREK_DXVK = "1.11.1";
    public static final String FEXCORE = "2601";
    public static final String BOX64_BIONIC = "0.4.1";
    public static final String WRAPPER = "System";
    public static final String WRAPPER_ADRENO = "turnip" + TURNIP;

    public static String DXVK() {
        return DXVK(null);
    }

    public static String DXVK(String vulkanDriver) {
        int vkApiVersion = 0;
        if (vulkanDriver != null && vulkanDriver.equals(GraphicsDrivers.VORTEK)) vkApiVersion = GPUHelper.vkGetApiVersion();
        return vulkanDriver == null || vulkanDriver.equals(GraphicsDrivers.TURNIP) || vkApiVersion >= GPUHelper.vkMakeVersion(1, 3, 0) ? MAJOR_DXVK : MINOR_DXVK;
    }

    public static String valueOf(String name) {
        switch (name.toUpperCase(Locale.ENGLISH)) {
            case "BOX64": return BOX64;
            case "TURNIP": return TURNIP;
            case "VORTEK": return VORTEK;
            case "ZINK": return ZINK;
            case "VIRGL": return VIRGL;
            case "GLADIO": return GLADIO;
            case "D8VK": return D8VK;
            case "DXVK": return DXVK();
            case "MINOR_DXVK": return MINOR_DXVK;
            case "MAJOR_DXVK": return MAJOR_DXVK;
            case "VKD3D": return VKD3D;
            case "WINED3D": return WINED3D;
            case "CNC_DDRAW": return CNC_DDRAW;
            case "SOUNDFONT": return SOUNDFONT;
            case "FEXCORE": return FEXCORE;
            case "BOX64_BIONIC": return BOX64_BIONIC;
            case "WRAPPER": return WRAPPER;
            case "WRAPPER_ADRENO": return WRAPPER_ADRENO;
            default: return "0.0";
        }
    }
}