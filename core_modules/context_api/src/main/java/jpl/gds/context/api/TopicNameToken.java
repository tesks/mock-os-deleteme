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
package jpl.gds.context.api;

import java.util.Arrays;
import java.util.List;

/**
 * An enumeration of topic name tokens. These tokens are used in config files
 * as stand-ins for actual topic names.
 * 
 *
 * @since R8
 *
 */
public enum TopicNameToken {
    /**
     * The root flight application topic.
     */
    APPLICATION,
    /**
     * The EVR data subtopic of the flight application topic.
     */
    APPLICATION_EVR("evr"),
    /**
     * The EHA data subtopic of the flight application topic.
     */
    APPLICATION_EHA("eha"),
    /**
     * The PRODUCT data subtopic of the flight application topic.
     */
    APPLICATION_PRODUCT("product"),
    /**
     * The COMMAND data subtopic of the flight application topic.
     */
    APPLICATION_COMMAND("command"),
    /**
     * The FRAME data subtopic of the flight application topic.
     */
    APPLICATION_FRAME("frame"),
    /**
     * The PACKET data subtopic of the flight application topic.
     */
    APPLICATION_PACKET("packet"),
    /**
     * The PDU data subtopic of the flight application topic.
     */
    APPLICATION_PDU("pdu"),
    /**
     * The CFDP data subtopic of the flight application topic.
     */
    APPLICATION_CFDP("cfdp"),
    /**
     * The alarm topic.
     */
    APPLICATION_ALARM("alarm"),
    /**
     * The Station  topic.
     */
    APPLICATION_STATION("station"),
    /**
     * The root SSE application topic.
     */
    APPLICATION_SSE,
    /**
     * The EVR data subtopic of the SSE application topic.
     */
    APPLICATION_SSE_EVR("evr"),
    /**
     * The EHA data subtopic of the SSE application topic.
     */
    APPLICATION_SSE_EHA("eha"),
    /**
     * The COMMAND data subtopic of the SSE application topic.
     */
    APPLICATION_SSE_COMMAND("command"),
    /**
     * The PACKET data subtopic of the SSE application topic.
     */
    APPLICATION_SSE_PACKET("packet"),
    /**
     * The SSE Alarm topic.
     */
    APPLICATION_SSE_ALARM("alarm"),
    /**
     * The general topic.
     */
    GENERAL("general"),
    /**
     * The client topic.
     */
    CLIENT("client"),
    /**
     * The perspective topic.
     */
    PERSPECTIVE("perspective");
    
    private String topicNameComponent;
    
    
    /** Topic component delimiter */
    public static final String DELIMITER = ".";

    
    /**
     * Constructor.
     */
    private TopicNameToken() {
        //do nothing
    }
    
    /**
     * Constructor that takes a component name. This is a token used in the topic
     * name itself to distinguish it from other topics.
     * 
     * @param nameComponent topic component name associated with this topic.
     */
    private TopicNameToken(final String nameComponent) {
        this.topicNameComponent = nameComponent;
    }
    
    /**
     * Gets the topic component name associated with this topic.
     * @return topic component name
     */
    public String getTopicNameComponent() {
        return this.topicNameComponent;
    }
    
    /**
     * Gets the data subtopic for the given application/root topic by appending
     * the topic component name. 
     * @param applicationTopic the root application topic name
     * @return the associated data subtopic name
     */
    public String getApplicationDataTopic(final String applicationTopic) {
        return applicationTopic + DELIMITER + this.topicNameComponent;
    }
    
    /**
     * Indicates if this topic name token is for an SSE topic.
     * 
     * @return true if the topic token is for SSE, false if not
     */
    public boolean isSse() {
        switch(this) {
            case APPLICATION_SSE:
            case APPLICATION_SSE_COMMAND:
            case APPLICATION_SSE_EHA:
            case APPLICATION_SSE_EVR:
            case APPLICATION_SSE_PACKET:
            case APPLICATION_SSE_ALARM:
                return true;
            default:
                return false;
            
        }
    }
    
    /**
     * Returns a list of all data subtopic tokens for flight or SSE.
     * 
     * @param forSse
     *            true to return the SSE list, false for flight
     * 
     * @return list of tokens
     */
    public static List<TopicNameToken> getAllDataTopicTokens(final boolean forSse) {
        if (forSse) {
            return Arrays.asList(APPLICATION_SSE_COMMAND, APPLICATION_SSE_EHA, APPLICATION_SSE_EVR,
                    APPLICATION_SSE_PACKET, APPLICATION_SSE_ALARM);
        } else {
            return Arrays.asList(APPLICATION_COMMAND, APPLICATION_EHA, APPLICATION_EVR, APPLICATION_PACKET,
                                 APPLICATION_FRAME, APPLICATION_PRODUCT, APPLICATION_PDU, APPLICATION_CFDP,
                                 APPLICATION_ALARM, APPLICATION_STATION);
        }
    }

    /**
     * Determines if the given topic appears to be a data subtopic.  If the topic
     * name ends with one of the data topic name components, this returns true. Checks
     * for match to both flight and SSE data topics.
     * 
     * @param topic topic name to check
     * @return true if the topic appears to map to a data subtopic, false if not.
     */
    public static boolean isDataTopic(final String topic) {
        if (topic == null) {
            throw new IllegalArgumentException("topic is null");
        }
        for (final TopicNameToken t: getAllDataTopicTokens(false)) {
            if (topic.endsWith(t.getTopicNameComponent())) {
                return true;
            }
        }
        
        for (final TopicNameToken t: getAllDataTopicTokens(true)) {
            if (topic.endsWith(t.getTopicNameComponent())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Indicates if the topic token represents a root topic.
     * 
     * @return true if application/root level topic, false if not
     */
    public boolean isRootTopic() {
        return this == APPLICATION || this == APPLICATION_SSE;
    }
    
    /**
     * Determines if the input topic is a potential match for this
     * topic name token based upon the trailing topic name component.
     * 
     * @param topicName name to match
     * @return true if topic name is a potential match to this token
     */
    public boolean matches(final String topicName) {
        if (isRootTopic()) {
             for (final TopicNameToken t : TopicNameToken.values()) {
                 if (t.isRootTopic()) {
                     continue;
                 }
                 if (t.matches(topicName)) {
                     return false;
                 }
             }
             return true;
        }
        else {
            return topicName.endsWith(DELIMITER + this.topicNameComponent);
        }
    }
    
}
