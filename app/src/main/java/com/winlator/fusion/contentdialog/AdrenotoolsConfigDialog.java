package com.winlator.fusion.contentdialog;

import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.winlator.fusion.R;
import com.winlator.fusion.contents.AdrenotoolsManager;
import com.winlator.fusion.core.AppUtils;
import com.winlator.fusion.core.KeyValueSet;
import com.winlator.fusion.core.StringUtils;

import java.util.ArrayList;

public class AdrenotoolsConfigDialog extends ContentDialog {
    public AdrenotoolsConfigDialog(final View anchor) {
        super(anchor.getContext(), R.layout.adrenotools_config_dialog);
        Context context = anchor.getContext();
        setIcon(R.drawable.icon_display_settings);
        setTitle("Adrenotools " + context.getString(R.string.configuration));

        final Spinner sDriver = findViewById(R.id.SAdrenotoolsDriver);
        final Spinner sVkMaxVersion = findViewById(R.id.SVkMaxVersion);
        final Spinner sMaxDeviceMemory = findViewById(R.id.SMaxDeviceMemory);

        AdrenotoolsManager adrenotoolsManager = new AdrenotoolsManager(context);
        ArrayList<String> installedDrivers = adrenotoolsManager.enumerateInstalledDrivers();

        String[] assetDrivers = context.getResources().getStringArray(R.array.wrapper_graphics_driver_version_entries);
        ArrayList<String> allDrivers = new ArrayList<>();

        for (String driver : assetDrivers) {
            if (!allDrivers.contains(driver)) allDrivers.add(driver);
        }
        for (String driver : installedDrivers) {
            if (!allDrivers.contains(driver)) allDrivers.add(driver);
        }
        allDrivers.add(0, "default");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, allDrivers);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sDriver.setAdapter(adapter);

        KeyValueSet config = new KeyValueSet(anchor.getTag());

        String adrenotoolsDriver = config.get("adrenotoolsDriver");
        if (adrenotoolsDriver != null && !adrenotoolsDriver.isEmpty()) {
            int pos = allDrivers.indexOf(adrenotoolsDriver);
            if (pos >= 0) sDriver.setSelection(pos);
        } else {
            String currentDriverId = (String) anchor.getTag();
            if (currentDriverId != null && !currentDriverId.isEmpty() && !currentDriverId.contains("=")) {
                int pos = allDrivers.indexOf(currentDriverId);
                if (pos >= 0) sDriver.setSelection(pos);
            }
        }

        AppUtils.setSpinnerSelectionFromValue(sVkMaxVersion, config.get("vkMaxVersion", "1.3"));
        AppUtils.setSpinnerSelectionFromMemorySize(sMaxDeviceMemory, config.get("maxDeviceMemory", "0"));

        setOnConfirmCallback(() -> {
            KeyValueSet newConfig = new KeyValueSet();
            String selectedDriver = (String) sDriver.getSelectedItem();
            if (selectedDriver != null && !selectedDriver.equals("default")) {
                newConfig.put("adrenotoolsDriver", selectedDriver);
            }
            String vkMaxVersionValue = sVkMaxVersion.getSelectedItem() != null ? sVkMaxVersion.getSelectedItem().toString() : "1.3";
            if (vkMaxVersionValue.isEmpty()) vkMaxVersionValue = "1.3";
            newConfig.put("vkMaxVersion", vkMaxVersionValue);
            newConfig.put("maxDeviceMemory", StringUtils.parseMemorySize(sMaxDeviceMemory.getSelectedItem()));
            anchor.setTag(newConfig.toString());
        });
    }

    public static boolean isAvailable(Context context, boolean isBionic) {
        return isBionic;
    }
}
