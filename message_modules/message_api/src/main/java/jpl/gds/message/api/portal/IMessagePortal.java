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
package jpl.gds.message.api.portal;

import jpl.gds.context.api.TopicNameToken;
import jpl.gds.shared.interfaces.IService;
import jpl.gds.shared.message.MessageSubscriber;

/**
 * An interface to be implemented by portals to a message service.
 */
public interface IMessagePortal extends IService, MessageSubscriber {

    /**
     * Clears all messages queued for send by the portal.
     */
    public void clearAllQueuedMessages();

    /**
     * Sets the flag indicating that the message portal should flush all messages
     * it receives to the message service immediately, as opposed to batching them up
     * or delaying for any reason.
     * 
     * @param b true to enable, false to disable
     */
    public void enableImmediateFlush(boolean b);

    /**
     * Gets the total number of internal messages received for publication by the portal.
     * 
     * @return message count
     */
    public long getReceivedMessageCount();
    
    /**
     * Set custom topics that the message portal should enable
     * @param topics -> list of topics enum to enable
     */
    public void enableSpecificTopics(TopicNameToken[] topics);

}
