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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.util.HostPortUtility;


/**
 * A factory for generating messaging topic names based on information to
 * be found in the application context.
 * 
 * 
 *
 */
public class ContextTopicNameFactory {

    private static final String TOPIC_PREFIX = "mpcs";
    private static final String REQUIRED_TOPIC_PREFIX = TOPIC_PREFIX + TopicNameToken.DELIMITER;
    private static final String THE_VALUES = "The value(s) for option --";
    private static final String NULL_VENUE = "Null venue";
    private static final String NULL_MISSION = "Null mission";
    private static final String NULL_APP_CONTEXT = "Null app context";

    /**
     * Shared logger.
     */
    protected static Tracer logger = TraceManager.getDefaultTracer();

    private ContextTopicNameFactory() {
        // enforces static nature.
    }
    
    /**
     * Creates a flight venue-specific topic based upon current context/session parameters.
     * 
     * @param venueType the current venue type
     * @param venueName the current venue name
     * @param streamId the current downlink stream ID, for testbed/ATLO venues
     * @param subtopic the current messaging subtopic
     * @param mission the current mission
     * 
     * @return topic name
     */
    protected static String buildVenueTopic(final VenueType venueType, final String venueName,
            final DownlinkStreamType streamId, final String subtopic, final String mission) {
        if (venueType == null) {
            throw new IllegalArgumentException(NULL_VENUE);
        } else if (mission == null) {
            throw new IllegalArgumentException(NULL_MISSION);
        } 

        final StringBuilder topic = new StringBuilder(128);

        topic.append(getTopicPrefix(venueType, mission.toLowerCase()));
         
        
        switch (venueType) {
        case TESTSET:
            topic.append(venueName);
            break;

        case TESTBED:
        case ATLO:
            topic.append(venueName);
            topic.append(TopicNameToken.DELIMITER);

            if (streamId != null)
            {
                topic.append(streamId);
            }
            else
            {
                topic.append(DownlinkStreamType.NOT_APPLICABLE);
            }

            break;

        case CRUISE:
        case SURFACE:
        case ORBIT:
        case OPS:
            /* Removed anything to do with OPS
             * stream IDs. In OPS, only the subtopic is appended, if configured.
             */
            if (subtopic != null) {
                topic.append(subtopic);
            }
         
            break;

        default:
            throw new IllegalArgumentException("Unrecognized venue type: "
                    + venueType.toString());
        }

        return (topic.toString());
    }

    /**
     * Creates an SSE venue-specific topic based upon current context/session parameters.
     * 
     * @param venueType the current venue type
     * @param venueName the current venue name
     * @param subtopic the current messaging subtopic
     * @param mission the current mission
     * 
     * @return topic name
     */
    protected static String buildSseVenueTopic(final VenueType venueType, final String venueName,
            final String subtopic, final String mission) {
        if (venueType == null) {
            throw new IllegalArgumentException(NULL_VENUE);
        } else if (mission == null) {
            throw new IllegalArgumentException(NULL_MISSION);
        }

        final StringBuilder topic = new StringBuilder(128);

        topic.append(getTopicPrefix(venueType, mission.toLowerCase()));
        switch(venueType) {
            case TESTSET:
                topic.append(venueName);
                break;

            case TESTBED:
            case ATLO:
                topic.append(venueName);
                break;

            case CRUISE:
            case SURFACE:
            case ORBIT:
            case OPS:
                /* R8 Refactor - No idea why we have this handling since we support subtopic
                 * only for OPS and do not support SSE in OPS. I am leaving this only to match previous
                 * behavior.
                 */
                if (subtopic != null) {
                    topic.append(subtopic);
                }
             
                break;

            default:
                throw new IllegalArgumentException("Unrecognized venue type: "
                        + venueType.toString());
            }
        
        return (topic.toString());
    }


