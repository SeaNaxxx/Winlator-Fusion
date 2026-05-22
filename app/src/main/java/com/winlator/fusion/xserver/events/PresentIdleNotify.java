package com.winlator.fusion.xserver.events;

import com.winlator.fusion.xconnector.XOutputStream;
import com.winlator.fusion.xconnector.XStreamLock;
import com.winlator.fusion.xserver.Pixmap;
import com.winlator.fusion.xserver.Window;
import com.winlator.fusion.xserver.extensions.PresentExtension;

import java.io.IOException;

public class PresentIdleNotify extends Event {
    private final PresentExtension presentExtension;
    private final int eventId;
    private final Window window;
    private final Pixmap pixmap;
    private final int serial;
    private final int idleFence;

    public PresentIdleNotify(PresentExtension presentExtension, int eventId, Window window, Pixmap pixmap, int serial, int idleFence) {
        super(35);
        this.presentExtension = presentExtension;
        this.eventId = eventId;
        this.window = window;
        this.serial = serial;
        this.pixmap = pixmap;
        this.idleFence = idleFence;
    }

    @Override
    public void send(short sequenceNumber, XOutputStream outputStream) throws IOException {
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(code);
            outputStream.writeByte(presentExtension.getMajorOpcode());
            outputStream.writeShort(sequenceNumber);
            outputStream.writeInt(0);
            outputStream.writeShort(getEventType());
            outputStream.writeShort((short)0);
            outputStream.writeInt(eventId);
            outputStream.writeInt(window.id);
            outputStream.writeInt(serial);
            outputStream.writeInt(pixmap.id);
            outputStream.writeInt(idleFence);
        }
    }

    public static short getEventType() {
        return 2;
    }

    public static int getEventMask() {
        return 1<<getEventType();
    }
}
