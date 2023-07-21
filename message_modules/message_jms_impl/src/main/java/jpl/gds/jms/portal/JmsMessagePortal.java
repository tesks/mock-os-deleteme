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

import static jpl.gds.shared.exceptions.ExceptionTools.rollUpMessages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ssl.SSLException;
import javax.validation.constraints.NotNull;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.types.IFillSupport;
import jpl.gds.common.types.IIdleSupport;
import jpl.gds.common.types.IRealtimeRecordedSupport;
import jpl.gds.context.api.ContextTopicNameFactory;
import jpl.gds.context.api.IGeneralContextInformation;
import jpl.gds.context.api.TopicNameToken;
import jpl.gds.jms.config.JmsProperties;
import jpl.gds.jms.config.PublishableMessageConfig;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.message.api.external.CheckMessageService;
import jpl.gds.message.api.external.IAsyncTopicPublisher;
import jpl.gds.message.api.external.IMessageClientFactory;
import jpl.gds.message.api.external.ITopicPublisher;
import jpl.gds.message.api.external.MessageServiceException;
import jpl.gds.message.api.portal.IMessagePortal;
import jpl.gds.message.api.spill.SpillProcessorException;
import jpl.gds.shared.config.PerformanceProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.performance.IPerformanceData;
import jpl.gds.shared.performance.IPerformanceProvider;
import jpl.gds.shared.performance.PerformanceSummaryPublisher;
import jpl.gds.shared.performance.QueuePerformanceData;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.thread.SleepUtilities;

/**
 * The MessagePortal class is a bridge that subscribes to selected internal GDS
 * messages and republishes them as XML on the external JMS bus. After an
 * instance is created, the method startup() must be invoked to start
 * subscriptions and make the JMS connection. Currently, JMS publication will
 * not occur unless the useJms property in the config file is set. <br>
 * In general, the design can be described as follows. There is the producing
 * thread creating the internal messages. Its entry port to the portal is via
 * the handleMessage() method. From there, messages are put into the translator
 * queue for XML translation. The translator runs on a separate thread and
 * translates messages. Once translated, the messages are placed onto the
 * publish queue. The message publishing thread pulls the messages from this
 * queue and sends them to a message preparer unique to the message's type. The
 * batcher determines when enough internal messages of a given type have been
 * assembled to publish a JMS message containing the entire batch of internal
 * messages. At that time, it pushes the message content to a JMS publisher.
 * <br>
 * Be sure to call the stop() method to stop subscriptions, or the JMS
 * subscriber thread will hang the application upon exit.
 *
 * Implemented TraceListener for external log messages so these can be published to JMS via the portal.
 */
public class JmsMessagePortal implements IMessagePortal, Runnable, IPerformanceProvider {

	private static final long DEFAULT_CONNECT_INTERVAL = 30000;
	private static final long PUBLICATION_THREAD_JOIN_TIMEOUT = 10000;
	private static final long PUBLICATION_QUEUE_WAIT_TIMEOUT = 250;
	private static final long SHUTDOWN_CHECK_SLEEP = 100;

	private static final String PROVIDER_NAME = "Message Portal";

	/** Received message count */
	private long receivedMessageCount = 0;

	/** JMS connection thread timer */
	private Timer connectTimer;

	/** Flush timer */
	private Timer flushTimer;

	/** Config flag indicating whether messages are batched. */
	private boolean batching = true;
	/** Config flag indicating whether messages to publish to JMS. */
	private boolean useJms;
	/** List of topics to enable. Overrides the default list if not null */
	private TopicNameToken[] customTopics = new TopicNameToken[0];

	/* Publication thread control members */
	/** The publication thread */
	private Thread thisThread;
	/** Flag indicating the thread has exited the run() method. */
	private final AtomicBoolean threadDone = new AtomicBoolean(false);
	/** Flag indicating the portal is in stopping phase. */
	private final AtomicBoolean portalStopping = new AtomicBoolean(false);
	/** Flag indicating the portal is stopped phase. Initialize to stopped state. */
	private final AtomicBoolean portalStopped = new AtomicBoolean(true);
	/** Flag indicating the JMS connections are active */
	private final AtomicBoolean haveJmsConnection = new AtomicBoolean(false);

	/* Members for performance tracking. */
    /** Performance Data for the publication queue */
    private QueuePerformanceData queuePerformance = null;
    
