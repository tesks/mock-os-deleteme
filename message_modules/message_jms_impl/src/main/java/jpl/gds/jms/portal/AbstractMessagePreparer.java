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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.jms.config.JmsProperties;
import jpl.gds.message.api.external.IAsyncTopicPublisher;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessageType;

/**
 * This is the base class for all implementations of the MessagePreparer
 * interface.  It assumes a non-batching preparer. Batching preparers
 * must override the batching and queue-related methods.
 */
public abstract class AbstractMessagePreparer implements IMessagePreparer {

    /**
     * JMS debug logger. Using non-publishing fast tracer.
     */
    protected final Tracer jmsDebugLogger;

    /**
     * Number of messages received.
     */
    protected long receiveCount;
    /**
     * Number of messages pushed to publishers. Does not
     * vary no matter how many publishers there are. 
     */
    protected long publishTotal;
    /**
     * Number of published messages by type.
     */
    private final Map<IMessageType, AtomicLong> publishTypeTotal = new HashMap<IMessageType, AtomicLong>();   
 
    /**
     * List of topic publishers to which this preparer will send messages.
     */
    protected final List<IAsyncTopicPublisher> publishers;
   
    /**
     * JMS configuration reference.
     */
    protected final JmsProperties portalConfig;
 
    /**
     * The context configuration containing metadata context to publish in message headers
     */
    protected final IContextConfiguration headerContext;
    
    /**
     * Indicates if the message preparer is shutdown.
     */
    protected final AtomicBoolean isShutdown = new AtomicBoolean(false);
  
    /**
     * Type of message handled by this preparer.
     */
    protected final IMessageType messageType;
    
    /**
     * The current application context.
     */
    protected final ApplicationContext appContext;
    
    /**
     * The current mission properties.
     */
    protected final MissionProperties missionProps;

    /**
     * Creates a message preparer for the given internal message type (as
     * determine by message.getType().
     * 
     * @param appContext the current application context
     * @param type
     *            the internal message type handled by this preparer
     * @param pubs
     *            the JMS topic publishers to push messages to
     */
    public AbstractMessagePreparer(final ApplicationContext appContext, 
    		final IMessageType type, 
            final List<IAsyncTopicPublisher> pubs) {
    	
    	this.appContext = appContext;
    	this.missionProps = appContext.getBean(MissionProperties.class);
    	this.portalConfig = appContext.getBean(JmsProperties.class);
    	
        publishers = pubs;
        headerContext = appContext.getBean(IContextConfiguration.class);
        messageType = type;        
        jmsDebugLogger = TraceManager.getTracer(appContext, Loggers.JMS);
        jmsDebugLogger.setPrefix("Message Preparer");
        init();
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.jms.portal.IMessagePreparer#getPublishTotal()
     */
    @Override
	public long getPublishTotal() {
        return publishTotal;
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.jms.portal.IMessagePreparer#getPublishTotalByType()
     */
    @Override
	public synchronized Map<IMessageType, AtomicLong> getPublishTotalByType(){
        return Collections.unmodifiableMap(publishTypeTotal);
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.jms.portal.IMessagePreparer#getQueuedMessageCount()
     */
    @Override
	public int getQueuedMessageCount() {
    	// By default, preparers do not batch, and so have no queued message count
        return 0;
    }

	/**
	 * Sends messages to the JMS publishers. The supplied list of messages
	 * should all be of the same type. They will be batched into a single JMS
	 * message with session header.
	 * 
	 * @param messages
	 *            an array of the internal messages to publish
	 * @param type
	 *            the internal message type to be used when determining the
	 *            external message type
	 * @return true if publication was successful
	 */
    protected abstract boolean pushToPublishers(
            List<TranslatedMessage> messages, IMessageType type);
       
    /**
     * {@inheritDoc}
     * @see jpl.gds.jms.portal.IMessagePreparer#messageReceived(jpl.gds.jms.portal.TranslatedMessage)
     */
    @Override
  	public synchronized void messageReceived(final TranslatedMessage m) {

          if (isShutdown.get()) {
              throw new IllegalStateException(
                      "Message preparer received message after shutdown");
          }
          
          /*  This could be faster if there was also a pushToPublishers
           * for a single message, but the publish methods need some
           * rework to do so. 
           */
          final List<TranslatedMessage> toSend = Arrays.asList(m); 
          pushToPublishers(toSend, m.getMessage().getType());
      
          receiveCount++;
      }
      

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.jms.portal.IMessagePreparer#shutdown()
     */
    @Override
	public void shutdown() {
        isShutdown.set(true);
    }

    /**
     * Initializes the batcher.
     */
    protected void init() {
        this.portalConfig.init(); // complete initialization (if required) before use.
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.jms.portal.IMessagePreparer#getMessageType()
     */
    @Override
	public IMessageType getMessageType() {
        return messageType;
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.jms.portal.IMessagePreparer#flushQueue()
     */
    @Override
	public void flushQueue() {
    	// By default, preparers do not queue, so flush needs to do nothing
    }
    
    /**
     * Adds count value to the message counter for the specified message
     * type. Does nothing unless the "JmsDebug" logger is enabled.
     * @param type message type
     * @param count delta value to add
     */
    protected void addToMessageCount(final IMessageType type, final int count) {
    	if (jmsDebugLogger.isDebugEnabled()) {
    		final AtomicLong messageCount = publishTypeTotal.get(type);
    		if (messageCount != null){
    			messageCount.addAndGet(count);
    		} else {
    			synchronized (publishTypeTotal) {
    				publishTypeTotal.put(type, new AtomicLong(count));
    			}
    		}
    	}
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.jms.portal.IMessagePreparer#enableBatching(boolean)
     */
    @Override
	public void enableBatching(final boolean enable) {
    	//By default, preparers do not batch, so do nothing
    }
}
