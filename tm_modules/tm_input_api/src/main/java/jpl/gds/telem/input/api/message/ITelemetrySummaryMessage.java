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
package jpl.gds.telem.input.api.message;

import java.util.Date;

import jpl.gds.shared.log.IPublishableLogMessage;

/**
 * An interface to be implemented by telemetry input (raw input) status summary 
 * messages.
 * 
 *
 * @since R8
 */
public interface ITelemetrySummaryMessage extends IPublishableLogMessage {

    /**
     * Increment read count.
     */
    public void incrementReadCount();

    /**
     * Get read count.
     *
     * @return Count
     */
    public long getReadCount();

    /**
     * Set read count.
     *
     * @param readCount Read count
     */
    public void setReadCount(long readCount);

    /**
     * Get last-data-read time.
     *
     * @return Date
     */
    public Date getLastDataReadTime();

    /**
     * Set last data read time.
     *
     * @param lastDataReadTime Time
     */
    public void setLastDataReadTime(Date lastDataReadTime);

    /**
     * Get connected state.
     *
     * @return True if connected
     */
    public boolean isConnected();

    /**
     * Set connected state.
     *
     * @param connected Connected state
     */
    public void setConnected(boolean connected);

    /**
     * Get data flowing state.
     *
     * @return True if flowing
     */
    public boolean isFlowing();

    /**
     * Set data flowing state.
     *
     * @param flowing Flow state
     */
    public void setFlowing(boolean flowing);
    
    /**
     * Get the input buffer information.
     * 
     * @return input buffer info string
     */
    public String getBufferedRawInputStreamInfo();
    
    /**
     * Set the input buffer information.
     * 
     * @param bufferedRawInputStreamInfo String containing the input buffer info
     */
    public void setBuffereredRawInputStreamInfo(String bufferedRawInputStreamInfo);

}