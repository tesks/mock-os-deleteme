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
package jpl.gds.watcher;


import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.context.api.ContextTopicNameFactory;
import jpl.gds.context.api.IGeneralContextInformation;
import jpl.gds.context.api.IVenueConfiguration;
import jpl.gds.context.api.TopicNameToken;
import jpl.gds.context.api.message.IEndOfContextMessage;
import jpl.gds.message.api.app.MessageAppConstants;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.message.api.external.CheckMessageService;
import jpl.gds.message.api.external.IClientHeartbeatListener;
import jpl.gds.message.api.external.IClientHeartbeatPublisher;
import jpl.gds.message.api.external.IExternalMessage;
import jpl.gds.message.api.external.IExternalMessageUtility;
import jpl.gds.message.api.external.IMessageClientFactory;
import jpl.gds.message.api.external.IMessageServiceListener;
import jpl.gds.message.api.external.MessageServiceException;
import jpl.gds.message.api.handler.IQueuingMessageHandler;
import jpl.gds.message.api.util.MessageFilterMaker;
import jpl.gds.session.message.SessionMessageType;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessageConfiguration;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.string.StringUtil;



/**
 * MessageRouter is the message dispatching class for the message supervisor
 * application. Based upon the configuration, it instantiates a set of
 * message handler objects and routes a message to the appropriate handlers
 * based upon message type. The topics it listens to are defined in the GDS
 * configuration.
 * 
 * Implemented IClientHeartbeatListener interface so this class can detect JMS outages.
 */
public class MessageRouter implements IMessageServiceListener, IClientHeartbeatListener {
	
	private static final String AND = " AND ";

    private final Tracer tracer;
    private final Tracer jmsTracer;

	private final Map<IMessageType,IMessageHandler> typesToHandler;
	private final Map<String,IMessageHandler> handlerInstances;
	private final Map<String,IQueuingMessageHandler> subscribers;

	private boolean verbose;
	private boolean initialized;
	private final MessageServiceConfiguration jmsConfig;
	private final String responderName;
	private final IResponderAppHelper appHelper;
	private boolean shuttingDown;
	private boolean routeEndOfContext;
	private final WatcherProperties responderProps;

	private long contextNumber;
	private String contextHost;
	private IClientHeartbeatPublisher hbPublisher;
	private String[] messageTypes;
	private final ApplicationContext appContext;
    private final IExternalMessageUtility externalMessageUtil;
    
    private final int queueSize;
    private final SseContextFlag                      sseFlag;

	/**
     * Creates an instance of MessageRouter.
     * 
     * @param appContext
     *            the current application context
     * @param responderName
     *            the name of the responder configuration to user from the GDS
     * @param app
     *            responder application helper
     * 
     * @param queueLen
     *            Queue length
     */
	public MessageRouter(final ApplicationContext appContext, final String responderName, final IResponderAppHelper app,
	        final int queueLen)
	{
		this.appContext = appContext;
        tracer = TraceManager.getDefaultTracer(appContext);
        jmsTracer = TraceManager.getTracer(appContext, Loggers.JMS);
        this.sseFlag = appContext.getBean(SseContextFlag.class);
        this.typesToHandler = new HashMap<>(16);
		this.handlerInstances = new HashMap<>(16);
		this.subscribers = new HashMap<>(16);
		this.jmsConfig = appContext.getBean(MessageServiceConfiguration.class);
		this.responderName = responderName;
		this.appHelper = app;
        this.responderProps = new WatcherProperties(this.responderName, sseFlag);
		this.externalMessageUtil = appContext.getBean(IExternalMessageUtility.class);
		this.queueSize = queueLen;
	}

	/**
	 * Enables/disables verbose output to console regarding message receipt.
	 * To apply to contained handlers, this method must be called prior
	 * to init().
	 * @param enable true to enable verbose output; false to shut it up
	 */
	public void setVerbose(final boolean enable) {
		this.verbose = enable;
	}

