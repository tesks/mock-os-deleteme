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
package jpl.gds.db.api.sql;

public interface IDbTableNames {

    /** Log Message table name */
    public final String        DB_LOG_MESSAGE_DATA_TABLE_NAME       = "LogMessage";

    /** Channel Value table name */
    public final String        DB_CHANNEL_VALUE_TABLE_NAME          = "ChannelValue";

    /** Monitor Channel Value table name */
    public final String        DB_MONITOR_CHANNEL_VALUE_TABLE_NAME  = "MonitorChannelValue";

    /** Header Channel Value table name */
    public final String        DB_HEADER_CHANNEL_VALUE_TABLE_NAME   = "HeaderChannelValue";

    /** SSE Channel Value table name */
    public final String        DB_SSE_CHANNEL_VALUE_DATA_TABLE_NAME = "SseChannelValue";

    /** Channel Metadata table name */
    public final String        DB_CHANNEL_DATA_TABLE_NAME           = "ChannelData";

    /** Session table name */
    public final String        DB_SESSION_DATA_TABLE_NAME           = "Session";

    /** Session table name abbreviation */
    public final String        DB_SESSION_DATA_TABLE_NAME_ABBREV    = "ts";

    /** End Session table name */
    public final String        DB_END_SESSION_DATA_TABLE_NAME       = "EndSession";

    /** Host Store table name */
    public final String        DB_HOST_STORE_TABLE_NAME             = "Host";

    /** Command Message table name */
    static final String        DB_COMMAND_MESSAGE_DATA_TABLE_NAME   = "CommandMessage";

    /** Command Status table name */
    static final String        DB_COMMAND_STATUS_TABLE_NAME         = "CommandStatus";
    
    /** Command Updater table name */
    static final String        DB_COMMAND_UPDATER_TABLE_NAME        = "CommandUpdater";

    /** EVR table name */
    public final String        DB_EVR_DATA_TABLE_NAME               = "Evr";

    /** EVR Metadata table name */
    public final String        DB_EVR_METADATA_TABLE_NAME           = "EvrMetadata";

    /** SSE EVR table name */
    public final String        DB_SSE_EVR_DATA_TABLE_NAME           = "SseEvr";

    /** SSE EVR Metadata table name */
    public final String        DB_SSE_EVR_METADATA_TABLE_NAME       = "SseEvrMetadata";

    /** Packet table name */
    public final String        DB_PACKET_DATA_TABLE_NAME            = "Packet";

    /** Packet Body table name */
    public static final String DB_PACKET_BODY_TABLE_NAME            = "PacketBody";

    /** SSE Packet table name */
    public final String        DB_SSE_PACKET_DATA_TABLE_NAME        = "SsePacket";

    /** SSE Packet Body table name */
    public final String        DB_SSE_PACKET_BODY_TABLE_NAME        = "SsePacketBody";

    /** Frame table name */
    public final String        DB_FRAME_DATA_TABLE_NAME             = "Frame";

    /** Frame Body table name */
    public final String        DB_FRAME_BODY_TABLE_NAME             = "FrameBody";

    /** Product table name */
    public final String        DB_PRODUCT_DATA_TABLE_NAME           = "Product";

    /** CFDP Indication table name */
    public final String        DB_CFDP_INDICATION_DATA_TABLE_NAME   = "CfdpIndication";

    /** CFDP Indication table name */
    public final String        DB_CFDP_FILE_GENERATION_DATA_TABLE_NAME   = "CfdpFileGeneration";

    /** CFDP File Generation table name */
    public final String        DB_CFDP_FILE_UPLINK_FINISHED_DATA_TABLE_NAME   = "CfdpFileUplinkFinished";

    /** CFDP File Uplink Finished table name */
    public final String        DB_CFDP_REQUEST_RECEIVED_DATA_TABLE_NAME   = "CfdpRequestReceived";

    /** CFDP Request Result table name */
    public final String        DB_CFDP_REQUEST_RESULT_DATA_TABLE_NAME   = "CfdpRequestResult";

    /** CFDP PDU Received table name */
    public final String        DB_CFDP_PDU_RECEIVED_DATA_TABLE_NAME   = "CfdpPduReceived";

    /** CFDP PDU Sent table name */
    public final String        DB_CFDP_PDU_SENT_DATA_TABLE_NAME   = "CfdpPduSent";

    /** ContextConfig table name */
    public final String        DB_CONTEXT_CONFIG_TABLE_NAME   = "ContextConfig";

    /** ContextConfig key value table name */
    public final String        DB_CONTEXT_CONFIG_KEYVAL_TABLE_NAME   = "ContextConfigKeyValue";
    
    /** Channel aggregate table name */
    public final String        DB_CHANNEL_AGGREGATE_TABLE_NAME      = "ChannelAggregate";
    
    /** Monitor Channel aggregate table name */
    public final String        DB_MONITOR_CHANNEL_AGGREGATE_TABLE_NAME      = "MonitorChannelAggregate";

    /** Monitor Channel aggregate table name */
    public final String        DB_HEADER_CHANNEL_AGGREGATE_TABLE_NAME      = "HeaderChannelAggregate";

    /** SSE Channel aggregate table name */
    public final String        DB_SSE_CHANNEL_AGGREGATE_TABLE_NAME      = "SseChannelAggregate";

}
