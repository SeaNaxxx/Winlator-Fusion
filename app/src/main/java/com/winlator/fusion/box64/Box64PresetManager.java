package com.winlator.fusion.box64;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import androidx.preference.PreferenceManager;

import com.winlator.fusion.R;
import com.winlator.fusion.core.AppUtils;
import com.winlator.fusion.core.EnvVars;
import com.winlator.fusion.core.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

public abstract class Box64PresetManager {
    public static EnvVars getEnvVars(Context context, String id) {
        EnvVars envVars = new EnvVars();

        if (id.equals(Box64Preset.STABILITY)) {
            envVars.put("BOX64_DYNAREC_SAFEFLAGS", "2");
            envVars.put("BOX64_DYNAREC_FASTNAN", "0");
            envVars.put("BOX64_DYNAREC_FASTROUND", "0");
            envVars.put("BOX64_DYNAREC_X87DOUBLE", "1");
            envVars.put("BOX64_DYNAREC_BIGBLOCK", "0");
            envVars.put("BOX64_DYNAREC_STRONGMEM", "2");
            envVars.put("BOX64_DYNAREC_FORWARD", "128");
            envVars.put("BOX64_DYNAREC_CALLRET", "0");
            envVars.put("BOX64_DYNAREC_WAIT", "0");
            envVars.put("BOX64_DYNAREC_NATIVEFLAGS", "0");
            envVars.put("BOX64_DYNAREC_WEAKBARRIER", "0");
        }
        else if (id.equals(Box64Preset.CONSERVATIVE)) {
            envVars.put("BOX64_DYNAREC_SAFEFLAGS", "2");
            envVars.put("BOX64_DYNAREC_FASTNAN", "0");
            envVars.put("BOX64_DYNAREC_FASTROUND", "0");
            envVars.put("BOX64_DYNAREC_X87DOUBLE", "1");
            envVars.put("BOX64_DYNAREC_BIGBLOCK", "1");
            envVars.put("BOX64_DYNAREC_STRONGMEM", "1");
            envVars.put("BOX64_DYNAREC_FORWARD", "128");
            envVars.put("BOX64_DYNAREC_CALLRET", "0");
            envVars.put("BOX64_DYNAREC_WAIT", "1");
            envVars.put("BOX64_DYNAREC_NATIVEFLAGS", "0");
            envVars.put("BOX64_DYNAREC_WEAKBARRIER", "1");
        }
        else if (id.equals(Box64Preset.INTERMEDIATE)) {
            envVars.put("BOX64_DYNAREC_SAFEFLAGS", "2");
            envVars.put("BOX64_DYNAREC_FASTNAN", "1");
            envVars.put("BOX64_DYNAREC_FASTROUND", "0");
            envVars.put("BOX64_DYNAREC_X87DOUBLE", "1");
            envVars.put("BOX64_DYNAREC_BIGBLOCK", "2");
            envVars.put("BOX64_DYNAREC_STRONGMEM", "0");
            envVars.put("BOX64_DYNAREC_FORWARD", "128");
            envVars.put("BOX64_DYNAREC_CALLRET", "0");
            envVars.put("BOX64_DYNAREC_WAIT", "1");
            envVars.put("BOX64_DYNAREC_NATIVEFLAGS", "0");
            envVars.put("BOX64_DYNAREC_WEAKBARRIER", "2");
        }
        else if (id.equals(Box64Preset.PERFORMANCE)) {
            envVars.put("BOX64_DYNAREC_SAFEFLAGS", "1");
            envVars.put("BOX64_DYNAREC_FASTNAN", "1");
            envVars.put("BOX64_DYNAREC_FASTROUND", "1");
            envVars.put("BOX64_DYNAREC_X87DOUBLE", "0");
            envVars.put("BOX64_DYNAREC_BIGBLOCK", "3");
            envVars.put("BOX64_DYNAREC_STRONGMEM", "0");
            envVars.put("BOX64_DYNAREC_FORWARD", "512");
            envVars.put("BOX64_DYNAREC_CALLRET", "1");
            envVars.put("BOX64_DYNAREC_WAIT", "1");
            envVars.put("BOX64_DYNAREC_NATIVEFLAGS", "1");
            envVars.put("BOX64_DYNAREC_WEAKBARRIER", "2");
        }
        else if (id.startsWith(Box64Preset.CUSTOM)) {
            for (String[] preset : customPresetsIterator(context)) {
                if (preset[0].equals(id)) {
                    envVars.putAll(preset[2]);
                    break;
                }
            }
        }

        return envVars;
    }

