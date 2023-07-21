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
package jpl.gds.monitor.guiapp.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.context.api.ContextTopicNameFactory;
import jpl.gds.context.api.IGeneralContextInformation;
import jpl.gds.context.api.IVenueConfiguration;
import jpl.gds.context.api.TopicNameToken;
import jpl.gds.message.api.external.IClientHeartbeatListener;
import jpl.gds.message.api.external.IClientHeartbeatPublisher;
import jpl.gds.message.api.external.IExternalMessage;
import jpl.gds.message.api.external.IExternalMessageUtility;
import jpl.gds.message.api.external.IMessageServiceListener;
import jpl.gds.message.api.external.MessageServiceException;
import jpl.gds.message.api.handler.IQueuingMessageHandler;
import jpl.gds.message.api.util.MessageCaptureHandler;
import jpl.gds.message.api.util.MessageFilterMaker;
import jpl.gds.monitor.config.MonitorGuiProperties;
import jpl.gds.monitor.guiapp.channel.ChannelMessageDistributor;
import jpl.gds.monitor.guiapp.common.GeneralMessageDistributor;
import jpl.gds.monitor.guiapp.common.MonitorSubscriber;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.metadata.MetadataKey;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * MonitorMessageController is the central message reception point for
 * chill_monitor. It establishes and keeps the message service subscribers, receives
 * messages from them, and distributes them to appropriate message distributors
 * and handlers.
 * 
 */
public final class MonitorMessageController implements IMessageServiceListener, IClientHeartbeatListener {

	private boolean subscribersCreated = false;
	private final HashMap<String,MonitorSubscriber> subscribers = new HashMap<>();
	private final List<String> subscribedTopics = new ArrayList<>(4);
	private final MessageCaptureHandler captureDistributor;
	private final ChannelMessageDistributor channelDistributor;
	private final GeneralMessageDistributor generalDistributor;
	private final AtomicLong receiptCount = new AtomicLong(0);

	private IClientHeartbeatPublisher hbPublisher;
	private MonitorPerspectiveActor perspectiveActor;
	
	private final ApplicationContext appContext;
    private final IExternalMessageUtility externalMessageUtil;
	
    private final Tracer                             jmsTracer;
    private boolean started;
    private final SseContextFlag                     sseFlag;


	/**
	 * Creates an instance of MonitorMessageController. 
	 * @param appContext the curernt application context
	 * 
	 */
	public MonitorMessageController(final ApplicationContext appContext) {
		this.appContext = appContext;
        jmsTracer = TraceManager.getTracer(appContext, Loggers.DEFAULT);
		channelDistributor = appContext.getBean(ChannelMessageDistributor.class);
		generalDistributor = appContext.getBean(GeneralMessageDistributor.class);
		captureDistributor = appContext.getBean(MessageCaptureHandler.class);
		externalMessageUtil = appContext.getBean(IExternalMessageUtility.class);
        sseFlag = appContext.getBean(SseContextFlag.class);
	}
	
	/**
	 * Sets the perspective actor instance for the GUI. This
	 * is used to tie the Client Heartbeat notifications into
	 * the actor so it knows if the message service goes down.
	 * 
	 * @param actor perspective actor object
	 */
	public void setPerspectiveActor(final MonitorPerspectiveActor actor) {
		this.perspectiveActor = actor;
	}
	
	/**
	 * Gets the names of the message service topics this object has subscriptions for.
	 * 
	 * @return list of topic names
	 */
	public List<String> getSubscribedTopics() {
		return (subscribedTopics);
	}

	/**
	 * Gets the total message receive count for this object.
	 * 
	 * @return Returns the receipt count.
	 */
	public long getReceiptCount() {
		return (receiptCount.get());
	}

