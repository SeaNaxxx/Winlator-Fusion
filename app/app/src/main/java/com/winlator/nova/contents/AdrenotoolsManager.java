package com.winlator.nova.contents;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.util.Log;

import com.winlator.nova.container.Container;
import com.winlator.nova.container.ContainerManager;
import com.winlator.nova.container.GraphicsDrivers;
import com.winlator.nova.container.Shortcut;
import com.winlator.nova.core.EnvVars;
import com.winlator.nova.core.FileUtils;
import com.winlator.nova.core.GPUHelper;
import com.winlator.nova.core.TarCompressorUtils;
import com.winlator.nova.xenvironment.RootFS;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.json.JSONException;
import org.json.JSONObject;

public class AdrenotoolsManager {
    private static final String TAG = "AdrenotoolsManager";
    private File adrenotoolsContentDir;
    private Context mContext;

    public AdrenotoolsManager(Context context) {
        this.mContext = context;
        this.adrenotoolsContentDir = new File(mContext.getFilesDir(), "contents/adrenotools");
        if (!adrenotoolsContentDir.exists())
            adrenotoolsContentDir.mkdirs();
    }

    public String getLibraryName(String adrenoToolsDriverId) {
        String libraryName = "";
        File driverPath = new File(adrenotoolsContentDir, adrenoToolsDriverId);
        try {
            File metaProfile = new File(driverPath, "meta.json");
            JSONObject jsonObject = new JSONObject(FileUtils.readString(metaProfile));
            libraryName = jsonObject.getString("libraryName");
        } catch (JSONException e) {
            Log.e(TAG, "Failed to read libraryName from meta.json", e);
        }
        return libraryName;
    }

    public String getDriverName(String adrenoToolsDriverId) {
        String driverName = "";
        File driverPath = new File(adrenotoolsContentDir, adrenoToolsDriverId);
        try {
            File metaProfile = new File(driverPath, "meta.json");
            JSONObject jsonObject = new JSONObject(FileUtils.readString(metaProfile));
            driverName = jsonObject.getString("name");
        } catch (JSONException e) {
            Log.e(TAG, "Failed to read name from meta.json", e);
        }
        return driverName;
    }

    public String getDriverVersion(String adrenoToolsDriverId) {
        String driverVersion = "";
        File driverPath = new File(adrenotoolsContentDir, adrenoToolsDriverId);
        try {
            File metaProfile = new File(driverPath, "meta.json");
            JSONObject jsonObject = new JSONObject(FileUtils.readString(metaProfile));
            driverVersion = jsonObject.getString("driverVersion");
        } catch (JSONException e) {
            Log.e(TAG, "Failed to read driverVersion from meta.json", e);
        }
        return driverVersion;
    }

    public String getDriverPath(String adrenotoolsDriverId) {
        return adrenotoolsContentDir.getAbsolutePath() + "/" + adrenotoolsDriverId + "/";
    }

    public void removeDriver(String adrenoToolsDriverId) {
        Log.d(TAG, "Removing driver " + adrenoToolsDriverId);
        reloadContainers(adrenoToolsDriverId);
        File driverPath = new File(adrenotoolsContentDir, adrenoToolsDriverId);
        FileUtils.delete(driverPath);
    }

    private void reloadContainers(String adrenoToolsDriverId) {
        ContainerManager containerManager = new ContainerManager(mContext);
        for (Container container : containerManager.getContainers()) {
            String graphicsDriver = container.getGraphicsDriver();
            if (graphicsDriver.contains("adrenotools-" + adrenoToolsDriverId)) {
                // Reset to default driver
                String defaultDriver = GPUHelper.isAdreno(mContext) ? "turnip,gladio" : "vortek,gladio";
                container.setGraphicsDriver(defaultDriver);
                container.saveData();
            }
        }
        for (Shortcut shortcut : containerManager.loadShortcuts()) {
            String graphicsDriver = shortcut.getExtra("graphicsDriver", shortcut.container.getGraphicsDriver());
            if (graphicsDriver.contains("adrenotools-" + adrenoToolsDriverId)) {
                String defaultDriver = GPUHelper.isAdreno(mContext) ? "turnip,gladio" : "vortek,gladio";
                shortcut.putExtra("graphicsDriver", defaultDriver);
                shortcut.saveData();
            }
        }
    }