    /**
     * Creates a flight venue-specific topic based upon current context/session parameters
     * and the current default mission.
     * 
     * @param venueType
     *            the current venue type
     * @param venueName
     *            the current venue name
     * @param streamId
     *            the current downlink stream ID, for testbed/ATLO venues
     * @param subtopic
     *            the current messaging subtopic
     * @param sseFlag
     *            the current SSE context flag
     * 
     * @return topic name
     */
    public static String getVenueTopic(final VenueType venueType, final String venueName,
                                       final DownlinkStreamType streamId, final String subtopic,
                                       final SseContextFlag sseFlag) {

        final String mission = GdsSystemProperties.getSystemMissionIncludingSse(sseFlag.isApplicationSse());
                
        String topicString = buildVenueTopic(venueType, venueName, streamId, subtopic, mission);

        topicString = formatTopicString(topicString);

        logger.debug(mission + " session topic is " + topicString);

        return (topicString);
    }

    /**
     * Creates an SSE venue-specific topic based upon current context/session parameters
     * for the current default mission.
     * 
     * @param venueType the current venue type
     * @param venueName the current venue name
     * @param subtopic the current messaging subtopic
     * 
     * @return topic name
     */
    public static String getSseVenueTopic(final VenueType venueType, final String
            venueName, final String subtopic) {
      
        final String mission = GdsSystemProperties.getSseNameForSystemMission();
        String topicString = buildSseVenueTopic(venueType, venueName, subtopic, mission);

        topicString = formatTopicString(topicString);

        logger.debug(mission + " session SSE topic is " + topicString);

        return (topicString);
    }

    /**
     * Returns the general topic from the venue type.
     * 
     * @param vt the current venue type
     * @return general topic name
     */
    public static String getGeneralTopic(final VenueType vt) {
        if (vt == null) {
            throw new IllegalArgumentException(NULL_VENUE);
        }

        final StringBuilder topic = new StringBuilder(128);

        final String mission = GdsSystemProperties.getSystemMission();
        topic.append(getTopicPrefix(vt, mission));
        topic.append(TopicNameToken.GENERAL.getTopicNameComponent());

        final String topicString = formatTopicString(topic.toString());

        logger.debug("General topic is " + topicString);

        return (topicString);
    }
    
    /**
     * Returns the general topic without a venue qualification.
     * 
     * @return general topic name
     */
    public static String getGeneralTopic() {

        final StringBuilder topic = new StringBuilder(128);

        final String mission = GdsSystemProperties.getSystemMission();
        topic.append(getTopicPrefix(null, mission));
        topic.append(TopicNameToken.GENERAL.getTopicNameComponent());

        final String topicString = formatTopicString(topic.toString());

        logger.debug("General topic is " + topicString);

        return (topicString);
    }

    /**
     * Returns perspective topic for the specified venue.
     * 
     * @param vt the current venue type
     * @return perspective topic
     */
    public static String getPerspectiveTopic(final VenueType vt) {
        if (vt == null) {
            throw new IllegalArgumentException(NULL_VENUE);
        }

        final StringBuilder topic = new StringBuilder(128);

        final String mission = GdsSystemProperties.getSystemMission();
        topic.append(getTopicPrefix(vt, mission));
        topic.append(TopicNameToken.PERSPECTIVE.getTopicNameComponent());

        final String topicString = formatTopicString(topic.toString());

        logger.debug("Perspective topic is " + topicString);

        return (topicString);
    }

    /**
     * Gets a topic prefix based upon venue type and mission.
     * 
     * @param venueType the current venue type
     * @param mission the current mission
     * @return topic prefix
     */
    protected static String getTopicPrefix(final VenueType venueType,
            final String mission) {
    	
		/*
		 * We cannot require a venue type to publish to the general
		 * topic because some messages are not session based.
		 */
        if (mission == null) {
            throw new IllegalArgumentException(NULL_MISSION);
        }

        final StringBuilder prefix = new StringBuilder(128);

        prefix.append(TOPIC_PREFIX);
        prefix.append(TopicNameToken.DELIMITER);
        prefix.append(mission);
        prefix.append(TopicNameToken.DELIMITER);

        if (venueType != null) {
	        final String topicNameDelim = venueType.getTopicNameDelimiter();
	        if (topicNameDelim != null) {
	            prefix.append(topicNameDelim);
	            prefix.append(TopicNameToken.DELIMITER);
	        }
        }

        return (prefix.toString());
    }


