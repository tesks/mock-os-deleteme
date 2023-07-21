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

import java.util.Map;

/**
 * An interface to be implemented by message topic publishers that interact with
 * the external message service.
 */
public interface ITopicPublisher {

	/**
	 * Closes the publisher connection.
	 */
	public abstract void close();

	/**
	 * Opens the publisher connection. Called automatically by the
	 * constructor, but can also be called by itself.
	 * 
	 * @throws MessageServiceException if any error occurs
	 */
	public abstract void open() throws MessageServiceException;

	/**
	 * Commit the transacted publications.
     * @throws MessageServiceException if any error occurs
	 */
	public abstract void commit() throws MessageServiceException;

	/**
	 * Gets the topic name of the publisher.
	 * @return the topic name
	 */
	public abstract String getName();

	/**
     * Creates an external binary message without publishing it.
     * @param blob Binary information to encapsulate.
     * @param properties Header properties for the the message.
     * @throws MessageServiceException if any error occurs
     * @return external message instance
     */
    public IExternalMessage createBinaryMessage(byte[] blob, Map<String, Object> properties) throws MessageServiceException;

    /**
     * Publishes a binary message.
     * @param blob Binary information to encapsulate.
     * @throws MessageServiceException if any error occurs
     */
    public abstract void publishBinaryMessage(byte[] blob)
            throws MessageServiceException;

    /**
     * Publishes a binary message.
     * @param blob Binary information to encapsulate.
     * @param properties Header properties for the the message.
     * @throws MessageServiceException if any error occurs
     */
    public abstract void publishBinaryMessage(byte[] blob, Map<String, Object> properties)
            throws MessageServiceException;
    
	/**
	 * Publishes a binary message.
	 * @param blob Binary information to encapsulate.
	 * @param properties Properties for the message header.
	 * @param timeToLive Time to live for the message.
	 * @param deliveryMode Delivery mode for the message.
     * @throws MessageServiceException if any error occurs
	 */
	public abstract void publishBinaryMessage(byte[] blob,
			Map<String, Object> properties, long timeToLive, ExternalDeliveryMode deliveryMode)
			throws MessageServiceException;
	
	/**
     * Creates an external text message without publishing it.
     * @param text Textual information to encapsulate.
     * @param properties Header properties for the the message.
     * @throws MessageServiceException if any error occurs
     * @return external message instance
     */
	public IExternalMessage createTextMessage(String text, Map<String, Object> properties) throws MessageServiceException;
	
	/**
	 * Published any pre-created external message.
	 * 
	 * @param m message to publish
	 * @throws MessageServiceException if any error occurs
	 */
	public abstract void publishMessage(IExternalMessage m) throws MessageServiceException;
	
	/**
	 * Publishes a text message.
	 * @param text String to publish.
     * @throws MessageServiceException if any error occurs
	 */
	public abstract void publishTextMessage(String text) throws MessageServiceException;

	/**
	 * Published a text message.
	 * @param text Text to publish
	 * @param properties Message properties to assign to the message header.
     * @throws MessageServiceException if any error occurs
	 */
	public abstract void publishTextMessage(String text,
			Map<String, Object> properties) throws MessageServiceException;

	/**
	 * Published a text message.
	 * @param text Text to publish.
	 * @param properties Message properties to assign to the message header.
	 * @param timeToLive Time to live for the message.
	 * @param deliveryMode Mode of delivery for the message.
     * @throws MessageServiceException if any error occurs
	 */
	public abstract void publishTextMessage(String text,
			Map<String, Object> properties, long timeToLive, ExternalDeliveryMode deliveryMode)
			throws MessageServiceException;

	/**
     * Gets the publisher's message topic.
     * 
     * @return the topic name
     */
    public String getTopic();

    /**
	 * Indicates if publication is transactional.
	 * 
	 * @return true if transactional, false if not.
	 */
	public abstract boolean isTransacted();

	/**
	 * Indicates if published messages will be persistent by default.
	 * 
	 * @return true if persistent, false if not.
	 */
	public abstract boolean isPersistent();

}