    /** High water mark for the publication queue */
    private long highWaterMark = 0;

    /* Track queue size without using queue.size() */
    private final AtomicInteger queueSize = new AtomicInteger(0);

    /*
     * The map tracks message preparers by message type. There may be duplicate
     * preparers in the map, so a list is used to track the unique instances.
     * Both should always be synchronized and modified at the same time.
     */
    /** Map of message type to MessagePreparer instance */
    private final Map<IMessageType, IMessagePreparer> preparerMap = new HashMap<>();
    /** List of unique MessagePreparer instances */
    /* Fix concurrent modification exception */
    private final List<IMessagePreparer> uniquePreparers = new CopyOnWriteArrayList<>();
    
    /**
     * Add flag for spill exception.
     * A spill exception probably cannot be recovered from with a JMS reconnect.
     */
    private boolean spillError = false;
    
    /**
	  * Add flag for ssl exception. A ssl exception
	  * cannot be recovered from within a JMS reconnect.
	  */    
    private boolean sslError = false;
    
    
    /** The publication queue. This is the queue the local run method works from. */
    private BlockingQueue<TranslatedMessage> publishQueue;
    
    /* Members related to topic publishers */
    /** Complete list of topics for which there are publishers */
    private final List<String> publishTopics = new ArrayList<>(8);
    /** Map of topic name to publisher instance */
    private final Map<String, IAsyncTopicPublisher> topicToPublisherMap = 
    		new HashMap<>();
    /** Map of message type to list of publishers for that type */
    private final Map<IMessageType, List<IAsyncTopicPublisher>> typeToPublishersMap = 
    		new HashMap<>();
   
    
    /** Cached SSE flag */
    @NotNull
    private final boolean                                       isSse;

    @NotNull
    private final long                                          globalFlushTimeout;

    /** Non-publishing fast tracer. */
    @NotNull
    private final Tracer                                        jmsTracer;

    /* Configuration references and members */
    @NotNull
    private final JmsProperties                                 jmsConfig;
    
    @NotNull
    private final String outputDir;
    
    @NotNull
    private final IMessagePublicationBus bus;
    @NotNull
    private final String generalTopic;
    @NotNull
    private final ApplicationContext appContext;

   
    /**
     * Constructs a JmsMessagePortal for the given application context.
     * 
     * @param serviceContext the current application context
     */
    public JmsMessagePortal(final ApplicationContext serviceContext) {
        appContext = serviceContext;
    	bus = serviceContext.getBean(IMessagePublicationBus.class);
        outputDir = serviceContext.getBean(IGeneralContextInformation.class).getOutputDir();  
        generalTopic = ContextTopicNameFactory.getGeneralTopic(serviceContext);
        jmsConfig = serviceContext.getBean(JmsProperties.class);
        globalFlushTimeout = jmsConfig.getPortalFlushTimeout();
        jmsTracer = TraceManager.getTracer(serviceContext, Loggers.JMS);
        isSse = serviceContext.getBean(SseContextFlag.class).isApplicationSse();
        init();
    }
     
    /**
     * {@inheritDoc}
     * 
     * This is the central reception point for all messages from external 
     * entities. Note this method will block until there is room in the
     * serialization/translation queue.
     * 
     */
    @Override
	public synchronized void handleMessage(final jpl.gds.shared.message.IMessage message) {
		/* Do not accept messages before started or once stopping. */
		if (portalStopping.get() || portalStopped.get()) {
			return;
		}

		/* Track received message count */
		receivedMessageCount++;
		/*
		 * Should be checking the local useJms flag here,
		 * not the one in the config, because the state can change.
		 */
		if (!useJms) {
			return;
		}
        /* Check if this message can be published externally */
        if (!message.isExternallyPublishable()) {
            return;
        }

		/*
		 * This really just accounts for SSE messages that have not been properly
		 * flagged by the components that create the message. TODO: There really should
		 * be a better way to ensure this on the front end.
		 */
		/* Use cached SSE flag here */
		if (isSse) {
			message.setFromSse(true);
		}

		/*
		 * End of session message is kept until flushEndOfSessionMessage() is invoked.
		 * Recorded EVR and EHA messages are only published if enabled in the config.
		 * Fill packets are not published.
		 * 
		 * Pass any other messages onto the message translator for serialization.
		 */

		if (message instanceof IRealtimeRecordedSupport) {
			if (!((IRealtimeRecordedSupport) message).isRealtime() && !jmsConfig.isPublishRecorded(message.getType())) {
				return;
			}
		}

		if (message instanceof IFillSupport) {
			if (((IFillSupport) message).isFill() && !jmsConfig.isPublishFillPacket()) {
				return;
			}
		}

		if (message instanceof IIdleSupport) {
			if (((IIdleSupport) message).isIdle() && !jmsConfig.isPublishIdleFrame()) {
				return;
			}
		}

		/*
		 * Bypass the Translator Queue to improve performance.
		 * Do message serialization here and queue the message.
		 */
		queueForPublication(createSerializedMessage(message));

	}

