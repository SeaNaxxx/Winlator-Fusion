package com.winlator.fusion.xserver.extensions;

import com.winlator.fusion.xconnector.XInputStream;
import com.winlator.fusion.xconnector.XOutputStream;
import com.winlator.fusion.xserver.XClient;
import com.winlator.fusion.xserver.XServer;
import com.winlator.fusion.xserver.errors.XRequestError;

import java.io.IOException;

public abstract class Extension {
    public static final byte START_MAJOR_OPCODE = -100;
    private final byte majorOpcode;
    protected final XServer xServer;
    private byte firstEventId = 0;
    private byte firstErrorId = 0;

    public Extension(XServer xServer, byte majorOpcode) {
        this.xServer = xServer;
        this.majorOpcode = majorOpcode;
    }

    public abstract String getName();

    public byte getMajorOpcode() {
        return majorOpcode;
    }

    public int getNumEvents() {
        return 0;
    }

    public int getNumErrors() {
        return 0;
    }

    public void setFirstEventId(byte id) {
        this.firstEventId = id;
    }

    public void setFirstErrorId(byte id) {
        this.firstErrorId = id;
    }

    public byte getFirstEventId() {
        return firstEventId;
    }

    public byte getFirstErrorId() {
        return firstErrorId;
    }

    public abstract void handleRequest(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError;
}
