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

import java.util.Map;

import jpl.gds.shared.log.IPublishableLogMessage;

/**
 * An interface to be implemented by frame processing summary (statistics) messages.
 * 
 *
 * @since R8
 */
public interface IFrameSummaryMessage extends IPublishableLogMessage {

    /**
     * Sets the frame summary map, which contains an entry for each
     * frame type/VCID combination.
     * 
     * @param map the map to set.
     */
    void setFrameSummaryMap(Map<String, FrameSummaryRecord> map);

    /**
     * This gets the current frame summary map, which contains an entry for each
     * frame type/VCID combination.
     * 
     * @return The frame summary map.
     */
    Map<String, FrameSummaryRecord> getFrameSummaryMap();

    /**
     * This sets the encoding summary map, which contains an entry for each
     * frame encoding/VCID combination.
     * 
     * @param map The map to set
     */
    void setEncodingSummaryMap(Map<String, EncodingSummaryRecord> map);

    /**
     * This gets the encoding summary map, which contains an entry for each
     * frame encoding/VCID combination.
     * 
     * @return the encoding summary map
     */
    Map<String, EncodingSummaryRecord> getEncodingSummaryMap();

    /**
     * This gets the number of dead frames seen so far.
     * 
     * @return the number of deadc0de Frames.
     */
    long getDeadFrames();

    /**
     * This returns the number of in-sync frame bytes processed so far.
     * 
     * @return the number of frame bytes (data + idle frame byte count).
     */
    long getFrameBytes();

    /**
     * This returns the number of idle frames seen so far.
     * 
     * @return the number of idle frames.
     */
    long getIdleFrames();

    /**
     * This returns the in sync flag, indicating whether the frame synchronizer is currently
     * in lock state.
     * 
     * @return true if in sync, false if not
     */
    boolean isInSync();

    /**
     * This returns the number of data + idle (in-sync) frames seen so far.
     * 
     * @return the number of data + idle frames.
     */
    long getNumFrames();

    /**
     * This returns the number of bytes out of sync seen so far.
     * @return the number of bytes out of sync.
     */
    long getOutOfSyncBytes();


    /**
     * This returns the number of bad (invalid) frames seen so far.
     * @return the number of bad frames.
     */
    long getBadFrames();

    /**
     * This returns the number of times out-of-sync events have occurred so far.
     * @return the number of times out-of-sync occurred
     */
    long getOutOfSyncCount();

    /**
     * Gets the latest frame processing bitrate.
     * 
     * @return bitrate in bps
     * 
     */

    double getBitrate();
}