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

package jpl.gds.mds.server.tcp;

import jpl.gds.mds.server.disruptor.MessageEvent;

/**
 * Interface for Client Connection Manager
 */
public interface IClientConnectionManager {

    /**
     * Flush the internal message buffer
     */
    void flushBuffer();

    /**
     * Get the total number of received message count
     *
     * @return message count
     */
    long getTotalReceivedCount();

    /**
     * Get the total number of sent message count
     *
     * @return message count
     */
    long getTotalSentCount();

    /**
     * Handle message
     *
     * @param message Message to handle
     */
    void handleMessage(MessageEvent message);
}
