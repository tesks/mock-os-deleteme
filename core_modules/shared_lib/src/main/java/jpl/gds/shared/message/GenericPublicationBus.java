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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.metadata.context.ContextKey;
import jpl.gds.shared.metadata.context.IContextKey;

/**
 * The GenericMessageContext class is the central access point for accessing the
 * internal message passing facility. It provides a mechanism for an object to
 * publish messages without caring what objects will be receiving them. It also
 * provides a mechanism for subscribers to receive messages without caring who
 * sent them. This enables the creation of components to read, process, and
 * output data that can be combined in different ways to construct different
 * applications. It also makes it a lot easier to unit test those components.
 * <p>
 * <br>
 * <b>Caveats</b>
 * <p>
 * <br>
 * 
 * This is a singleton object. There is one shared instance, which must be
 * obtained by calling getInstance().
 * <p>
 * <br>
 * This is not for passing messages between processes, just between objects
 * running in the same JVM. And it is really just a glorified listener
 * interface, not an asynchronous message passing capability. Publish calls are
 * synchronous and will block until all subscribers have returned from the
 * handleMessage() call.
 * <p>
 * <br>
 * This message passing facility is not synchronized for performance reasons. It
 * is possible for multiple threads to publish messages simultaneously.
 * <p>
 * <br>
 * To publish:
 * <p>
 * 
 * <pre>
 * IMessageContext context = GenericMessageContext.getInstance();
 * Message message = (some message instance);
 * context.publish(message);
 * </pre>
 * <p>
 * To subscribe, first the subscribing class must implement the
 * MessageSubscriber interface. Then there are two ways to get the message type
 * to subscribe to:
 * <p>
 * 
 * <pre>
 * IMessageContext context = GenericMessageContext.getInstance();
 * Message m = (some message instance);
 * String type = m.getType();
 * context.subscribe(type, this);
 * </pre>
 * <p>
 * or more likely:
 * <p>
 * 
 * <pre>
 * IMessageContext context = GenericMessageContext.getInstance()
 * String type = MessageClassName.TYPE;
 * context.subscribe(type, this);
 * </pre>
 * <p>
 * Unsubscribing is fairly self-explanatory.
 * <p>
 * <br>
 * The implementation of the MessageSubscriber interface looks like this:
 * <p>
 * 
 * <pre>
 * public class ExampleClass implements MessageSubscriber {
 *      ...
 *      public void handleMessage(Message message) {
 *          (cast message to expected message type here)
 *          (do what needs doing with the message)
 *      }
 * </pre>
 * 
 * To see verbose output, enable debug for the Default Tracer and call
 * setVerbose(true).
 * <p>
 * 
 * @see MessageSubscriber
 * @see IMessage
 * 
 */
public final class GenericPublicationBus implements IMessagePublicationBus {

    private static final String                                        ANY_TYPE      = "[ANY]";

    /**
     * Tracer for verbose logging
     */
    private final Tracer                                                     messageLog;

    private IContextKey                                                contextKey;

    /**
     * Map of subscriber lists.
     */
    private final Map<String, CopyOnWriteArrayList<MessageSubscriber>> subscriberMap = new ConcurrentHashMap<String, CopyOnWriteArrayList<MessageSubscriber>>();


    /**
     * Test constructor
     */
    public GenericPublicationBus() {
        this(new ContextKey(), TraceManager.getDefaultTracer());
    }

    /**
     * Default constructor. Does nothing. Private to enforce singleton pattern.
     * 
     * @param key
     *            IContextKey to use
     * @param log
     *            Tracer to log with
     * 
     **/
    public GenericPublicationBus(final IContextKey key, final Tracer log) {
        this.messageLog = log;
        this.setContextKey(key);
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.message.IMessagePublicationBus#publish(IMessage)
     */
    @Override
    public void publish(final IMessage message) {
        publish(message, false);
    }

    /**
     * This sends the message to all subscribers listening for its type of
     * message. Subscribers are objects specified by calls to the
     * <code>subscribe()</code> method of a MessageContext objects.
     *
     * @param message        IMessage object to be published
     * @param keepContextKey if true, message's context key will not be overwritten; if false, behaves same as
     *                       {@code jpl.gds.shared.message.IMessagePublicationBus#publish(jpl.gds.shared.message.IMessage)}
     */
    @Override
    public void publish(IMessage message, boolean keepContextKey) {

        final String type = message.getType().getSubscriptionTag();

        if (!keepContextKey && this.contextKey != null) {
            message.setContextKey(this.contextKey);
        }

        if (messageLog.isDebugEnabled()) {
            messageLog.debug("Publishing ", type, " = ", message.toString());
        }
        final List<MessageSubscriber> subscribers = subscriberMap.get(type);
        final List<MessageSubscriber> anySubscribers = subscriberMap.get(ANY_TYPE);

        if ((subscribers == null || subscribers.isEmpty()) && (anySubscribers == null || anySubscribers.isEmpty())) {
            messageLog.debug("no subscribers for type ", type);

            return;
        }

        if (subscribers != null) {
            for (final MessageSubscriber sub : subscribers) {
                if (messageLog.isDebugEnabled()) {
                    messageLog.debug("Sending ", type, " to ", sub.getClass().getName());
                }

                sub.handleMessage(message);
            }
        }

        if (anySubscribers != null) {
            for (final MessageSubscriber sub : anySubscribers) {
                if (messageLog.isDebugEnabled()) {
                    messageLog.debug("Sending ", type, " to ", sub.getClass().getName());
                }

                sub.handleMessage(message);
            }
        }

    }

    private void subscribe(final String type, final MessageSubscriber subscriber) {

        CopyOnWriteArrayList<MessageSubscriber> tempList = subscriberMap.get(type);
        if (tempList == null) {
            tempList = new CopyOnWriteArrayList<MessageSubscriber>();
            subscriberMap.put(type, tempList);
        }
        tempList.add(subscriber);

        messageLog.debug("Message Subscription: ", subscriber.getClass().getName(), " subscribed to ", type);

    }

    @Override
    public void subscribe(final IMessageType type, final MessageSubscriber subscriber) {
        subscribe(type.getSubscriptionTag(), subscriber);

    }

    @Override
    public void subscribe(final MessageSubscriber subscriber) {
        subscribe(ANY_TYPE, subscriber);
    }

    private void unsubscribe(final String type, final MessageSubscriber subscriber) {

        final List<MessageSubscriber> list = subscriberMap.get(type);
        if (list != null) {
            list.remove(subscriber);
        }

        messageLog.debug("Message Subscriber: ", subscriber.getClass().getName(), " unsubscribed from ", type);

    }

    @Override
    public void unsubscribe(final IMessageType config, final MessageSubscriber subscriber) {
        unsubscribe(config.getSubscriptionTag(), subscriber);
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.message.IMessagePublicationBus#unsubscribeAll()
     */
    @Override
    public void unsubscribeAll() {
        subscriberMap.clear();
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.message.IMessagePublicationBus#unsubscribeAll(jpl.gds.shared.message.MessageSubscriber)
     */
    @Override
    public void unsubscribeAll(final MessageSubscriber subscriber) {

        /*
         * Iteration here is safe because it's a ConcurrentHashMap.
         */
        for (final String key : subscriberMap.keySet()) {

            final List<MessageSubscriber> list = subscriberMap.get(key);
            if (list.remove(subscriber)) {
                messageLog.debug("Message Subscription: ", subscriber.getClass().getName(), " unsubscribed from ", key);

            }
        }
    }

    @Override
    public void setContextKey(final IContextKey toSet) {
        this.contextKey = toSet;
    }

}
