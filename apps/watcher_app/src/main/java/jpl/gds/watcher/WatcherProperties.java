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

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import jpl.gds.context.api.TopicNameToken;
import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.spring.context.flag.SseContextFlag;


/**
 * WatcherProperties manages configuration information specific to the Message
 * Watcher applications. A constructor is supplied that takes no responder/watcher
 * name, which can be used for dumping all watcher properties. At runtime when
 * used by an actual watcher application, this object must be constructed using
 * a specific responder/watcher name.
 */
public final class WatcherProperties extends GdsHierarchicalProperties
{
    
    /** Configured queue limit for products */
    private static final int DEFAULT_QUEUE_LIMIT = 3000;
    
    /** How long to sleep while waiting for thread to drain and exit */
    public static final long DEFAULT_JOIN_TIMEOUT = 20000;

    private final String responderName;

    private static final String PROPERTY_FILE = "watchers.properties";
    
    private static final String PROPERTY_PREFIX = "watcher.";
    private static final String HANDLER_PROPERTY_SUFFIX = ".handler";
    private static final String TOPICS_PROPERTY_SUFFIX = ".topics";
    private static final String WAIT_PROPERTY_SUFFIX = ".waitForProcess";
    private static final String FILE_EXCHANGE_PROPERTY_SUFFIX = ".useFileExchange";
    private static final String ROUTE_END_PROPERTY_SUFFIX = ".routeEndOfSession";
    private static final String QUEUE_LIMIT_PROPERTY_SUFFIX = ".queueLimit";
    private static final String DRAIN_TIME_PROPERTY_SUFFIX = ".drainTime";
    private static final String USE_PATTERNS_PROPERTY_SUFFIX = ".useProductTypePatterns";
    private static final String PRODUCT_TYPE_PROPERTY_PIECE = ".productType.";


    /**
     * Constructor that loads the default properties file.
     * with responder name "none".
     */
    public WatcherProperties() {
        this("none");
    }
    
    /**
     * Test Constructor
     * 
     * @param responderName
     *            the responder name
     */            
    public WatcherProperties(final String responderName) {
        this(responderName, new SseContextFlag());
    }

    /**
     * Creates an instance of WatcherProperties that loads the default properties file.
     * 
     * @param responderName
     *            the responder name
     * @param sseFlag
     *            The SSE context flag
     */
    public WatcherProperties(final String responderName, final SseContextFlag sseFlag) {
        super(PROPERTY_FILE, sseFlag);

        this.responderName = responderName;
    }
    
    /**
     * Indicates whether product type patterns are supported by the current product message responder.
     * @return true if patterns supported
     */
    public boolean useProductTypePatterns() {
        return getBooleanProperty(PROPERTY_PREFIX + responderName + USE_PATTERNS_PROPERTY_SUFFIX, false);
    }


    /**
     * Get the handler name for the responder's specific product type.
     * We got that from the configured APIDs.
     *
     * @param type Product type
     *
     * @return Handler name
     */
    public String getHandlerNameForProductType(final String type)
    {
        String handlerProperty = PROPERTY_PREFIX + responderName +
                                       PRODUCT_TYPE_PROPERTY_PIECE +
                                       type + HANDLER_PROPERTY_SUFFIX;
   
        String handler = getProperty(handlerProperty, null);
        
        if (handler == null) {
            handlerProperty = PROPERTY_PREFIX + responderName +
                    PRODUCT_TYPE_PROPERTY_PIECE +
                    type.toUpperCase() + HANDLER_PROPERTY_SUFFIX;
            
            handler = getProperty(handlerProperty, null);
        }

        if (handler == null)
        {
            throw new IllegalArgumentException(
                          "Responder "                   +
                          responderName                 +
                          " has no product handler for " +
                          type);
        }

        return handler;
    }
    
    /**
     * Gets the in-memory message queue length for queuing message responders.
     * 
     * @return length
     */
    public int getQueueLimit() {
        return getIntProperty(PROPERTY_PREFIX + 
                responderName   +
                QUEUE_LIMIT_PROPERTY_SUFFIX, DEFAULT_QUEUE_LIMIT);
    }
    
    /**
     * Gets the queue drain timeout in milliseconds, for queuing message responders.
     * 
     * @return drain milliseconds
     */
    public long getQueueDrainTime() {
        return getLongProperty(PROPERTY_PREFIX + 
                responderName   +
                DRAIN_TIME_PROPERTY_SUFFIX, DEFAULT_JOIN_TIMEOUT);
    }

    /**
     * Indicates whether the end of context message should be routed to message handlers 
     * like any other message.
     * 
     * @return true to route end of context to handler, false to not
     */
    public boolean routeEndOfContext() {
        return getBooleanProperty(PROPERTY_PREFIX + 
                responderName   +
                ROUTE_END_PROPERTY_SUFFIX, false);
    }
    
    /**
     * For responders that spawn processes in response to messages. Indicates whether the
     * responder should wait for the subprocess to complete, or just fire-and-forget the
     * subprocess.
     * 
     * @return true to wait for subprocesses, false to not
     */
    public boolean waitForProcess() {
        return getBooleanProperty(PROPERTY_PREFIX + 
                responderName   +
                WAIT_PROPERTY_SUFFIX, false);
    }
    
    /**
     * For responders that exchange messages with other applications or subprocesses, indicates
     * whether a file should be used for the exchange of message data, as opposed to providing
     * message content over a socket or on a spawned command line.
     * 
     * @return true to use a message file, false to not
     */
    public boolean useFileExchange() {
        return getBooleanProperty(PROPERTY_PREFIX + 
                responderName   +
                FILE_EXCHANGE_PROPERTY_SUFFIX, false);
    }
    
    /**
     * Gets a custom message responder property.
     * 
     * @param propName name of the property
     * @param defaultVal default value; may be be null
     * @return property value or the default
     */
    public String getCustomProperty(final String propName, final String defaultVal) {
        return getProperty(PROPERTY_PREFIX +  responderName   + "." + propName, defaultVal);
    }

    /**
     * Gets the list of topics/topic tokens for the responder to subscribe to.
     * 
     * @return list of topics and/or topic config tokens
     */
    public List<TopicNameToken> getTopics() {
        final String propName = PROPERTY_PREFIX + responderName + TOPICS_PROPERTY_SUFFIX;
        final List<String> topicTokenStrs = getListProperty(propName, TopicNameToken.APPLICATION.name(), ",");
        
        final Set<TopicNameToken> nameVals = new TreeSet<>();
        for (final String tokenStr : topicTokenStrs) {
            try {
                final TopicNameToken name = TopicNameToken.valueOf(tokenStr);
                nameVals.add(name);
            } catch (final IllegalArgumentException e) {
                log.error("Illegal topic token name " + topicTokenStrs + 
                        " for configuration property "+ propName + ". Value will be ignored.");
            }
        }
        
        return new LinkedList<>(nameVals);
    }
    
    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }

}
