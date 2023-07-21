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
package jpl.gds.db.api.sql.store;


/**
 * Enum of all Inserter types, i.e., assignments to DB tables.
 * 
 * Note no Session or EndSession or Host or bodies, and:
 * - EvrMetadata goes with Evr
 * - ChannelData goes with channel-values
 * - ContextConfigKeyValue goes with ContextConfig
 */
public enum StoreIdentifier {
    // @formatter:off
    None,
    
    Session,
    ContextConfig,
    EndSession,
    Host,
    CommandUpdate,
    
    Frame,
    Packet,
    SsePacket,

    ChannelValue,
    HeaderChannelValue,
    MonitorChannelValue,
    SseChannelValue,

    Evr,
    SseEvr,

    CommandMessage,
    LogMessage,

    Product,
      
    CfdpIndication,
    CfdpFileGeneration,
    CfdpFileUplinkFinished,
    CfdpRequestReceived,
    CfdpRequestResult,
    CfdpPduReceived,
    CfdpPduSent,

    ChannelAggregate,
    HeaderChannelAggregate,
    SseChannelAggregate,
    MonitorChannelAggregate;
    // @formatter:on
}