	/**
	 * Initializes handlers and starts the router listening for messages.
	 * 
	 * @param overrideTypes
	 *            Override the message type handling
	 * @param contextNumber
	 *            context id or less than 1 if filter not wanted
	 * @param contextHost
	 *            context host or null if filter not wanted
	 * 
	 * @return true if the router is successfully initialized
	 */
	public boolean init(final String[] overrideTypes,
			final long     contextNumber,
			final String   contextHost)
	{
		this.contextNumber = contextNumber;
		this.contextHost = contextHost;

		// This special handling allows unit tests to operate. Do not
		// remove it.
        if (GdsSystemProperties.getSystemProperty("GdsIsResponderTest") != null) {
			messageTypes = new String[] {"TestType1","TestType2","EndOfSession"};
		} else if (overrideTypes != null) {
			messageTypes = overrideTypes;
		} else {
            messageTypes = MessageRegistry.getAllSubscriptionTags(true).toArray(new String[] {});
		}

		// Create message handlers.
		if (!createMessageHandlers()) {
		    return false;
		}
	
		// Configure whether EndOfContext should be processed just as
		// other messages, in addition to notifyEndOfContext
		this.routeEndOfContext = responderProps.routeEndOfContext();

		// Create and start the message service subscribers
		if (! CheckMessageService.checkMessageServiceRunning(this.jmsConfig, null, 0, tracer, true))
		{
			return false;
		}

		this.initialized = createSubscribers();

		// Start the client heartbeat.
		if (this.initialized) {
		    startClientHeartbeat();
			writeLog(MessageAppConstants.MESSAGE_ROUTER_UP_MESSAGE);
		}


		return this.initialized;
	}

	private boolean createMessageHandlers() {
	    for (int i = 0; i < messageTypes.length; i++) {

	        final IMessageConfiguration mc = MessageRegistry.getMessageConfig(messageTypes[i]);
	        if (mc == null) {
	            writeError("Could not find registered message configuration for message type " + messageTypes[i]);
	            return false;
	        }
	        IMessageHandler handler = null;
	        try {
				handler = appContext.getBean(IMessageHandler.class, mc.getMessageType());
			} catch (NoSuchBeanDefinitionException e) {
	        	// do nothing
				writeLog("Unable to instantiate message handler for message type " + mc.getMessageType() + ". Retrying with message type 'Any.'");
			}
	        if (handler == null) {
				handler = appContext.getBean(IMessageHandler.class, ResponderMessageType.Any);
			}
	        if (handler != null) {
	            handler.setVerbose(this.verbose);
	            handler.setAppHelper(this.appHelper);
	            this.handlerInstances.put(handler.getClass().getName(), handler);
	            this.typesToHandler.put(mc.getMessageType(), handler);
	            writeLog("MessageRouter added handler for message type " +
	                    mc.getMessageType().getSubscriptionTag());
	        } else {
	            writeError("Unable to instantiate message handler for message type " + mc.getMessageType());
	        }
	    }

	    return true;
	}
	
	private void startClientHeartbeat() {
	    this.hbPublisher = appContext.getBean(IClientHeartbeatPublisher.class);
        this.hbPublisher.addListener(this);
        if (!hbPublisher.startPublishing()) {
            tracer.error("Unable to start client heartbeat");
        } else {
            tracer.info("Client heartbeat started for " , hbPublisher.getClientId());
        }

	}

