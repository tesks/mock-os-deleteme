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
package jpl.gds.tm.service.api.frame;

import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.station.api.IStationTelemInfo;


/**
 * An interface to be implemented by pre-sync frame messages, which carry chunks
 * of raw telemetry frame data destined for frame synchronization.
 * 
 * @since R8
 */
public interface IPresyncFrameMessage extends IMessage {

    /**
     * Gets the station information object associated with this chunk of frame data.
     *
     * @return the station information object describing the raw data
     */
    IStationTelemInfo getStationInfo();

    /**
     * Gets the number of bytes in this data chunk.
     *
     * @return the length of the raw data in bytes
     */
    int getNumBytes();

    /**
     * Gets the data.
     *
     * @return the buffer containing the raw data
     */
    byte[] getData();

    /**
     * Gets the byte value at the given offset in the raw data buffer.
     * 
     * @param off the byte offset to fetch
     * @return the byte value
     * @throws ArrayIndexOutOfBoundsException On index out of bounds
     */
    int get(int off) throws ArrayIndexOutOfBoundsException;
    
    /**
     * Gets the ERT of this data chunk.
     * 
     * @return ERT 
     */
    IAccurateDateTime getErt();
   
}