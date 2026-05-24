package com.winlator.fusion.container;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.winlator.fusion.core.FileUtils;
import com.winlator.fusion.core.StringUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Iterator;

public class Shortcut {
    public static final String EXECUTION_BACKEND_BOX64 = "Box64";
    public static final String EXECUTION_BACKEND_FEXCORE = "FEXCore";
    public static final String EXECUTION_BACKEND_WOWBOX64 = "WOWBox64";
    public static final String EXECUTION_BACKEND_HODLL = "HODLL";

    public final Container container;
    public final String name;
    public final String path;
    public Bitmap icon;
    public final File file;
    public File iconFile;
    public final String wmClass;
    private final JSONObject extraData = new JSONObject();

    public Shortcut(Container container, File file) {
        this.container = container;
        this.file = file;

        if (file.isDirectory()) {
            this.name = file.getName();
            this.path = null;
            this.icon = null;
            this.iconFile = null;
            this.wmClass = "";
        }
        else {
            String execArgs = "";
            Bitmap icon = null;
            File iconFile = null;
            String wmClass = "";
            String section = "";

            final short[] iconSizes = {64, 48, 32, 24, 16, 128, 256};
            int index;
            for (String line : FileUtils.readLines(file, true)) {
                if (line.startsWith("#")) continue;
                if (line.startsWith("[")) {
                    section = line.substring(1, line.indexOf("]"));
                }
                else {
                    index = line.indexOf("=");
                    if (index == -1) continue;
                    String key = line.substring(0, index);
                    String value = line.substring(index+1);

                    if (section.equals("Desktop Entry")) {
                        if (key.equals("Exec")) execArgs = value;
                        if (key.equals("Icon")) {
                            for (short iconSize : iconSizes) {
                                File iconsDir = container.getIconsDir(iconSize);
                                if (iconsDir == null) continue;
                                iconFile = new File(iconsDir, value+".png");
                                if (iconFile.isFile()){
                                    icon = BitmapFactory.decodeFile(iconFile.getPath());
                                    break;
                                }
                            }
                        }
                        if (key.equals("StartupWMClass")) wmClass = value;
                    }
                    else if (section.equals("Extra Data")) {
                        try {
                            extraData.put(key, value);
                        }
                        catch (JSONException e) {}
                    }
                }
            }

            this.name = FileUtils.getBasename(file.getPath());
            this.icon = icon;
            this.iconFile = iconFile;
            this.wmClass = wmClass;

            String path = !execArgs.isEmpty() ? StringUtils.unescapeDOSPath(execArgs.substring(execArgs.lastIndexOf("wine ") + 4)) : "";
            index = path.indexOf("start.exe ");
            if (index != -1) path = path.substring(index+10);

            this.path = path;
            Container.checkObsoleteOrMissingProperties(extraData);
        }
    }

    public String getExtra(String name) { return getExtra(name, ""); }

    public String getExtra(String name, String fallback) {
        try { return extraData.has(name) ? extraData.getString(name) : fallback; }
        catch (JSONException e) { return fallback; }
    }

    public void putExtra(String name, String value) {
        try { if (value != null) extraData.put(name, value); else extraData.remove(name); }
        catch (JSONException e) {}
    }

    public void saveData() {
        String content = "[Desktop Entry]\n";
        for (String line : FileUtils.readLines(file)) {
            if (line.contains("[Extra Data]")) break;
            if (!line.contains("[Desktop Entry]") && !line.isEmpty()) content += line+"\n";
        }

        if (extraData.length() > 0) {
            content += "\n[Extra Data]\n";
            Iterator<String> keys = extraData.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                try { content += key + "=" + extraData.getString(key) + "\n"; }
                catch (JSONException e) {}
            }
        }

