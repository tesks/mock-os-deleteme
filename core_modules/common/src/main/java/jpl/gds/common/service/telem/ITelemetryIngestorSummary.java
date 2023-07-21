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
package jpl.gds.common.service.telem;

/**
 * The Interface for a POJO to return Telemetry Ingestor Summary Status via RESTful interface
 * 
 * @since R8.1
 */
public interface ITelemetryIngestorSummary extends ITelemetrySummary {



    /**
     * Gets the count of in-sync frames received.
     * 
     * @return in-sync frame counter
     */
    long getInSyncFrames();

    /**
     * Gets the total out of sync byte count.
     * 
     * @return the count of out of sync bytes
     */
    long getOutOfSyncData();

    /**
     * Gets the out of sync event count.
     * 
     * @return the count of out of seync events.
     */
    long getOutOfSyncCount();

    /**
     * Gets the valid packet count.
     * 
     * @return the packet count.
     */
    long getPackets();

    /**
     * Gets the station monitor packet count.
     * 
     * @return the station packet count.
     * 
     */
    long getStationPackets();

    /**
     * Gets the invalid frame count.
     * 
     * @return count of bad frames.
     */
    long getBadFrames();

    /**
     * Gets the invalid packet count.
     * 
     * @return the bad packet count
     */
    long getBadPackets();

    /**
     * Gets the dead frame count.
     * 
     * @return the count of dead frames
     */
    long getDeadFrames();

    /**
     * Gets the count of frame sequence gaps.
     * 
     * @return the frame gap count
     */
    long getFrameGaps();

    /**
     * Sets the frame sequence regression count.
     * 
     * @return the count of frame regressions
     */
    long getFrameRegressions();

    /**
     * Sets the frame sequence repeat count.
     * 
     * @return count of frame repeats.
     */
    long getFrameRepeats();

    /**
     * Gets the idle frame count.
     * 
     * @return count of idle frames
     */
    long getIdleFrames();


    /**
     * Gets the product packet count.
     * 
     * @return count of product packets
     */
    long getProductPackets();

    /**
     * Gets the fill/idle packet count.
     *
     * @return count of fill/idle packets
     */
    long getFillPackets();
	
	/**
     * Gets the CFDP packet count.
     * 
     * @return count of fill packets
     */
    long getCfdpPackets();

}
