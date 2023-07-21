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


import org.springframework.context.ApplicationContext;

import jpl.gds.ccsds.api.config.CcsdsProperties;
import jpl.gds.ccsds.api.packet.ISpacePacketHeader;
import jpl.gds.ccsds.api.packet.PacketHeaderFactory;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.tc.api.ITelecommandPacket;
import jpl.gds.tc.api.packet.ITelecommandPacketFactory;

/**
 * Basic implementation of a Telecommand Packet builder.
 * 
 *
 */
public class TelecommandPacketBuilder implements ITelecommandPacketFactory {

    private final CcsdsProperties ccsdsProperties;
    private final Tracer          trace;

    /**
     * @param appContext
     */
    public TelecommandPacketBuilder(final ApplicationContext appContext) {
        ccsdsProperties = appContext.getBean(CcsdsProperties.class);
        trace = TraceManager.getTracer(appContext, Loggers.UPLINK);
    }


    @Override
    public ITelecommandPacket buildPacketFromPdu(final byte[] pdu, final int apid) {
        final ISpacePacketHeader header = buildPacketHeader(pdu.length, apid, 0); // no seq count

        return buildPacketFromHeader(header, pdu);

    }

    @Override
    public ITelecommandPacket buildPacketFromHeader(final ISpacePacketHeader header, final byte[] data) {
        // +1 below because packet data length field contains "less one" of the actual data length
        final byte[] packet = new byte[header.getPrimaryHeaderLength() + header.getPacketDataLength() + 1];
        trace.debug("Creating packet of size ", packet.length);

        // First, copy the primary header to the packet byte array
        System.arraycopy(header.getBytes(), 0, packet, 0, header.getPrimaryHeaderLength());

        // Now determine the secondary header data
        // First byte of secondary header: Setting Criticality to 0 and Function
        // Code to 0 (Deane Sibol confirmed that FC is 0 for PDUs)
        packet[header.getPrimaryHeaderLength()] = (byte) 0;

        // Second byte of secondary header is the checksum
        // For the later checksum calculation, first initialize the checksum field with 0
        packet[header.getPrimaryHeaderLength() + 1] = (byte) 0;

        // Now copy the data into the packet
        System.arraycopy(data, 0, packet, header.getPrimaryHeaderLength() + 2, data.length);
        trace.debug("Copied ", data.length, " bytes of pdu data into the packet");

        return new TelecommandPacket(header, packet);
    }

    @Override
    public ISpacePacketHeader buildPacketHeader(final int dataLength, final int apid, final int sequenceCount) {
        final ISpacePacketHeader header = PacketHeaderFactory.create(ccsdsProperties.getPacketHeaderFormat());

        header.setVersionNumber((byte) 0);
        header.setPacketType((byte) 1);
        header.setSecondaryHeaderFlag((byte) 1);
        header.setApid((short) apid); // 0x4FF (1279) for CFDP PDUs
        header.setGroupingFlags((byte) 3);
        // TODO: Do I pass in packet sequence counter?
        header.setSourceSequenceCount((short) (sequenceCount % 16384)); // 0 should be replaced with packet sequence


        // Determine packet data length (packet data length = CFDP PDU length + secondary header 2 bytes - 1)
        header.setPacketDataLength(dataLength + 1);
        trace.debug("Created packet header ", header);

        return header;
    }

}