	/**
	 * Creates the message service subscribers for this message router.
	 * 
	 * @return true if subscribers created; false if there were errors
	 */
	private boolean createSubscribers() {

		boolean connectedJms = false;
		
		adjustSubscriptionTopics(this.responderProps.getTopics());

		try {
			final IMessageClientFactory clientFactory = appContext.getBean(IMessageClientFactory.class);
	
			for (final String topic : appContext.getBean(IGeneralContextInformation.class).getSubscriptionTopics())
			{

				final StringBuilder filter = new StringBuilder();
				final String  sc = StringUtil.emptyAsNull(MessageFilterMaker.createFilterFromContext(appContext));
				
				if (sc != null)
				{
					filter.append(sc);
				}

				final String mt = StringUtil.emptyAsNull(MessageFilterMaker.createFilterForMessageTypes(messageTypes));

				if (mt != null)
				{
					if (filter.length() > 0)
					{
						filter.append(AND);
					}

					filter.append(mt);
				}
				//don't subscribe to the same topic twice
				if(this.subscribers.get(topic) != null)
				{
					continue;
				}

				jmsTracer.info("Subscribing to topic " , topic , " with filter " , filter);

			   final IQueuingMessageHandler tempSubscriber = appContext.getBean(IQueuingMessageHandler.class, 
			           this.queueSize);
			   tempSubscriber.setSubscription(topic, filter.toString(), true);

				// If we get here we did not take the exception path and the message service is up.
				this.initialized = true;
				tempSubscriber.addListener(this);
				tempSubscriber.start();
				connectedJms = true;
				this.subscribers.put(topic, tempSubscriber);
				writeLog("MessageRouter is now listening on message topic " + topic);
			}
		}
		catch (final MessageServiceException e)
		{
			connectedJms = false;
			writeError("Cannot initialize MessageRouter due to a messaging problem", e);
		}

		return connectedJms;
	}
	
	private void adjustSubscriptionTopics(final List<TopicNameToken> tokens) {
	    final Collection<String> result = new TreeSet<>();
	    final IGeneralContextInformation info = appContext.getBean(IGeneralContextInformation.class);
	    final Collection<String> applicationTopics = info.getSubscriptionTopics();
	    
	    if (applicationTopics.isEmpty()) {
	        for (final TopicNameToken t: tokens) {
	            final String topicName = ContextTopicNameFactory.getTopicNameFromConfigValue(appContext, t);
	            final boolean topicIsSse = t.isSse();

	            //this will prevent us from creating a topic with the name "sse" when we don't want to
                if ((appContext.getBean(IVenueConfiguration.class).getVenueType().isOpsVenue()
                        || (!appContext.getBean(SseContextFlag.class).isApplicationSse() && !appContext.getBean(MissionProperties.class).missionHasSse()))
                        && topicIsSse)
	            {
	                continue;
	            }
	            result.add(topicName);
	        }
	        info.setSubscriptionTopics(result);
	    } 
	}

