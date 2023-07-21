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
package jpl.gds.jms.portal;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import jpl.gds.shared.message.IMessageType;


/**
 * IMessagePreparer is an interface implemented by classes responsible for
 * preparing message content and pushing it to JMS publishers.
 */
public interface IMessagePreparer {
	
	/**
	 * Shuts down this preparer.
	 */	 
	 public void shutdown();
	 
	 /**
	  * Returns the current message queue length for this preparer, if it has a queue.
	  * 
	  * @return message count
	  */
	 public int getQueuedMessageCount();
	 
	 /**
	  * Sends a new message to this preparer for transfer to a publisher.
	  * Note that this method may block if one of the publisher queues
	  * associated with this publisher is blocked.
	  * 
	  * @param m a TranslatedMessage object to publish
	  */
	 public void messageReceived(TranslatedMessage m);
	 
	 /**
	  * Gets the total number of messages this preparer has sent to JMS publishers.
	  * @return message publish total
	  */
	 public long getPublishTotal(); 
	 
	/**
	 * Returns a non-modifiable map of pairs of message type to publish total.
	 * This map is kept only when debug logging is enabled on the "JmsDebug"
	 * tracer.
	 * 
	 * @return map of message type to long total
	 */
	 public Map<IMessageType, AtomicLong> getPublishTotalByType();
	 
	 /**
	  * Returns the message type being handled by this preparer.
	  * @return message type
	  */
	 public IMessageType getMessageType();
	 
	/**
	 * For preparers that support batching of multiple internal messages into
	 * larger JMS messages, enables or disables the batch handling
	 * 
	 * @param val
	 *            true to enable batching, false to disable
	 */
	 public void enableBatching(boolean val);
	 
	 /**
	  * Instructs the preparer to immediately flush any internal queue it has
	  * to the publishers. May block if the publishers block.
	  */
	 public void flushQueue();
}
