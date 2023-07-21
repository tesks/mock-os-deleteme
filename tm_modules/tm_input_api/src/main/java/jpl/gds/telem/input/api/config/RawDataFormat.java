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
package jpl.gds.telem.input.api.config;

import jpl.gds.common.config.types.TelemetryInputType;

import static jpl.gds.common.config.types.TelemetryInputType.*;

/**
 * An enumeration of the raw data format types processable by the
 * data processors in the raw input system. Regardless of the TelemetryInputType
 * of the input stream, data must be reduced to one of these formats to
 * be handled by a data processor.
 * 
 *
 * MPCS-7677 - 9/16/15. Added javadoc. Added method
 *          to get RawDataFormat given RawInputType.
 */
public enum RawDataFormat {
    /** The format of the data to be processed is raw transfer frame. */
	TRANSFER_FRAME,
    /** The format of the data to be processed is raw space packet. */
	PACKET,
	/** The format if the data to be processed is unknown */
	UNKNOWN;
	
	/**
     * Get the RawDataFormat value that matches the given TelemetryInputType.
     *
     * @param inputType the telemetry input type to match
     * @return RawDataFormat enum value
     * 
     */
    public static RawDataFormat getRawDataFormat(final TelemetryInputType inputType) {

        if (inputType.equals(RAW_TF)) {
            return RawDataFormat.TRANSFER_FRAME;
        }
        else  if (inputType.equals(RAW_PKT)) {
            return RawDataFormat.PACKET;
        }
        else  if (inputType.equals(SFDU_TF)) {
            return RawDataFormat.TRANSFER_FRAME;
        }
        else  if (inputType.equals(SFDU_PKT)) {
            return RawDataFormat.PACKET;
        }
        else  if (inputType.equals(LEOT_TF)) {
            return RawDataFormat.TRANSFER_FRAME;
        }
        //check for dynamic values (sync transfer frame)
        else if(!inputType.equals(TelemetryInputType.CMD_ECHO) && !inputType.equals(TelemetryInputType.UNKNOWN) &&
                TelemetryInputType.valuesAsString().contains(inputType.name())){
            return RawDataFormat.TRANSFER_FRAME;
        }
        else{
            return RawDataFormat.UNKNOWN;
        }
    }
}
