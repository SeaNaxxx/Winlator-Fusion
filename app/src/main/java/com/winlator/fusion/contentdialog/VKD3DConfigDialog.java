package com.winlator.fusion.contentdialog;

import android.content.Context;
import android.view.View;
import android.widget.Spinner;

import com.winlator.fusion.R;
import com.winlator.fusion.core.AppUtils;
import com.winlator.fusion.core.DefaultVersion;
import com.winlator.fusion.core.EnvVars;
import com.winlator.fusion.core.GeneralComponents;
import com.winlator.fusion.core.KeyValueSet;
import com.winlator.fusion.xenvironment.RootFS;

public class VKD3DConfigDialog extends ContentDialog {
    public static final String DEFAULT_FEATURE_LEVEL = "12.2";

    public VKD3DConfigDialog(final View anchor) {
        super(anchor.getContext(), R.layout.vkd3d_config_dialog);
        Context context = anchor.getContext();
        setIcon(R.drawable.icon_display_settings);
        setTitle("VKD3D "+context.getString(R.string.configuration));

        final Spinner sVersion = findViewById(R.id.SVersion);
        final Spinner sFeatureLevel = findViewById(R.id.SFeatureLevel);

        KeyValueSet config = new KeyValueSet(anchor.getTag());
        AppUtils.setSpinnerSelectionFromValue(sFeatureLevel, config.get("featureLevel", DEFAULT_FEATURE_LEVEL));

        String version = config.get("version");
        GeneralComponents.initViews(GeneralComponents.Type.VKD3D, findViewById(R.id.VKD3DToolbox), sVersion, version, DefaultVersion.VKD3D);

        setOnConfirmCallback(() -> {
            KeyValueSet newConfig = new KeyValueSet();
            newConfig.put("version", sVersion.getSelectedItem().toString());
            newConfig.put("featureLevel", sFeatureLevel.getSelectedItem().toString());
            anchor.setTag(newConfig.toString());
        });
    }

    public static void setEnvVars(KeyValueSet config, EnvVars envVars) {
        envVars.put("DXVK_LOG_LEVEL", "none");
        envVars.put("DXVK_STATE_CACHE_PATH", RootFS.getDosUserCachePath());
        envVars.put("VKD3D_FEATURE_LEVEL", config.get("featureLevel", VKD3DConfigDialog.DEFAULT_FEATURE_LEVEL).replace(".", "_"));
        envVars.put("VKD3D_SHADER_CACHE_PATH", RootFS.getDosUserCachePath());
    }
}