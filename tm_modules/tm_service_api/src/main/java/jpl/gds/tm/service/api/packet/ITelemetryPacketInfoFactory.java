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

import jpl.gds.ccsds.api.packet.ISpacePacketHeader;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeader;

/**
 * An interface to be implemented by factories that create ITelemetryPacketInfo
 * objects.
 * 
 * @since R8
 */
public interface ITelemetryPacketInfoFactory {

    /**
     * Creates an empty ITelemetryPacketInfo object. 
     * <p>
     * <br>
     * CAVEAT: This factory method should only be used by tests and by
     * ITelemetryPacketInfo and message classes to create empty objects
     * to de-serialize content into. Other code should always be using the
     * factory methods that require an ISpacePacketHeader instance.
     * 
     * @return ITelemetryPacketInfo object
     * 
     * MPCS-7215 - 4/9/15 - Updated to use caching, general cleanup.
     */
	ITelemetryPacketInfo create();

    /**
     * Creates an ITelemetryPacketInfo object and sets basic fields from the supplied
     * ISpacePacketHeader and length. 
     * 
     * @param header
     *            The ISpacePacketHeader to base the ITelemetryPacketInfo object
     *            on
     * @param entirePacketLength
     *            The entire packet length, including header.
     * @return ITelemetryPacketInfo object
     * 
     * MPCS-7215 - 4/9/15 - Updated to use caching, general cleanup.
     */
	ITelemetryPacketInfo create(ISpacePacketHeader header,
			int entirePacketLength);

    /**
     * Creates an ITelemetryPacketInfo object and sets basic fields from the
     * supplied ISpacePacketHeader, ISecondaryPacketHeader and length.
     * 
     * @param primaryHeader
     *            The ISpacePacketHeader to base the ITelemetryPacketInfo object
     *            on
     * @param entirePacketLength
     *            The entire packet length, including header.
     * @param secondaryHeader
     *            The pre-populated secondary header to base the
     *            ITelemetryPacketInfo object on
     * @return ITelemetryPacketInfo object
     * 
     * MPCS-7215 - 4/9/15 - Updated to use caching, general cleanup.
     */
	ITelemetryPacketInfo create(
			ISpacePacketHeader primaryHeader, int entirePacketLength,
			ISecondaryPacketHeader secondaryHeader);

}