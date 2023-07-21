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
 * Classes implementing this interface can subscribe to Message objects
 * published using a MessageContext object.
 * 
 * @see jpl.gds.shared.message.IMessage
 * @see jpl.gds.shared.message.IMessagePublicationBus
 * 
 */
public interface MessageSubscriber {

	/**
	 * This method is called by a MessageContext object when it publishes a
	 * message.
	 * 
	 * @param message
	 *            the published message
	 */
	void handleMessage(IMessage message);
}
