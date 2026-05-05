package com.winlator.nova.xenvironment.components;

import com.winlator.nova.xenvironment.EnvironmentComponent;
import com.winlator.nova.xconnector.XConnectorEpoll;
import com.winlator.nova.xconnector.UnixSocketConfig;
import com.winlator.nova.xserver.XClientConnectionHandler;
import com.winlator.nova.xserver.XClientRequestHandler;
import com.winlator.nova.xserver.XServer;

public class XServerComponent extends EnvironmentComponent {
    private XConnectorEpoll connector;
    private final XServer xServer;
    private final UnixSocketConfig socketConfig;

    public XServerComponent(XServer xServer, UnixSocketConfig socketConfig) {
        this.xServer = xServer;
        this.socketConfig = socketConfig;
    }

    @Override
    public void start() {
        if (connector != null) return;
        connector = new XConnectorEpoll(socketConfig, new XClientConnectionHandler(xServer), new XClientRequestHandler());
        connector.setInitialInputBufferCapacity(4096);
        connector.setInitialOutputBufferCapacity(4096);
        connector.setCanReceiveAncillaryMessages(true);
        connector.start();
    }

    @Override
    public void stop() {
        if (connector != null) {
            connector.destroy();
            connector = null;
        }
    }

    public XServer getXServer() {
        return xServer;
    }
}
