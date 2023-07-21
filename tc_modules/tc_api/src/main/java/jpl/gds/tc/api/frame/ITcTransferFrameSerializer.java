/*
 * Copyright 2006-2019. California Institute of Technology.
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
package jpl.gds.tc.api.frame;

import jpl.gds.tc.api.ITcTransferFrame;

/**
 * Interface for TcTransferFrameSerializers
 *
 */
public interface ITcTransferFrameSerializer {

    /**
     * The length in bits of the frame version number
     */
    short VERSION_NUMBER_BIT_LENGTH = 2;
    /**
     * The minimum allowable value of the frame version number
     */
    long VERSION_NUMBER_MIN_VALUE = 0;
    /**
     * The maximum allowable value of the frame version number
     */
    long VERSION_NUMBER_MAX_VALUE = Math.round(Math.pow(2, VERSION_NUMBER_BIT_LENGTH)) - 1;
    /**
     * The length in bits of the frame bypass flag
     */
    short BYPASS_FLAG_BIT_LENGTH = 1;
    /**
     * The minimum allowable value of the bypass flag
     */
    long BYPASS_FLAG_MIN_VALUE = 0;
    /**
     * The maximum allowable value of the bypass flag
     */
    long BYPASS_FLAG_MAX_VALUE = Math.round(Math.pow(2, BYPASS_FLAG_BIT_LENGTH)) - 1;

    /**
     * The length in bits of the control command flag
     */
    short CONTROL_COMMAND_FLAG_BIT_LENGTH = 1;
    /**
     * The minimum allowable value for the CC flag
     */
    long CONTROL_COMMAND_FLAG_MIN_VALUE = 0;
    /**
     * The maximum allowable value for the CC flag
     */
    long CONTROL_COMMAND_FLAG_MAX_VALUE = Math.round(Math.pow(2, CONTROL_COMMAND_FLAG_BIT_LENGTH)) - 1;

    /**
     * The length in bits of the spare bits
     */
    short SPARE_BIT_LENGTH = 2;
    /**
     * The minimum allowable value for the spare field
     */
    long SPARE_MIN_VALUE = 0;
    /**
     * The maximum allowable value for the spare field
     */
    long SPARE_MAX_VALUE = Math.round(Math.pow(2, SPARE_BIT_LENGTH)) - 1;

    /**
     * The length in bits of the spacecraft ID
     */
    short SPACECRAFT_ID_BIT_LENGTH = 10;
    /**
     * The minimum allowable value for the spacecraft ID
     */
    long SPACECRAFT_ID_MIN_VALUE = 0;
    /**
     * The maximum allowable value for the spacecraft ID
     */
    long SPACECRAFT_ID_MAX_VALUE = Math.round(Math.pow(2, SPACECRAFT_ID_BIT_LENGTH)) - 1;

    /**
     * The length in bits of the virtual channel ID
     */
    short VIRTUAL_CHANNEL_ID_BIT_LENGTH = 6;
    /**
     * The length in bits of the virtual channel number field
     */
    short VIRTUAL_CHANNEL_NUMBER_BIT_LENGTH = 3;
    /**
     * The minimum allowable value for the virtual channel number
     */
    long VIRTUAL_CHANNEL_NUMBER_MIN_VALUE = 0;
    /**
     * The maximum allowable value for the virtual channel number
     */
    long VIRTUAL_CHANNEL_NUMBER_MAX_VALUE = Math.round(Math.pow(2, VIRTUAL_CHANNEL_NUMBER_BIT_LENGTH) - 1);
    /**
     * The length in bits of the execution string field
     */
    short EXECUTION_STRING_BIT_LENGTH = 3;
    /**
     * The maximum allowable value for the execution string field
     */
    long EXECUTION_STRING_MAX_VALUE = Math.round(Math.pow(2, EXECUTION_STRING_BIT_LENGTH)) - 1;
    /**
     * The minimum allowable value for the execution string field
     */
    long EXECUTION_STRING_MIN_VALUE = 0;

    /**
     * The length in bits of the frame length
     */
    short LENGTH_BIT_LENGTH = 10;
    /**
     * The minimum allowable value for the length field
     */
    long LENGTH_MIN_VALUE = 0;
    /**
     * The maximum allowable value for the length field
     */
    long LENGTH_MAX_VALUE = Math.round(Math.pow(2, LENGTH_BIT_LENGTH)) - 1;

    /**
     * The length in bits of sequence number (determines the max allowable session size)
     */
    short SEQUENCE_NUMBER_BIT_LENGTH = 8;
    /**
     * The minimum allowable value for the sequence number field
     */
    long SEQUENCE_NUMBER_MIN_VALUE = 0;
    /**
     * The maximum allowable value for the sequence number field
     */
    long SEQUENCE_NUMBER_MAX_VALUE = Math.round(Math.pow(2, SEQUENCE_NUMBER_BIT_LENGTH)) - 1;

    /**
     * The total length in bits of a telecommand frame header
     */
    int TOTAL_HEADER_BIT_LENGTH = VERSION_NUMBER_BIT_LENGTH +
            BYPASS_FLAG_BIT_LENGTH +
            CONTROL_COMMAND_FLAG_BIT_LENGTH +
            SPARE_BIT_LENGTH +
            SPACECRAFT_ID_BIT_LENGTH +
            VIRTUAL_CHANNEL_ID_BIT_LENGTH +
            LENGTH_BIT_LENGTH +
            SEQUENCE_NUMBER_BIT_LENGTH;

    /**
     * The total length in bytes of a telecommand frame header
     */
    int TOTAL_HEADER_BYTE_LENGTH = TOTAL_HEADER_BIT_LENGTH / 8;

    /**
     * Get the byte-level representation of this frame object.  NOTE: This will only work
     * if the header metadata and body data have already been set on this frame.
     *
     * @return The byte array representation of this frame
     */
    byte[] getBytes(final ITcTransferFrame tcFrame);

    /**
     * Get a binary representation of the telecommand frame header
     *
     * @return A byte array containing the binary representation of this frame's header
     */
    byte[] getHeaderBytes(final ITcTransferFrame tcFrame);

    /**
     * Calculate the total byte length of this frame
     *
     * @return The length of this frame in octets
     */
    short calculateLength(final ITcTransferFrame tcFrame);

    /**
     * Calculate the FECF for this frame.  NOTE: This will only work properly if the frame header
     * and frame data have already been set on this object.
     *
     * @param tcFrame the Telecommand frame to have an FECF value calculated
     *
     * @return The bytes representing the FECF value of this frame.
     */
    byte[] calculateFecf(final ITcTransferFrame tcFrame);

    /**
     * Calculate the FECF for this frame.  NOTE: This will only work properly if the frame header
     * and frame data have already been set on this object.
     *
     * @param tcFrame the Telecommand frame to have an FECF value calculated
     *
     * @param algorithm the algorithm to be used with calculating the FECF value
     *
     * @return The bytes representing the FECF value of this frame.
     */
    byte[] calculateFecf(final ITcTransferFrame tcFrame, final FrameErrorControlFieldAlgorithm algorithm);
}
