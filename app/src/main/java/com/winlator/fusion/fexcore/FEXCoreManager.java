package com.winlator.fusion.fexcore;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.winlator.fusion.contents.ContentProfile;
import com.winlator.fusion.contents.ContentsManager;
import com.winlator.fusion.core.AppUtils;
import com.winlator.fusion.core.DefaultVersion;
import com.winlator.fusion.core.EnvVars;
import com.winlator.fusion.core.FileUtils;
import com.winlator.fusion.xenvironment.ImageFs;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class FEXCoreManager {

    private FEXCoreManager() {}

    public static void writeToConfigFile(File targetConfigFile, String preset, Context context) {
        EnvVars envVars = FEXCorePresetManager.getEnvVars(context, preset);

        try {
            JSONObject config = new JSONObject();
            String multiblock = envVars.has("FEX_MULTIBLOCK") ? envVars.get("FEX_MULTIBLOCK") : "1";
            String tsoEnabled = envVars.has("FEX_TSOENABLED") ? envVars.get("FEX_TSOENABLED") : "1";
            String vectorTSO = envVars.has("FEX_VECTORTSOENABLED") ? envVars.get("FEX_VECTORTSOENABLED") : "0";
            String memcpyTSO = envVars.has("FEX_MEMCPYSETTSOENABLED") ? envVars.get("FEX_MEMCPYSETTSOENABLED") : "0";
            String halfBarrier = envVars.has("FEX_HALFBARRIERTSOENABLED") ? envVars.get("FEX_HALFBARRIERTSOENABLED") : "1";
            String x87Reduced = envVars.has("FEX_X87REDUCEDPRECISION") ? envVars.get("FEX_X87REDUCEDPRECISION") : "1";
            JSONObject opts = new JSONObject()
                    .put("Multiblock", multiblock)
                    .put("TSOEnabled", tsoEnabled)
                    .put("VectorTSOEnabled", vectorTSO)
                    .put("MemcpySetTSOEnabled", memcpyTSO)
                    .put("HalfBarrierTSOEnabled", halfBarrier)
                    .put("X87ReducedPrecision", x87Reduced);
            config.put("Config", opts);
            FileUtils.writeString(targetConfigFile, config.toString());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Deprecated
    public static void writeToConfigFile(File targetConfigFile, String tsoPreset, String mblockValue, String x87ModePreset) {
        String preset = FEXCorePresetManager.migrateFromLegacyValues(tsoPreset, x87ModePreset, mblockValue);
        writeToConfigFileLegacy(targetConfigFile, preset);
    }

    private static void writeToConfigFileLegacy(File targetConfigFile, String preset) {
        EnvVars envVars = FEXCorePresetManager.getEnvVars(null, preset);

        try {
            JSONObject config = new JSONObject();
            String multiblock = envVars.has("FEX_MULTIBLOCK") ? envVars.get("FEX_MULTIBLOCK") : "1";
            String tsoEnabled = envVars.has("FEX_TSOENABLED") ? envVars.get("FEX_TSOENABLED") : "1";
            String vectorTSO = envVars.has("FEX_VECTORTSOENABLED") ? envVars.get("FEX_VECTORTSOENABLED") : "0";
            String memcpyTSO = envVars.has("FEX_MEMCPYSETTSOENABLED") ? envVars.get("FEX_MEMCPYSETTSOENABLED") : "0";
            String halfBarrier = envVars.has("FEX_HALFBARRIERTSOENABLED") ? envVars.get("FEX_HALFBARRIERTSOENABLED") : "1";
            String x87Reduced = envVars.has("FEX_X87REDUCEDPRECISION") ? envVars.get("FEX_X87REDUCEDPRECISION") : "1";
            JSONObject opts = new JSONObject()
                    .put("Multiblock", multiblock)
                    .put("TSOEnabled", tsoEnabled)
                    .put("VectorTSOEnabled", vectorTSO)
                    .put("MemcpySetTSOEnabled", memcpyTSO)
                    .put("HalfBarrierTSOEnabled", halfBarrier)
                    .put("X87ReducedPrecision", x87Reduced);
            config.put("Config", opts);
            FileUtils.writeString(targetConfigFile, config.toString());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static void createAppConfigFiles(Context ctx) {
        String[] programsName = {"winhandler.exe"};
        for (String programName : programsName) {
            File cfgFile = new File(ImageFs.find(ctx).getRootDir(), "/home/xuser/.fex-emu/AppConfig/" + programName + ".json");
            if (!cfgFile.exists()) {
                File cfgDir = cfgFile.getParentFile();
                if (cfgDir != null && !cfgDir.isDirectory()) cfgDir.mkdirs();

                switch (programName) {
                    case "winhandler.exe":
                        writeToConfigFile(cfgFile, FEXCorePreset.PERFORMANCE, ctx);
                        break;
                }
            }
        }
    }

    public static void ensureAppConfigOverrides(Context ctx) {
        String[] appConfigExeNames = {
            "RockstarService.exe", "RockstarSteamHelper.exe", "SocialClubHelper.exe",
            "UplayWebCore.exe", "steamservice.exe", "steamwebhelper.exe", "steam.exe"
        };

        File appConfigDir = new File(ImageFs.find(ctx).getRootDir(), "/home/xuser/.fex-emu/AppConfig");
        if (!appConfigDir.isDirectory()) appConfigDir.mkdirs();

        for (String exeName : appConfigExeNames) {
            File cfgFile = new File(appConfigDir, exeName + ".json");
            if (!cfgFile.exists()) {
                try {
                    JSONObject config = new JSONObject();
                    JSONObject opts = new JSONObject()
                            .put("Multiblock", "0")
                            .put("X87ReducedPrecision", "1")
                            .put("VectorTSOEnabled", "1")
                            .put("HalfBarrierTSOEnabled", "1")
                            .put("MonoHacks", "0");
                    config.put("Config", opts);
                    FileUtils.writeString(cfgFile, config.toString());
                } catch (JSONException e) {}
            }
        }
    }

    public static void loadFEXCoreVersion(Context context, ContentsManager contentsManager, Spinner spinner, String fexcoreVersion) {
        LinkedHashSet<String> versions = new LinkedHashSet<>();
        versions.add(DefaultVersion.FEXCORE);
        if (fexcoreVersion != null && !fexcoreVersion.isEmpty()) {
            versions.add(fexcoreVersion);
        }
        for (ContentProfile profile : contentsManager.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_FEXCORE)) {
            if (profile.remoteUrl != null) continue;
            String entryName = ContentsManager.getEntryName(profile);
            int firstDashIndex = entryName.indexOf('-');
            versions.add(entryName.substring(firstDashIndex + 1));
        }
        List<String> itemList = new ArrayList<>(versions);
        spinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, itemList));
        AppUtils.setSpinnerSelectionFromValue(spinner, fexcoreVersion);
    }
}