	/**
	 * Close subscribers with extreme prejudice, because the message service may be down, and
	 * reset member variables to the state needed to restart the subscribers.
	 */
	private void closeSubscribersNoDisconnect() {
		final Iterator<String> subscriberIt = this.subscribers.keySet().iterator();
		while (subscriberIt.hasNext()){
			final String subscriberName = subscriberIt.next();
			if (this.subscribers.get(subscriberName) != null) {
				this.subscribers.get(subscriberName).shutdown(true, false);
			}
		}
		this.subscribers.clear();
		this.initialized = false;

		writeError("Message service subscriber connections on all monitored topics were lost");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void onMessage(final IExternalMessage m)
	{
		if (!this.initialized) {
			throw new IllegalStateException("MessageRouter has not been initialized");
		}
		if (this.shuttingDown) {
			return;
		}
		
		/* TODO - Somewhere here or in a base MessageHandler we need
		 * to check messages for changes in context that are incompatible with the 
		 * previous context. We don't want to be switching venues, spacecraft, or
		 * dictionaries mid-stream in our clients. 
		 */
		try {
			final IMessageType type = externalMessageUtil.getInternalType(m);
			writeLog("MessageRouter received message of type " + type);

            if (IMessageType.matches(type, SessionMessageType.EndOfSession)) {
				notifyEndOfContext(m);
				if (!this.routeEndOfContext) {
					return;
				}
			}
			final IMessageHandler handler = this.typesToHandler.get(type);
			if (handler == null) {
				return;
			}
			writeLog("MessageRouter is sending message to handler " +
			        handler.getClass().getName());
			handler.handleMessage(m);
			
		} catch (final MessageServiceException e) {
			writeError("MessageRouter encountered an error processing message: " +
					e.toString(), e);
		}
	}

	/**
	 * Shuts down message handling and invokes shutdown method on all handlers.
	 */
	public synchronized void shutdown() {
		writeLog("MessageRouter is shutting down handlers");
		this.shuttingDown = true;

		final Collection<IMessageHandler> col = this.typesToHandler.values();
		final Iterator<IMessageHandler> it = col.iterator();
		while (it.hasNext()) {
		    it.next().shutdown();
		}
		this.typesToHandler.clear();
		this.handlerInstances.clear();

		if (this.hbPublisher != null) {
			this.hbPublisher.stopPublishing();
		}

        writeLog(MessageAppConstants.LISTENERS_DOWN_MESSAGE);
		final Iterator<String> subscriberIt = this.subscribers.keySet().iterator();
		while (subscriberIt.hasNext()){
			final String subscriberName = subscriberIt.next();
			if (this.subscribers.get(subscriberName) != null) {
				writeLog("CLOSING " + subscriberName);
				this.subscribers.get(subscriberName).shutdown(false, true);
			}

		}
		/*
		 * Change to use a constant for shutdown message so chill_down can listen for it.
		 */
		writeLog(MessageAppConstants.MESSAGE_ROUTER_DOWN_MESSAGE);
	}

	/**
	 * Notifies all handlers when an End Of Context message is received.
	 * @param m the Message containing an End of Context message
	 */
	private void notifyEndOfContext(final IExternalMessage m) {
		if (!this.initialized) {
			throw new IllegalStateException("MessageRouter has not been initialized");
		}
		try {
			final jpl.gds.shared.message.IMessage[] messages = externalMessageUtil.instantiateMessages(m);
			if (messages != null) {
                if (!(messages[0] instanceof IEndOfContextMessage)) {
					writeError("MessageRouter expected EndOfContext message, got "
							+ messages[0].getType() + " message");
					return;
				}
				final Collection<IMessageHandler> allLists = this.typesToHandler.values();
				final Iterator<IMessageHandler> allListIt = allLists.iterator();
				while (allListIt.hasNext()) {
					 allListIt.next().handleEndOfContext((IEndOfContextMessage)messages[0]);				
				}
			} else {
				writeError("MessageRouter expected end of context message, got null message");
			}
		} catch (final Exception e) {
			writeError("MessageRouter could not parse end of context message", e);
		}
	}

	/**
	 * Logs a timestamped message to standard output if verbose mode is enabled.
	 * @param message the message text to log
	 */
	protected void writeLog(final String message) {
		if (this.verbose) {
            tracer.info(message);
		}
	}

	/**
	 * Logs a timestamped message to standard error.
	 * @param message the message text to log
	 */
	protected void writeError(final String message) {
        tracer.error(message);
	}
	
	/**
     * Logs a timestamped message to standard error.
     * @param message the message text to log
	 * @param e exception that caused this error
     */
    protected void writeError(final String message, final Throwable e) {
        tracer.error(message, e);
    }

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.message.api.external.IClientHeartbeatListener#publicationFailed()
	 */
	@Override
	public void publicationFailed() {

		// Client heartbeat has failed, so the message service has gone down. Kill all the
		// subscribers.
		writeError("Message service subscriber connections on all monitored topics were lost");
		closeSubscribersNoDisconnect();	
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.message.api.external.IClientHeartbeatListener#publicationRegained()
	 */
	@Override
	public void publicationRegained() {

		// Client heartbeat regained, so the message service is back up. Recreate the subscribers
		// and resume message receipt.
		this.initialized = createSubscribers();
		if (this.initialized) {
			writeLog("Message service subscriber connections on all monitored topics were recreated");
		} else {
			writeError("Message service subscribers could not be recreated after message service reconnect");
		}		
	}
}
