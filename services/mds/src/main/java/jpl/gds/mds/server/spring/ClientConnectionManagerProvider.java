/*
 * Copyright 2006-2021. California Institute of Technology.
 * ALL RIGHTS RESERVED.
 * U.S. Government sponsorship acknowledged.
 *
 * This software is subject to U. S. export control laws and
 * regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 * extent that the software is subject to U.S. export control laws
 * and regulations, the recipient has the responsibility to obtain
 * export licenses or other export authority as may be required
 * before exporting such information to foreign countries or
 * providing access to foreign nationals.
 */

package jpl.gds.mds.server.spring;

import jpl.gds.mds.server.disruptor.IMessageEventConsumer;
import jpl.gds.mds.server.tcp.IClientConnectionManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.Socket;

/**
 * Provider for client connection managers. Spring aware.
 */
public class ClientConnectionManagerProvider {
    @Autowired
    private ObjectProvider<IClientConnectionManager> clientConnectionManagerProvider;
    @Autowired
    private SpillProcessorProvider                   spillProcessorProvider;

    /**
     * Get a client connection manager.
     *
     * @param messageDistributor message distributor
     * @param socket             client socket
     * @return
     */
    public IClientConnectionManager getClientConnectionManager(final IMessageEventConsumer messageDistributor,
                                                               final Socket socket) {

        return clientConnectionManagerProvider.getObject(messageDistributor, socket, spillProcessorProvider);
    }
}
