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
package jpl.gds.tm.service.api;

/**
 * An interface containing bean names for the Spring configuration for the
 * tm_service projects.
 * 
 * @since R8
 */
public interface TmServiceApiBeans {
    /** Bean name for the TelemetryServiceProperties bean */
    String TELEMETRY_SERVICE_PROPERTIES = "TELEMETRY_SERVICE_PROPERTIES";
    /** Bean name for the ITelemetryPacketInfo bean */
    String TELEMETRY_PACKET_INFO_FACTORY = "TELEMETRY_PACKET_INFO_FACTORY";
    /** Bean name for the ITelemetryFrameInfo bean */
    String TELEMETRY_FRAME_INFO_FACTORY = "TELEMETRY_FRAME_INFO_FACTORY";
    /** Bean name for the IFrameMessageFactory bean */
    String FRAME_MESSAGE_FACTORY = "FRAME_MESSAGE_FACTORY";
    /** Bean name for the IFrameSyncService bean */
    String FRAME_SYNC_SERVICE = "FRAME_SYNC_SERVICE";
    /** Bean name for the IFrameTrackingService bean */
    String FRAME_TRACKING_SERVICE = "FRAME_TRACKING_SERVICE";
    /** Bean name for the IPacketMessageFactory bean */
    String PACKET_MESSAGE_FACTORY = "PACKET_MESSAGE_FACTORY";
    /** Bean name for the IPacketTrackingService bean */
    String PACKET_TRACKING_SERVICE = "PACKET_TRACKING_SERVICE";
    /** Bean name for the IPacketExtractService bean */
    String PACKET_EXTRACT_SERVICE = "PACKET_EXTRACT_SERVICE";
    /** Bean name for the IPduExtractService bean */
    String PDU_EXTRACT_SERVICE = "PDU_EXTRACT_SERVICE";
    /** Bean name for the ICfdpMessageFactory bean */
    String CFDP_MESSAGE_FACTORY = "CFDP_MESSAGE_FACTORY";
}
