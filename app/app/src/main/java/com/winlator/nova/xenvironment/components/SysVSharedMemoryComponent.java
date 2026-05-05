package com.winlator.nova.xenvironment.components;

import com.winlator.nova.sysvshm.SysVSHMConnectionHandler;
import com.winlator.nova.sysvshm.SysVSHMRequestHandler;
import com.winlator.nova.sysvshm.SysVSharedMemory;
import com.winlator.nova.xconnector.UnixSocketConfig;
import com.winlator.nova.xconnector.XConnectorEpoll;
import com.winlator.nova.xenvironment.EnvironmentComponent;
import com.winlator.nova.xserver.SHMSegmentManager;
import com.winlator.nova.xserver.XServer;

public class SysVSharedMemoryComponent extends EnvironmentComponent {
    private XConnectorEpoll connector;
    public final UnixSocketConfig socketConfig;
    private SysVSharedMemory sysVSharedMemory;
    private final XServer xServer;

    public SysVSharedMemoryComponent(XServer xServer, UnixSocketConfig socketConfig) {
        this.xServer = xServer;
        this.socketConfig = socketConfig;
    }

    @Override
    public void start() {
        if (connector != null) return;
        sysVSharedMemory = new SysVSharedMemory();
        connector = new XConnectorEpoll(socketConfig, new SysVSHMConnectionHandler(sysVSharedMemory), new SysVSHMRequestHandler());
        connector.start();

        xServer.setSHMSegmentManager(new SHMSegmentManager(sysVSharedMemory));
    }

    @Override
    public void stop() {
        if (connector != null) {
            connector.destroy();
            connector = null;
        }

        sysVSharedMemory.deleteAll();
    }
}
