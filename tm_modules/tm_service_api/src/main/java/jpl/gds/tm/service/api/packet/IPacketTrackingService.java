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

import jpl.gds.shared.interfaces.IService;

/**
 * An interface to be implemented by telemetry packet tracking services, which
 * watch the internal message stream for messages related to telemetry packet
 * processing and keep various statistics. This includes some frame statistics
 * for frame events that are detected by the packet extraction process.
 * 
 * @TODO R8 Refactor TODO - Move all frame statistics to the frame tracking
 *       service, and make sure that service runs even if the input format is
 *       not frames.
 * 
 *
 * MPCS-10266 12/14/18: Added CFDP packet tracking
 */
public interface IPacketTrackingService extends IService {
    
    /**
     * Gets the number of bytes of packet data processed.
     * @return the number of data bytes
     */
    long getDataByteCount();

    /**
     * Returns the EHA packet count.
     * @return packet count
     */
    long getEhaPackets();

    /**
     * Returns the EVR packet count.
     * @return packet count
     */
    long getEvrPackets();

    /**
     * Returns the fill packet count.
     * 
     * @return packet count
     */
    long getFillPackets();

    /**
     * Returns the CFDP packet count.
     * 
     * @return packet count
     */
    long getCfdpPackets();

    /**
     * Returns the invalid packet count.
     * @return packet count
     */
    long getInvalidPackets();

    /**
     * Returns the product packet count.
     * @return packet count
     */
    long getProductPackets();

    /**
     * Returns the valid packet count. A valid packet is
     * either a bona-fide data packet or a fill packet.
     * @return packet count
     */
    long getValidPackets();

    /**
     * Returns the number of station monitor packets seen.
     * 
     * @return station packet count
     * 
     */
    long getStationPackets();

    /**
     * Returns the frame gap count.
     * @return gap count
     */
    long getFrameGaps();

    /**
     * Returns the frame regression count.
     * @return regression count
     */
    long getFrameRegressions();

    /**
     * Returns the frame repeat count.
     * @return repeat count.
     */
    long getFrameRepeats();

}