    /**
     * Returns the topic name from the config value based on the passed session
     * configuration. The config values represent a specific token found in
     * configuration files and are defined as constants in this class
     * 
     * @param missionProps
     *            the current MissionProperties object
     * @param vt
     *            the current venue type
     * @param venueName
     *            the current venue name
     * @param streamId
     *            the current downlink stream ID, for testbed/ATLO venues
     * @param subtopic
     *            the current messaging subtopic
     * @param configValue
     *            config value
     * @param sseFlag
     *            The SSE context flag
     * @return topic name
     */
    public static String getTopicNameFromConfigValue(final MissionProperties missionProps, final VenueType vt,
                                                     final String venueName, final DownlinkStreamType streamId,
                                                     final String subtopic, final TopicNameToken configValue,
                                                     final SseContextFlag sseFlag) {
        
        if (configValue == null) {
            throw new IllegalArgumentException("config value cannot be null");
        }
        switch(configValue) {
            case APPLICATION:
                if (sseFlag.isApplicationSse()) {
                    return (getSseVenueTopic(vt, venueName, subtopic));
                } else {
                    return (getVenueTopic(vt, venueName, streamId, subtopic, sseFlag));
                }
            case APPLICATION_COMMAND:
            case APPLICATION_EHA:
            case APPLICATION_EVR:
            case APPLICATION_FRAME:
            case APPLICATION_PACKET:
            case APPLICATION_PDU:
            case APPLICATION_PRODUCT:
            case APPLICATION_CFDP:
            case APPLICATION_ALARM:
            case APPLICATION_STATION:
                if (sseFlag.isApplicationSse()) {
                    return (configValue.getApplicationDataTopic(getSseVenueTopic(vt, venueName, subtopic)));
                } else {
                    return (configValue.getApplicationDataTopic(getVenueTopic(vt, venueName, streamId, subtopic,
                                                                              sseFlag)));
                }
            case APPLICATION_SSE:
                if (sseFlag.isApplicationSse() || missionProps.missionHasSse()) {
                    return (getSseVenueTopic(vt, venueName, subtopic));
                } else {
                    return null;
                }
            case APPLICATION_SSE_COMMAND:
            case APPLICATION_SSE_EHA:
            case APPLICATION_SSE_EVR:
            case APPLICATION_SSE_PACKET:
            case APPLICATION_SSE_ALARM:
                if (sseFlag.isApplicationSse() || missionProps.missionHasSse()) {
                    return (configValue.getApplicationDataTopic(getSseVenueTopic(vt, venueName, subtopic)));
                } else {
                    return null;
                }
            case CLIENT:
                return getClientTopic();
            case GENERAL:
                return (getGeneralTopic(vt));
            case PERSPECTIVE:
                return (getPerspectiveTopic(vt));
            default:
                return null;
            
        }
        
    }

    /**
     * Utility method to strip unsupported characters out of a topic name.
     * 
     * @param input the input topic name
     * @return the adjusted topic name
     */
    protected static String formatTopicString(final String input) {
        if (input == null) {
            return (input);
        }

        return (input.toLowerCase().replaceAll("[ ]{1,}", "_"));
    }
    
    /**
     * Creates a venue name.
     * 
     * @param vt the current venue name
     * @param tbName the current testbed name, for venues that support testbeds
     * @param venueId the venue ID string; will be defaulted if null
     * @return venue name
     */
    public static String getVenueName(final VenueType vt, final String tbName, final String venueId) {
        String venueName = "";
        if (vt.hasTestbedName()) {
            venueName = tbName;
        } else if (!vt.isOpsVenue()) {
            venueName = venueId == null ? (HostPortUtility.getLocalHostName() + TopicNameToken.DELIMITER + 
                    GdsSystemProperties.getSystemUserName()) : venueId;
        } 
        return venueName;
    }
    
    
    /**
     * Returns mission session topic based upon the application context.
     * @param appContext the context to get the topic for
     * 
     * @return mission topic
     */
    public static String getMissionSessionTopic(final ApplicationContext appContext) {
        if (appContext == null) {
            throw new IllegalArgumentException(NULL_APP_CONTEXT);
        }
        
        return (getVenueTopic(appContext.getBean(IVenueConfiguration.class).getVenueType(),
                getVenueName(appContext),
                appContext.getBean(IVenueConfiguration.class).getDownlinkStreamId(), 
                getSubtopic(appContext), 
                appContext.getBean(SseContextFlag.class)));
    }

