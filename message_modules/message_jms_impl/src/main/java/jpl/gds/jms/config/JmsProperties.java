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
package jpl.gds.jms.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import jpl.gds.context.api.TopicNameToken;
import jpl.gds.message.api.external.ExternalDeliveryMode;
import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.config.JndiProperties;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.message.IMessageConfiguration;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.spring.context.flag.SseContextFlag;


/**
 * JmsConfiguration is responsible for loading and tracking JMS-related
 * configuration properties for the AMPCS multimission core. This class contains
 * a nested JndiProperties member. The configuration is loaded statically when
 * the class is loaded. This is a singleton class.
 */
public final class JmsProperties extends GdsHierarchicalProperties {
    
    
    /**
     * An enumeration of message preparation class types.
     */
    public enum PreparerClassType {
        /**
         * Preparer for one simple text message.
         */
        SIMPLE_TEXT,
        /**
         * Preparer for a batch of text messages.
         */
        BATCHING_TEXT,
        /**
         * Preparer for one simple binary message.
         */
        SIMPLE_BINARY,
        /**
         * Preparer for a batch of binary messages.
         */
        BATCHING_BINARY,
        /**
         * Custom preparer.
         */
        CUSTOM;
        
    }
 
    private static final String PROPERTY_FILE = "jms.properties";
    
    /* Default values for properties */
    private static final boolean DEFAULT_SPILL_ENABLE = true;
    private static final boolean DEFAULT_SPILL_KEEP = false;
    private static final int DEFAULT_SPILL_QUEUE_SIZE = Integer.MAX_VALUE;
    private static final long DEFAULT_SPILL_OUTPUT_WAIT = 100L;
    private static final long MINIMUM_SPILL_OUTPUT_WAIT = 10L;
    private static final int DEFAULT_PUB_QUEUE_SIZE = 10000;   
    private static final int DEFAULT_RED_LEVEL = 100;
    private static final int DEFAULT_YELLOW_LEVEL = 80;
    private static final int DEFAULT_SERVER_PENDING_LIMIT = 2048;
    private static final int DEFAULT_CLIENT_PREFETCH = 2048;
    private static final long DEFAULT_RECONNECT_WAIT = 10000L;
    private static final long DEFAULT_HEARTBEAT_INTERVAL = 10000L;
    private static final long DEFAULT_CLOSE_TIMEOUT = 5000L;
    private static final int DEFAULT_TRANSLATED_QUEUE_SIZE = 5000;
    private static final long DEFAULT_TIME_TO_LIVE = 120000;
    private static final long DEFAULT_FLUSH_TIMEOUT = 3000;
    
    private static final String PROPERTY_PREFIX = "jms.";
    private static final String PROPERTY_PREFIX_INTERNAL = PROPERTY_PREFIX + "internal.";
    
    private static final String PROVIDER_BLOCK = PROPERTY_PREFIX_INTERNAL + "provider.";
    private static final String RT_PUB_PROVIDER_PROPERTY = PROVIDER_BLOCK + "name";
    private static final String TOPIC_CONNECTION_FACTORY_PREFIX = PROVIDER_BLOCK + "topicConnectionFactoryName.";
    private static final String QUEUE_CONNECTION_FACTORY_PREFIX = PROVIDER_BLOCK + "queueConnectionFactoryName.";
    
    private static final String SERVER_BLOCK = PROPERTY_PREFIX_INTERNAL + "server.";
    private static final String SERVER_PENDING_LIMIT_PROPERTY= SERVER_BLOCK + "pendingMessageLimit";
    
    private static final String CLIENT_BLOCK = PROPERTY_PREFIX + "client.";
    private static final String CLIENT_BLOCK_INTERNAL = PROPERTY_PREFIX_INTERNAL + "client.";
    private static final String CLIENT_RECONNECT_WAIT_PROPERTY = CLIENT_BLOCK_INTERNAL + "reconnectInterval";
    private static final String CLIENT_HEARTBEAT_INTERVAL_PROPERTY = CLIENT_BLOCK_INTERNAL + "heartbeatInterval";  
    
    private static final String SUBSCRIBER_BLOCK = CLIENT_BLOCK_INTERNAL + "subscriber.";
    private static final String SUB_PREFETCH_PROPERTY = SUBSCRIBER_BLOCK + "prefetchLimit";
    private static final String SUB_CLOSE_TIMEOUT_PROPERTY= SUBSCRIBER_BLOCK + "closeTimeout";
    