    public static ArrayList<Box64Preset> getPresets(Context context) {
        ArrayList<Box64Preset> presets = new ArrayList<>();
        presets.add(new Box64Preset(Box64Preset.STABILITY, context.getString(R.string.stability)));
        presets.add(new Box64Preset(Box64Preset.CONSERVATIVE, context.getString(R.string.conservative)));
        presets.add(new Box64Preset(Box64Preset.INTERMEDIATE, context.getString(R.string.intermediate)));
        presets.add(new Box64Preset(Box64Preset.PERFORMANCE, context.getString(R.string.performance)));
        for (String[] preset : customPresetsIterator(context)) presets.add(new Box64Preset(preset[0], preset[1]));
        return presets;
    }

    public static Box64Preset getPreset(Context context, String id) {
        for (Box64Preset preset : getPresets(context)) if (preset.id.equals(id)) return preset;
        return null;
    }

    private static Iterable<String[]> customPresetsIterator(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        final String customPresetsStr = preferences.getString("box64_custom_presets", "");
        final String[] customPresets = customPresetsStr.split(",");
        final int[] index = {0};
        return () -> new Iterator<String[]>() {
            @Override
            public boolean hasNext() {
                return index[0] < customPresets.length && !customPresetsStr.isEmpty();
            }

            @Override
            public String[] next() {
                return customPresets[index[0]++].split("\\|");
            }
        };
    }

    public static int getNextPresetId(Context context) {
        int maxId = 0;
        for (String[] preset : customPresetsIterator(context)) {
            maxId = Math.max(maxId, Integer.parseInt(preset[0].replace(Box64Preset.CUSTOM+"-", "")));
        }
        return maxId+1;
    }

    public static void editPreset(Context context, String id, String name, EnvVars envVars) {
        final String key = "box64_custom_presets";
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String customPresetsStr = preferences.getString(key, "");

        if (id != null) {
            String[] customPresets = customPresetsStr.split(",");
            for (int i = 0; i < customPresets.length; i++) {
                String[] preset = customPresets[i].split("\\|");
                if (preset[0].equals(id)) {
                    customPresets[i] = id+"|"+name+"|"+envVars.toString();
                    break;
                }
            }
            customPresetsStr = String.join(",", customPresets);
        }
        else {
            String preset = Box64Preset.CUSTOM+"-"+getNextPresetId(context)+"|"+name+"|"+envVars.toString();
            customPresetsStr += (!customPresetsStr.isEmpty() ? "," : "")+preset;
        }
        preferences.edit().putString(key, customPresetsStr).apply();
    }

    public static void duplicatePreset(Context context, String id) {
        ArrayList<Box64Preset> presets = getPresets(context);
        Box64Preset originPreset = null;
        for (Box64Preset preset : presets) {
            if (preset.id.equals(id)) {
                originPreset = preset;
                break;
            }
        }
        if (originPreset == null) return;

        String newName;
        for (int i = 1;;i++) {
            newName = originPreset.name+" ("+i+")";
            boolean found = false;
            for (Box64Preset preset : presets) {
                if (preset.name.equals(newName)) {
                    found = true;
                    break;
                }
            }
            if (!found) break;
        }

        editPreset(context, null, newName, getEnvVars(context, originPreset.id));
    }