	/**
	 * Creates the subscribers for the currently configured topics. Message subscriptions
	 * will be automatically filtered by the current session spacecraft ID. Both FSW and 
	 * SSE topics will be subscribed to, if appropriate. Note that no messages will be
	 * received until startMessageReceipt() is invoked.
	 * 
	 * @see #startMessageReceipt()
	 */
	public synchronized void createSubscribers() {
		
		// Do not do anything if subscriptions already exist
		if (subscribersCreated) {
			return;
		}

		/*
		 * Applications that subscribe to the message
		 * service  generate a heartbeat that can detect
		 * when it is down.
		 */
		startHeartbeat();

		final List<TopicNameToken> topics = appContext.getBean(MonitorGuiProperties.class).getTopics();
		if (topics == null) {
			throw new IllegalStateException(
					"No topics for monitor in the monitor GUI configuration");
		}
		
		adjustSubscriptionTopics(topics);
		
		// Loop through the configured topic tokens
		for (final String topicName: appContext.getBean(IGeneralContextInformation.class).getSubscriptionTopics()) {
		    
	        final String filter = MessageFilterMaker.createFilterFromContext(appContext);

		    // Make sure we are not already subscribed to this topic
		    if (!subscribedTopics.contains(topicName)) {
		        try {

		            // Create the subscription to the message service and set this object as message listener.
		            jmsTracer.info("Subscribing to topic " + topicName  + " with filter " + filter);
		            final IQueuingMessageHandler sub = appContext.getBean(IQueuingMessageHandler.class, getConfiguredQueueSize(topicName));
		            sub.setSubscription(topicName, filter, false);
		            
		            if (TopicNameToken.APPLICATION_EHA.matches(topicName) || TopicNameToken.APPLICATION_SSE_EHA.matches(topicName)) {
		                sub.addListener(this.channelDistributor);
		            } else {
		                sub.addListener(this.generalDistributor);
		            }
		            
		            sub.addListener(captureDistributor);
		            sub.addListener(this);

		            // Create the subscriber tracking object. Add the tracker to the subscriber table
		            // and the topic to the subscribed topic list
                    final MonitorSubscriber subObject = new MonitorSubscriber(sub, topicName, jmsTracer);
		            subscribers.put(topicName, subObject);
		            subscribedTopics.add(topicName);

		        } catch (final Exception e) {
		            throw new IllegalStateException(
		                    "Unable to create message service subscribers. Monitor cannot proceed. Check if the message server is running:"
		                    + e.toString());
		        }
		    }
		}
	
		subscribersCreated = true;
	}
	
	private int getConfiguredQueueSize(final String topicName) {
	    final MonitorGuiProperties mprops = appContext.getBean(MonitorGuiProperties.class);
	    int size = mprops.getDefaultSubscriberQueueSize();
	    if (TopicNameToken.APPLICATION_EHA.matches(topicName) || TopicNameToken.APPLICATION_SSE_EHA.matches(topicName)) {
	        size = mprops.getEhaSubscriberQueueSize();   
	    } else if (TopicNameToken.APPLICATION_EVR.matches(topicName) || TopicNameToken.APPLICATION_SSE_EVR.matches(topicName)) {
	        size = mprops.getEvrSubscriberQueueSize();   
	    }  else if (TopicNameToken.APPLICATION_PRODUCT.matches(topicName)) {
	        size = mprops.getProductSubscriberQueueSize();  
	    }
	    jmsTracer.debug("Subscriber queue size for topic " + topicName + " is " + size);
	    return size;
	}

	/**
	 * Starts actual listening for messages. Must be called AFTER createSubscribers().
	 * 
	 * @see #createSubscribers()
	 * 
	 */
	public synchronized void startMessageReceipt() {
		if (!subscribersCreated) {
			throw new IllegalStateException("Subscribers have not been created");
		}
		
		if (started) {
		    return;
		}
		
		for (final MonitorSubscriber sub : subscribers.values()) {
			try {
                sub.getSubscriber().start();
            } catch (final MessageServiceException e) {
                throw new IllegalStateException(
                        "Unable to start message service subscribers. Monitor cannot proceed. Check if the message server is running:"
                        + e.toString());
            }
		}

		channelDistributor.start();
		
		started = true;
	}