    private static final String PUBLISHER_BLOCK = CLIENT_BLOCK + "publisher.";
    private static final String PUBLISHER_BLOCK_INTERNAL = CLIENT_BLOCK_INTERNAL + "publisher.";
    private static final String ASYNC_PUB_QUEUE_RED_PERCENTAGE =  PUBLISHER_BLOCK + "queueRedLevel";
    private static final String ASYNC_PUB_QUEUE_YELLOW_PERCENTAGE = PUBLISHER_BLOCK + "queueYellowLevel";
    private static final String ASYNC_PUB_QUEUE_SIZE_PROPERTY = PUBLISHER_BLOCK_INTERNAL + "queueSize";
    
    private static final String SPILL_BLOCK = PROPERTY_PREFIX + "spill.";
    private static final String SPILL_BLOCK_INTERNAL = PROPERTY_PREFIX_INTERNAL + "spill.";
    private static final String KEEP_SPILL_FILES_PROPERTY = SPILL_BLOCK_INTERNAL + "keepOutput";
    private static final String SPILL_QUEUE_SIZE_PROPERTY = SPILL_BLOCK_INTERNAL + "queueSize";
    private static final String SPILL_ENABLE_PROPERTY = SPILL_BLOCK + "enable";
    private static final String SPILL_OUTPUT_WAIT_PROPERTY = SPILL_BLOCK_INTERNAL + "outputWait";

    private static final String PORTAL_PROPERTY_BLOCK = PROPERTY_PREFIX + "portal.";
    private static final String PORTAL_PROPERTY_BLOCK_INTERNAL = PROPERTY_PREFIX_INTERNAL + "portal.";
    private static final String MESSAGE_TYPE_BLOCK = PORTAL_PROPERTY_BLOCK
            + "messageType.";
    private static final String TRANSLATION_QUEUE_LEN_PROPERTY = PORTAL_PROPERTY_BLOCK_INTERNAL
            + "maxQueueLen";
    private static final String PUBLISH_TYPES_PROPERTY = PORTAL_PROPERTY_BLOCK
            + "publishTypes";
    private static final String PUBLISH_RECORDED_PREFIX = PORTAL_PROPERTY_BLOCK + "publishRecorded.";
    private static final String COMMIT_SIZE_PROPERTY = PORTAL_PROPERTY_BLOCK_INTERNAL
            + "commitSize";
    private static final String DEFAULT_TIME_TO_LIVE_PROPERTY = PORTAL_PROPERTY_BLOCK
            + "defaultTimeToLive";
    private static final String DEFAULT_DELIVERY_MODE_PROPERTY = PORTAL_PROPERTY_BLOCK
            + "defaultToPersistent";
    private static final String DEFAULT_TOPICS_PROPERTY = PORTAL_PROPERTY_BLOCK
            + "defaultTopics";   
    private static final String DEFAULT_PREPARER_PROPERTY = PORTAL_PROPERTY_BLOCK_INTERNAL
            + "defaultPreparer";   
    private static final String DEFAULT_BINARY_PROPERTY = PORTAL_PROPERTY_BLOCK_INTERNAL
            + "defaultToBinary";
    private static final String PORTAL_FLUSH_TIMEOUT = PORTAL_PROPERTY_BLOCK_INTERNAL + "flushInterval";
    
    private static final String TIME_TO_LIVE_PROPERTY = ".timeToLive";
    private static final String DELIVERY_MODE_PROPERTY = ".persistent";
    private static final String TOPICS_PROPERTY = ".topics";
    private static final String BATCH_SIZE_PROPERTY = ".batchSize";
    private static final String BATCH_TIMEOUT_PROPERTY = ".batchTimeout";
    private static final String MESSAGE_PREPARER_PROPERTY = ".preparer";
    private static final String BINARY_PROPERTY = ".isBinary";
    
    private static final String PUBLISH_FILL_PACKET_PROPERTY = PORTAL_PROPERTY_BLOCK + "publishFillPacket";
    private static final String PUBLISH_IDLE_FRAME_PROPERTY = PORTAL_PROPERTY_BLOCK + "publishIdleFrame";
    
    private static final String LIST_DELIM = ",";

    /**
     * Name of the environment variable that specifies which JMS provider to use.
     */
    private static final String RT_PUB_ENV_VAR = "CHILL_MESSAGE_PROVIDER";
    