        FileUtils.writeString(file, content);
    }

    public File getLinkFile() {
        String name = file.getName().replace(".desktop", ".lnk");
        return new File(file.getParentFile(), name);
    }

    public void remove() {
        if (file.isDirectory()) {
            FileUtils.delete(file);
        }
        else {
            File linkFile = getLinkFile();
            if (file.delete()) {
                if (iconFile != null) iconFile.delete();
                if (linkFile.isFile()) linkFile.delete();
            }
        }
    }

    public int getContainerId() { return container.id; }

    public String getExecutionBackend() {
        String v = getExtra("executionBackend", null);
        return v != null ? v : container.getEmulator();
    }

    public void setExecutionBackend(String backend) { putExtra("executionBackend", backend); }

    public boolean isFEXCoreEnabled() {
        String v = getExtra("useFex", null);
        return v != null ? v.equals("1") : container.getEmulator().equals(EXECUTION_BACKEND_FEXCORE);
    }

    public void setFEXCoreEnabled(boolean enabled) { putExtra("useFex", enabled ? "1" : "0"); }

    public boolean isWOWBox64Enabled() {
        String v = getExtra("useWowBox64", null);
        return v != null ? v.equals("1") : container.getEmulator().equals(EXECUTION_BACKEND_WOWBOX64);
    }

    public void setWOWBox64Enabled(boolean enabled) { putExtra("useWowBox64", enabled ? "1" : "0"); }

    public boolean isHODLLEnabled() {
        String v = getExtra("hodll", null);
        return v != null ? v.equals("1") : container.getEmulator().equals(EXECUTION_BACKEND_HODLL);
    }

    public void setHODLLEnabled(boolean enabled) { putExtra("hodll", enabled ? "1" : "0"); }

    public String getWoW64Backend() {
        String v = getExtra("wow64Backend", null);
        return v != null ? v : "";
    }

    public void setWoW64Backend(String backend) { putExtra("wow64Backend", backend); }

    public boolean isARM64ECMode() {
        String v = getExtra("arm64ecMode", null);
        if (v != null) return v.equals("1");
        return container.isArm64EC();
    }

    public void setARM64ECMode(boolean enabled) { putExtra("arm64ecMode", enabled ? "1" : "0"); }

    public String getGraphicsDriver() {
        String v = getExtra("graphicsDriver", null);
        return v != null ? v : container.getGraphicsDriver();
    }

    public void setGraphicsDriver(String driver) { putExtra("graphicsDriver", driver); }

    public String getDXWrapper() {
        String v = getExtra("dxwrapper", null);
        return v != null ? v : container.getDXWrapper();
    }

    public void setDXWrapper(String wrapper) { putExtra("dxwrapper", wrapper); }

    public String getAudioDriver() {
        String v = getExtra("audioDriver", null);
        return v != null ? v : container.getAudioDriver();
    }

    public void setAudioDriver(String driver) { putExtra("audioDriver", driver); }

    public String getBox64Preset() {
        String v = getExtra("box64Preset", null);
        return v != null ? v : container.getBox64Preset();
    }

    public void setBox64Preset(String preset) { putExtra("box64Preset", preset); }

    public String getFEXCorePreset() {
        String v = getExtra("fexcorePreset", null);
        return v != null ? v : container.getFEXCorePreset();
    }

    public void setFEXCorePreset(String preset) { putExtra("fexcorePreset", preset); }

    public byte getStartupSelection() {
        String v = getExtra("startupSelection", null);
        if (v != null) {
            try { return Byte.parseByte(v); }
            catch (NumberFormatException e) { return container.getStartupSelection(); }
        }
        return container.getStartupSelection();
    }

    public void setStartupSelection(byte selection) { putExtra("startupSelection", String.valueOf(selection)); }

    public String getEnvVars() {
        String v = getExtra("envVars", null);
        return v != null ? v : container.getEnvVars();
    }

    public void setEnvVars(String envVars) { putExtra("envVars", envVars); }

    public boolean isEsyncEnabled() {
        String v = getExtra("wineEsync", null);
        return v != null ? v.equals("1") : container.getEnvVars().contains("WINEESYNC=1");
    }

    public void setEsyncEnabled(boolean enabled) { putExtra("wineEsync", enabled ? "1" : "0"); }

    public boolean isFsyncEnabled() {
        String v = getExtra("wineFsync", null);
        return v != null ? v.equals("1") : container.getEnvVars().contains("WINEFSYNC=1");
    }

    public void setFsyncEnabled(boolean enabled) { putExtra("wineFsync", enabled ? "1" : "0"); }

    public boolean getRendererNative() {
        String v = getExtra("rendererNative", null);
        return v != null ? v.equals("1") : container.isRendererNative();
    }

    public void setRendererNative(boolean v) { putExtra("rendererNative", v ? "1" : "0"); }

    public String getRendererPresentMode() {
        String v = getExtra("rendererPresentMode", null);
        return v != null && !v.isEmpty() ? v : container.getRendererPresentMode();
    }

    public void setRendererPresentMode(String v) { putExtra("rendererPresentMode", v != null ? v : ""); }

    public String getRendererDriverId() {
        String v = getExtra("rendererDriverId", null);
        return v != null && !v.isEmpty() ? v : container.getRendererDriverId();
    }

    public void setRendererDriverId(String v) { putExtra("rendererDriverId", v != null ? v : ""); }

    public int getRendererFilterMode() {
        String v = getExtra("rendererFilterMode", null);
        try { return v != null && !v.isEmpty() ? Integer.parseInt(v) : container.getRendererFilterMode(); }
        catch (NumberFormatException e) { return 0; }
    }

    public void setRendererFilterMode(int v) { putExtra("rendererFilterMode", String.valueOf(v)); }

    public int getRendererRefreshRateLimit() {
        String v = getExtra("rendererRefreshRateLimit", null);
        try { return v != null && !v.isEmpty() ? Integer.parseInt(v) : container.getRendererRefreshRateLimit(); }
        catch (NumberFormatException e) { return 60; }
    }

    public void setRendererRefreshRateLimit(int v) { putExtra("rendererRefreshRateLimit", String.valueOf(v > 0 ? v : 0)); }

    public boolean getRendererSwapRB() {
        String v = getExtra("rendererSwapRB", null);
        return v != null ? v.equals("1") : container.getRendererSwapRB();
    }

    public void setRendererSwapRB(boolean v) { putExtra("rendererSwapRB", v ? "1" : "0"); }

    public boolean cloneToContainer(Container newContainer) {
        try {
            File newShortcutFile = new File(newContainer.getDesktopDir(), this.file.getName());
            if (newShortcutFile.getParentFile() != null && !newShortcutFile.getParentFile().exists()) {
                newShortcutFile.getParentFile().mkdirs();
            }
            FileUtils.copy(this.file, newShortcutFile);

            if (this.iconFile != null && this.iconFile.isFile()) {
                File newIconDir = newContainer.getIconsDir(64);
                if (newIconDir != null) {
                    if (!newIconDir.exists()) newIconDir.mkdirs();
                    File newIconFile = new File(newIconDir, this.iconFile.getName());
                    FileUtils.copy(this.iconFile, newIconFile);
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
