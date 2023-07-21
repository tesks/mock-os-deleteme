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

package jpl.gds.shared.metadata;


/**
 * An enumeration of primitive metadata context values.
 * 
 * @since R8
 *
 */
public enum MetadataKey implements IMetadataKey {
    /* NOTE: CONTEXT_ID must be first on the list */
	/** Key for ID of the current metadata context */
    CONTEXT_ID(MetadataDataType.STRING),
    /** Key for context fragment number */
    CONTEXT_FRAGMENT(MetadataDataType.UNSIGNED_SHORT, "contextFragment,sessionFragment"),
    /** Key for host of the current metadata context */
    CONTEXT_HOST(MetadataDataType.STRING),
    /** Key for host of the current metadata context */
    CONTEXT_HOST_ID(MetadataDataType.UNSIGNED_INT),
	/** Key for number of the current metadata context */
    CONTEXT_NUMBER(MetadataDataType.UNSIGNED_LONG, "sessionNumber,contextNumber"),
	/** Key for user of the current metadata context */
    CONTEXT_USER(MetadataDataType.STRING),
    /** Key for context type */
    CONTEXT_TYPE(MetadataDataType.STRING),
    /** Key for root application output directory */
    APPLICATION_OUTPUT_DIRECTORY(MetadataDataType.STRING),
    /** Key for root application messaging topic */
    APPLICATION_ROOT_TOPIC(MetadataDataType.STRING),
    /** Key for configured DSS ID - formerly session DSS ID */
    CONFIGURED_DSSID(MetadataDataType.UNSIGNED_SHORT, "sessionDssId"),
    /** Key for configured downlink VCID - formerly session VCID */
    CONFIGURED_VCID(MetadataDataType.UNSIGNED_BYTE, "sessionVcid"),
    /** Key for message or object creation time */
    CREATE_TIME(MetadataDataType.STRING),
    /** Key for message or object destruction time */
    END_TIME(MetadataDataType.STRING),
    /** Key for flight dictionary directory */
    FSW_DICTIONARY_DIR(MetadataDataType.STRING),
    /** Key for flight dictionary version */
    FSW_DICTIONARY_VERSION(MetadataDataType.STRING),
    /** Key for flight downlink port */
    FSW_DOWNLINK_PORT(MetadataDataType.UNSIGNED_INT),
    /** Key for flight downlink host */
    FSW_UPLINK_HOST(MetadataDataType.STRING),
    /** Key for flight uplink port */
    FSW_UPLINK_PORT(MetadataDataType.UNSIGNED_INT),
    /** Key for flight uplink host */
    FSW_DOWNLINK_HOST(MetadataDataType.STRING),
    /** Key for message counter */
    MESSAGE_COUNTER(MetadataDataType.UNSIGNED_LONG),
    /** Key for message type */
    MESSAGE_TYPE(MetadataDataType.STRING),
    /** Key for mission ID */
    MISSION_ID(MetadataDataType.SHORT),
    /** Key for mission name */
    MISSION_NAME(MetadataDataType.STRING),
    /** Key for perspective ID */
    PERSPECTIVE_ID(MetadataDataType.STRING),
    /** Key for message source process ID */
    SOURCE_PID(MetadataDataType.UNSIGNED_INT),
    /** Key for spacecraft ID */
    SPACECRAFT_ID(MetadataDataType.UNSIGNED_SHORT, "spacecraftId"),
    /** Key for spacecraft bane */
    SPACECRAFT_NAME(MetadataDataType.STRING),
    /** Key for SSE dictionary directory */
    SSE_DICTIONARY_DIR(MetadataDataType.STRING),
    /** Key for SSE dictionary version */
    SSE_DICTIONARY_VERSION(MetadataDataType.STRING),
    /** Key for SSE downlink port */
    SSE_DOWNLINK_PORT(MetadataDataType.UNSIGNED_INT),
    /** Key for SSE downlink host */
    SSE_DOWNLINK_HOST(MetadataDataType.STRING),
    /** Key for SSE uplink port */
    SSE_UPLINK_PORT(MetadataDataType.UNSIGNED_INT),
    /** Key for SSE uplink host */
    SSE_UPLINK_HOST(MetadataDataType.STRING),
    /** Key for SSE enabled */
    SSE_ENABLED(MetadataDataType.BOOLEAN),
    /** Key for telemetry (downlink) stream type */
    TELEMETRY_STREAM_TYPE(MetadataDataType.STRING),
    /** Key for time comparison strategy */
    TIME_COMPARISON_STRATEGY(MetadataDataType.STRING),
    /** Key for telemetry (downlink) input type */
    TELEMETRY_INPUT_TYPE(MetadataDataType.STRING),
    /** Key for connection type */
    CONNECTION_TYPE(MetadataDataType.STRING),
    /** Key for testbed name */
    TESTBED_NAME(MetadataDataType.STRING),
    /** Key for venue type */
    VENUE_TYPE(MetadataDataType.STRING, "venueType"),
    /** Key for AMPCS version */
    AMPCS_VERSION(MetadataDataType.STRING),
    /** Key for the REST service port */
    REST_PORT(MetadataDataType.UNSIGNED_INT),
    /** Key for session ID list */
    SESSION_IDS(MetadataDataType.STRING);

    private String templateVar;
    private MetadataDataType type;
    
    private MetadataKey(final MetadataDataType theType) {
        final String[] pieces = name().toLowerCase().split("_");
        final StringBuilder temp = new StringBuilder(pieces[0]);
        for (int i = 1; i < pieces.length; i++) {
            temp.append(Character.toUpperCase(pieces[i].charAt(0)) + 
                    pieces[i].substring(1));
        }
        templateVar = temp.toString();
        type = theType;
    }
    
    private MetadataKey(final MetadataDataType theType, final String templateTag) {
        this.templateVar = templateTag;
        type = theType;
    }

    @Override
    public String getTemplateVariable() {
        return templateVar;
    }

    @Override
    public String getName() {
        return name();
    }

    @Override
    public short getSerializationKey() {
        return (short) ordinal();
    }
    
    @Override
    public MetadataDataType getDataType() {
        return this.type;
    }
    
    @Override
    public String toString() {
        return this.name();
    }
    
}