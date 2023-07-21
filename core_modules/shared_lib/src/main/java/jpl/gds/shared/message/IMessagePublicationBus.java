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

import jpl.gds.shared.metadata.context.IContextKey;

/**
 * Interface for message subscription handling.
 *
 */
public interface IMessagePublicationBus {

    /**
     * This sends the message to all subscribers listening for its type of
     * message. Subscribers are objects specified by calls to the
     * <code>subscribe()</code> method of a MessageContext objects.
     * <p>
     * This method will overwrite the provided message's context key.
     *
     * @param message IMessage object to be published
     */
    public abstract void publish(final IMessage message);

    /**
     * This sends the message to all subscribers listening for its type of
     * message. Subscribers are objects specified by calls to the
     * <code>subscribe()</code> method of a MessageContext objects.
     *
     * @param message        IMessage object to be published
     * @param keepContextKey if true, message's context key will not be overwritten; if false, behaves same as
     *                       {@code jpl.gds.shared.message.IMessagePublicationBus#publish(jpl.gds.shared.message.IMessage)}
     */
    void publish(final IMessage message, final boolean keepContextKey);

    /**
     * Adds a subscriber for all message types.
     *
     * @param subscriber object implementing MessageSubscriber interface
     */
    public abstract void subscribe(final MessageSubscriber subscriber);

//	/**
//	 * This adds a subscriber to the list of subscribers listening for the
//	 * specified message type. After subscribing, every published message of the
//	 * specified type will cause the <code>handleMessage()</code> method of the
//	 * subscriber to be called.
//	 * 
//	 * @param type
//	 *            type of message
//	 * @param subscriber
//	 *            object implementing MessageSubscriber interface
//	 */
//	public abstract void subscribe(final String type,
//			final MessageSubscriber subscriber);

    /**
     * This adds a subscriber to the list of subscribers listening for the
     * messages with the specified configuration. After subscribing, every
     * published message of the specified type will cause the
     * <code>handleMessage()</code> method of the subscriber to be called.
     *
     * @param type       IMessageConfiguration of messages to subscribe to
     * @param subscriber object implementing MessageSubscriber interface
     */
    public abstract void subscribe(final IMessageType type,
                                   final MessageSubscriber subscriber);
//
//	/**
//	 * Unsubscribe a message subscriber.
//	 * 
//	 * @param type
//	 *            type of the messages to unsubscribe from
//	 * @param subscriber
//	 *            the subscriber
//	 */
//	public abstract void unsubscribe(final String type,
//			final MessageSubscriber subscriber);
//	

    /**
     * Unsubscribe a message subscriber.
     *
     * @param type       IMessageConfiguration of messages to unsubscribe from
     * @param subscriber the subscriber
     */
    public void unsubscribe(IMessageType type, final MessageSubscriber subscriber);

    /**
     * Unsubscribes all message subscribers.
     */
    public abstract void unsubscribeAll();

    /**
     * Unsubscribes the subscriber from all message types.
     *
     * @param subscriber the subscriber
     */
    public abstract void unsubscribeAll(final MessageSubscriber subscriber);


    /**
     * Sets the default context key to be applied to messages
     * published on this bus instance.
     *
     * @param toSet context key to apply
     */
    public void setContextKey(IContextKey toSet);

}
