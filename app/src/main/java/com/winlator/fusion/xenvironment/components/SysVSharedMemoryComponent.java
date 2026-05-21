package com.winlator.fusion.xenvironment.components;

import com.winlator.fusion.sysvshm.SysVSHMConnectionHandler;
import com.winlator.fusion.sysvshm.SysVSHMRequestHandler;
import com.winlator.fusion.sysvshm.SysVSharedMemory;
import com.winlator.fusion.xconnector.UnixSocketConfig;
import com.winlator.fusion.xconnector.XConnectorEpoll;
import com.winlator.fusion.xenvironment.EnvironmentComponent;
import com.winlator.fusion.xserver.SHMSegmentManager;
import com.winlator.fusion.xserver.XServer;

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
