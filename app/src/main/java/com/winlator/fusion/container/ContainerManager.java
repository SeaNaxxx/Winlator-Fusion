package com.winlator.fusion.container;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.winlator.fusion.R;
import com.winlator.fusion.core.Callback;
import com.winlator.fusion.core.FileUtils;
import com.winlator.fusion.core.TarCompressorUtils;
import com.winlator.fusion.core.WineInfo;
import com.winlator.fusion.xenvironment.FusionFS;
import com.winlator.fusion.xenvironment.FusionFSInstaller;
import com.winlator.fusion.xenvironment.ImageFs;
import com.winlator.fusion.xenvironment.RootFS;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Executors;

public class ContainerManager {
    private static final String TAG = "ContainerManager";
    private final ArrayList<Container> containers = new ArrayList<>();
    private int maxContainerId = 0;
    private final File rootfsHomeDir;
    private final File imagefsHomeDir;
    private final Context context;

    public ContainerManager(Context context) {
        this.context = context;
        FusionFS fusionFS = FusionFS.find(context);
        File rootfsDir = fusionFS.getGlibcDir();
        File imagefsDir = fusionFS.getBionicDir();
        if (!imagefsDir.isDirectory()) imagefsDir.mkdirs();
        rootfsHomeDir = new File(rootfsDir, "home");
        imagefsHomeDir = new File(imagefsDir, "home");
        loadContainers();
    }

    public Context getContext() {
        return context;
    }

    public ArrayList<Container> getContainers() {
        return containers;
    }

    private void loadContainers() {
        containers.clear();
        maxContainerId = 0;
        loadContainersFromDir(rootfsHomeDir, false);
        loadContainersFromDir(imagefsHomeDir, true);
    }

