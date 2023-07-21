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
package jpl.gds.message.api.external;

/**
 * An interface to be implemented by classes that want to listen for messages
 * delivered to a message service subscriber. When a subscriber to the external
 * message service is created, a listener may be added to be asynchronously
 * notified when messages arrive.
 */
public interface IMessageServiceListener {
    
    /**
     * Called when a message is received.
     * 
     * @param message the message that was received
     */
    public void onMessage(IExternalMessage message);

}
