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

import jpl.gds.shared.interfaces.IService;

/**
 * An interface to be implemented by telemetry frame tracking (statistics) service.
 * 
 *
 * @since R8
 */
public interface IFrameTrackingService extends IService {

    /**
     * Gets the Mbps data rate of frame processing.
     * 
     * @return the data rated in Mega-bits per second
     */
    float getDataMbps();

    /**
     * Gets the frames per second rate of frame processing.
     * @return the frames per second
     */
    float getFramesPerSecond();

    /**
     * Gets the number of bytes of in-sync frame data processed.
     * @return the number of data bytes
     */
    long getDataByteCount();

    /**
     * This gets the bad/invalid frame count.
     * 
     * @return the bad frame count.
     */
    long getBadFrames();

    /**
     * This gets the dead frames count.
     * 
     * @return the dead frames count.
     */
    long getDeadFrames();

    /**
     * This returns the idle frames count.
     * 
     * @return the idle frames count.
     */
    long getIdleFrames();

    /**
     * This gets the out of sync bytes count.
     * @return the out of sync byte count.
     */
    long getOutOfSyncBytes();

    /**
     * This gets the valid frame count.
     * @return the valid frame count.
     */
    long getValidFrames();

    /**
     * This gets the number of times out-of-sync events occurred.
     * 
     * @return the number of times out-of-sync occurred
     */
    long getOutOfSyncCount();

}