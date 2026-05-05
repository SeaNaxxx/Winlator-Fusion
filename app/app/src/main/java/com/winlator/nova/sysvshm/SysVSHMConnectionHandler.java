package com.winlator.nova.sysvshm;

import com.winlator.nova.xconnector.ConnectedClient;
import com.winlator.nova.xconnector.ConnectionHandler;

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
