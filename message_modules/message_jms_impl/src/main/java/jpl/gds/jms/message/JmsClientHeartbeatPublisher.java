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
package jpl.gds.jms.message;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.context.api.ContextTopicNameFactory;
import jpl.gds.jms.config.JmsProperties;
import jpl.gds.message.api.BaseMessageHeader;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.message.api.external.CheckMessageService;
import jpl.gds.message.api.external.IClientHeartbeatListener;
import jpl.gds.message.api.external.IClientHeartbeatPublisher;
import jpl.gds.message.api.external.IMessageClientFactory;
import jpl.gds.message.api.external.ITopicPublisher;
import jpl.gds.message.api.external.MessageServiceException;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.CommonMessageType;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.time.IAccurateDateTime;

/**
 * This class will periodically publish a client heartbeat message to the
 * JMS.  It allows IClientHeartbeatListeners to register for notification 
 * when the heartbeat publisher connection is lost or regained. This, in turn,
 * can be used by an application that subscribed to JMS to know that the JMS
 * connection is dead. Otherwise, JMS subscribers will never know. The heartbeat
 * interval is configurable.
 */
public class JmsClientHeartbeatPublisher implements Runnable, IClientHeartbeatPublisher {

	private final Tracer tracer; 


	private ITopicPublisher publisher;
	private final List<IClientHeartbeatListener> listeners = new LinkedList<IClientHeartbeatListener>();
	private final String clientId;
	private Thread thisThread;
	private final AtomicBoolean done = new AtomicBoolean(false);
	private final AtomicBoolean jmsIsActive = new AtomicBoolean(false);
	private final String topicName;
	private final long heartbeatInterval;
	private final ApplicationContext appContext;

	/**
	 * Create a ClientHeartbeatPublisher for a client with the given
	 * client source identifier.
	 * 
	 * @param appContext the current application context
	 * @param clientSource a unique string that identifies the client
	 * publishing heartbeat
	 */
	public JmsClientHeartbeatPublisher(final ApplicationContext appContext, final String clientSource) {
		if (clientSource == null) {
			throw new IllegalArgumentException("clientSource argument may not be null");
		}
		this.appContext = appContext;
        this.tracer = TraceManager.getTracer(appContext, Loggers.JMS);
		this.clientId = clientSource;
		heartbeatInterval = appContext.getBean(JmsProperties.class).getClientHeartbeatInterval();
		this.topicName = ContextTopicNameFactory.getClientTopic() + "." + clientId;
	}
	
