package com.winlator.fusion.renderer;

import com.winlator.fusion.widget.XServerView;

public interface Renderer {
    XServerView getXServerView();
    boolean isFullscreen();
    void toggleFullscreen();
    void setCursorVisible(boolean visible);
    boolean isCursorVisible();
    void setScreenOffsetYRelativeToCursor(boolean relative);
    void setMagnifierZoom(float zoom);
    float getMagnifierZoom();
}