    /**
     * Returns the sse mission session topic based upon the application context.
     * 
     * @param appContext
     *            the context used to build the topic from
     * @return sse mission session topic
     */
    public static String getSseMissionSessionTopic(
            final ApplicationContext appContext) {
        if (appContext == null) {
            throw new IllegalArgumentException("Null appContext");
        }

        return getSseVenueTopic(appContext.getBean(IVenueConfiguration.class).getVenueType(), getVenueName(appContext), getSubtopic(appContext));
    }

    /**
     * Returns the sse session topic based upon the application context.
     * 
     * @param appContext
     *            context used to build the topic from
     * @return sse session topic
     */
    public static String getSseSessionTopic(
            final ApplicationContext appContext) {
        if (appContext == null) {
            throw new IllegalArgumentException("Null appContext");
        }

        if (appContext.getBean(SseContextFlag.class).isApplicationSse()) {
            return (getSseMissionSessionTopic(appContext));
        } else if (appContext.getBean(MissionProperties.class).missionHasSse()) {
            final String mission = GdsSystemProperties.getSseNameForSystemMission();
            final String venueName = getVenueName(appContext);
            String topicString = buildSseVenueTopic(appContext.getBean(IVenueConfiguration.class).getVenueType(),
                    venueName,
                    getSubtopic(appContext),
                    mission);

            topicString = formatTopicString(topicString);

            logger.debug(mission + " session topic is " + topicString);

            return (topicString);
        }

        return (null);
    }

    private static String getVenueName(final ApplicationContext appContext) {
        return getVenueName(appContext.getBean(IVenueConfiguration.class).getVenueType(), 
        		appContext.getBean(IVenueConfiguration.class).getTestbedName(), 
        		appContext.getBean(IContextIdentification.class).getHost() + "." + 
        	    appContext.getBean(IContextIdentification.class).getUser());
    }
    

    /**
     * Returns the general topic given the application context.
     * 
     * @param appContext
     *            context used to build the general topic from
     * @return general topic
     */
    public static String getGeneralTopic(
            final ApplicationContext appContext) {
        if (appContext == null) {
            throw new IllegalArgumentException(NULL_APP_CONTEXT);
        }

        return getGeneralTopic(appContext.getBean(IVenueConfiguration.class).getVenueType());
    }

    /**
     * Returns perspective topic from an application context.
     * 
     * @param appContext
     *            context to build the perspective topic from.
     * @return perspective topic
     */
    public static String getPerspectiveTopic(
            final ApplicationContext appContext) {
        if (appContext == null) {
            throw new IllegalArgumentException(NULL_APP_CONTEXT);
        }

        return getPerspectiveTopic(appContext.getBean(IVenueConfiguration.class).getVenueType());
    }


    /**
     * Returns the topic name from the config value (token) based upon supplied
     * application context.
     * 
     * @param appContext
     *            context used to build the topic name
     * @param configValue
     *            config value
     * @return topic name
     */
    public static String getTopicNameFromConfigValue(
            final ApplicationContext appContext, final TopicNameToken configValue) {
        
        if (appContext == null) {
            throw new IllegalArgumentException(NULL_APP_CONTEXT);
        }
        
        return (getTopicNameFromConfigValue(appContext.getBean(MissionProperties.class),
        		appContext.getBean(IVenueConfiguration.class).getVenueType(),
                getVenueName(appContext),
                appContext.getBean(IVenueConfiguration.class).getDownlinkStreamId(), 
                getSubtopic(appContext),
                                            configValue, appContext.getBean(SseContextFlag.class)));
       
    }

