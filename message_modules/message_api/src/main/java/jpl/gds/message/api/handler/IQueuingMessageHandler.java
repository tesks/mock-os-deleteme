/*
 * Copyright 2006-2018. California Institute of Technology.
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
package jpl.gds.message.api.handler;

import jpl.gds.message.api.external.IExternalMessage;
import jpl.gds.message.api.external.IMessageServiceListener;
import jpl.gds.message.api.external.MessageServiceException;

/**
 * Queuing message handler interface
 */
public interface IQueuingMessageHandler {

    /**
     * Add listener
     * @param listener IMessageServiceListener implementation
     */
    void addListener(IMessageServiceListener listener);

    /**
     * Remove listener
     * @param listener IMessageServiceListener implementation
     */
    void removeListener(IMessageServiceListener listener);

    /**
     * Remove all listeners
     */
    void clearListeners();

    /**
     * Set subscription
     * @param topic Subscription topic
     * @param filter Subscription filter
     * @param sharedConnection Whether the connection is shared
     * @throws MessageServiceException For message related issues
     */
    void setSubscription(String topic, String filter, boolean sharedConnection) throws MessageServiceException;

    /**
     * Start handler
     * @throws MessageServiceException For message related issues
     */
    void start() throws MessageServiceException;

    /**
     * Shutdown handler
     * @param abortSubscribers Whether to abort subscribers
     * @param waitToClear Whether to wait to finish processing
     */
    void shutdown(boolean abortSubscribers, boolean waitToClear);

    /**
     * Message handler
     * @param m External message
     */
    void onMessage(IExternalMessage m);

    /**
     * Check to see if there are more items to process in backlog
     * @return hasBacklog True if more items, false otherwise
     */
    boolean hasBacklog();
}
