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
package jpl.gds.tc.api.message;

import jpl.gds.shared.message.IMessageType;

/**
 * MPCS-10869 - 06/03/19 - added FileCfdp and CltuF
 */
public enum CommandMessageType implements IMessageType {
    /** Command file load message type */
    FileLoad,
    /** Flight command message type */
    FlightSoftwareCommand,
    /** Hardware command message type */
    HardwareCommand,
    /** Raw uplink data data message type */
    RawUplinkData,
    /** SCMF message type */
    Scmf,
    /** SSE command message type */
    SequenceDirective,
    /** Command Sequence directive message type */
    SseCommand,
    /** Uplink status message type */
    UplinkStatus,
    /** Uplink GUI log message */
    UplinkGuiLog, 
    /** Clear GUI log message */
    ClearUplinkGuiLog, 
    /** Internal CPD uplink status message */
    InternalCpdUplinkStatus,
    /** Command echo messages */
    CommandEcho,
    /** send file via CFDP message type*/
    FileCfdp,
    /** CLTU F message type */
    CltuF;

    @Override
    public String getSubscriptionTag() {
        return name();
    }
}