    private final Map<IMessageType, PublishableMessageConfig> messageConfigs = new HashMap<IMessageType, PublishableMessageConfig>();
  
    private final JndiProperties jndiProperties;
    
    private final AtomicBoolean                               initialized                        = new AtomicBoolean(false);
    
    /**
     * Test Constructor
     */
    public JmsProperties() {
        this(new SseContextFlag());
    }

    /**
     * Creates an instance of JmsProperties using the default properties file
     * 
     * @param sseFlag
     *            The SSE context flag
     */
    public JmsProperties(final SseContextFlag sseFlag) {
        super(PROPERTY_FILE, sseFlag);
        final JmsMessageProvider prov = this.getRealtimePublicationProvider();
        this.jndiProperties = new JndiProperties(prov.name().toLowerCase() + "." + 
                JndiProperties.DEFAULT_JNDI_PROPERTIES, sseFlag);
    }

    /**
     * Initialize JmsProperties object and create a Map of configuration types to external message classes
     * IMPORTANT: This method is no longer being called by the constructor. The reason for this is that,
     *            the init() method builds a table of publishable messages, it must be called after all
     *            publishable messages have been registered. This occurs in many individual beans'
     *            constructors. I have, so far, found no way to reliably delegate this initialization 
     *            (in the proper order) to spring-mvc or spring-boot. Therefore, it is now being called
     *            one time only by when the message portal is created.
     */
    public void init() {
        if (initialized.getAndSet(true)) {
            return;
        }

        final ExternalDeliveryMode defaultPersistence = getDefaultDeliveryMode();
        final PreparerClassType defaultPreparer = getDefaultPreparerType();
        final boolean defaultToBinary = defaultIsBinary();
        final long defaultFlush = getPortalFlushTimeout();
        final IMessageType[] allTypes = getPublishTypes();
        final long defaultTtl = getDefaultTimeToLive();
        final List<TopicNameToken> defaultTopics = getDefaultTopics();

        for (final IMessageType type : allTypes) {
            final boolean isPersistent = getBooleanProperty(
                    MESSAGE_TYPE_BLOCK + type.getSubscriptionTag() + DELIVERY_MODE_PROPERTY,
                    defaultPersistence == ExternalDeliveryMode.PERSISTENT);
            ExternalDeliveryMode mode = ExternalDeliveryMode.PERSISTENT;
            if (!isPersistent) {
                mode = ExternalDeliveryMode.NON_PERSISTENT;
            }
            final long ttl = getLongProperty(MESSAGE_TYPE_BLOCK + type.getSubscriptionTag()
                    + TIME_TO_LIVE_PROPERTY, defaultTtl);
            final List<TopicNameToken> topicNamesTemp = getTopicNameTokenListProperty(MESSAGE_TYPE_BLOCK
                    + type.getSubscriptionTag() + TOPICS_PROPERTY, null);
            List<TopicNameToken> topicNames = defaultTopics;
            if (topicNamesTemp != null && !topicNamesTemp.isEmpty()) {
                topicNames = topicNamesTemp;
            }
            final int batchSize = getIntProperty(MESSAGE_TYPE_BLOCK + type.getSubscriptionTag()
                    + BATCH_SIZE_PROPERTY, 1);
            final long batchTimeout = getLongProperty(MESSAGE_TYPE_BLOCK
                    + type.getSubscriptionTag() + BATCH_TIMEOUT_PROPERTY, defaultFlush);
            final String prepName = getProperty(MESSAGE_TYPE_BLOCK
                    + type.getSubscriptionTag() + MESSAGE_PREPARER_PROPERTY, null);
            PreparerClassType prepClassType = defaultPreparer;
            if (prepName != null) {
                try {
                    prepClassType = PreparerClassType.valueOf(prepName);
                } catch (final IllegalArgumentException e) {
                    // do nothing - will default
                }
            }
            final boolean isBinary = getBooleanProperty(MESSAGE_TYPE_BLOCK
                    + type + BINARY_PROPERTY, defaultToBinary);
            final PublishableMessageConfig msgConfig = new PublishableMessageConfig(type, ttl, mode,
                    topicNames.toArray(new TopicNameToken[] {}), batchSize, batchTimeout, prepClassType, isBinary);
            messageConfigs.put(type, msgConfig);
        }
    }
      
