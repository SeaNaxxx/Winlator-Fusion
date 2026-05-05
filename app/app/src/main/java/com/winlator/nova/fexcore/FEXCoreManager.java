package com.winlator.nova.fexcore;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.winlator.nova.contents.ContentProfile;
import com.winlator.nova.contents.ContentsManager;
import com.winlator.nova.core.AppUtils;
import com.winlator.nova.core.DefaultVersion;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public abstract class FEXCoreManager {
    public static void loadFEXCoreVersion(Context context, ContentsManager contentsManager, Spinner spinner, String fexcoreVersion) {
        LinkedHashSet<String> versions = new LinkedHashSet<>();
        versions.add(DefaultVersion.FEXCORE);
        if (fexcoreVersion != null && !fexcoreVersion.isEmpty()) {
            versions.add(fexcoreVersion);
        }
        for (ContentProfile profile : contentsManager.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_FEXCORE)) {
            if (profile.remoteUrl != null) continue;
            String entryName = ContentsManager.getEntryName(profile);
            int firstDashIndex = entryName.indexOf('-');
            versions.add(entryName.substring(firstDashIndex + 1));
        }
        List<String> itemList = new ArrayList<>(versions);
        spinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, itemList));
        AppUtils.setSpinnerSelectionFromValue(spinner, fexcoreVersion);
    }
}
