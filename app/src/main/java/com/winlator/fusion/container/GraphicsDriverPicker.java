package com.winlator.fusion.container;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

import com.winlator.fusion.contentdialog.AdrenotoolsConfigDialog;
import com.winlator.fusion.contentdialog.TurnipConfigDialog;
import com.winlator.fusion.contentdialog.VirGLConfigDialog;
import com.winlator.fusion.contentdialog.VortekConfigDialog;
import com.winlator.fusion.contentdialog.ZinkConfigDialog;
import com.winlator.fusion.core.KeyValueSet;
import com.winlator.fusion.core.StringUtils;
import com.winlator.fusion.widget.TaggedSelectionBox;

public class GraphicsDriverPicker {
    private final LinearLayout container;
    private final boolean isBionic;

    public GraphicsDriverPicker(LinearLayout container, String selectedGraphicsDriver, String graphicsDriverConfig) {
        this(container, selectedGraphicsDriver, graphicsDriverConfig, false);
    }

    public GraphicsDriverPicker(LinearLayout container, String selectedGraphicsDriver, String graphicsDriverConfig, boolean isBionic) {
        this.container = container;
        this.isBionic = isBionic;
        final Context context = container.getContext();
        container.removeAllViews();
        String[] identifiers = GraphicsDrivers.parseIdentifiers(selectedGraphicsDriver);
        KeyValueSet[] configs = GraphicsDrivers.parseConfigs(selectedGraphicsDriver, graphicsDriverConfig);

        final String[] apiNames = {"Vulkan", "OpenGL"};
        for (int i = 0; i < apiNames.length; i++) {
            final TaggedSelectionBox taggedSelectionBox = new TaggedSelectionBox(context);
            taggedSelectionBox.setLabel(apiNames[i]);
            taggedSelectionBox.setItems(GraphicsDrivers.getItems(apiNames[i], isBionic));
            taggedSelectionBox.setSelectedItem(GraphicsDrivers.getName(identifiers[i]));
            taggedSelectionBox.setTag(configs[i].toString());
            taggedSelectionBox.setOnButtonClickListener(() -> {
                final String graphicsDriver = StringUtils.parseIdentifier(taggedSelectionBox.getSelectedItem());
                showGraphicsDriverConfigDialog(graphicsDriver, taggedSelectionBox);
            });
            container.addView(taggedSelectionBox);
        }
    }

    public String getGraphicsDriver() {
        StringBuilder graphicsDriver = new StringBuilder();
        for (int i = 0; i < container.getChildCount(); i++) {
            TaggedSelectionBox taggedSelectionBox = (TaggedSelectionBox)container.getChildAt(i);
            if (graphicsDriver.length() > 0) graphicsDriver.append(',');
            graphicsDriver.append(StringUtils.parseIdentifier(taggedSelectionBox.getSelectedItem()));
        }
        return graphicsDriver.toString();
    }

    public String getGraphicsDriverConfig() {
        StringBuilder graphicsDriverConfig = new StringBuilder();
        for (int i = 0; i < container.getChildCount(); i++) {
            TaggedSelectionBox taggedSelectionBox = (TaggedSelectionBox)container.getChildAt(i);
            if (graphicsDriverConfig.length() > 0) graphicsDriverConfig.append('|');
            graphicsDriverConfig.append(taggedSelectionBox.getTag().toString());
        }
        return graphicsDriverConfig.toString();
    }

    private static void showGraphicsDriverConfigDialog(String graphicsDriver, View anchor) {
        switch (graphicsDriver) {
            case GraphicsDrivers.TURNIP:
                (new TurnipConfigDialog(anchor)).show();
                break;
            case GraphicsDrivers.VORTEK:
                (new VortekConfigDialog(anchor)).show();
                break;
            case GraphicsDrivers.VIRGL:
                (new VirGLConfigDialog(anchor)).show();
                break;
            case GraphicsDrivers.WRAPPER:
                (new AdrenotoolsConfigDialog(anchor)).show();
                break;
            case GraphicsDrivers.ZINK:
                (new ZinkConfigDialog(anchor)).show();
                break;
            default:
                break;
        }
    }
}
