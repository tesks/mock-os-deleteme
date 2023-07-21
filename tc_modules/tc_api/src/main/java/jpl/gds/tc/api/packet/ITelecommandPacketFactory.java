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
package jpl.gds.tc.api.packet;

import jpl.gds.ccsds.api.packet.ISpacePacketHeader;
import jpl.gds.tc.api.ITelecommandPacket;

/**
 * Interface for the TelecommandPacketFactory
 * 
 *
 */
public interface ITelecommandPacketFactory {

    /**
     * Builds a TelecommandPacket with pdu data
     * 
     * @param data
     *            pdu
     * @param apid
     *            apid to use
     * @return ITelecommandPacket
     */
    ITelecommandPacket buildPacketFromPdu(final byte[] data, final int apid);

    /**
     * @param dataLength
     *            length of the data field
     * @param apid
     *            apid to use
     * @param sequenceCount
     *            sequence count
     * @return ITelecommandPacket
     */
    ISpacePacketHeader buildPacketHeader(final int dataLength, final int apid, final int sequenceCount);

    /**
     * @param header
     *            ISpacePacketHeader to use
     * @param data
     *            byte array of data to put in the packet
     * @return ITelecommandPacket
     */
    ITelecommandPacket buildPacketFromHeader(ISpacePacketHeader header, final byte[] data);

}
