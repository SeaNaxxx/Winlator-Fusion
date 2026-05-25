package com.winlator.fusion.contentdialog;

import android.view.View;
import android.widget.CheckBox;
import android.widget.Spinner;

import com.winlator.fusion.R;
import com.winlator.fusion.container.Container;

public class RendererOptionsDialog extends ContentDialog {
    private Runnable afterConfirmCallback;

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
        String presentMode = getTagPresentMode(anchor, R.id.rendererPresentMode, "fifo");
        int filterMode = getTagInt(anchor, R.id.rendererFilterMode, Container.FILTER_MODE_LINEAR);
        int refreshRate = getTagInt(anchor, R.id.rendererRefreshRate, 0);

        sRendererType.setSelection(rendererType);
        cbNativeMode.setChecked(nativeMode);
        sPresentMode.setSelection(presentModeToIndex(presentMode));
        sFilterMode.setSelection(filterMode);

        int refreshIndex = refreshRateToIndex(refreshRate);
        sRefreshRate.setSelection(refreshIndex);

        setOnConfirmCallback(() -> {
            anchor.setTag(R.id.rendererType, (byte) sRendererType.getSelectedItemPosition());
            anchor.setTag(R.id.rendererNative, cbNativeMode.isChecked());
            anchor.setTag(R.id.rendererPresentMode, indexToPresentMode(sPresentMode.getSelectedItemPosition()));
            anchor.setTag(R.id.rendererFilterMode, sFilterMode.getSelectedItemPosition());
            anchor.setTag(R.id.rendererRefreshRate, indexToRefreshRate(sRefreshRate.getSelectedItemPosition()));
            if (afterConfirmCallback != null) afterConfirmCallback.run();
        });
    }

    public void setAfterConfirmCallback(Runnable callback) {
        this.afterConfirmCallback = callback;
    }

    private static int presentModeToIndex(String presentMode) {
        if (presentMode == null) return 0;
        switch (presentMode) {
            case "fifo": return 0;
            case "mailbox": return 1;
            case "immediate": return 2;
            default: return 0;
        }
    }

    private static String indexToPresentMode(int index) {
        switch (index) {
            case 1: return "mailbox";
            case 2: return "immediate";
            default: return "fifo";
        }
    }

    private static int refreshRateToIndex(int refreshRate) {
        switch (refreshRate) {
            case 30: return 1;
            case 60: return 2;
            case 90: return 3;
            case 120: return 4;
            default: return 0;
        }
    }

    private static int indexToRefreshRate(int index) {
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
        container.setRendererPresentMode(getTagPresentMode(anchor, R.id.rendererPresentMode, "fifo"));
        container.setRendererFilterMode(getTagInt(anchor, R.id.rendererFilterMode, Container.FILTER_MODE_LINEAR));
        container.setRendererRefreshRateLimit(getTagInt(anchor, R.id.rendererRefreshRate, 0));
    }

    public static void loadFromContainer(View anchor, Container container) {
        anchor.setTag(R.id.rendererType, container.getRendererType());
        anchor.setTag(R.id.rendererNative, container.isRendererNative());
        anchor.setTag(R.id.rendererPresentMode, container.getRendererPresentMode());
        anchor.setTag(R.id.rendererFilterMode, container.getRendererFilterMode());
        anchor.setTag(R.id.rendererRefreshRate, container.getRendererRefreshRateLimit());
    }

    public static byte getTagByte(View anchor, int key, byte defaultValue) {
        Object val = anchor.getTag(key);
        if (val instanceof Byte) return (Byte) val;
        if (val instanceof Integer) return ((Integer) val).byteValue();
        return defaultValue;
    }

    public static int getTagInt(View anchor, int key, int defaultValue) {
        Object val = anchor.getTag(key);
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Byte) return ((Byte) val).intValue();
        return defaultValue;
    }

    public static boolean getTagBoolean(View anchor, int key, boolean defaultValue) {
        Object val = anchor.getTag(key);
        if (val instanceof Boolean) return (Boolean) val;
        return defaultValue;
    }

    public static String getTagPresentMode(View anchor, int key, String defaultValue) {
        Object val = anchor.getTag(key);
        if (val instanceof String) return (String) val;
        if (val instanceof Byte) return indexToPresentMode(((Byte) val).intValue());
        if (val instanceof Integer) return indexToPresentMode((Integer) val);
        return defaultValue;
    }
}