	/**
	 * Serializes a message to its publication form (binary or XML).
	 * 
	 * @param m the IMessage to translate
	 * @return serialized message object
	 */
	private TranslatedMessage createSerializedMessage(final IMessage m) {
		final TranslatedMessage tmo = new TranslatedMessage(m);
		if (jmsConfig.getMessageConfig(m.getType()).isBinary()) {
			tmo.setTranslation(m.toBinary());
		} else {
			tmo.setTranslation(m.toXml());
		}
		tmo.setTranslated(true);
		return tmo;
	}

	/**
	 * Initializes the portal by reading the configuration, starting the translation
	 * queue for serialization, creating the queue for translated messages, and
	 * registering for external log trace messages.
	 */
	private void init() {

		this.jmsConfig.init(); // complete initialization (if required) before use.
		useJms = appContext.getBean(MessageServiceConfiguration.class).getUseMessaging();

		/* Create the publication queue with length according to config */
		final int maxQueueLen = jmsConfig.getTranslatedQueueLength();
		publishQueue = new ArrayBlockingQueue<>(maxQueueLen);

		/* Add queue data object and units for performance tracking. */
		queuePerformance = new QueuePerformanceData(appContext.getBean(PerformanceProperties.class),
				"Message Publication", maxQueueLen, false, true, "messages");

		// Subscribe to all types that will be published and load their
		// times to live
		final IMessageType[] publishTypes = jmsConfig.getPublishTypes();

		if (publishTypes == null || publishTypes.length == 0) {
			jmsTracer.warn("No internal message types configured for external publication.");
		} else {
			for (final IMessageType type : publishTypes) {
				bus.subscribe(type, this);
				jmsTracer.debug("Subscribed to internal message type ", type);
			}
		}

		/*
		 * Register for external log messages. These get forwarded to the JMS as well.
		 * Register as a performance provider with the SessionBasedPerformanceSummaryPublisher.
		 */
		appContext.getBean(PerformanceSummaryPublisher.class).registerProvider(this);
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.message.api.portal.IMessagePortal#enableImmediateFlush(boolean)
	 */
	@Override
	public void enableImmediateFlush(final boolean enable) {
		batching = !enable;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Main publication thread run method.
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {

		jmsTracer.trace("run() is waiting for internal messages");

		/* Temporary list for transfer of messages to preparers. */
		final List<TranslatedMessage> transferQueue = new LinkedList<>();

		/* Run until the stopping flag is set. */
		while (!portalStopping.get()) {
			try {

				try {

					/* Track queue high water mark without using queue.size() */
					this.highWaterMark = Math.max(queueSize.get(), this.highWaterMark);

					/*
					 * Get a message from the queue of translated/serialized messages. This will
					 * block until something arrives or it is interrupted.
					 */

					final TranslatedMessage nextMessage = publishQueue.take();

					if (nextMessage != null) {
						/*
						 * There are messages. Pull all the ones we have off the translated message
						 * queue onto the transfer list.
						 */
						transferQueue.add(nextMessage);
						publishQueue.drainTo(transferQueue);

						/* Track queue size without using queue.size() */
						queueSize.addAndGet(-transferQueue.size());

						/*
						 * If there is a JMS connection, send them to the preparers. Otherwise, we
						 * discard them. Doing anything else would just fill up the queues in no time,
						 * and result in old messages being delivered to clients when the connection
						 * comes up.
						 */
						if (haveJmsConnection.get()) {
							jmsTracer.trace("Sending ", transferQueue.size(), " messages to preparers");
							sendToPreparers(transferQueue);
						}

						/* Clear the transfer list */
						transferQueue.clear();
					}
				} catch (final InterruptedException e) {
					jmsTracer.debug("Message Publisher thread interrupted; probably stopping",
							ExceptionTools.getMessage(e), e);
				}

			} catch (final Exception e) {
				jmsTracer.error("Unexpected exception in run() method", ExceptionTools.getMessage(e), e);
			}
		}

		/* Signal the stop method that the thread is finished. */
		threadDone.set(true);
	}

	/**
	 * Starts the publication thread.
	 */
	private void startPublicationThread() {
		threadDone.set(false);
		portalStopping.set(false);
		thisThread = new Thread(this, "Message Publisher");
		thisThread.start();
		portalStopped.set(false);

		/* Added flush timer. */
		startFlushTimer();
	}

	/**
	 * Creates the JMS connection and starts the main publication thread.
	 */
	@Override
	public synchronized boolean startService() {

		/* First establish JMS publisher connections on all topics. */
		if (useJms) {
			connectToJMS();
		}

		receivedMessageCount = 0;

		/*
		 * If JMS connection was made, start the main publication thread. Otherwise, a
		 * background thread eventually does this.
		 */
		if (haveJmsConnection.get()) {
			startPublicationThread();

		} else if (useJms) {

			/*
			 * We are using JMS but the connection has not been made
			 *
			 * Check flag for spill exception. A spill exception probably cannot be recovered
			 * from with a reconnect, since it has nothing to do with JMS.
			 * 
			 * Add Check for unrecoverable SSL error
	 	 	 */
			if (!(spillError && sslError)) {
				thisThread = null;
				jmsTracer.error("Unable to connect to JMS.  Retrying connection...");
			} else {
				this.useJms = false;
				jmsTracer.error("Unable to create JMS spill processor. No messages can be published.");
				return false;
			}
		}
		return true;
	}

	/**
	 * Unsubscribes from all messages and shuts down the JMS connection, after
	 * sending all queued messages. Note that the worker thread will not stop until
	 * all messages are published.
	 */
	@Override
	public synchronized void stopService() {

		if (portalStopped.get()) {
			jmsTracer.debug("Message Portal already stopped");
			return;
		}

		jmsTracer.debug("Stopping Message Portal");

		/* Stop receipt of messages from external entities */
		bus.unsubscribeAll(this);

		/* Stop any JMS reconnection attempts */
		if (connectTimer != null) {
			connectTimer.cancel();
		}

		/* Stop the message flush timer. */
		if (flushTimer != null) {
			flushTimer.cancel();
		}

		/* Wait for translator, translated, and preparer queues to clear */
		waitToClearMessages();

		/*
		 * Signal the main thread (translated queue worker) thread to stop and join with
		 * it
		 */
		portalStopping.set(true);
		this.thisThread.interrupt();
		SleepUtilities.checkedJoin(this.thisThread, PUBLICATION_THREAD_JOIN_TIMEOUT, "Message Publisher", jmsTracer);
		this.thisThread = null;

		/* Collect and print final statistics if debug is on */

		if (preparerMap != null && jmsTracer.isDebugEnabled()) {
			long publishTotal = 0;

			final Map<IMessageType, Long> messageTypeCount = new HashMap<>();

			for (final IMessagePreparer b : this.uniquePreparers) {

				b.shutdown();

				for (final Entry<IMessageType, AtomicLong> entry : b.getPublishTotalByType().entrySet()) {
					final Long tCount = messageTypeCount.get(entry.getKey());
					if (tCount != null) {
						messageTypeCount.put(entry.getKey(), tCount + entry.getValue().get());
					} else {
						messageTypeCount.put(entry.getKey(), Long.valueOf(entry.getValue().get()));
					}
				}
			}
			for (final Entry<IMessageType, Long> entry : messageTypeCount.entrySet()) {
				jmsTracer.debug("Publish total for ", entry.getKey(), " ", entry.getValue());
				publishTotal += entry.getValue();
			}

			jmsTracer.debug("Total count of messages sent to JMS publishers: ", publishTotal);
		}

		jmsTracer.debug("Message Portal is closing publishers");

		for (final ITopicPublisher publisher : topicToPublisherMap.values()) {
			publisher.close();
		}

		/* De-register as performance provider. */
		appContext.getBean(PerformanceSummaryPublisher.class).deregisterProvider(this);

		jmsTracer.debug("Message Portal stopped");
		portalStopped.set(true);
	}

	/**
	 * Try to make JMS publisher connections in the background.
	 */
	private void connectToJMSBackground() {

		if (connectTimer == null) {
			connectTimer = new Timer("Message Portal JMS Connect Timer", true); // as daemon
			long connectInterval = 0;
			connectInterval = jmsConfig.getReconnectWait();
			if (connectInterval == 0) {
				connectInterval = DEFAULT_CONNECT_INTERVAL;
			}

			// Start a timer to retry the JMS connection at regular intervals.

			connectTimer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					try {
						if (connectTimer == null) {
							return;
						}
						final boolean ok = connectToJMS();
						/*
						 * A spill exception probably cannot be recovered from with a reconnect, since
						 * it has nothing to do with JMS. Stop attempting to reconnect if a spill error
						 * is detected.
						 */
						if (ok) {
							useJms = true;
							connectTimer.purge();
							connectTimer.cancel();
							connectTimer = null;
							jmsTracer.info("Successfully connected to JMS after retry");
							startPublicationThread();
						} else if (!(spillError && sslError)) {
							jmsTracer.error("JMS reconnection failed.  Trying again...");
						} else {
							useJms = false;
							jmsTracer.error("Unable to create JMS spill processor. No messages can be published.");
							connectTimer.cancel();
						}
					} catch (final Exception e) {
						jmsTracer.error(ExceptionTools.getMessage(e), e);
					}
				}
			}, connectInterval, connectInterval);
		}
	}

	/**
	 * Creates the JMS connections for publications, if the useJms configuration
	 * property is set. If the connection attempt is not immediately successful, a
	 * timer is started to re-attempt the connection after a defined interval. The
	 * timer runs until a connection is made or shutdown() is invoked.
	 * 
	 * @return true is JMS connection succeeded; false otherwise.
	 */
	private boolean connectToJMS() {

		if (!CheckMessageService.checkMessageServiceRunning(appContext.getBean(MessageServiceConfiguration.class), null,
				0, jmsTracer, false, false)) {

			// Try to connect in the background, if not already trying
			connectToJMSBackground();

			return false;
		}

		jmsTracer.trace("MessagePortal is attempting connection to JMS");

		final IMessageClientFactory clientFactory = appContext.getBean(IMessageClientFactory.class);

		final String applicationTopic = appContext.getBean(IGeneralContextInformation.class).getRootPublicationTopic();

		try {

			final IMessageType[] internalTypes = jmsConfig.getPublishTypes();
			if (internalTypes != null) {
				// loop through each message type
				for (int i = 0; i < internalTypes.length; i++) {
					final List<IAsyncTopicPublisher> publishers = new ArrayList<>(8);

					// Check if custom topics is being enforced.
					// loop through all the topics that this message is
					// published to
					final TopicNameToken[] topics = (customTopics.length > 0) ? customTopics
							: jmsConfig.getMessageConfig(internalTypes[i]).getTopics();

					for (int j = 0; j < topics.length; j++) {
						String topicName = null;

						if (topics[j] == TopicNameToken.GENERAL) {
							topicName = this.generalTopic;
						} else if (topics[j] == TopicNameToken.PERSPECTIVE) {
							topicName = ContextTopicNameFactory.getPerspectiveTopic(appContext);
						} else if (topics[j] == TopicNameToken.APPLICATION) {
							topicName = applicationTopic;
						} else {
							topicName = topics[j].getApplicationDataTopic(applicationTopic);
						}

						if (!publishTopics.contains(topicName)) {
							synchronized (publishTopics) {
								publishTopics.add(topicName);
							}
							jmsTracer.info(Markers.BUS, "Publishing to topic ", topicName);

						}

						// get the publisher for this specific topic or create
						// it if it
						// doesn't already exist
						IAsyncTopicPublisher atp = topicToPublisherMap.get(topicName);
						if (atp == null) {
							atp = clientFactory.getTransactedAsyncTopicPublisher(topicName, true, this.outputDir);
							topicToPublisherMap.put(topicName, atp);
						}

						// add this publisher to the list of publishers for
						// this type
						publishers.add(atp);
					}

					// map the message type to all of its topic publishers
					typeToPublishersMap.put(internalTypes[i], publishers);
				}
			}

			if (connectTimer != null) {
				connectTimer.cancel();
			}

			haveJmsConnection.set(true);

			return haveJmsConnection.get();

		} catch (final MessageServiceException e) {
			/*
			 * Set flag for spill exception. A spill exception probably cannot be recovered
			 * from with a reconnect. Note that this exception is not specifically thrown by
			 * the Async Topic Publisher because then every class going through the JmsFactory
			 * to create a publisher would have to catch and handle this exception, which really
			 * affects nothing but this message portal.
			 */
			if (e.getCause() instanceof SpillProcessorException) {
				spillError = true;
			}
			
 	 	 	if (e.getCause() instanceof SSLException) {
 	 	 	    sslError = true;
	 	 	    jmsTracer.error("Unable to instantiate a secure JMS connection. Verify SSL "
	 	 	    		+ "configurations are properly set. ", e);
	 	 	} else {
	 	 		jmsTracer.error("Unable to create topic connection to JMS", e);
	        }
 	 	 	
		} catch (final Exception ex) {
			jmsTracer.error("Error creating Message Portal JMS connections ", ExceptionTools.getMessage(ex), ex);
		}

		/*
		 * Try to connect in the background, if not already trying
		 *
		 * Check flag for spill exception. A spill exception probably cannot be recovered
		 * from with a reconnect, since it has nothing to do with JMS.
		 * *
	 	 * Check flag for ssl exception. A SSL exception cannot be recovered from within a reconnect.
		 */
		if (!(spillError && sslError)) {
			connectToJMSBackground();
		}

		haveJmsConnection.set(false);
		
		return haveJmsConnection.get();

	}
	/**
	 * Returns the count of internal messages received since the call to startup().
	 * 
	 * @return message count
	 */
	@Override
	public long getReceivedMessageCount() {
		return receivedMessageCount;
	}
	/**
	 * Queues the supplied translated messages to the appropriate message ss. Will
	 * block until the preparer has submitted the message to the publisher or is
	 * interrupted.
	 * 
	 * @param messages the translated messages to queue
	 */
	private void sendToPreparers(final List<TranslatedMessage> messages) {

		for (final TranslatedMessage tmo : messages) {

			final IMessageType type = tmo.getMessage().getType();

			final List<IAsyncTopicPublisher> publishers = typeToPublishersMap.get(type);
			if (publishers == null) {
				throw new IllegalStateException("Could not retrieve Topic Publisher for type \"" + type + "\"");
			}

			IMessagePreparer b = preparerMap.get(type);
			if (b == null) {
				jmsTracer.trace("Creating MessagePreparer for message type ", type);

				final PublishableMessageConfig msgConfig = jmsConfig.getMessageConfig(type);
				final JmsProperties.PreparerClassType preparerName = msgConfig.getPreparerType();
				b = appContext.getBean(IMessagePreparer.class, preparerName, type, publishers);
				synchronized (this.preparerMap) {
					preparerMap.put(type, b);
					this.uniquePreparers.add(b);
				}
				b.enableBatching(batching);
			}

			b.messageReceived(tmo);
		}
	}

	/**
	 * Queues a translated/serialized message to the publication queue, where the
	 * run() method will pick it up. This method will block until there is room in
	 * the publication queue or it is interrupted.
	 * 
	 * @param tmo the translated message to publish
	 */
	public void queueForPublication(final TranslatedMessage tmo) {

		if (this.portalStopping.get()) {
			jmsTracer.warn("Message translator attempted to queue message to portal during stop phase");
			return;
		}

		/* Loop until the message is queued or the thread is interrupted. */
    	boolean queued = false;
    	while (!queued && !portalStopping.get()) {
    		try {
    			while (!publishQueue.offer(tmo, PUBLICATION_QUEUE_WAIT_TIMEOUT, TimeUnit.MILLISECONDS)) {
   				   if (jmsTracer.isEnabledFor(TraceSeverity.TRACE)) {
   				       jmsTracer.trace("Message portal queue for publication wait time elapsed: " ,
    						publishQueue.remainingCapacity());
   				   }
    			}

				queued = true;
				/* Track queue size without calling queue.size() */
				queueSize.incrementAndGet();

			} catch (final InterruptedException eie) {
				jmsTracer.debug("Message Portal queue for publication interrupted: ", rollUpMessages(eie),
						ExceptionTools.getMessage(eie), eie);
			}
		}

	}

	/**
	 * Waits for the serialization, publication, and publisher queues to clear. Note
	 * this method is not synchronized. This is because it can take a very long
	 * time, and if handleMessage() blocks for that long, bad things happen. We have
	 * to be able to handle incoming messages while we are doing this. This method
	 * should only be called when the system is almost quiet, or it is unlikely to
	 * ever return. It works this way because we may still be publishing log or
	 * heartbeat messages, but we want all the previous messages generated by the
	 * session to be pushed out.
	 * 
	 * If JMS is not connected, this method does nothing.
	 */
	@Override
	public void clearAllQueuedMessages() {
		if (haveJmsConnection.get()) {
			waitToClearMessages();
		}
	}

	/**
	 * Waits for serialization, publication, and publisher queues to clear.
	 */
	private void waitToClearMessages() {

		if (portalStopped.get()) {
			jmsTracer.debug("Attempting to clear message after portal stop");
			return;
		}

		jmsTracer.debug("Waiting to clear message queues");

		/**
		 * Disabled the translator queue. To re-enable, add this back.
		 */
		if (!publishQueue.isEmpty()) {
			jmsTracer.debug("Queuing translated messages to publish before proceeding: ", publishQueue.size());
		}

		while (!publishQueue.isEmpty()) {
			SleepUtilities.checkedSleep(SHUTDOWN_CHECK_SLEEP);
		}

		jmsTracer.debug("Flushing message preparers");
		synchronized (this.preparerMap) {
			for (final IMessagePreparer b : this.uniquePreparers) {
				b.flushQueue();
			}
		}

		jmsTracer.debug("Flushing publishers");

		for (final IAsyncTopicPublisher publisher : topicToPublisherMap.values()) {
			publisher.flushQueue();
		}

		jmsTracer.debug("Internal message queues cleared");
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.performance.IPerformanceProvider#getPerformanceData()
	 */
	@Override
	public List<IPerformanceData> getPerformanceData() {
		/*
		 * Make sure queue size is reported >= 0. Because this is being loosely tracked
		 * without using a call to the queue itself, it can get slightly out of sync,
		 * and may be < 0.
		 * Track queue size without using queue.size()
		 *
		 * Note that there can be inconsistency between queue size and high water
		 * marker depending on timing of the run thread. It does not matter. Performance
		 * figures do not have to be exact.
		 */
		this.queuePerformance.setCurrentQueueSize(Math.max(0, queueSize.get()));
		this.queuePerformance.setHighWaterMark(this.highWaterMark);
		final List<IPerformanceData> temp = new LinkedList<>();

		temp.add(this.queuePerformance);

		for (final IAsyncTopicPublisher pub : this.topicToPublisherMap.values()) {
			temp.addAll(pub.getPerformanceData());
		}
		return temp;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.performance.IPerformanceProvider#getProviderName()
	 */
	@Override
	public String getProviderName() {
		return PROVIDER_NAME;
	}

	/**
	 * Start the message flush timer.
	 */
	private void startFlushTimer() {

		if (flushTimer == null) {
			flushTimer = new Timer("Message Portal Flush Timer", true); // as daemon

			// Start a timer to flush preparers
			flushTimer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					try {
						if (flushTimer == null) {
							return;
						}
						for (final IMessagePreparer p : uniquePreparers) {
							p.flushQueue();
						}

					} catch (final Exception e) {
						jmsTracer.error(ExceptionTools.getMessage(e), e);
					}
				}
			}, globalFlushTimeout, globalFlushTimeout);
		}
	}

	/**
	 * Added a new mothod to the IMessagePortal that allows to enable certain topics by passing them in.
	 * Example: cfdp processor only uses the root.cfdp, so it can use this method
	 * to enable only that topic in the jms.
	 */
	@Override
	public void enableSpecificTopics(final TopicNameToken[] topics) {
		if (topics == null) {
			// do nothing. Let jms enable all the topics.
		} else if (topics.length > 0) {
			customTopics = topics;
		}
	}

}

