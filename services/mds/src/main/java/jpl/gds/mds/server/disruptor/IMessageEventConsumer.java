/*
 * Copyright 2006-2020. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */

package jpl.gds.mds.server.disruptor;

import com.lmax.disruptor.EventHandler;
import jpl.gds.mds.server.tcp.IClientConnectionManager;

/**
 * Interface for a Message Event Consumer
 */
public interface IMessageEventConsumer extends IMessageHandler {

    /**
     * Get the Ring Buffer Event Handler
     *
     * @return EvetHandler<messageEvent>
     */
    EventHandler<MessageEvent> getEventHandler();

    /**
     * Add new client socket connection manager
     *
     * @param clientConnectionManager the socket connection manager
     */
    void addSocketConnection(IClientConnectionManager clientConnectionManager);

    /**
     * Remove existing client socket connection manager
     *
     * @param clientConnectionManager the socket connection manager
     */
    void removeSocketConnection(IClientConnectionManager clientConnectionManager);
}
