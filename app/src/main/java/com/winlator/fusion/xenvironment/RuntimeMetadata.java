package com.winlator.fusion.xenvironment;

import android.content.Context;

import com.winlator.fusion.container.Container;
import com.winlator.fusion.core.FileUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

public class RuntimeMetadata {
    private static final String METADATA_DIR = ".winlator";
    private static final String METADATA_FILE = "runtime_metadata.json";

    private String runtime;
    private String version;
    private String wineVersion;
    private String translator;
    private String graphics;
    private long installedAt;
    private long updatedAt;

    public RuntimeMetadata() {
        this.runtime = "unknown";
        this.version = "0";
        this.wineVersion = "";
        this.translator = "";
        this.graphics = "";
        this.installedAt = System.currentTimeMillis();
        this.updatedAt = this.installedAt;
    }

    public static RuntimeMetadata loadForVariant(Context context, String variant) {
        FusionFS fusionFS = FusionFS.find(context);
        File dir = fusionFS.getDirForVariant(variant);
        return load(dir);
    }

    public static RuntimeMetadata load(File runtimeDir) {
        File metadataFile = new File(runtimeDir, METADATA_DIR + "/" + METADATA_FILE);
        RuntimeMetadata metadata = new RuntimeMetadata();
        if (metadataFile.exists()) {
            try {
                String json = FileUtils.readString(metadataFile);
                metadata.fromJson(new JSONObject(json));
            } catch (Exception e) {}
        }
        return metadata;
    }

    public void save(Context context, String variant) {
        FusionFS fusionFS = FusionFS.find(context);
        File dir = fusionFS.getDirForVariant(variant);
        save(dir);
    }

    public void save(File runtimeDir) {
        this.updatedAt = System.currentTimeMillis();
        File metadataDir = new File(runtimeDir, METADATA_DIR);
        metadataDir.mkdirs();
        File metadataFile = new File(metadataDir, METADATA_FILE);
        try {
            metadataFile.createNewFile();
            FileUtils.writeString(metadataFile, toJson().toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getRuntime() {
        return runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getWineVersion() {
        return wineVersion;
    }

    public void setWineVersion(String wineVersion) {
        this.wineVersion = wineVersion;
    }

    public String getTranslator() {
        return translator;
    }

    public void setTranslator(String translator) {
        this.translator = translator;
    }

    public String getGraphics() {
        return graphics;
    }

    public void setGraphics(String graphics) {
        this.graphics = graphics;
    }

    public long getInstalledAt() {
        return installedAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public boolean isGlibc() {
        return Container.GLIBC.equals(runtime);
    }

    public boolean isBionic() {
        return Container.BIONIC.equals(runtime);
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("runtime", runtime);
            json.put("version", version);
            json.put("wine", wineVersion);
            json.put("translator", translator);
            json.put("graphics", graphics);
            json.put("installedAt", installedAt);
            json.put("updatedAt", updatedAt);
        } catch (JSONException e) {}
        return json;
    }

    public void fromJson(JSONObject json) {
        try {
            this.runtime = json.optString("runtime", "unknown");
            this.version = json.optString("version", "0");
            this.wineVersion = json.optString("wine", "");
            this.translator = json.optString("translator", "");
            this.graphics = json.optString("graphics", "");
            this.installedAt = json.optLong("installedAt", System.currentTimeMillis());
            this.updatedAt = json.optLong("updatedAt", this.installedAt);
        } catch (Exception e) {}
    }

    public static RuntimeMetadata createGlibcMetadata() {
        RuntimeMetadata metadata = new RuntimeMetadata();
        metadata.setRuntime(Container.GLIBC);
        metadata.setVersion("2.0");
        metadata.setWineVersion("wine-10.10-custom");
        metadata.setTranslator("box64");
        metadata.setGraphics("vortek,turnip,zink,virgl,gladio");
        return metadata;
    }

    public static RuntimeMetadata createBionicMetadata() {
        RuntimeMetadata metadata = new RuntimeMetadata();
        metadata.setRuntime(Container.BIONIC);
        metadata.setVersion("2.0");
        metadata.setWineVersion("proton-9.0-x86_64");
        metadata.setTranslator("fexcore,box64");
        metadata.setGraphics("wrapper,turnip,zink,virgl,gladio");
        return metadata;
    }
}