    public ArrayList<String> enumerateInstalledDrivers() {
        ArrayList<String> driversList = new ArrayList<>();
        File[] files = adrenotoolsContentDir.listFiles();
        if (files == null) return driversList;

        for (File f : files) {
            boolean fromResources = isFromResources(f.getName());
            if (!fromResources && new File(f, "meta.json").exists())
                driversList.add(f.getName());
        }
        return driversList;
    }

    public boolean isFromResources(String adrenotoolsDriverId) {
        String driver = "graphics_driver/adrenotools-" + adrenotoolsDriverId + ".tzst";
        AssetManager am = mContext.getResources().getAssets();
        InputStream is = null;
        boolean isFromResources = true;

        try {
            is = am.open(driver);
            is.close();
        } catch (IOException e) {
            isFromResources = false;
        }

        return isFromResources;
    }

    public boolean extractDriverFromResources(String adrenotoolsDriverId) {
        String src = "graphics_driver/adrenotools-" + adrenotoolsDriverId + ".tzst";
        boolean hasExtracted;

        File dst = new File(adrenotoolsContentDir, adrenotoolsDriverId);
        if (dst.exists())
            return true;

        dst.mkdirs();
        Log.d(TAG, "Extracting " + src + " to " + dst.getAbsolutePath());
        hasExtracted = TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, mContext, src, dst);

        if (!hasExtracted)
            dst.delete();

        return hasExtracted;
    }

    public String installDriver(Uri driverUri) {
        File tmpDir = new File(adrenotoolsContentDir, "tmp");
        if (tmpDir.exists()) FileUtils.delete(tmpDir);
        tmpDir.mkdirs();
        ZipInputStream zis;
        InputStream is;
        String name = "";

        try {
            is = mContext.getContentResolver().openInputStream(driverUri);
            zis = new ZipInputStream(is);
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                File dstFile = new File(tmpDir, entry.getName());
                Files.copy(zis, dstFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                entry = zis.getNextEntry();
            }
            zis.close();
            if (new File(tmpDir, "meta.json").exists()) {
                name = getDriverName(tmpDir.getName());
                File dst = new File(adrenotoolsContentDir, name);
                if (!dst.exists() && !name.equals(""))
                    tmpDir.renameTo(dst);
                else {
                    name = "";
                    FileUtils.delete(tmpDir);
                }
            } else {
                Log.d(TAG, "Failed to install driver, meta.json not found");
                FileUtils.delete(tmpDir);
            }
        } catch (IOException e) {
            Log.d(TAG, "Failed to install driver: " + e.getMessage());
            FileUtils.delete(tmpDir);
        }

        return name;
    }

    public void setDriverById(EnvVars envVars, RootFS rootFS, String adrenotoolsDriverId) {
        boolean isFromResources = isFromResources(adrenotoolsDriverId);

        if (isFromResources || enumerateInstalledDrivers().contains(adrenotoolsDriverId)) {
            if (isFromResources) {
                extractDriverFromResources(adrenotoolsDriverId);
            }

            String driverPath = getDriverPath(adrenotoolsDriverId);

            if (!getLibraryName(adrenotoolsDriverId).equals("")) {
                envVars.put("ADRENOTOOLS_DRIVER_PATH", driverPath);
                envVars.put("ADRENOTOOLS_HOOKS_PATH", rootFS.getLibDir());
                envVars.put("ADRENOTOOLS_DRIVER_NAME", getLibraryName(adrenotoolsDriverId));

                File winlatorDir = new File("/storage/emulated/0/Winlator");
                File qglConfig = new File(winlatorDir, "qgl_config.txt");
                if (qglConfig.exists())
                    envVars.put("ADRENOTOOLS_REDIRECT_DIR", winlatorDir.getAbsolutePath() + "/");
            }
        }
    }
}
