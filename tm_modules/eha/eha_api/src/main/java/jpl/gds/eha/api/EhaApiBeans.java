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
package jpl.gds.eha.api;

/**
 * An interface defining Spring bean names for beans in the EHA projects.
 * 
 * @since R8
 */
public interface EhaApiBeans {
    
    /** Pre-channelized adapter bean */
    public static final String PRECHANNELIZED_ADAPTOR              = "PRECHANNELIZED_ADAPTOR";
    /** Internal Channel LAD bean */
    public static final String CHANNEL_LAD = "CHANNEL_LAD";
    /** Derivation map bean */
    public static final String DERIVATION_MAP = "DERIVATION_MAP";
    /** Alarm factory bean */
    public static final String ALARM_FACTORY = "ALARM_FACTORY";
    /** Alarm history table bean */
    public static final String ALARM_HISTORY = "ALARM_HISTORY";
    /** Alarm value factory bean */
    public static final String ALARM_VALUE_FACTORY = "ALARM_VALUE_FACTORY";
    /** Alarm value set factory bean */
    public static final String ALARM_VALUE_SET_FACTORY = "ALARM_VALUE_SET_FACTORY";
    /** Alarm publisher service bean */
    public static final String ALARM_PUBLISHER_SERVICE = "ALARM_PUBLISHER_SERVICE";
    /** Alarm notifier service bean */
    public static final String ALARM_NOTIFIER_SERVICE = "ALARM_NOTIFIER_SERVICE";
    /** EHA message factory bean */
    public static final String EHA_MESSAGE_FACTORY = "EHA_MESSAGE_FACTORY";
    /** Channel value factory bean */
    public static final String CHANNEL_VALUE_FACTORY = "CHANNEL_VALUE_FACTORY";
    /** Channel LAD service bean */
    public static final String CHANNEL_LAD_SERVICE = "CHANNEL_LAD_SERVICE";
    /** Channel publisher utility bean */
    public static final String CHANNEL_PUBLISHER_UTILITY = "CHANNEL_PUBLISHER_UTILITY";
    /** Decom listener factory bean */
    public static final String DECOM_LISTENER_FACTORY = "DECOM_LISTENER_FACTORY";
    /** Pre-channelized EHA publisher service bean */
    public static final String PRECHANNELIZED_PUBLISHER_SERVICE    = "PRECHANNELIZED_PUBLISHER_SERVICE";
    /** Frame header channelization service bean */
    public static final String FRAME_HEADER_CHANNELIZER = "FRAME_HEADER_CHANNELIZER";
    /** Monitor data channel processor bean */
    public static final String MONITOR_DATA_CHANNEL_PROCESSOR = "MONITOR_DATA_CHANNEL_PROCESSOR";
    /** Generic packet decom service bean */
    public static final String GENERIC_PACKET_DECOM_SERVICE = "GENERIC_PACKET_DECOM_SERVICE";
    /** Hybrid generic decom service bean */
    public static final String HYBRID_GENERIC_PACKET_DECOM_SERVICE = "HYBRID_GENERIC_PACKET_DECOM_SERVICE";
    /** NEN status decom service bean */
    public static final String NEN_STATUS_DECOM_SERVICE = "NEN_STATUS_DECOM_SERVICE";
    /** Packet header channelization service bean */
    public static final String PACKET_HEADER_CHANNELIZER_SERVICE = "PACKET_HEADER_CHANNELIZER_SERVICE";
    /** SFDU header channelization service bean */
    public static final String SFDU_HEADER_CHANNELIZER_SERVICE = "SFDU_HEADER_CHANNELIZER_SERVICE";
    /** Grouped EHA channel aggregation service bean */
    public static final String GROUPED_CHANNEL_AGGREGATION_SERVICE = "GROUPED_CHANNEL_AGGREGATION_SERVICE";
    /** Suspect channel service bean */
    public static final String SUSPECT_CHANNEL_SERVICE = "SUSPECT_CHANNEL_SERVICE";
    /** EHA properties bean */
    public static final String EHA_PROPERTIES                      = "EHA_PROPERTIES";
    /** EHA feature manager bean */
    public static final String EHA_FEATURE_MANAGER                 = "EHA_FEATURE_MANAGER";
	
    public static final String ALARM_HISTORY_FACTORY = "ALARM_HISTORY_FACTORY";
}
