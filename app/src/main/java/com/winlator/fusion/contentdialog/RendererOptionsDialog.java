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

        byte rendererType = getTagByte(anchor, R.id.rendererType, Container.RENDERER_GL);
        boolean nativeMode = getTagBoolean(anchor, R.id.rendererNative, false);
        byte presentMode = getTagByte(anchor, R.id.rendererPresentMode, Container.PRESENT_MODE_FIFO);
        byte filterMode = getTagByte(anchor, R.id.rendererFilterMode, Container.FILTER_MODE_LINEAR);
        byte refreshRate = getTagByte(anchor, R.id.rendererRefreshRate, (byte) 0);

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
        container.setRendererType(getTagByte(anchor, R.id.rendererType, Container.RENDERER_GL));
        container.setRendererNative(getTagBoolean(anchor, R.id.rendererNative, false));
        container.setRendererPresentMode(getTagByte(anchor, R.id.rendererPresentMode, Container.PRESENT_MODE_FIFO));
        container.setRendererFilterMode(getTagByte(anchor, R.id.rendererFilterMode, Container.FILTER_MODE_LINEAR));
        container.setRendererRefreshRate(getTagByte(anchor, R.id.rendererRefreshRate, (byte) 0));
    }

    public static void loadFromContainer(View anchor, Container container) {
        anchor.setTag(R.id.rendererType, container.getRendererType());
        anchor.setTag(R.id.rendererNative, container.isRendererNative());
        anchor.setTag(R.id.rendererPresentMode, container.getRendererPresentMode());
        anchor.setTag(R.id.rendererFilterMode, container.getRendererFilterMode());
        anchor.setTag(R.id.rendererRefreshRate, container.getRendererRefreshRate());
    }

    private static byte getTagByte(View anchor, int key, byte defaultValue) {
        Object val = anchor.getTag(key);
        if (val instanceof Byte) return (Byte) val;
        if (val instanceof Integer) return ((Integer) val).byteValue();
        return defaultValue;
    }

    private static boolean getTagBoolean(View anchor, int key, boolean defaultValue) {
        Object val = anchor.getTag(key);
        if (val instanceof Boolean) return (Boolean) val;
        return defaultValue;
    }
}
