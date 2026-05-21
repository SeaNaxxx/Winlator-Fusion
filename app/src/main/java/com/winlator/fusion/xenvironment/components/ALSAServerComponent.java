package com.winlator.fusion.xenvironment.components;

import com.winlator.fusion.alsaserver.ALSAClient;
import com.winlator.fusion.alsaserver.ALSAClientConnectionHandler;
import com.winlator.fusion.alsaserver.ALSARequestHandler;
import com.winlator.fusion.xconnector.UnixSocketConfig;
import com.winlator.fusion.xconnector.XConnectorEpoll;
import com.winlator.fusion.xenvironment.EnvironmentComponent;

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
