package com.winlator.fusion.contents;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.util.Log;

import com.winlator.fusion.container.Container;
import com.winlator.fusion.container.ContainerManager;
import com.winlator.fusion.core.DefaultVersion;
import com.winlator.fusion.core.EnvVars;
import com.winlator.fusion.core.FileUtils;
import com.winlator.fusion.core.GPUHelper;
import com.winlator.fusion.core.TarCompressorUtils;
import com.winlator.fusion.xenvironment.ImageFs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.json.JSONException;
import org.json.JSONObject;

public class AdrenotoolsManager {

    private static final String TAG = "AdrenotoolsManager";
    private final File adrenotoolsContentDir;
    private final Context mContext;

    public AdrenotoolsManager(Context context) {
        this.mContext = context;
        this.adrenotoolsContentDir = new File(mContext.getFilesDir(), "contents/adrenotools");
        if (!adrenotoolsContentDir.exists())
            adrenotoolsContentDir.mkdirs();
    }

    public String getLibraryName(String adrenoToolsDriverId) {
        File driverPath = new File(adrenotoolsContentDir, adrenoToolsDriverId);
        try {
            File metaProfile = new File(driverPath, "meta.json");
            JSONObject jsonObject = new JSONObject(FileUtils.readString(metaProfile));
            return jsonObject.getString("libraryName");
        } catch (JSONException e) {
            return "";
        }
    }

    public String getDriverName(String adrenoToolsDriverId) {
        File driverPath = new File(adrenotoolsContentDir, adrenoToolsDriverId);
        try {
            File metaProfile = new File(driverPath, "meta.json");
            JSONObject jsonObject = new JSONObject(FileUtils.readString(metaProfile));
            return jsonObject.getString("name");
        } catch (JSONException e) {
            return "";
        }
    }

    public String getDriverVersion(String adrenoToolsDriverId) {
        File driverPath = new File(adrenotoolsContentDir, adrenoToolsDriverId);
        try {
            File metaProfile = new File(driverPath, "meta.json");
            JSONObject jsonObject = new JSONObject(FileUtils.readString(metaProfile));
            return jsonObject.getString("driverVersion");
        } catch (JSONException e) {
            return "";
        }
    }

    public String getDriverPath(String adrenotoolsDriverId) {
        return adrenotoolsContentDir.getAbsolutePath() + "/" + adrenotoolsDriverId + "/";
    }

    private void reloadContainers(String adrenoToolsDriverId) {
        String removedDriverName = getDriverName(adrenoToolsDriverId);
        if (removedDriverName.isEmpty()) return;

        ContainerManager containerManager = new ContainerManager(mContext);
        String fallbackDriver = GPUHelper.isAdreno(mContext) ? DefaultVersion.WRAPPER_ADRENO : DefaultVersion.WRAPPER;

        for (Container container : containerManager.getContainers()) {
            String config = container.getGraphicsDriverConfig();
            if (config.contains(removedDriverName)) {
                container.setGraphicsDriverConfig(config.replace(removedDriverName, fallbackDriver));
                container.saveData();
            }
        }
    }

    public void removeDriver(String adrenoToolsDriverId) {
        Log.d(TAG, "Removing driver " + adrenoToolsDriverId);
        reloadContainers(adrenoToolsDriverId);
        File driverPath = new File(adrenotoolsContentDir, adrenoToolsDriverId);
        FileUtils.delete(driverPath);
    }

    public ArrayList<String> enumerateInstalledDrivers() {
        ArrayList<String> driversList = new ArrayList<>();
        File[] files = adrenotoolsContentDir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory() && new File(f, "meta.json").exists())
                    driversList.add(f.getName());
            }
        }
        return driversList;
    }

    public boolean isFromResources(String adrenotoolsDriverId) {
        String driver = "graphics_driver/adrenotools-" + adrenotoolsDriverId + ".tzst";
        AssetManager am = mContext.getResources().getAssets();
        try {
            InputStream is = am.open(driver);
            is.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean extractDriverFromResources(String adrenotoolsDriverId) {
        String src = "graphics_driver/adrenotools-" + adrenotoolsDriverId + ".tzst";
        File dst = new File(adrenotoolsContentDir, adrenotoolsDriverId);
        if (dst.exists()) return true;

        dst.mkdirs();
        Log.d(TAG, "Extracting " + src + " to " + dst.getAbsolutePath());
        boolean hasExtracted = TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, mContext, src, dst);
        if (!hasExtracted) dst.delete();

        return hasExtracted;
    }

    public String installDriver(Uri driverUri) {
        File tmpDir = new File(adrenotoolsContentDir, "tmp");
        if (tmpDir.exists()) FileUtils.delete(tmpDir);
        tmpDir.mkdirs();

        String name = "";
        try (InputStream is = mContext.getContentResolver().openInputStream(driverUri);
             ZipInputStream zis = new ZipInputStream(is)) {
            String tmpDirCanonical = tmpDir.getCanonicalPath();
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                File dstFile = new File(tmpDir, entry.getName());
                if (!dstFile.getCanonicalPath().startsWith(tmpDirCanonical + File.separator)) {
                    throw new IOException("ZIP entry outside target dir: " + entry.getName());
                }
                Files.copy(zis, dstFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                entry = zis.getNextEntry();
            }
            if (new File(tmpDir, "meta.json").exists()) {
                name = getDriverName(tmpDir.getName());
                File dst = new File(adrenotoolsContentDir, name);
                if (!dst.exists() && !name.isEmpty())
                    tmpDir.renameTo(dst);
                else {
                    name = "";
                    FileUtils.delete(tmpDir);
                }
            } else {
                Log.d(TAG, "Failed to install driver, a valid driver has not been selected");
                FileUtils.delete(tmpDir);
            }
        } catch (IOException e) {
            Log.d(TAG, "Failed to install driver: " + e.getMessage());
            FileUtils.delete(tmpDir);
        }

        return name;
    }

    public void setDriverById(EnvVars envVars, ImageFs imagefs, String adrenotoolsDriverId) {
        boolean isFromResources = isFromResources(adrenotoolsDriverId);

        if (isFromResources || enumerateInstalledDrivers().contains(adrenotoolsDriverId)) {
            // For bundled drivers, extract first if not already extracted
            if (isFromResources) {
                extractDriverFromResources(adrenotoolsDriverId);
            }

            String driverPath = getDriverPath(adrenotoolsDriverId);

            if (!getLibraryName(adrenotoolsDriverId).isEmpty()) {
                envVars.put("ADRENOTOOLS_DRIVER_PATH", driverPath);
                // ADRENOTOOLS_HOOKS_PATH must point to the app's nativeLibraryDir for adrenotools hooking
                envVars.put("ADRENOTOOLS_HOOKS_PATH", mContext.getApplicationInfo().nativeLibraryDir);
                envVars.put("ADRENOTOOLS_DRIVER_NAME", getLibraryName(adrenotoolsDriverId));
                envVars.put("ADRENOTOOLS_REDIRECT_DIR", imagefs.getRootDir().getAbsolutePath());
            }
        }
    }
}
