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
package jpl.gds.station.api;

import jpl.gds.shared.annotation.CustomerAccessible;

import java.io.IOException;
import java.time.Instant;

/**
 * An interface to be implemented by non-SFDU station telemetry headers.
 *
 *
 * @since R8
 */
@CustomerAccessible(immutable = true)
public interface IStationTelemHeader {

    /** Defined fixed LEOT header size in bytes */
    public static final int LEOT_HEADER_SIZE = 10;

    /**
     * Indicates if the header is valid.
     * 
     * @return true or false
     */
    public boolean isValid();

    /**
     * Loads the station header fields from a byte buffer.
     * 
     * @param buff
     *            the buffer containing the station data
     * @param start
     *            the starting offset of the station header in the buffer
     * @return the offset of the data following the station header in the buffer
     * 
     * @throws IOException
     *             If unable to load
     */
    public int load(byte[] buff, int start) throws IOException;

    /**
     * Gets the header size.
     * 
     * @return the size of the frame header in bytes
     */
    public int getSizeBytes();

    /**
     * Gets a flag indicating if the station header indicated that what follows is 
     * a bad frame.
     *  
     * @return true if frame is bad, false if not
     */
    public boolean isBadFrame();

    /**
     * Gets the reason a frame was bad, if the station header so indicates.
     * 
     * @return the reason code
     */
    public InvalidFrameCode getBadReason();

    /**
     * Gets the data length from the station header.
     * 
     * @return the data length in bytes. This includes header bytes and attached data bytes.
     */
    public int getDataLength();

    /**
     * Gets the data class from the header. Use and support of this value is station-specific.
     * 
     * @return data class value
     */
    public int getDataClass();

    /**
     * Get frame sequence error state from the header.
     * 
     * @return True if the frame the header applies to is out-of-sequence with the previous frame
     */
    public boolean isOutOfSequence();

    /**
     * Get ERT from header. Note that this is only to the millisecond.
     * 
     * @return ERT as Instant
     */
    public Instant getErt();

    /**
     * Getter for header as raw bytes.
     * 
     * @return Header
     * 
     */
    public byte[] getHeader();
}