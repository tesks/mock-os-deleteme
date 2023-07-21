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
/**
 * 
 */
package jpl.gds.monitor.guiapp.common;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import jpl.gds.message.api.handler.IQueuingMessageHandler;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessageType;

/**
 * The MonitorSubscriber class tracks statistics for a message bus subscriber in the
 * monitor and is responsible for committing transacted message service subscribers when
 * transaction size is reached.
 *
 */
public class MonitorSubscriber {
	private String topic;
	private long receiveCount = 0L;
	private long lastMessageNumber = 0L;
	private long lastReceiveTime = 0L;
	private IQueuingMessageHandler subscriber;
	private long lagTime = 0L;
	private final StringBuilder sb;
	
	/**
	 * Maps the message type and message source ID ("<type>/<sourceID>" string
	 * as key) to an object that contains the number of dropped messages and the
	 * number of the last received message
	 */
	private final Map<String, DroppedMessageCounter> typeAndSourceToDroppedCountMapper = new HashMap<>();
    private final Tracer log;

	/**
	 * Constructor.
	 * 
	 * @param sub subscriber object for this subscriber
	 * @param topic the topic name for this subscriber
	 * @param logger trace logger to use
	 */
	public MonitorSubscriber(final IQueuingMessageHandler sub, final String topic, final Tracer logger) {
		subscriber = sub;
		this.topic = topic;
		sb = new StringBuilder(64);
		this.log = logger;
	}
	
	/**
	 * Retrieves the lag time, which is the time between last message publication time and 
	 * receive time.
	 * 
	 * @return the lagTime in milliseconds
	 */
	public long getLagTime() {
		return lagTime;
	}

	/**
	 * Sets the lag time, which is the time between last message publication time and 
	 * receive time.
	 * 
	 * @param lagTime
	 *            the lagTime to set, in milliseconds
	 */
	public void setLagTime(final long lagTime) {
		this.lagTime = lagTime;
	}

	/**
	 * Retrieves the topic for this subscriber.
	 * 
	 * @return the topic
	 */
	public String getTopic() {
		return topic;
	}

	/**
	 * Sets the topic for this subscriber.
	 * 
	 * @param topic
	 *            the topic to set
	 */
	public void setTopic(final String topic) {
		this.topic = topic;
	}

	/**
	 * Retrieves the message receive count for this subscriber.
	 * 
	 * @return the receiveCount
	 */
	public long getReceiveCount() {
		return receiveCount;
	}

	/**
	 * Sets the message receive count for this subscriber.
	 * 
	 * @param receiveCount
	 *            the receiveCount to set
	 */
	public void setReceiveCount(final long receiveCount) {
		this.receiveCount = receiveCount;
	}

	/**
	 * Retrieves the last message number received by this subscriber.
	 * 
	 * @return the lastMessageNumber
	 */
	public long getLastMessageNumber() {
		return lastMessageNumber;
	}

	/**
	 * Sets the last message number received by this subscriber.
	 * 
	 * @param lastMessageNumber
	 *            the lastMessageNumber to set
	 */
	public void setLastMessageNumber(final long lastMessageNumber) {
		this.lastMessageNumber = lastMessageNumber;
	}

	/**
	 * Retrieves the last message receive time.
	 * 
	 * @return the lastReceiveTime in milliseconds
	 */
	public long getLastReceiveTime() {
		return lastReceiveTime;
	}

	/**
	 * Sets the last message receive time.
	 * 
	 * @param lastReceiveTime
	 *            the lastReceiveTime to set, in milliseconds
	 */
	public void setLastReceiveTime(final long lastReceiveTime) {
		this.lastReceiveTime = lastReceiveTime;
	}

	/**
	 * Retrieves the subscriber object.
	 * 
	 * @return the subscriber
	 */
	public IQueuingMessageHandler getSubscriber() {
		return subscriber;
	}

	/**
	 * Sets the subscriber object.
	 * 
	 * @param subscriber
	 *            the subscriber to set
	 */
	public void setSubscriber(final IQueuingMessageHandler subscriber) {
		this.subscriber = subscriber;
	}

	/**
	 * Gets the map that stores the dropped messages per message type.
	 * 
	 * @return Message type to dropped message count mapping.
	 */
	public Map<String, DroppedMessageCounter> getDroppedMessages() {
		return typeAndSourceToDroppedCountMapper;
	}

	/**
	 * Updates the statistics in this object using the given message receive 
	 * and publish times. Commits the transaction if the subscriber has 
	 * reached commit count.
	 * 
	 * @param receiveTime the time the message was received, in milliseconds
	 * @param publishTime the time the message was published, in milliseconds
	 * @param messageSrcPid the source PID (or unique ID) of the message source
	 * @param messageNumber the incrementing number that is tagged in the 
	 * 						header of each incoming message
	 * @param type the message type
	 * 
	 * connection
	 */
	public void update(final long receiveTime, final long publishTime, final int messageSrcPid, final int messageNumber, final IMessageType type) {
		receiveCount++;

		lastReceiveTime = receiveTime;

		++lastMessageNumber;

		lagTime = lastReceiveTime - publishTime;
		
		sb.setLength(0);
		sb.append(type.getSubscriptionTag());
		sb.append("/");
		sb.append(messageSrcPid);
		final String key = sb.toString();

		if(!typeAndSourceToDroppedCountMapper.containsKey(key)) {
			typeAndSourceToDroppedCountMapper.put(key, new DroppedMessageCounter(messageNumber));
		}
		else {
			typeAndSourceToDroppedCountMapper.get(key).calculateDroppedMessages(messageNumber);
		}
	}
	
	/**
	 * Set all the dropped counts to zero. 
	 */
	public void reset() {
		final Iterator<DroppedMessageCounter> it = typeAndSourceToDroppedCountMapper.values().iterator();
		
		while(it.hasNext()) {
			it.next().resetCount();
		}

	}
	
	/**
	 * Dumps debug information about message loss for this subscriber.
	 */
	public void dumpMessageLoss() {
	    for (final Entry<String, DroppedMessageCounter> entry: typeAndSourceToDroppedCountMapper.entrySet()) {
	        final String type = entry.getKey().substring(0, entry.getKey().indexOf("/"));
	        log.debug("MESSAGE LOSS COUNTER: For topic " + topic + ": " + entry.getValue().getCount() + " messages of type " + type + " were dropped");
	    }
	}
}