	/**
	 * @{inheritDoc}
	 * @see jpl.gds.message.api.external.IClientHeartbeatPublisher#getClientId()
	 */
	@Override
    public String getClientId() {
	    return this.clientId;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.message.api.external.IClientHeartbeatPublisher#addListener(jpl.gds.message.api.external.IClientHeartbeatListener)
     */
	@Override
    public void addListener(final IClientHeartbeatListener listener) {

		synchronized (this.listeners) {
			if (!this.listeners.contains(listener)) {
				this.listeners.add(listener);
			}
		}
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.message.api.external.IClientHeartbeatPublisher#removeListener(jpl.gds.message.api.external.IClientHeartbeatListener)
     */
	@Override
    public void removeListener(final IClientHeartbeatListener listener) {

		synchronized (this.listeners) {
			this.listeners.remove(listener);
		}
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.message.api.external.IClientHeartbeatPublisher#startPublishing()
     */
	@Override
    public synchronized boolean startPublishing() {

		if (this.publisher != null) {
			System.out.println("Publisher is not null");
			return false;
		}

		if (!createPublisher()) {
			System.out.println("Publisher is not ok");
			return false;
		}

		this.done.set(false);
		this.thisThread = new Thread(this, "Message Client Heartbeat Publisher");
		this.thisThread.start();
		return true;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.message.api.external.IClientHeartbeatPublisher#stopPublishing()
     */
	@Override
    public synchronized void stopPublishing() {

		this.done.set(true);
		this.thisThread.interrupt();
		this.publisher.close();
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {

		// We publish until stopped by a stopPublishing() call.
		while (!this.done.get()) {

			try {
				// If the JMS connection is not active, we try to re-create it.
				if (!this.jmsIsActive.get()) {
					final boolean ok = createPublisher();
					if (ok) {
						tracer.info("Message service publisher connection on topic "
								, this.topicName , " was recreated");
						notifyListenersOfSuccess();
					}
				}
				// If the JMS connection is not active, publish a heartbeat
				if (this.jmsIsActive.get()) {
					publishHeartbeat();
				}

				// Either way, sleep for the heartbeat interval before trying
				// again.
				Thread.sleep(heartbeatInterval);

			} catch (final InterruptedException e) {
				// do nothing. Done check will suffice.

			} catch (final MessageServiceException e) {
			    
			    if (e.isDisconnect()) {
			        // This is where we end up if the JMS connection died, but oddly enough,
			        // only usually when JMS comes back. Close the publisher and set
			        // the JMS active state to false.
			        closePublisherAndNotify();
			        continue;
			    }

				e.printStackTrace();

				// could be due to loss of ActiveMQ. Check the connection, and if JMS is not
				// there, set the JMS active state to false and close the publisher.
				if (!this.jmsIsActive.get() && !CheckMessageService
						.checkMessageServiceRunning(appContext.getBean(MessageServiceConfiguration.class), 
								null, 0, tracer, false, false)) {
					closePublisherAndNotify();
					tracer.warn("Message server appears to be down  (topic "
							, this.topicName
							, "); no client heartbeats will be published until it is back up");

				}
			}
		}
	}

	/**
     * Creates the actual JMS publisher.
     * 
     * @return true if publisher was successfully created, false if not
     */
    private boolean createPublisher() {
    	try {
    		this.publisher =  appContext.getBean(IMessageClientFactory.class).getNonPersistentTopicPublisher(this.topicName);
    		this.jmsIsActive.set(true);
    		return true;
    	} catch (final MessageServiceException e) {
    		if (tracer.isDebugEnabled()) {
    			e.printStackTrace();
    			tracer.debug("ClientHeartbeatPublisher could not create publisher due to MessageServiceException", ExceptionTools.getMessage(e), e);
    		}
    		return false;
    	} 
    }

    /**
	 * Closes the current publisher and notifies IClientHeartbeatListeners
	 * of the lost connection.
	 */
	private void closePublisherAndNotify() {

		tracer.error("Message service publisher connection on topic " , this.topicName
				, " was lost");
		this.publisher.close();
		this.jmsIsActive.set(false);
		notifyListenersOfFailure();
	}

	/**
	 * Notifies IClientHeartbeatListeners that the heartbeat connection to
	 * JMS has been lost.
	 */
	private void notifyListenersOfFailure() {

		final List<IClientHeartbeatListener> temp = new LinkedList<IClientHeartbeatListener>();

		synchronized (this.listeners) {
			temp.addAll(this.listeners);
		}

		for (final IClientHeartbeatListener l : temp) {
			l.publicationFailed();
		}
	}

	/**
	 * Notifies IClientHeartbeatListeners that the heartbeat connection to
	 * JMS has been regained.
	 */
	private void notifyListenersOfSuccess() {

		final List<IClientHeartbeatListener> temp = new LinkedList<IClientHeartbeatListener>();

		synchronized (this.listeners) {
			temp.addAll(this.listeners);
		}

		for (final IClientHeartbeatListener l : temp) {
			l.publicationRegained();
		}
	}

	/**
	 * Actually publishes the heartbeat message to JMS.
	 * 
	 * @throws MessageServiceException if publication to JMS fails
	 */
	private void publishHeartbeat() throws MessageServiceException {

		final IMessage msg = appContext.getBean(IStatusMessageFactory.class).createClientHeartbeatMessage(this.clientId);
        final IAccurateDateTime eventTime = msg.getEventTime();
		final BaseMessageHeader header = new BaseMessageHeader(appContext.getBean(MissionProperties.class), msg.getType(),
				eventTime);
		final String body = msg.toXml();
		final String wholeMsg = header.wrapContent(CommonMessageType.ClientHeartbeat, body);
		
		
		this.publisher.publishTextMessage(wholeMsg, header.getPropertiesWithStringKeys());
	}

}
