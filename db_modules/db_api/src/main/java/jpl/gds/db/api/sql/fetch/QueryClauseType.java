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
package jpl.gds.db.api.sql.fetch;

/**
 * Enumeration of all query-clauses available.
 *
 */
public enum QueryClauseType
{
    /** NO_JOIN_BODY_HEADER */
    NO_JOIN_BODY_HEADER,

    /** NO_JOIN_BODY */
    NO_JOIN_BODY,

    /** NO_JOIN */
    NO_JOIN,

    /** NO_JOIN_SSE MPCS-5008 */
    NO_JOIN_SSE,

    /** JOIN_SINGLE */
    JOIN_SINGLE,

    /** NO_JOIN_REVERSE */
    NO_JOIN_REVERSE,

    /** ANY_SESSION_SELECT */
    ANY_SESSION_SELECT,

    /** COUNT_SELECT */
    COUNT_SELECT,

    /** ECDR_GROUP_BY */
    ECDR_GROUP_BY,

    /** END_SESSION_SELECT */
    END_SESSION_SELECT,

    /** FROM_SSE_CONDITION */
    FROM_SSE_CONDITION,

    /** RECORD_ID_CONDITION */
    RECORD_ID_CONDITION,

    /** PRODUCT_APID_SUMMARY */
    PRODUCT_APID_SUMMARY,

    /** PRODUCT_APID_SUMMARY_COMPLETION */
    PRODUCT_APID_SUMMARY_COMPLETION,

    /** PRODUCT_PARTIAL_CONDITION */
    PRODUCT_PARTIAL_CONDITION,

    /** MODULE_CONDITION */
    MODULE_CONDITION,

    /** REALTIME_CONDITION */
    REALTIME_CONDITION,

    /** REQUEST_ID_IN_CONDITION */
    REQUEST_ID_IN_CONDITION,

    /** MESSAGE_CONDITION */
    MESSAGE_CONDITION,

    /** COMMAND_TYPE_CONDITION */
    COMMAND_TYPE_CONDITION,

    /** EVR_NAME_CONDITION */
    EVR_NAME_CONDITION,

    /** EVR_LEVEL_CONDITION */
    EVR_LEVEL_CONDITION,

    /** EVR_EVENT_ID_CONDITION */
    EVR_EVENT_ID_CONDITION,

    /** LOG_CLASSIFICATION_CONDITION */
    LOG_CLASSIFICATION_CONDITION,

    /** DISTINCT_CHANNEL_STEM */
    DISTINCT_CHANNEL_STEM,

    /** DISTINCT_CHANNEL_ID */
    DISTINCT_CHANNEL_ID,

    /** STEM_CONDITION */
    STEM_CONDITION,

    /** JOIN_CHANNEL_DATA */
    JOIN_CHANNEL_DATA,

    /** COUNT_SELECT_CHANNEL_DATA */
    COUNT_SELECT_CHANNEL_DATA,

    /** DISTINCT_CHANNEL_STEM_COUNT */
    DISTINCT_CHANNEL_STEM_COUNT,

    /** DISTINCT_CHANNEL_ID_COUNT */
    DISTINCT_CHANNEL_ID_COUNT,

    /** PACKET_JOIN */
    PACKET_JOIN,

    /** PACKET_JOIN_SSE MPCS-5008 */
    PACKET_JOIN_SSE,

    /** MPCS-7106 */

    ECDR_JOIN,
    ECDR_JOIN_SSE,
    ECDR_FORCE,
    
    INDICATION_TYPE_CONDITION,

    CONTEXT_JOIN,
    CONTEXT_KEY_VALUE_SELECT,
    
    PACKET_JOIN_GROUP_BY, 
    PACKET_JOIN2;


    /**
     * Convert to XML tag.
     *
     * @return XML tag as string
     */    
    public String getXmlTag()
    {
    	switch (this)
        {
    	    case NO_JOIN_BODY_HEADER:
                return "NoJoinBodyHeader";
    	    case NO_JOIN_BODY:
                return "NoJoinBody";
            case NO_JOIN:
                return "NoJoin";

            /** MPCS-5008 */
            case NO_JOIN_SSE:
                return "NoJoinSse";

            case JOIN_SINGLE:
                return "JoinSingle";
            case NO_JOIN_REVERSE:
                return "NoJoinReverse";
    	    case ANY_SESSION_SELECT:
                return "AnySessionSelect";
            case CONTEXT_JOIN:
                return "ContextJoin";
            case CONTEXT_KEY_VALUE_SELECT:
                return "ContextKeyValueSelect";
    	    case COUNT_SELECT:
                return "CountSelect";
    	    case ECDR_GROUP_BY:
                return "EcdrGroupBy";
    	    case END_SESSION_SELECT:
                return "EndSessionSelect";
    	    case FROM_SSE_CONDITION:
                return "FromSseCondition";
    	    case RECORD_ID_CONDITION:
                return "RecordIdCondition";
    	    case PRODUCT_APID_SUMMARY:
                return "ProductSummaryByApid";
    	    case PRODUCT_APID_SUMMARY_COMPLETION:
                return "ProductSummaryByApidComplete";
    	    case PRODUCT_PARTIAL_CONDITION:
                return "ProductPartialCondition";
    	    case MODULE_CONDITION:
                return "ModuleCondition";
    	    case REALTIME_CONDITION:
                return "RealtimeCondition";
    	    case REQUEST_ID_IN_CONDITION:
                return "RequestIdInCondition";
    	    case MESSAGE_CONDITION:
                return "MessageCondition";
    	    case COMMAND_TYPE_CONDITION:
                return "CommandTypeCondition";
    	    case EVR_NAME_CONDITION:
                return "EvrNameCondition";
    	    case EVR_LEVEL_CONDITION:
                return "EvrLevelCondition";
    	    case EVR_EVENT_ID_CONDITION:
                return "EvrEventIdCondition";
    	    case LOG_CLASSIFICATION_CONDITION:
                return "LogClassificationCondition";
    	    case DISTINCT_CHANNEL_STEM:
                return "DistinctChannelStemSelect";
    	    case DISTINCT_CHANNEL_ID:
                return "DistinctChannelIdSelect";
    	    case STEM_CONDITION:
                return "StemCondition";
    	    case JOIN_CHANNEL_DATA:
                return "JoinChannelData";
    	    case COUNT_SELECT_CHANNEL_DATA:
                return "CountSelectChannelData";
    	    case DISTINCT_CHANNEL_STEM_COUNT:
                return "DistinctChannelStemCount";
    	    case DISTINCT_CHANNEL_ID_COUNT:
                return "DistinctChannelIdCount";
            case PACKET_JOIN:
                return "PacketJoin";
            case PACKET_JOIN2:
                return "PacketJoin2";
            /** MPCS-5008 */
            case PACKET_JOIN_SSE:
                return "PacketJoinSse";

            /** MPCS-7106 Add */
            case ECDR_JOIN:
                return "EcdrJoin";

            case ECDR_JOIN_SSE:
                return "EcdrJoinSse";

            case ECDR_FORCE:
                return "EcdrForce";

    	    case INDICATION_TYPE_CONDITION:
                return "IndicationTypeCondition";
                
    	    case PACKET_JOIN_GROUP_BY:
    	    	return "PacketJoinGroupBy";
    	    	
    	    default:
    		    return "";
    	}
    }
}
