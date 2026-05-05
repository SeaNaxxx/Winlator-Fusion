package com.winlator.nova.xenvironment.components;

import com.winlator.nova.alsaserver.ALSAClient;
import com.winlator.nova.alsaserver.ALSAClientConnectionHandler;
import com.winlator.nova.alsaserver.ALSARequestHandler;
import com.winlator.nova.xconnector.UnixSocketConfig;
import com.winlator.nova.xconnector.XConnectorEpoll;
import com.winlator.nova.xenvironment.EnvironmentComponent;

public class ALSAServerComponent extends EnvironmentComponent {
    private XConnectorEpoll connector;
    private final UnixSocketConfig socketConfig;
    private final ALSAClient.Options options;

    public ALSAServerComponent(UnixSocketConfig socketConfig, ALSAClient.Options options) {
        this.socketConfig = socketConfig;
        this.options = options;
    }

    @Override
    public void start() {
        if (connector != null) return;
        ALSAClient.assignFramesPerBuffer(environment.getContext());
        connector = new XConnectorEpoll(socketConfig, new ALSAClientConnectionHandler(options), new ALSARequestHandler());
        connector.setMultithreadedClients(true);
        connector.start();
    }

    @Override
    public void stop() {
        if (connector != null) {
            connector.destroy();
            connector = null;
        }
    }
}