    private void loadContainersFromDir(File homeDir, boolean isBionicDir) {
        File[] files = homeDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    if (file.getName().startsWith(RootFS.USER+"-")) {
                        try {
                            int id = Integer.parseInt(file.getName().replace(RootFS.USER+"-", ""));
                            if (getContainerById(id) != null) continue;
                            Container container = new Container(id);
                            container.setRootDir(new File(homeDir, RootFS.USER+"-"+container.id));
                            JSONObject data = new JSONObject(FileUtils.readString(container.getConfigFile()));
                            container.loadData(data);
                            if (container.isBionic() != isBionicDir) {
                                Log.w(TAG, "Container " + id + " variant mismatch: isBionic=" + container.isBionic() + " but found in " + (isBionicDir ? "imagefs" : "rootfs") + " - skipping");
                                continue;
                            }
                            containers.add(container);
                            maxContainerId = Math.max(maxContainerId, container.id);
                        } catch (NumberFormatException | JSONException e) {}
                    }
                }
            }
        }
    }

    public void activateContainer(Container container) {
        File homeDir = getHomeDirForContainer(container);
        container.setRootDir(new File(homeDir, RootFS.USER+"-"+container.id));
        File file = new File(homeDir, RootFS.USER);
        file.delete();
        FileUtils.symlink(RootFS.USER+"-"+container.id, file.getPath());
    }

    private File getHomeDirForContainer(Container container) {
        return container.isBionic() ? imagefsHomeDir : rootfsHomeDir;
    }

    private File getHomeDirForVariant(String containerVariant) {
        return (containerVariant != null && containerVariant.equals(Container.BIONIC)) ? imagefsHomeDir : rootfsHomeDir;
    }

    public void createContainerAsync(final JSONObject data, Callback<Container> callback) {
        final Handler handler = new Handler();
        Executors.newSingleThreadExecutor().execute(() -> {
            final Container container = createContainer(data);
            handler.post(() -> callback.call(container));
        });
    }

    public void duplicateContainerAsync(Container container, Runnable callback) {
        final Handler handler = new Handler();
        Executors.newSingleThreadExecutor().execute(() -> {
            duplicateContainer(container);
            handler.post(callback);
        });
    }

    public void removeContainerAsync(Container container, Runnable callback) {
        final Handler handler = new Handler();
        Executors.newSingleThreadExecutor().execute(() -> {
            removeContainer(container);
            handler.post(callback);
        });
    }

    private Container createContainer(JSONObject data) {
        try {
            int id = maxContainerId + 1;
            data.put("id", id);

            String containerVariant = data.optString("containerVariant", Container.DEFAULT_VARIANT);
            File homeDir = getHomeDirForVariant(containerVariant);
            File containerDir = new File(homeDir, RootFS.USER+"-"+id);

            if (Container.BIONIC.equals(containerVariant)) {
                FusionFSInstaller.ensureMinimalFusionFSStructure(context);
                homeDir = getHomeDirForVariant(containerVariant);
                containerDir = new File(homeDir, RootFS.USER+"-"+id);
            }

            if (!containerDir.mkdirs()) return null;

            if (Container.BIONIC.equals(containerVariant) && !data.has("wineVersion")) {
                data.put("wineVersion", WineInfo.BIONIC_WINE_IDENTIFIER);
            }

            Container container = new Container(id);
            container.setRootDir(containerDir);
            container.loadData(data);

            if (data.has("wineVersion")) container.setWineVersion(data.getString("wineVersion"));

            if (!extractContainerPatternFile(container.getWineVersion(), containerDir, containerVariant)) {
                FileUtils.delete(containerDir);
                return null;
            }

            container.saveData();
            maxContainerId++;
            containers.add(container);
            return container;
        }
        catch (Exception e) {
            Log.e(TAG, "Failed to create container", e);
            return null;
        }
    }

    private void duplicateContainer(Container srcContainer) {
        int id = maxContainerId + 1;
        File homeDir = getHomeDirForContainer(srcContainer);
        File dstDir = new File(homeDir, RootFS.USER+"-"+id);
        if (!dstDir.mkdirs()) return;

        if (!FileUtils.copy(srcContainer.getRootDir(), dstDir, (file) -> FileUtils.chmod(file, 0771))) {
            FileUtils.delete(dstDir);
            return;
        }

        Container dstContainer = new Container(id);
        dstContainer.setRootDir(dstDir);
        dstContainer.setName(srcContainer.getName()+" ("+context.getString(R.string.copy)+")");
        dstContainer.setScreenSize(srcContainer.getScreenSize());
        dstContainer.setEnvVars(srcContainer.getEnvVars());
        dstContainer.setCPUList(srcContainer.getCPUList());
        dstContainer.setCPUListWoW64(srcContainer.getCPUListWoW64());
        dstContainer.setGraphicsDriver(srcContainer.getGraphicsDriver());
        dstContainer.setGraphicsDriverConfig(srcContainer.getGraphicsDriverConfig());
        dstContainer.setDXWrapper(srcContainer.getDXWrapper());
        dstContainer.setDXWrapperConfig(srcContainer.getDXWrapperConfig());
        dstContainer.setAudioDriver(srcContainer.getAudioDriver());
        dstContainer.setAudioDriverConfig(srcContainer.getAudioDriverConfig());
        dstContainer.setWinComponents(srcContainer.getWinComponents());
        dstContainer.setDrives(srcContainer.getDrives());
        dstContainer.setHUDMode(srcContainer.getHUDMode());
        dstContainer.setStartupSelection(srcContainer.getStartupSelection());
        dstContainer.setBox64Preset(srcContainer.getBox64Preset());
        dstContainer.setDesktopTheme(srcContainer.getDesktopTheme());
        dstContainer.setContainerVariant(srcContainer.getContainerVariant());
        dstContainer.setEmulator(srcContainer.getEmulator());
        dstContainer.setFEXCoreVersion(srcContainer.getFEXCoreVersion());
        dstContainer.setBox64Version(srcContainer.getBox64Version());
        dstContainer.setFEXCorePreset(srcContainer.getFEXCorePreset());
        dstContainer.setWineVersion(srcContainer.getWineVersion());
        String srcExtraData = srcContainer.getExtraData();
        if (srcExtraData != null && !srcExtraData.isEmpty()) {
            try {
                dstContainer.setExtraData(new JSONObject(srcExtraData));
            } catch (JSONException e) {
            }
        }
        dstContainer.saveData();

        maxContainerId++;
        containers.add(dstContainer);
    }

    private void removeContainer(Container container) {
        if (FileUtils.delete(container.getRootDir())) containers.remove(container);
    }

    public ArrayList<Shortcut> loadShortcuts(Shortcut selectedFolder) {
        ArrayList<Shortcut> shortcuts = new ArrayList<>();

        if (selectedFolder != null) {
            File[] files = selectedFolder.file.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".desktop") || file.isDirectory()) {
                        shortcuts.add(new Shortcut(selectedFolder.container, file));
                    }
                }
            }
        }
        else {
            for (Container container : containers) {
                File desktopDir = new File(container.getUserDir(), "Desktop");
                File[] files = desktopDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.getName().endsWith(".desktop") || file.isDirectory()) {
                            shortcuts.add(new Shortcut(container, file));
                        }
                    }
                }
            }
        }

        shortcuts.sort((a, b) -> {
            int value = Boolean.compare(b.file.isDirectory(), a.file.isDirectory());
            if (value == 0) value = a.name.compareTo(b.name);
            return value;
        });
        return shortcuts;
    }

    public ArrayList<FileInfo> loadFiles(Container container, FileInfo parent) {
        ArrayList<FileInfo> fileInfos = new ArrayList<>();

        if (parent != null) {
            fileInfos = parent.list();
        }
        else {
            String rootPath = container.getRootDir().getPath();
            fileInfos.add(new FileInfo(container, "C:", rootPath+"/.wine/drive_c", FileInfo.Type.DRIVE));
            for (Drive drive : container.drivesIterator()) {
                fileInfos.add(new FileInfo(container, drive.letter+":", drive.path, FileInfo.Type.DRIVE));
            }

            File userDir = container.getUserDir();
            File documentsDir = new File(userDir, "Documents");
            File favoritesDir = new File(userDir, "Favorites");

            fileInfos.add(new FileInfo(container, documentsDir.getName(), documentsDir.getPath(), FileInfo.Type.DIRECTORY));
            fileInfos.add(new FileInfo(container, favoritesDir.getName(), favoritesDir.getPath(), FileInfo.Type.DIRECTORY));

            Collections.sort(fileInfos);
        }
        return fileInfos;
    }

    public int getNextContainerId() {
        return maxContainerId + 1;
    }

    public Container getContainerById(int id) {
        for (Container container : containers) if (container.id == id) return container;
        return null;
    }

    private void copyCommonDlls(String srcName, String dstName, JSONObject commonDlls, File containerDir) throws JSONException {
        FusionFS fusionFS = FusionFS.find(context);
        File srcDir = new File(fusionFS.getWineDir(), "/lib/wine/"+srcName);
        JSONArray dlnames = commonDlls.getJSONArray(dstName);

        for (int i = 0; i < dlnames.length(); i++) {
            String dlname = dlnames.getString(i);
            File dstFile = new File(containerDir, ".wine/drive_c/windows/"+dstName+"/"+dlname);
            FileUtils.copy(new File(srcDir, dlname), dstFile);
        }
    }

    private boolean extractContainerPatternFile(String wineVersion, File containerDir, String containerVariant) {
        try {
            if (containerVariant != null && containerVariant.equals(Container.BIONIC)) {
                String containerPattern = WineInfo.getContainerPatternAssetName(wineVersion);
                boolean result = false;
                if (assetExists(context, containerPattern)) {
                    result = TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context, containerPattern, containerDir);
                }
                if (!result) {
                    FusionFS fusionFS = FusionFS.find(context);
                    File installedPattern = new File(fusionFS.getInstalledWineDir(), containerPattern);
                    if (installedPattern.isFile()) {
                        result = TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, installedPattern, containerDir);
                    }
                    if (!result) {
                        String legacyName = "container-pattern-" + wineVersion + ".tzst";
                        File legacyPattern = new File(fusionFS.getInstalledWineDir(), legacyName);
                        if (legacyPattern.isFile()) {
                            result = TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, legacyPattern, containerDir);
                        }
                    }
                }
                if (!result) {
                    WineInfo wineInfo = WineInfo.fromIdentifier(context, wineVersion);
                    if (wineInfo.path != null) {
                        File prefixPackFile = new File(wineInfo.path + "/prefixPack.txz");
                        if (prefixPackFile.isFile()) {
                            result = TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, prefixPackFile, containerDir);
                        }
                    }
                }

                if (result) {
                    final String containerDirPath = containerDir.getPath();
                    if (assetExists(context, "container_pattern_common.tzst")) {
                        TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context, "container_pattern_common.tzst", containerDir, (file, size) -> {
                            String path = file.getPath();
                            String prefix = containerDirPath + "/home/xuser/";
                            if (path.startsWith(prefix)) {
                                return new File(containerDirPath, path.substring(prefix.length()));
                            }
                            return file;
                        });
                    }

                    try {
                        WineInfo wineInfo = WineInfo.fromIdentifier(context, wineVersion);
                        copyBionicCommonDlls(wineInfo, containerDir);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to copy Bionic common DLLs for " + wineVersion, e);
                    }
                }
                return result;
            } else {
                String patternAsset = WineInfo.getContainerPatternAssetName(wineVersion);
                boolean result = false;

                if (assetExists(context, patternAsset)) {
                    result = TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context, patternAsset, containerDir);
                }

                if (!result) {
                    FusionFS fusionFS = FusionFS.find(context);
                    File installedPattern = new File(fusionFS.getInstalledWineDir(), patternAsset);
                    if (installedPattern.isFile()) {
                        result = TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, installedPattern, containerDir);
                    }
                    if (!result) {
                        String legacyName = "container-pattern-" + wineVersion + ".tzst";
                        File legacyPattern = new File(fusionFS.getInstalledWineDir(), legacyName);
                        if (legacyPattern.isFile()) {
                            result = TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, legacyPattern, containerDir);
                        }
                    }
                }

                if (!result) {
                    WineInfo wineInfo = WineInfo.fromIdentifier(context, wineVersion);
                    if (wineInfo.path != null) {
                        File prefixPackFile = new File(wineInfo.path + "/prefixPack.txz");
                        if (prefixPackFile.isFile()) {
                            result = TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, prefixPackFile, containerDir);
                        }
                    }
                }

                if (result && WineInfo.isMainWineVersion(wineVersion)) {
                    try {
                        JSONObject commonDlls = new JSONObject(FileUtils.readString(context, "common_dlls.json"));
                        copyCommonDlls("x86_64-windows", "system32", commonDlls, containerDir);
                        copyCommonDlls("i386-windows", "syswow64", commonDlls, containerDir);
                    } catch (JSONException e) {
                        return false;
                    }
                }
                return result;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to extract container pattern for " + wineVersion, e);
            return false;
        }
    }

    private boolean assetExists(Context context, String assetName) {
        try {
            context.getAssets().open(assetName).close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void copyBionicCommonDlls(WineInfo wineInfo, File containerDir) throws JSONException {
        if (wineInfo.isArm64EC()) {
            copyBionicDllsFromDir(wineInfo.path + "/lib/wine/aarch64-windows", "system32", containerDir);
        } else {
            copyBionicDllsFromDir(wineInfo.path + "/lib/wine/x86_64-windows", "system32", containerDir);
        }
        copyBionicDllsFromDir(wineInfo.path + "/lib/wine/i386-windows", "syswow64", containerDir);
    }

    private void copyBionicDllsFromDir(String srcPath, String dstName, File containerDir) {
        File srcDir = new File(srcPath);
        if (!srcDir.isDirectory()) return;
        File[] files = srcDir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isFile()) {
                File dstFile = new File(containerDir, ".wine/drive_c/windows/" + dstName + "/" + file.getName());
                if (!dstFile.exists()) FileUtils.copy(file, dstFile);
            }
        }
    }
}
