/*
 * Copyright 2006-2019. California Institute of Technology.
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
package jpl.gds.tc.impl.frame;

import jpl.gds.tc.api.ITcTransferFrame;
import jpl.gds.tc.api.frame.ITcTransferFrameSerializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * The common code for all TcTransferFrameSerializers
 *
 */
public abstract class AbstractTcTransferFrameSerializer implements ITcTransferFrameSerializer {

    private static final int                    BITMASK_VERSION_NUMBER         = 0b00000011;
    private static final int                    BITMASK_BYPASS_FLAG            = 0b00000001;
    private static final int                    BITMASK_CTRL_CMD_FLAG          = 0b00000001;
    private static final int                    BITMASK_SPARE                  = 0b00000011;
    private static final int                    BITMASK_SCID_FIRST_BYTE        = 0b00000011;
    private static final int                    BITMASK_LENGTH_FIRST_BYTE      = 0b00000011;
    private static final int                    BITMASK_VCID                   = 0b00111111;

    @Override
    public byte[] getBytes(ITcTransferFrame tcFrame) {
        if (tcFrame.getData() == null) {
            throw new IllegalStateException("Frame data is null.");
        }
        int length = calculateLength(tcFrame) + 1;
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream(length)) {
            baos.write(getHeaderBytes(tcFrame));
            baos.write(tcFrame.getData());
            if (tcFrame.hasFecf()) {
                baos.write(tcFrame.getFecf());
            }
            return baos.toByteArray();

        } catch (final IOException e) {
            // do nothing
            return null;
        }


    }

    @Override
    public byte[] getHeaderBytes(ITcTransferFrame tcFrame) {
        final byte[] header = new byte[ITcTransferFrameSerializer.TOTAL_HEADER_BYTE_LENGTH];

        header[0] |= (tcFrame.getVersionNumber() & BITMASK_VERSION_NUMBER) << 6; // bits 1 and 2
        header[0] |= (tcFrame.getBypassFlag() & BITMASK_BYPASS_FLAG) << 5; // bit 3
        header[0] |= (tcFrame.getControlCommandFlag() & BITMASK_CTRL_CMD_FLAG) << 4; // bit 4
        header[0] |= (tcFrame.getSpare() & BITMASK_SPARE) << 2; // bit 5 and 6
        final byte[] headerScid = ByteBuffer.allocate(4).putInt(tcFrame.getSpacecraftId()).array();

        header[0] |= (headerScid[2] & BITMASK_SCID_FIRST_BYTE); // bit 7 and 8

        header[1] = headerScid[3]; // byte

        header[2] |= (tcFrame.getVirtualChannelId() & BITMASK_VCID) << 2; // bits 1-6
        final byte[] headerFrameLength = ByteBuffer.allocate(4).putInt(tcFrame.getLength()).array();
        header[2] |= (headerFrameLength[2] & BITMASK_LENGTH_FIRST_BYTE); // bits 7 and 8
        header[3] = headerFrameLength[3]; // byte

        header[4] = ByteBuffer.allocate(4).putInt(tcFrame.getSequenceNumber()).array()[3]; // byte

        return header;
    }

    @Override
    public short calculateLength(ITcTransferFrame tcFrame) {
        if (tcFrame.getData() == null) {
            throw new IllegalStateException("Can't calculate frame length, data is null.");
        }

        int length = getHeaderBytes(tcFrame).length + tcFrame.getData().length;

        if (tcFrame.hasFecf()) {
            length += tcFrame.getFecf().length;
        }

        return (short) (length - 1);
    }
}
