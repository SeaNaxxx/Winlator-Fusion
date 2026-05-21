package com.winlator.fusion.contentdialog;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Spinner;

import com.winlator.fusion.R;
import com.winlator.fusion.core.AppUtils;
import com.winlator.fusion.core.EnvVars;
import com.winlator.fusion.core.KeyValueSet;
import com.winlator.fusion.core.StringUtils;

public class ZinkConfigDialog extends ContentDialog {
    public static final String DEFAULT_GL_VERSION = "4.5";

    public ZinkConfigDialog(final View anchor) {
        super(anchor.getContext(), R.layout.zink_config_dialog);
        Context context = anchor.getContext();
        setIcon(R.drawable.icon_settings);
        setTitle("Zink "+context.getString(R.string.configuration));

        final Spinner sGLVersion = findViewById(R.id.SGLVersion);
        final CheckBox cbNoError = findViewById(R.id.CBNoError);
        final CheckBox cbEmulatePointSmooth = findViewById(R.id.CBEmulatePointSmooth);

        KeyValueSet config = new KeyValueSet(anchor.getTag());
        AppUtils.setSpinnerSelectionFromIdentifier(sGLVersion, config.get("glVersion", DEFAULT_GL_VERSION));
        cbNoError.setChecked(config.getBoolean("noError", true));
        cbEmulatePointSmooth.setChecked(config.getBoolean("emulatePointSmooth"));

        setOnConfirmCallback(() -> {
            KeyValueSet newConfig = new KeyValueSet();
            newConfig.put("glVersion", StringUtils.parseNumber(sGLVersion.getSelectedItem()));
            newConfig.put("noError", cbNoError.isChecked() ? "1" : "0");
            newConfig.put("emulatePointSmooth", cbEmulatePointSmooth.isChecked() ? "1" : "0");
            anchor.setTag(newConfig.toString());
        });
    }

    public static void setEnvVars(KeyValueSet config, EnvVars envVars) {
        envVars.put("MESA_GL_VERSION_OVERRIDE", config.get("glVersion", DEFAULT_GL_VERSION));
        if (config.getBoolean("noError", true)) {
            envVars.put("MESA_NO_ERROR", "1");
        }
        if (config.getBoolean("emulatePointSmooth")) {
            envVars.put("ZINK_EMULATE_POINT_SMOUTH", "true");
        }
    }
}