    /**
     * Returns the server pending message limit. This is the number of messages
     * kept in the server topic queue for each client. When this limit is
     * exceeded, the server will discard messages. Applies only to the ACTIVEMQ
     * provider.
     * 
     * @return pending message limit, as message count
     */
    public int getPendingMessageLimit() {
        return getIntProperty(SERVER_PENDING_LIMIT_PROPERTY, DEFAULT_SERVER_PENDING_LIMIT);
    }

    /**
     * Returns the client pre-fetch message limit. This is the maximum number of
     * messages that will be sent to a JMS client without client
     * acknowledgement. When the client dispatch queue reaches this limit
     * without the client acknowledging any messages, the server pending queue
     * begins to fill. Applies only to the ACTIVEMQ provider.
     * 
     * @return client pre-fetch size, as message count
     */
    public int getPrefetchLimit() {
        return getIntProperty(SUB_PREFETCH_PROPERTY, DEFAULT_CLIENT_PREFETCH);
    }

    /**
     * Returns the JMS Topic Connection Factory class name.
     * 
     * @return factory name; this is a base class name, without package
     */
    public String getTopicFactoryName() {
    
        return getProperty(TOPIC_CONNECTION_FACTORY_PREFIX + getRealtimePublicationProvider().toString());
    }

    /**
     * Returns the JMS Queue Connection Factory class name.
     * 
     * @return factory name; this is a base class name, without package
     */
    public String getQueueFactoryName() {

        return getProperty(QUEUE_CONNECTION_FACTORY_PREFIX + getRealtimePublicationProvider());
    }
    
    /**
     * Gets the wait interval in milliseconds between JMS reconnection attempts.
     * 
     * @return interval, milliseconds
     */
    public long getReconnectWait() {
        return Math.max(1, getLongProperty(CLIENT_RECONNECT_WAIT_PROPERTY, DEFAULT_RECONNECT_WAIT));
    }
    
    /**
     * Gets the time in milliseconds that subscribers will wait to close
     * connections. If the subscriber is very busy because messages are pouring
     * in, the JMS connection will often not close in a reasonable about of
     * time. If this amount of time elapses after a close attempt and the close
     * has not completed, the connection is is killed.
     * 
     * @return timeout, milliseconds
     */
    public long getSubscriberCloseTimeout() {
        return Math.max(1, getLongProperty(SUB_CLOSE_TIMEOUT_PROPERTY, DEFAULT_CLOSE_TIMEOUT));
    }
    
    /**
     * Gets the interval in milliseconds between client heartbeat messages.
     * Clients publish heartbeats in order to detect a lost JMS connection.
     * 
     * @return interval, milliseconds
     */
   public long getClientHeartbeatInterval() {
       return Math.max(1, getLongProperty(CLIENT_HEARTBEAT_INTERVAL_PROPERTY, DEFAULT_HEARTBEAT_INTERVAL));
   }

    /**
     * Indicates whether files containing JMS messages spilled to disk
     * by asynchronous publishers should be kept even after messages are sent.
     * 
     * @return true if spill files should be kept, false if not
     */
    public boolean isKeepSpillFilesEnabled() {
        return getBooleanProperty(KEEP_SPILL_FILES_PROPERTY, DEFAULT_SPILL_KEEP);
    }

    /**
     * Gets the maximum length (as JMS message count) of the external spill queue,
     * i.e., the maximum number of messages spilled to disk by an asynchronous
     * publisher. The value used is always the minimum of this value and the
     * configured queue size. This value is really just for testing. It can be
     * from [1, Integer.MAX_VALUE]. We use Integer.MAX_VALUE so that in the
     * default case, the value is not limited.
     * 
     * @return the spill output queue size, if not defined returns
     *         DEFAULT_SPILL_QUEUE_SIZE.
     */
    public int getMaxSpillQueueSize() {
        return Math.max(1, getIntProperty(SPILL_QUEUE_SIZE_PROPERTY, DEFAULT_SPILL_QUEUE_SIZE));
    }
    
    /**
     * Gets the size of the internal queue (as JMS message count) for
     * asynchronous publishers using the spill processor. This is how long the
     * queue can get BEFORE the publisher spills messages to disk.
     * 
     * @return the asynchronous publisher internal queue size, if not defined
     *         returns DEFAULT_PUB_QUEUE_SIZE
     */
    public int getMaxAsyncPublisherQueueSize() {
        return Math.max(1, getIntProperty(ASYNC_PUB_QUEUE_SIZE_PROPERTY,DEFAULT_PUB_QUEUE_SIZE));
    }


