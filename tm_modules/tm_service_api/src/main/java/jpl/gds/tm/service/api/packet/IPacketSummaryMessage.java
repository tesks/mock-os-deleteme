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
package jpl.gds.tm.service.api.packet;

import java.util.Map;

import jpl.gds.shared.log.IPublishableLogMessage;

/**
 * An interface to be implemented by telemetry packet processing summary (statistics)
 * messages.
 * 
 *
 * @since R8
 */
public interface IPacketSummaryMessage extends IPublishableLogMessage {

    /**
     * Gets the map of packet summary objects from the message.
     * 
     * @return map of packet VCID/APID key to PacketSummaryRecord
     */
    Map<String, PacketSummaryRecord> getPacketSummaryMap();

    /**
     * Returns the number of frame gaps seen during packet extraction.
     *  
     * @return gap count
     */
    long getNumFrameGaps();

    /**
     * Returns the number of frame regressions seen during packet extraction.
     * 
     * @return regression count
     */
    long getNumFrameRegressions();

    /**
     * Returns the number of frame repeats seen during packet extraction.
     * 
     * @return repeat count
     */
    long getNumFrameRepeats();

    /**
     * Returns the number of fill packets seen.
     * 
     * @return packet count
     */
    long getNumFillPackets();

    /**
     * Returns the number of station monitor packets seen.
     * 
     * @return packet count
     * 
     */
    long getNumStationPackets();

    /**
     * Returns the number of invalid packets seen.
     * 
     * @return packet count
     */
    long getNumInvalidPackets();

    /**
     * Returns the number of valid packets seen.
     * 
     * @return packet count
     */
    long getNumValidPackets();

    /**
     * Returns the number of cfdp packets seen.
     * 
     * @return cfdp packet count
     */
    long getNumCfdpPackets();

}