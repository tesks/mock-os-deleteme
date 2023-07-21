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
package jpl.gds.shared.message;

/**
 * Useful for making anonymous classes when a class wants to handle multiple
 * types of messages. For example:
 * 
 * <pre>
 * messageContext.subscribe(SomeMessage.TYPE, new BaseMessageHandler() {
 * 	public void handleMessage(Message message) {
 * 		this.handle((SomeMessage) message);
 * 	}
 * });
 * </pre>
 * 
 */
public abstract class BaseMessageHandler implements MessageSubscriber {
	/**
     * Handles receipt of a message by the subscriber.
     * 
     * @param message
     *            the message to handle.
     */
	@Override
    public abstract void handleMessage(IMessage message);
}