    /**
     * Indicates whether asynchronous publishers should spill to disk if the internal
     * queue fills.
     * 
     * @return true if spilling enabled, false otherwise
     */
    public boolean isSpillEnabled() {
        return getBooleanProperty(SPILL_ENABLE_PROPERTY, DEFAULT_SPILL_ENABLE);
    }

    /**
     * Time we wait for another record to show up on a JMS Spill processor's
     * queue. If this delay expires, we check for unspilling.
     * 
     * @return the spill output wait time in milliseconds; if not defined returns
     *         DEFAULT_SPILL_OUTPUT_WAIT.
     */
    public long getSpillOutputWait() {
        return Math.max(MINIMUM_SPILL_OUTPUT_WAIT, getLongProperty(SPILL_OUTPUT_WAIT_PROPERTY, DEFAULT_SPILL_OUTPUT_WAIT));
    }
  
    /**
     * Gets the percentage of internal asynchronous publisher queue size that
     * indicates a RED health status.
     * 
     * @return red percentage, 0 - 100.
     */
    public long getRedQueuePercentage() {
        return Math.max(0, Math.min(100, getLongProperty(ASYNC_PUB_QUEUE_RED_PERCENTAGE, DEFAULT_RED_LEVEL)));
    }

    /**
     * Gets the percentage of internal publisher queue size that indicates a
     * YELLOW health status.
     * 
     * @return red percentage, 0 - 100.
     */
    public long getYellowQueuePercentage() {
        return Math.max(0, Math.min(100, getLongProperty(ASYNC_PUB_QUEUE_YELLOW_PERCENTAGE, DEFAULT_YELLOW_LEVEL)));
    }

    /**
     * Returns whether to publish recorded messages with the given type.
     * 
     * @param type the internal message type
     * 
     * @return true if publishing recorded messages
     */
    public boolean isPublishRecorded(final IMessageType type) {
        return getBooleanProperty(PUBLISH_RECORDED_PREFIX + type.getSubscriptionTag(), false);
    }

    /**
     * Returns whether to publish fill packet messages
     * 
     * @return true if publishing fill packets
     */
    public boolean isPublishFillPacket() {
        return getBooleanProperty(PUBLISH_FILL_PACKET_PROPERTY, false);
    }

    /**
     * Returns whether to publish idle frame messages
     * 
     * @return true if publishing idle frames
     */
    public boolean isPublishIdleFrame() {
        return getBooleanProperty(PUBLISH_IDLE_FRAME_PROPERTY, false);
    }
    
    /**
     * Returns the length of translated queue.
     * 
     * @return queue length
     */
    public int getTranslatedQueueLength() {
        return getIntProperty(TRANSLATION_QUEUE_LEN_PROPERTY,
                DEFAULT_TRANSLATED_QUEUE_SIZE);
    }

    /**
     * Returns array of publish types.
     * 
     * @return publish types
     */
    public IMessageType[] getPublishTypes() {
        final List<String> tempProp = getListProperty(PUBLISH_TYPES_PROPERTY, "", LIST_DELIM);
        final List<IMessageType> tempList = new ArrayList<IMessageType>(tempProp.size());
        for (final String subTag : tempProp) {
            final IMessageConfiguration mc = MessageRegistry.getMessageConfig(subTag);
            if (mc == null) {
                TraceManager.getDefaultTracer().debug("Found publish subscription tag ", subTag, " in ", PROPERTY_FILE,
                                                      " but no matching registered message type could be found");
            } else {
                tempList.add(mc.getMessageType());
            }
        }
        return tempList.toArray(new IMessageType[tempList.size()]);
    }

    /**
     * Returns publish commit size, or 1 if undefined.
     * 
     * @return publish commit size
     */
    public int getPublishCommitSize() {
        return getIntProperty(COMMIT_SIZE_PROPERTY, 1);
    }

    /**
     * Returns the message configuration for a specific type of message.
     * 
     * @param type
     *            type of message
     * @return message configuration corresponding to the type
     */
    public PublishableMessageConfig getMessageConfig(final IMessageType type) {
        return this.messageConfigs.get(type);
    }

