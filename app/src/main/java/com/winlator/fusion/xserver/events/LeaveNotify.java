package com.winlator.fusion.xserver.events;

import com.winlator.fusion.core.Bitmask;
import com.winlator.fusion.xserver.Window;

public class LeaveNotify extends PointerWindowEvent {
    public LeaveNotify(Detail detail, Window root, Window event, Window child, short rootX, short rootY, short eventX, short eventY, Bitmask state, Mode mode, boolean sameScreenAndFocus) {
        super(8, detail, root, event, child, rootX, rootY, eventX, eventY, state, mode, sameScreenAndFocus);
    }
}
