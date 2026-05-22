package com.winlator.fusion.contentdialog;

import android.view.View;
import android.widget.CheckBox;
import android.widget.Spinner;

import com.winlator.fusion.R;
import com.winlator.fusion.container.Container;

public class RendererOptionsDialog extends ContentDialog {
    public RendererOptionsDialog(final View anchor) {
        super(anchor.getContext(), R.layout.renderer_options_dialog);
        setIcon(R.drawable.icon_settings);
        setTitle(getContext().getString(R.string.renderer_options));

        final Spinner sRendererType = findViewById(R.id.SRendererType);
        final CheckBox cbNativeMode = findViewById(R.id.CBNativeMode);
        final Spinner sPresentMode = findViewById(R.id.SPresentMode);
        final Spinner sFilterMode = findViewById(R.id.SFilterMode);
        final Spinner sRefreshRate = findViewById(R.id.SRefreshRate);

        byte rendererType = (byte) anchor.getTag(R.id.rendererType);
        boolean nativeMode = (boolean) anchor.getTag(R.id.rendererNative);
        byte presentMode = (byte) anchor.getTag(R.id.rendererPresentMode);
        byte filterMode = (byte) anchor.getTag(R.id.rendererFilterMode);
        byte refreshRate = (byte) anchor.getTag(R.id.rendererRefreshRate);

        sRendererType.setSelection(rendererType);
        cbNativeMode.setChecked(nativeMode);
        sPresentMode.setSelection(presentMode);
        sFilterMode.setSelection(filterMode);

        int refreshIndex = refreshRateToIndex(refreshRate);
        sRefreshRate.setSelection(refreshIndex);

        setOnConfirmCallback(() -> {
            anchor.setTag(R.id.rendererType, (byte) sRendererType.getSelectedItemPosition());
            anchor.setTag(R.id.rendererNative, cbNativeMode.isChecked());
            anchor.setTag(R.id.rendererPresentMode, (byte) sPresentMode.getSelectedItemPosition());
            anchor.setTag(R.id.rendererFilterMode, (byte) sFilterMode.getSelectedItemPosition());
            anchor.setTag(R.id.rendererRefreshRate, indexToRefreshRate(sRefreshRate.getSelectedItemPosition()));
        });
    }

    private static int refreshRateToIndex(byte refreshRate) {
        switch (refreshRate) {
            case 30: return 1;
            case 60: return 2;
            case 90: return 3;
            case 120: return 4;
            default: return 0;
        }
    }

    private static byte indexToRefreshRate(int index) {
        switch (index) {
            case 1: return 30;
            case 2: return 60;
            case 3: return 90;
            case 4: return 120;
            default: return 0;
        }
    }

    public static void applyToContainer(View anchor, Container container) {
        Object rt = anchor.getTag(R.id.rendererType);
        if (rt != null) container.setRendererType((byte) rt);
        Object rn = anchor.getTag(R.id.rendererNative);
        if (rn != null) container.setRendererNative((boolean) rn);
        Object pm = anchor.getTag(R.id.rendererPresentMode);
        if (pm != null) container.setRendererPresentMode((byte) pm);
        Object fm = anchor.getTag(R.id.rendererFilterMode);
        if (fm != null) container.setRendererFilterMode((byte) fm);
        Object rr = anchor.getTag(R.id.rendererRefreshRate);
        if (rr != null) container.setRendererRefreshRate((byte) rr);
    }

    public static void loadFromContainer(View anchor, Container container) {
        anchor.setTag(R.id.rendererType, container.getRendererType());
        anchor.setTag(R.id.rendererNative, container.isRendererNative());
        anchor.setTag(R.id.rendererPresentMode, container.getRendererPresentMode());
        anchor.setTag(R.id.rendererFilterMode, container.getRendererFilterMode());
        anchor.setTag(R.id.rendererRefreshRate, container.getRendererRefreshRate());
    }
}
