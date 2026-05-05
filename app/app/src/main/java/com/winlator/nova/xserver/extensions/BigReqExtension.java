package com.winlator.nova.xserver.extensions;

import static com.winlator.nova.xserver.XClientRequestHandler.RESPONSE_CODE_SUCCESS;

import com.winlator.nova.xconnector.XInputStream;
import com.winlator.nova.xconnector.XOutputStream;
import com.winlator.nova.xconnector.XStreamLock;
import com.winlator.nova.xserver.XClient;
import com.winlator.nova.xserver.XServer;

import java.io.IOException;

public class BigReqExtension extends Extension {
    private static final int MAX_REQUEST_LENGTH = 4194303;

    public BigReqExtension(XServer xServer, byte majorOpcode) {
        super(xServer, majorOpcode);
    }

    @Override
    public String getName() {
        return "BIG-REQUESTS";
    }

    @Override
    public void handleRequest(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeInt(MAX_REQUEST_LENGTH);
            outputStream.writePad(20);
        }
    }
}
