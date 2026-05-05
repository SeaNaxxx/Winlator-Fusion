package com.winlator.nova.contentdialog;

import android.content.Context;
import android.view.View;
import android.widget.Spinner;

import com.winlator.nova.R;
import com.winlator.nova.core.GeneralComponents;
import com.winlator.nova.core.KeyValueSet;

public class AdrenotoolsConfigDialog extends ContentDialog {
    public AdrenotoolsConfigDialog(final View anchor) {
        super(anchor.getContext(), R.layout.adrenotools_config_dialog);
        Context context = anchor.getContext();
        setIcon(R.drawable.icon_display_settings);
        setTitle("Adrenotools " + context.getString(R.string.configuration));

        final Spinner sAdrenotoolsDriver = findViewById(R.id.SAdrenotoolsDriver);

        KeyValueSet config = new KeyValueSet(anchor.getTag());
        String adrenotoolsDriver = config.get("driverId");
        GeneralComponents.initViews(GeneralComponents.Type.ADRENOTOOLS_DRIVER, findViewById(R.id.AdrenotoolsDriverToolbox), sAdrenotoolsDriver, adrenotoolsDriver, "System");

        setOnConfirmCallback(() -> {
            KeyValueSet newConfig = new KeyValueSet();
            String selectedDriver = (String) sAdrenotoolsDriver.getSelectedItem();
            if (selectedDriver != null && !selectedDriver.equals("System")) {
                newConfig.put("driverId", selectedDriver);
            }
            anchor.setTag(newConfig.toString());
        });
    }
}
