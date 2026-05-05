package com.winlator.nova.xserver.events;

import com.winlator.nova.core.Bitmask;
import com.winlator.nova.xserver.Window;

public class ButtonRelease extends InputDeviceEvent {
    public ButtonRelease(byte detail, Window root, Window event, Window child, short rootX, short rootY, short eventX, short eventY, Bitmask state) {
        super(5, detail, root, event, child, rootX, rootY, eventX, eventY, state);
    }
}