    /**
     * Gets the flush interval for the message portal.
     * 
     * @return message flush interval, milliseconds
     */
    public long getPortalFlushTimeout() {
        return getLongProperty(PORTAL_FLUSH_TIMEOUT, DEFAULT_FLUSH_TIMEOUT);
    }
    
    
    /**
     * Gets the list of default topics name tokens for all messages to be published to unless overridden 
     * per message type.
     * 
     * @return list of topic name tokens; these will be topic config values to feed to the CommonTopicNameFactory
     */
    public List<TopicNameToken> getDefaultTopics() {
        return getTopicNameTokenListProperty(DEFAULT_TOPICS_PROPERTY, TopicNameToken.APPLICATION.name());    
    }
    
    private List<TopicNameToken> getTopicNameTokenListProperty(final String propertyName, final String defaultVal) {
        final List<String> topicTokenStrs = getListProperty(propertyName, 
                defaultVal, LIST_DELIM);
        final Set<TopicNameToken> nameVals = new TreeSet<>();
        for (final String tokenStr : topicTokenStrs) {
            try {
                final TopicNameToken name = TopicNameToken.valueOf(tokenStr);
                nameVals.add(name);
            } catch (final IllegalArgumentException e) {
                log.error("Illegal topic token name " + topicTokenStrs + 
                        " for configuration property "+ propertyName + ". Value will be ignored.");
            }
        }
 
        return new LinkedList<>(nameVals);   
    }
    
    /**
     * Gets the default time to live for all messages.
     * 
     * @return time to live
     */
    public long getDefaultTimeToLive() {
       return getLongProperty(DEFAULT_TIME_TO_LIVE_PROPERTY, DEFAULT_TIME_TO_LIVE);
    }
    
    /**
     * Gets the default delivery mode for all messages.
     * 
     * @return DeliveryMode.PERSISTENT or DeliveryMode.NON_PERSISTENT
     */
    public ExternalDeliveryMode getDefaultDeliveryMode() {
        final boolean defaultPersistence = getBooleanProperty(DEFAULT_DELIVERY_MODE_PROPERTY, true);
        if (defaultPersistence == false) {
            return ExternalDeliveryMode.NON_PERSISTENT;
        }
        return ExternalDeliveryMode.PERSISTENT;
    }
    
    /**
     * Gets the default message preparer type for all messages.
     * 
     * @return preparer type
     */
    public PreparerClassType getDefaultPreparerType() {
        final String prepName = getProperty(DEFAULT_PREPARER_PROPERTY, PreparerClassType.SIMPLE_TEXT.name());
        PreparerClassType prepClassType = PreparerClassType.SIMPLE_TEXT;
        try {
            prepClassType = PreparerClassType.valueOf(prepName);
        } catch (final IllegalArgumentException e) {
            // do nothing; will default
        }
        return prepClassType;
    }
    
    /**
     * Indicates whether the default publication mode is binary.
     * 
     * @return true if the default is binary, false if text
     */
    public boolean defaultIsBinary() {
        return getBooleanProperty(DEFAULT_BINARY_PROPERTY, false);
    }
    
    /**
     * Gets the configured message provider. Note that the value in the
     * config file can be overridden by an environment variable.
     * 
     * @return JmsMessageProvider
     */
    public JmsMessageProvider getRealtimePublicationProvider() {
        
        JmsMessageProvider provider = JmsMessageProvider.ACTIVEMQ;
        
        final String envStr = System.getenv(RT_PUB_ENV_VAR);
        if (envStr != null) {
            try {
                provider = Enum.valueOf(JmsMessageProvider.class, envStr); 
                TraceManager.getDefaultTracer().debug("Message provider defined by env variable ", RT_PUB_ENV_VAR,
                                                      " is ", provider);

                return provider;
            } catch (final IllegalArgumentException e) {
                TraceManager.getDefaultTracer().debug("Message provider setting ", envStr, " found in env variable ",
                                                      RT_PUB_ENV_VAR, " is invalid; defaulting to configured value");
             }
             
        }
        
        final String val = getProperty(RT_PUB_PROVIDER_PROPERTY, JmsMessageProvider.ACTIVEMQ.toString());
        try {
            provider = JmsMessageProvider.valueOf(val);
        } catch (final IllegalArgumentException e) {
            TraceManager.getDefaultTracer().debug("Message provider setting ", val, " found in ", PROPERTY_FILE,
                                                  " is invalid; defaulting to ", provider);

        }
        return provider;
    }

    /**
     * Gets the JNDI properties object to be used for making JMS connections
     * via a naming service.
     * 
     * @return JndiProperties object
     */
    public JndiProperties getJndiProperties() {
        return jndiProperties;
    }
    
    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }
}

