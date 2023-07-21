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

import jpl.gds.shared.message.IMessageType;

/**
 * An enumeration of public message types in the tm_service modules.
 * 
 * @since R8
 */
public enum TmServiceMessageType implements IMessageType {
    /** Bad frame message type */
    BadTelemetryFrame,
    /** CFDP PDU message type */
    CfdpPdu,
    /** Telemetry frame sequence anomaly message type */
    FrameSequenceAnomaly,
    /** In sync message type */
    InSync,
    /** Loss of frame sync message type */
    LossOfSync,
    /** Out of sync frame data message type */
    OutOfSyncData,
    /** Presync (raw) telemetry frame data message type */
    PresyncFrameData,
    /** Telemetry frame message type */
    TelemetryFrame,
    /** Telemetry frame summary message type */
    TelemetryFrameSummary,
    /** Telemetry packet message type */
    TelemetryPacket,
    /** Telemetry packet summary message type */
    TelemetryPacketSummary;

    @Override
    public String getSubscriptionTag() {
        return name();
    }
}
