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

import java.util.LinkedList;
import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.jms.config.PublishableMessageConfig;
import jpl.gds.message.api.external.IAsyncTopicPublisher;
import jpl.gds.shared.message.IMessageType;

/**
 * A message preparer that batches multiple instances of the same message type
 * into a single JMS Text message. Handles only a single message type.
 */
public class BatchingTextPreparer extends SimpleTextPreparer {
	
	private static final long DEFAULT_BATCH_TIMEOUT = 10000;

    private long lastTime;
    private int batchSize;
    private long timeout;
    private boolean batching = true;
    private final List<TranslatedMessage> toSend = new LinkedList<TranslatedMessage>();

    /**
     * Constructs the batch text preparer.
     *       
     * @param appContext the current application context
     * @param type
     *            the internal message type to support
     * @param pubs
     *            the list of publishers to send messages to
     */
    public BatchingTextPreparer(final ApplicationContext appContext,
    		final IMessageType type, 
            final List<IAsyncTopicPublisher> pubs) {
        super(appContext, type, pubs);
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.jms.portal.AbstractMessagePreparer#getQueuedMessageCount()
     */
    @Override
  	public synchronized int getQueuedMessageCount() {
          return this.toSend.size();
      }
    
    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.jms.portal.IMessagePreparer#messageReceived(jpl.gds.jms.portal.TranslatedMessage)
     */
    @Override
	public synchronized void messageReceived(final TranslatedMessage m) {

        if (isShutdown.get()) {
            throw new IllegalStateException(
                    "Message preparer received message after shutdown");
        }
        
        /* Add the message to the send queue */
        this.toSend.add(m);
        

        /* It is time to push this batch to publishers if the batch timeout has elapsed,
         * or the batch size has been reached, or batching is not enabled.
         */
        final long elapsed = System.currentTimeMillis() - this.lastTime;        
        final boolean shouldSend = !this.batching || (elapsed > this.timeout) || (this.toSend.size() >= this.batchSize);

        /* Push messages on the current queue to the publishers and clear the send queue */
        if (shouldSend) {
        	jmsDebugLogger.trace("messageReceived for " , this.messageType ,
        			" found " , toSend.size() , " messages to send ");
        	pushToPublishers(this.toSend, this.messageType);
        	this.toSend.clear();
        	this.lastTime = System.currentTimeMillis();
        }

        receiveCount++;
    }
  
    /**
     * {@inheritDoc}
     * 
     * Has the side effect of turning off batching.
     * 
     * @see jpl.gds.jms.portal.AbstractMessagePreparer#flushQueue()
     */
    @Override
  	public synchronized void flushQueue() {
    	if (toSend.isEmpty()) {
    		return;
    	}
    	jmsDebugLogger.trace("flush for " , this.messageType ,
    	        " found " , toSend.size() , " messages to send ");

    	/* Save and restore batching flag. */
    	final boolean saveBatching = this.batching;
    	this.batching = false;  
    	pushToPublishers(this.toSend, this.messageType);
    	this.toSend.clear();
    	this.lastTime = System.currentTimeMillis();
    	this.batching = saveBatching;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.jms.portal.SimpleTextPreparer#enableBatching(boolean)
     */
    @Override
    public void enableBatching(final boolean batch) {
        this.batching = batch;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.jms.portal.AbstractMessagePreparer#init()
     */
    @Override
    protected void init() {
        super.init();

        final PublishableMessageConfig msgConfig =
                portalConfig.getMessageConfig(this.messageType);
        if (msgConfig == null) {
            jmsDebugLogger.warn("No configuration block found for message type " ,
                            messageType ,". Defaults will be used.");
            this.batchSize = 1;
            return;
        }
        final int s = msgConfig.getBatchSize();
        if (s != 0) {
            this.batchSize = s;
        }

        this.timeout = msgConfig.getBatchTimeout();
        if (this.timeout == 0 && this.batchSize != 0) {
            this.timeout = DEFAULT_BATCH_TIMEOUT;
        }
        this.lastTime = System.currentTimeMillis();

    }
    
    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.jms.portal.IMessagePreparer#shutdown()
     */
    @Override
	public void shutdown() {
    	flushQueue();
        isShutdown.set(true);
    }
}