    public static void removePreset(Context context, String id) {
        final String key = "box64_custom_presets";
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String oldCustomPresetsStr = preferences.getString(key, "");
        String newCustomPresetsStr = "";

        String[] customPresets = oldCustomPresetsStr.split(",");
        for (int i = 0; i < customPresets.length; i++) {
            String[] preset = customPresets[i].split("\\|");
            if (!preset[0].equals(id)) newCustomPresetsStr += (!newCustomPresetsStr.isEmpty() ? "," : "")+customPresets[i];
        }

        preferences.edit().putString(key, newCustomPresetsStr).apply();
    }

    public static void exportPreset(Context context, String id) {
        File presetFile = null;
        String key = "box64_custom_presets";
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String[] customPresets = preferences.getString(key, "").split(",");

        for (int i = 0; i < customPresets.length; i++) {
            String[] preset = customPresets[i].split("\\|");
            if (preset.length > 0 && preset[0].equals(id)) {
                String uriPath = preferences.getString("winlator_path_uri", null);
                if (uriPath != null) {
                    Uri uri = Uri.parse(uriPath);
                    String path = FileUtils.getFilePathFromUri(uri);
                    presetFile = new File(path, "Presets/box64_" + preset[1] + ".wbp");
                }
                else {
                    presetFile = new File(com.winlator.fusion.SettingsFragment.DEFAULT_WINLATOR_PATH, "Presets/box64_" + preset[1] + ".wbp");
                }
                if (!presetFile.getParentFile().exists())
                    presetFile.getParentFile().mkdirs();

                try {
                    FileOutputStream fos = new FileOutputStream(presetFile);
                    PrintWriter pw = new PrintWriter(fos);
                    pw.write("ID:" + preset[0] + "\n");
                    pw.write("Name:" + preset[1] + "\n");
                    pw.write("EnvVars:" + preset[2] + "\n");
                    pw.close();
                    fos.close();
                } catch (IOException e) {}
                break;
            }
        }
        if (presetFile != null && presetFile.exists())
            AppUtils.showToast(context, context.getString(R.string.preset_exported) + " " + presetFile.getParentFile().getPath());
        else
            AppUtils.showToast(context, R.string.preset_export_failed);
    }

    public static void importPreset(Context context, InputStream stream) {
        String key = "box64_custom_presets";
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String customPresetStr = preferences.getString(key, "");

        try {
            String[] preset = new String[]{"", "", ""};
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] contents = line.split(":", 2);
                if (contents.length < 2) continue;
                switch (contents[0]) {
                    case "ID":
                        preset[0] = contents[1];
                        break;
                    case "Name":
                        preset[1] = contents[1];
                        break;
                    case "EnvVars":
                        preset[2] = contents[1];
                        break;
                }
            }
            if (preset[1].isEmpty()) return;
            if (preset[2].isEmpty()) return;
            customPresetStr = customPresetStr + (!customPresetStr.equals("") ? "," : "") + Box64Preset.CUSTOM+"-"+getNextPresetId(context) + "|" + preset[1] + "|" + preset[2];
        } catch (IOException e) {}

        preferences.edit().putString(key, customPresetStr).apply();
    }

    public static void loadSpinner(Spinner spinner, String selectedId) {
        Context context = spinner.getContext();
        ArrayList<Box64Preset> presets = getPresets(context);

        int selectedPosition = 0;
        for (int i = 0; i < presets.size(); i++) {
            if (presets.get(i).id.equals(selectedId)) {
                selectedPosition = i;
                break;
            }
        }

        spinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, presets));
        spinner.setSelection(selectedPosition);
    }

    public static String getSpinnerSelectedId(Spinner spinner) {
        SpinnerAdapter adapter = spinner.getAdapter();
        int selectedPosition = spinner.getSelectedItemPosition();
        if (adapter != null && adapter.getCount() > 0 && selectedPosition >= 0) {
            return ((Box64Preset)adapter.getItem(selectedPosition)).id;
        }
        else return Box64Preset.DEFAULT;
    }
}
