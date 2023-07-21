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
 * An interface to be implemented by factories that create message client
 * objects, namely publishers and subscribers.
 */
public interface IMessageClientFactory {

    /**
     * Creates a transient message topic subscriber. This is a subscriber
     * that messages are not kept for after the subscriber closes the connection.
     * 
     * @param topic the messaging topic to subscribe to
     * @param messageFilter
     *            boolean, JMS-style expression for filtering the message stream
     * @param shared true if this subscriber should share a connection with other
     *               shared subscribers, false if it should make have its own connection
     * @return the subscriber instance
     * @throws MessageServiceException
     *             Thrown if unable to create a connection to the message server, or
     *             if some other error creating the message client occurs.
     */
    public ITopicSubscriber getTopicSubscriber(String topic,
            String messageFilter, boolean shared) throws MessageServiceException;

    /**
     * Creates a durable message topic subscriber. This is a subscriber
     * that messages are kept for after the subscriber closes the connection.
     * 
     * @param topic the messaging topic to subscribe to
     * @param name
     *            Subscriber name.
     * @param messageFilter
     *            message filtering criteria, as a boolean statement
     * @param shared true if this subscriber should share a connection with other
     *               shared subscribers, false if it should make have its own connection
     * @return the subscriber instance
     * @throws MessageServiceException
     *             Thrown if unable to create a connection to the message server, or
     *             if some other error creating the message client occurs.
     */
    public ITopicSubscriber getDurableTopicSubscriber(String topic,
            String name, String messageFilter, boolean shared) throws MessageServiceException;

    /**
     * Creates an asynchronous topic publisher. This is a publisher that will
     * queue up messages submitted to it and publish them using another thread,
     * using a message transaction size configured in the
     * message-service-specific configuration. If the size of the message queue
     * exceeds configured limits, messages will be buffered on disk.
     * 
     * @param topic
     *            the messaging topic to publish to
     * @param persistent
     *            true if the messages on the topic should default to persistent
     *            status
     * @param spillDir
     *            output directory to overflow messages to
     * @return the publisher instance
     * @throws MessageServiceException
     *             Thrown if unable to create a connection to the message
     *             server, or if some other error creating the message client
     *             occurs.
     */
    public IAsyncTopicPublisher getTransactedAsyncTopicPublisher(String topic,
            boolean persistent, String spillDir)
            throws MessageServiceException;

    /**
     * Creates an asynchronous topic publisher. This is a publisher that will
     * queue up messages submitted to it and publish them using another thread,
     * using a transaction size of 1.  If the size of the message queue
     * exceeds configured limits, messages will be buffered on disk.
     * 
     * @param topic
     *            the messaging topic to publish to
     * @param persistent
     *            true if the messages on the topic should default to persistent
     *            status
     * @param spillDir
     *            output directory to overflow messages to
     * @return the publisher instance
     * @throws MessageServiceException
     *             Thrown if unable to create a connection to the message
     *             server, or if some other error creating the message client
     *             occurs.
     */
    public IAsyncTopicPublisher getAsyncTopicPublisher(String topic,
            boolean persistent, String spillDir) throws MessageServiceException;

    /**
     * Creates a persistent or non-persistent synchronous topic publisher. This
     * is a publisher that will publish one message at a time and block until it
     * has been accepted by the message service.
     * 
     * @param topic
     *            the messaging topic to publish to
     * @param persistent
     *            true if the messages on the topic should default to persistent
     *            status
     * @return the publisher instance
     * @throws MessageServiceException
     *             Thrown if unable to create a connection to the message
     *             server, or if some other error creating the message client
     *             occurs.
     * 
     * This method only creates synchronous publishers.
     */
    public ITopicPublisher getTopicPublisher(String topic, boolean persistent)
            throws MessageServiceException;

    /**
     * Creates a synchronous non-persistent topic publisher. This is a publisher
     * that will publish one message at a time and block until it has been
     * accepted by the message service. Messages on the topic will be
     * non-persistent by default.
     * 
     * @param topic
     *            the messaging topic to publish to
     * @return the publisher instance
     * @throws MessageServiceException
     *             Thrown if unable to create a connection to the message server, or
     *             if some other error creating the message client occurs.
     */
    public ITopicPublisher getNonPersistentTopicPublisher(String topic)
            throws MessageServiceException;

}