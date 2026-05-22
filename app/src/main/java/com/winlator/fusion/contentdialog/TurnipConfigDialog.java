package com.winlator.fusion.contentdialog;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Spinner;

import com.winlator.fusion.R;
import com.winlator.fusion.core.AppUtils;
import com.winlator.fusion.core.DefaultVersion;
import com.winlator.fusion.core.EnvVars;
import com.winlator.fusion.core.GPUHelper;
import com.winlator.fusion.core.GeneralComponents;
import com.winlator.fusion.core.KeyValueSet;
import com.winlator.fusion.core.StringUtils;

public class TurnipConfigDialog extends ContentDialog {
    public TurnipConfigDialog(final View anchor) {
        super(anchor.getContext(), R.layout.turnip_config_dialog);
        Context context = anchor.getContext();
        setIcon(R.drawable.icon_display_settings);
        setTitle("Turnip "+context.getString(R.string.configuration));

        final Spinner sVersion = findViewById(R.id.SVersion);
        final Spinner sMaxDeviceMemory = findViewById(R.id.SMaxDeviceMemory);
        final CheckBox cbUseHWBuf = findViewById(R.id.CBUseHWBuf);
        final CheckBox cbForceWaitForFences = findViewById(R.id.CBForceWaitForFences);

        KeyValueSet config = new KeyValueSet(anchor.getTag());
        cbUseHWBuf.setChecked(config.getBoolean("useHWBuf", true));
        cbForceWaitForFences.setChecked(config.getBoolean("forceWaitForFences"));
        AppUtils.setSpinnerSelectionFromMemorySize(sMaxDeviceMemory, config.get("maxDeviceMemory", "0"));

        String version = config.get("version");
        GeneralComponents.initViews(GeneralComponents.Type.TURNIP, findViewById(R.id.TurnipToolbox), sVersion, version, DefaultVersion.TURNIP);

        setOnConfirmCallback(() -> {
            KeyValueSet newConfig = new KeyValueSet();
            newConfig.put("version", StringUtils.parseNumber(sVersion.getSelectedItem()));
            newConfig.put("maxDeviceMemory", StringUtils.parseMemorySize(sMaxDeviceMemory.getSelectedItem()));
            newConfig.put("useHWBuf", cbUseHWBuf.isChecked() ? "1" : "0");
            newConfig.put("forceWaitForFences", cbForceWaitForFences.isChecked() ? "1" : "0");
            anchor.setTag(newConfig.toString());
        });
    }

    public static void setEnvVars(Context context, KeyValueSet config, EnvVars envVars) {
        String maxDeviceMemory = config.get("maxDeviceMemory", "0");
        if (!maxDeviceMemory.equals("0")) envVars.put("TU_OVERRIDE_HEAP_SIZE", maxDeviceMemory);
        if (config.getBoolean("useHWBuf", true)) envVars.put("MESA_VK_WSI_USE_HWBUF", "1");
        if (config.getBoolean("forceWaitForFences")) envVars.put("MESA_VK_WSI_FORCE_WAIT_FOR_FENCES", "1");
    }
}