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

import jpl.gds.shared.holders.FrameIdHolder;
import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.holders.TrailerHolder;

/**
 * An interface to be implemented by factories that create telemetry packet related
 * messages.
 * 
 * @since R8
 */
public interface IPacketMessageFactory {

    /**
     * Creates an IPacketMessage instance and assigns it the given packet
     * metadata object. The packet body remains unset by this constructor.
     * <br>
     * CAVEAT: This method should only by used by tests.
     * 
     * @param info
     *            the ITelemetryPacketInfo object containing packet metadata.
     * 
     * @return new message instance
     */
     ITelemetryPacketMessage createTelemetryPacketMessage(ITelemetryPacketInfo info);

    /**
     * Creates an instance of PacketMessage and assigns it the given packet
     * metadata object, packet ID, header, and trailer. If the packet id is
     * null, it will be created upon demand. The packet body remains unset by
     * this constructor.
     * 
     * @param info
     *            the IPacketInfo containing packet metadata
     * @param id
     *            Packet id or null
     * @param header
     *            Header holder; may be null
     * @param trailer
     *            Trailer holder; may be null
     * @param fid
     *            Frame id holder, not null
     *            
     * @return new message instance
     * 
     */
    ITelemetryPacketMessage createTelemetryPacketMessage(
            ITelemetryPacketInfo info, PacketIdHolder id, HeaderHolder header,
            TrailerHolder trailer, FrameIdHolder fid);

    /**
     * Creates a telemetry packet processing summary (statistics) message.
     * 
     * @param numGaps
     *            number of frame gaps seen so far by the packet extractor
     * @param numRegressions
     *            number of frame regressions seen so far by the packet extractor
     * @param numRepeats
     *            number of frame repeats seen so far by the packet extractor
     * @param numFill
     *            number of fill packets seen so far by the packet extractor
     * @param numInvalid
     *            number of invalid packets seen so far by the packet extractor
     * @param numValid
     *            number of valid packets seen so far by the packet extractor
     * @param numStation
     *            number of station monitor packets seen so far by the packet extractor
     * @param numCfdp
     *            number of cfdp packets seen so far by the packet extractor
     * @param summaries
     *            map of packet summary records by packet APID
     * 
     * @return new message instance
     */
    IPacketSummaryMessage createPacketSummaryMessage(long numGaps,
            long numRegressions, long numRepeats, long numFill,
            long numInvalid, long numValid, long numStation, long numCfdp,
            Map<String, PacketSummaryRecord> summaries);

}