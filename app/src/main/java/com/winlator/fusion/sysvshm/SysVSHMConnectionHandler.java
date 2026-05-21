package com.winlator.fusion.sysvshm;

import com.winlator.fusion.xconnector.ConnectedClient;
import com.winlator.fusion.xconnector.ConnectionHandler;

public class SysVSHMConnectionHandler implements ConnectionHandler {
    private final SysVSharedMemory sysVSharedMemory;

    public SysVSHMConnectionHandler(SysVSharedMemory sysVSharedMemory) {
        this.sysVSharedMemory = sysVSharedMemory;
    }

    @Override
    public void handleNewConnection(ConnectedClient client) {
        client.setTag(sysVSharedMemory);
    }

    @Override
    public void handleConnectionShutdown(ConnectedClient client) {}
}