	/**
	 * Closes the current message subscribers and shuts down the related message
	 * distributors. The client heartbeat is also stopped.
	 */
	public synchronized void closeSubscribers() {

		if (this.hbPublisher != null) {
			this.hbPublisher.stopPublishing();
		}
		
		for (final MonitorSubscriber subObject: subscribers.values()) {        
			subObject.getSubscriber().shutdown(false, false);
			subObject.dumpMessageLoss();			
		}

		subscribers.clear();
		subscribedTopics.clear();
		subscribersCreated = false;


		captureDistributor.shutdown();
		generalDistributor.shutdown();
		channelDistributor.shutdown();
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.message.api.external.IClientHeartbeatListener#publicationFailed()
	 *
	 */
	@Override
	public void publicationFailed() {
	
		// The message service is likely down because the heartbeat publisher failed.
		// Generate a log entry and then close the subscribers.
		
		jmsTracer.error("Message service subscriber connections on all monitored topics were lost");
		
		closeSubscribersNoDisconnect();
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.message.api.external.IClientHeartbeatListener#publicationRegained()
	 *
	 */
	@Override
	public void publicationRegained() {
	
		// JMS is back up because the heartbeat publisher reconnected.
		// Recreate the JMS subscribers and start receiving messages 
		// again.
		createSubscribers();

		jmsTracer.info("Message service subscriber connection on all monitored topics were recreated");
		
		startMessageReceipt();
		
	}

	/**
	 * Terminates subscriber connections with extreme prejudice because the message
	 * service is likely down.
	 *
	 */
	private synchronized void closeSubscribersNoDisconnect() {
		for (final MonitorSubscriber subObject: subscribers.values()) {        
			subObject.getSubscriber().shutdown(true, false);
			subObject.dumpMessageLoss(); 
		}
		subscribers.clear();
		subscribedTopics.clear();
		subscribersCreated = false;
	}
	
	/**
	 * Starts the client heartbeat to the message service.
	 *
	 */
	private void startHeartbeat() {
		if (this.hbPublisher == null) {
		    this.hbPublisher = appContext.getBean(IClientHeartbeatPublisher.class);
		    this.hbPublisher.addListener(this);
            if (!hbPublisher.startPublishing()) {
                jmsTracer.error("Unable to start client heartbeat");
            } else {
                jmsTracer.info("Client heartbeat started for " + hbPublisher.getClientId());
            }
			if (this.perspectiveActor != null) {
				this.hbPublisher.addListener(this.perspectiveActor);
			}
		}
	}


	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onMessage(final IExternalMessage m) {
		
		// Keep track of messages received by this object
		receiptCount.incrementAndGet();

		try {

			// Get the type of the message and route it to the right message distributor
			final IMessageType type = externalMessageUtil.getInternalType(m);
			final Integer messageSourcePid = externalMessageUtil.getIntHeaderProperty(m, MetadataKey.SOURCE_PID.toString());
			final Integer messageHeaderNumber =  externalMessageUtil.getIntHeaderProperty(m, MetadataKey.MESSAGE_COUNTER.toString());
			
			final String topic = externalMessageUtil.getTopic(m);
			final long time = externalMessageUtil.getMessageTimestamp(m);
			final long receiveTime = System.currentTimeMillis();

			final MonitorSubscriber subObject = subscribers.get(topic);
			
            subObject.update(receiveTime, time, messageSourcePid == null? 0 : messageSourcePid, 
					        messageHeaderNumber == null ? 0 : messageHeaderNumber, type);
					
		} catch (final Exception e) {
			jmsTracer.error("Unexpected error processing message in MonitorMessageController", e);
		}     
	}


	/**
	 * Gets a clone of the current list of subscriber tracking objects.
	 * 
	 * @return List of MonitorSubscriber objects
	 */
	public List<MonitorSubscriber> getSubscribers() {
		final ArrayList<MonitorSubscriber> subs = new ArrayList<>(subscribers.size());
		final Set<String> keySet = subscribers.keySet();
		final Iterator<String> it = keySet.iterator();
		while (it.hasNext())
		{        
			final MonitorSubscriber subObject = subscribers.get(it.next());
			subs.add(subObject);
		}
		return subs;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		closeSubscribers();
		super.finalize();
	}
	
	private void adjustSubscriptionTopics(final List<TopicNameToken> tokens) {
        final Collection<String> result = new TreeSet<>();
        final IGeneralContextInformation info = appContext.getBean(IGeneralContextInformation.class);
        final Collection<String> applicationTopics = info.getSubscriptionTopics();
        final MissionProperties mprops = appContext.getBean(MissionProperties.class);
        
        if (applicationTopics.isEmpty()) {
            for (final TopicNameToken t: tokens) {
                final String topicName = ContextTopicNameFactory.getTopicNameFromConfigValue(appContext, t);
                final boolean topicIsSse = t.isSse();

                //this will prevent us from creating a topic with the name "sse" when we don't want to
                if ((appContext.getBean(IVenueConfiguration.class).getVenueType().isOpsVenue()
                        || (!sseFlag.isApplicationSse() && !appContext.getBean(MissionProperties.class).missionHasSse()))
                        && topicIsSse)
                {
                    continue;
                }
                
                result.add(topicName);
            }
            
        } else {
            for (final String topicName: applicationTopics) {
                result.add(topicName);
                if (topicName.contains(GdsSystemProperties.getSseNameForSystemMission())) {
                    result.addAll(expandSseTopic(mprops, topicName));
                } else {
                    result.addAll(expandFlightTopic(mprops, topicName));  
                }
            }
        }

        info.setSubscriptionTopics(result);
	} 

	private List<String> expandFlightTopic(final MissionProperties mprops, final String topicName) {
	    final List<String> result = new LinkedList<>();
	    result.add(TopicNameToken.APPLICATION_EHA.getApplicationDataTopic(topicName));
        if (mprops.areEvrsEnabled()) {
            result.add(TopicNameToken.APPLICATION_EVR.getApplicationDataTopic(topicName));
        }
        if (mprops.areProductsEnabled()) {
            result.add(TopicNameToken.APPLICATION_PRODUCT.getApplicationDataTopic(topicName));
        }
        if (mprops.isUplinkEnabled()) {
            result.add(TopicNameToken.APPLICATION_COMMAND.getApplicationDataTopic(topicName));
        } 
        return result;
	}
	

    private List<String> expandSseTopic(final MissionProperties mprops, final String topicName) {
        final List<String> result = new LinkedList<>();
        result.add(TopicNameToken.APPLICATION_SSE_EHA.getApplicationDataTopic(topicName));
        if (mprops.areEvrsEnabled()) {
            result.add(TopicNameToken.APPLICATION_SSE_EVR.getApplicationDataTopic(topicName));
        }
        if (mprops.isUplinkEnabled()) {
            result.add(TopicNameToken.APPLICATION_SSE_COMMAND.getApplicationDataTopic(topicName));
        }
        return result;
    }

}