    private static String getSubtopic(final ApplicationContext appContext) {
    	
    	final String subtopic = appContext.getBean(IGeneralContextInformation.class).getSubtopic();
        if (subtopic != null) {
            return subtopic;
        }
        
        final String defaultSubtopic = appContext.getBean(MissionProperties.class).getDefaultSubtopic();
        if (defaultSubtopic != null) {
            return defaultSubtopic;
        }
    	
    	return "";
    }
    
    /**
     * Gets the message client topic. Used only if clients publish messages.
     * 
     * @return client topic name
     */
    public static String getClientTopic() {
        final StringBuilder topic = new StringBuilder();
        final String mission = GdsSystemProperties.getSystemMission();
        topic.append(getTopicPrefix(null, mission));
        topic.append(TopicNameToken.CLIENT.getTopicNameComponent());
        return topic.toString();
    }

    /**
     * Expands the given root topic and returns a list of all data topics for it.
     * Note that this method can only tell the root topic is an SSE topic if it contains
     * the mission SSE name, in which case it will return SSE data topics.  Otherwise,
     * it will return flight data topics.
     * 
     * @param rootTopic application topic to get data topics for
     * @return list of data topic names; the root topic is not included; never null
     */
    public static List<String> expandApplicationTopic(final String rootTopic) {
        final List<String> result = new ArrayList<>(4);
        if (rootTopic == null) {
            return result;
        }

        if (TopicNameToken.isDataTopic(rootTopic)) {
            return result;
        }
        
        for (final TopicNameToken token : TopicNameToken.getAllDataTopicTokens(
                rootTopic.contains(GdsSystemProperties.getSseNameForSystemMission()))) {
            result.add(token.getApplicationDataTopic(rootTopic));
        }

        return result;
    }
    
    /**
     * Checks a topic name entered as a command line option to ensure it is valid and throws if it is not.
     * @param topic topic name to check
     * @param longOptName the long name of the option being parsed
     * 
     * @throws ParseException if the topic name does not conform
     */
    public static void checkTopicCommandOption(final String topic, final String longOptName) throws ParseException {
        if(longOptName == null) {
            throw new IllegalArgumentException("longOptName is null");
        }
        
        final String result = checkTopic(topic, false);
        
        if (result != null) {
            throw new ParseException(THE_VALUES + longOptName + " are not valid; " + result);
        }
        
        return;
    }
    
    /**
     * Checks a topic name to ensure it is valid.
     * 
     * @param topic topic name to check
     * @param shouldBeRoot true if the topic should be a root topic
     * @return error string if the topic name is not valid, null if it is valid
     */
    public static String checkTopic(final String topic, final boolean shouldBeRoot) {
        if (topic == null) {
            throw new IllegalArgumentException("topic is null");
        } 
        for (int i = 0; i < topic.length(); i++) {
            final char c = topic.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != TopicNameToken.DELIMITER.charAt(0) &&
                    c != '-' && c != '_') {
                return "Topic can only contain letters, digits, and the period, dash, and underscore characters"; 
            }
        }
        if (!topic.startsWith(REQUIRED_TOPIC_PREFIX)) {
            return "Topic name must start with prefix \"" + REQUIRED_TOPIC_PREFIX + "\"";
        }
        
        if (shouldBeRoot && TopicNameToken.isDataTopic(topic)) {
            return "Supplied topic name is a data subtopic, but needs to be a root topic";
        }
        return null;
    }
    
    /**
     * Expands a list of application topics to include all data subtopics.
     * 
     * @param topics application topics to expand
     * @return list of application topics and data subtopics
     */
    public static Collection<String> expandTopics(final Collection<String> topics) {
        final Set<String> tempTopics = new TreeSet<>(topics);
        
        for (final String topic: topics) {
            tempTopics.addAll(ContextTopicNameFactory.expandApplicationTopic(topic));
        }
        tempTopics.addAll(topics);
        return new LinkedList<>(tempTopics);         
    }


}
