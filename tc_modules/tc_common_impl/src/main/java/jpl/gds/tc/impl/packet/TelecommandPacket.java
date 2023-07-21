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
package jpl.gds.tc.impl.packet;

import jpl.gds.ccsds.api.packet.ISpacePacketHeader;
import jpl.gds.tc.api.ITelecommandPacket;

/**
 * Intial implementation of a TelecommandPacket
 * 
 *
 */
public class TelecommandPacket implements ITelecommandPacket {

    final ISpacePacketHeader header;
    final byte[]             data;
    private final byte[]           allData;

    /**
     * TelecommandPacket constructor
     * 
     * @param header
     *            packet header
     * @param data
     *            packet data
     */
    public TelecommandPacket(final ISpacePacketHeader header, final byte[] data) {
        this.header = header;
        this.data = data;
        

        // Now set the correct checksum value into the field
        data[header.getPrimaryHeaderLength() + 1] = computeChecksum();

        // Create TC segment
        final byte[] segment = new byte[data.length + 1];
        segment[0] = (byte) 0xC0;
        System.arraycopy(data, 0, segment, 1, data.length);

        allData = new byte[header.getBytes().length + data.length];
        System.arraycopy(header.getBytes(), 0, allData, 0, header.getBytes().length);
        System.arraycopy(data, 0, allData, header.getBytes().length, data.length);

    }
    


    @Override
    public byte computeChecksum() {
        // Perform the checksum on the entire packet, with checksum field initialized to 0
        byte checksum = (byte) 0xFF;
        for (final byte b : data) {
            checksum ^= b;
        }
        return checksum;
    }

    @Override
    public byte[] getBytes() {
        return allData;
    }

    @Override
    public byte[] getHeaderBytes() {
        return header.getBytes();
    }

}
