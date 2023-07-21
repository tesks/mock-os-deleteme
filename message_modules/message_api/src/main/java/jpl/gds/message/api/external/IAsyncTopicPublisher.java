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

import java.util.List;
import java.util.Map;

import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.performance.IPerformanceData;

/**
 * An interface to be implemented by topic publishers that keep a queue
 * so messages can be published asynchronously to their submission.
 */
public interface IAsyncTopicPublisher extends ITopicPublisher {


	/**
	 * Flushes the asynchronous queue to the publisher.
	 */
	public abstract void flushQueue();

	/**
	 * {@inheritDoc}
	 * 
	 * clearQueue() should be called first.
	 * 
	 */
	@Override
	public abstract void close();

	/**
	 * Queues a data (binary) message for publication.
	 * @param type Message type.
	 * @param blob Message binary data.
	 * @param properties Properties of the message.
	 * @param timeToLive Time to live for the message.
	 * @param deliveryMode Delivery mode for the message.
	 * 
	 */
	public abstract void queueMessageForPublication(IMessageType type, byte[] blob,
			Map<String, Object> properties, long timeToLive, ExternalDeliveryMode deliveryMode);

	/**
	 * Queues a text message for publication.
	 * @param type Message type.
	 * @param text Message text.
	 * @param properties Properties of the message.
	 * @param timeToLive Time to live for the message.
	 * @param deliveryMode Delivery mode for the message.
	 * 
	 */
	public abstract void queueMessageForPublication(IMessageType type, String text,
			Map<String, Object> properties, long timeToLive, ExternalDeliveryMode deliveryMode);


	/**
	 * Starts asynchronous publication.
	 */
	public abstract void start();
	
	/**
	 * Gets the performance data for this publisher.
	 * 
	 * @return performance data object
	 */
	public abstract List<IPerformanceData> getPerformanceData();

}