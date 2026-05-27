package com.winlator.fusion.core;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.winlator.fusion.container.Container;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WineInfo implements Parcelable {
    public static final String MAIN_WINE_VERSION = "10.10";
    public static final String MAIN_WINE_IDENTIFIER = "wine-10.10-x86_64";
    public static final WineInfo MAIN_WINE_INFO = new WineInfo(MAIN_WINE_IDENTIFIER);
    public static final String BIONIC_WINE_VERSION = "9.0";
    public static final String BIONIC_WINE_IDENTIFIER = "proton-9.0-x86_64";
    public static final WineInfo BIONIC_WINE_INFO = new WineInfo("proton", BIONIC_WINE_VERSION, null, "x86_64", null);
    private static final Pattern pattern = Pattern.compile("^(wine|proton)\\-([0-9\\.]+)\\-?([0-9\\.]+)?\\-?(x86|x86_64|arm64ec)?$");
    public final String version;
    public final String subversion;
    public final String path;
    public final String type;
    public final String arch;

    public WineInfo(String version) {
        Matcher matcher = pattern.matcher(version);
        if (matcher.find()) {
            String type = matcher.group(1) != null ? matcher.group(1) : "wine";
            String arch = matcher.group(4) != null ? matcher.group(4) : "x86_64";
            this.version = matcher.group(2);
            this.subversion = matcher.group(3);
            this.path = null;
            this.type = type;
            this.arch = arch;
        } else {
            this.version = version;
            this.subversion = null;
            this.path = null;
            this.type = "wine";
            this.arch = "x86_64";
        }
    }

    public WineInfo(String version, String subversion, String path) {
        this.version = version;
        this.subversion = subversion != null && !subversion.isEmpty() ? subversion : null;
        this.path = path;
        this.type = "wine";
        this.arch = "x86_64";
    }

    public WineInfo(String type, String version, String subversion, String arch, String path) {
        this.type = type != null ? type : "wine";
        this.version = version;
        this.subversion = subversion != null && !subversion.isEmpty() ? subversion : null;
        this.path = path;
        this.arch = arch != null ? arch : "x86_64";
    }

    private WineInfo(Parcel in) {
        type = in.readString();
        version = in.readString();
        subversion = in.readString();
        arch = in.readString();
        path = in.readString();
    }

    public boolean isArm64EC() {
        return arch != null && arch.equals("arm64ec");
    }

    public boolean isProton() {
        return type != null && type.equals("proton");
    }

    public boolean isWin64() {
        return arch != null && (arch.equals("x86_64") || arch.equals("arm64ec"));
    }

    public String identifier() {
        if (type != null && type.equals("proton"))
            return "proton-" + fullVersion() + "-" + arch;
        return "wine-"+fullVersion()+"-"+arch;
    }

    public String fullVersion() {
        return version+(subversion != null ? "-"+subversion : "");
    }

    @NonNull
    @Override
    public String toString() {
        String archLabel = "";
        if (arch != null && !arch.isEmpty()) {
            if (type != null && type.equals("proton")) {
                archLabel = " (" + (arch.equals("arm64ec") ? "ARM64EC" : arch.toUpperCase()) + ")";
            } else if (!arch.equals("x86_64")) {
                archLabel = " (" + arch.toUpperCase() + ")";
            }
        }
        if (type != null && type.equals("proton"))
            return "Proton " + fullVersion() + archLabel;
        return "Wine " + fullVersion() + archLabel;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<WineInfo> CREATOR = new Parcelable.Creator<WineInfo>() {
        public WineInfo createFromParcel(Parcel in) {
            return new WineInfo(in);
        }
        public WineInfo[] newArray(int size) {
            return new WineInfo[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(type);
        dest.writeString(version);
        dest.writeString(subversion);
        dest.writeString(arch);
        dest.writeString(path);
    }

    @NonNull
    public static WineInfo fromIdentifier(Context context, String identifier) {
        Matcher matcher = pattern.matcher(identifier);
        if (matcher.find()) {
            String type = matcher.group(1) != null ? matcher.group(1) : "wine";
            String arch = matcher.group(4) != null ? matcher.group(4) : "x86_64";
            boolean isProton = type.equals("proton");

            com.winlator.fusion.xenvironment.FusionFS fusionFS = com.winlator.fusion.xenvironment.FusionFS.find(context);

            if (isMainWineVersion(identifier)) {
                File wineDir = fusionFS.getWineDir();
                return new WineInfo(type, matcher.group(2), matcher.group(3), arch, wineDir.getPath());
            }

            if (isProton) {
                File imagefsWinePath = new File(fusionFS.getBionicDir(), "opt/" + identifier);
                if (imagefsWinePath.isDirectory()) {
                    return new WineInfo(type, matcher.group(2), matcher.group(3), arch, imagefsWinePath.getPath());
                }
            }

            File installedWineDir = fusionFS.getInstalledWineDir();
            File winePath = new File(installedWineDir, identifier);
            if (winePath.isDirectory()) {
                return new WineInfo(type, matcher.group(2), matcher.group(3), arch, winePath.getPath());
            }

            if (!isProton) {
                File wineDir = fusionFS.getWineDir();
                if (wineDir.isDirectory()) {
                    return new WineInfo(type, matcher.group(2), matcher.group(3), arch, wineDir.getPath());
                }
                File imagefsWinePath = new File(fusionFS.getBionicDir(), "opt/" + identifier);
                if (imagefsWinePath.isDirectory()) {
                    return new WineInfo(type, matcher.group(2), matcher.group(3), arch, imagefsWinePath.getPath());
                }
            }

            File fallbackPath = isProton
                ? new File(fusionFS.getBionicDir(), "opt/" + identifier)
                : fusionFS.getWineDir();
            return new WineInfo(type, matcher.group(2), matcher.group(3), arch, fallbackPath.getPath());
        }
        com.winlator.fusion.xenvironment.FusionFS fusionFS = com.winlator.fusion.xenvironment.FusionFS.find(context);
        return new WineInfo("wine", MAIN_WINE_VERSION, null, "x86_64", fusionFS.getWineDir().getPath());
    }

    public static boolean isMainWineVersion(String wineVersion) {
        return wineVersion == null || wineVersion.equals(MAIN_WINE_INFO.identifier()) || wineVersion.equals("wine-"+MAIN_WINE_VERSION+"-custom");
    }

    public static boolean isBionicDefaultWineVersion(String wineVersion) {
        return wineVersion != null && wineVersion.equals(BIONIC_WINE_IDENTIFIER);
    }

    public static boolean isAnyArm64EC(String wineVersion) {
        return wineVersion != null && wineVersion.endsWith("-arm64ec");
    }

    public static String getContainerPatternAssetName(String identifier) {
        return identifier + "_container_pattern.tzst";
    }

    public static String getWineAssetName(String identifier) {
        return identifier + ".txz";
    }

    public static String defaultIdentifierForVariant(String containerVariant) {
        if (Container.BIONIC.equals(containerVariant)) return BIONIC_WINE_IDENTIFIER;
        return MAIN_WINE_INFO.identifier();
    }
}
