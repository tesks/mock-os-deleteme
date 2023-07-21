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
package jpl.gds.telem.input.api;

import jpl.gds.shared.message.IMessageType;

/**
 * Message configuration enumeration for message types that are entirely
 * internal to the telem_input implementation project.
 * 
 *
 * @since R8
 */
public enum InternalTmInputMessageType implements IMessageType {
    /** Status message for the telemetry input buffer */
	LoggingDiskBackedBufferedInputStream,
	/** Raw input data message for database content */
    RawDatabase,
    /** Raw input data message for CCSDS packet data */
    RawPacket,
    /** Raw input data message for CCSDS frame data */
    RawTransferFrame,
    /** Raw input data message for SFDU packet data */
    RawSfduPkt,
    /** Raw input data message for SFDU frame data */
    RawSfduTf;


    @Override
    public String getSubscriptionTag() {
        return name();
    }
    
}
