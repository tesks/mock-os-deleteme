package jpl.gds.message.api.external;


/**
 * An interface to be implemented by message topic subscribers that interact with
 * the external message service.
 */
public interface ITopicSubscriber {

    /**
     * Closes the subscriber connection.
     */
    public void close();

    /**
     * Basically nulls out the subscriber objects. Use this to reset things to
     * closed only if you know the message service connection is gone, because
     * in that case the close is going to hang or just take really long and
     * print a bunch of messages we do not want to see.
     */
    public void closeNoDisconnect();

    /**
     * If the subscriber is transacted, than commit the transaction.
     * 
     * @throws MessageServiceException
     *             Thrown if unable to commit the transaction.
     */
    public void commit() throws MessageServiceException;

    /**
     * Indicates whether the subscriber is transacted.
     * 
     * @return if true if the connection is transacted
     */
    public boolean isTransacted();

    /**
     * Receives a message from the subscriber. Blocks until a message is received.
     * 
     * @return a Message from the subscriber
     * @throws MessageServiceException
     *             Thrown if there is a message service error
     */
    public IExternalMessage receive() throws MessageServiceException;

    /**
     * Sets an asynchronous message listener for the subscriber. Messages
     * will be delivered to the listener as they arrive. There can be only
     * one listener per subscriber.
     * 
     * @param listener
     *            The message listener.
     * @throws MessageServiceException
     *             Thrown if there is a message service error.
     */
    public void setMessageListener(IMessageServiceListener listener)
            throws MessageServiceException;

    /**
     * Starts the receipt of messages.
     * @throws MessageServiceException Thrown if there is a message service error
     */
    public void start() throws MessageServiceException;

    /**
     * Cancels a durable named subscriber, such that the message service
     * will no longer keep messages for it when the subscriber is not
     * connected.
     * 
     * @throws MessageServiceException
     *             Thrown if there is a message service error.
     */
    public void unsubscribe() throws MessageServiceException;
    
    /**
     * Gets the message topic for this subscriber.
     * 
     * @return topic name
     */
    public String getTopic();

}