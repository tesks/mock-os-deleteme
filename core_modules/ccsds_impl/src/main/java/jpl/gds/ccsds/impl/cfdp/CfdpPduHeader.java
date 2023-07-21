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
package jpl.gds.ccsds.impl.cfdp;

import jpl.gds.ccsds.api.cfdp.CfdpPduDirection;
import jpl.gds.ccsds.api.cfdp.CfdpPduType;
import jpl.gds.ccsds.api.cfdp.CfdpTransmissionMode;
import jpl.gds.ccsds.api.cfdp.ICfdpPduHeader;
import jpl.gds.shared.gdr.GDR;

/**
 * The standard representation of CFDP PDU header.
 * The original header bytes is maintained and all values are retrieved
 * from it.
 * 
 * @since R8
 *
 */
class CfdpPduHeader implements ICfdpPduHeader {
    
    private byte[] headerBytes;

    protected CfdpPduHeader() {
    }

    @Override
    public int getVersion() {
        return GDR.cbits_to_u8(headerBytes, 0, 0, 3);
    }

    @Override
    public CfdpPduType getType() {
        final int type = GDR.cbits_to_u8(headerBytes, 0, 3, 1);
        if (type == 1) {
            return CfdpPduType.FILE;
        } else {
            return CfdpPduType.DIRECTIVE;
        }
    }

    @Override
    public CfdpPduDirection getDirection() {
        final int dir = GDR.cbits_to_u8(headerBytes, 0, 4, 1);
        if (dir == 1) {
            return CfdpPduDirection.TOWARD_SENDER;
        } else {
            return CfdpPduDirection.TOWARD_RECEIVER;
        }
    }

    @Override
    public CfdpTransmissionMode getTransmissionMode() {
        final int mode = GDR.cbits_to_u8(headerBytes, 0, 5, 1);
        if (mode == 1) {
            return CfdpTransmissionMode.UNACKNOWLEDGED;
        } else {
            return CfdpTransmissionMode.ACKNOWLEDGED;
        }
    }

    @Override
    public boolean hasCrc() {
        return GDR.cbits_to_u8(headerBytes, 0, 6, 1) == 1;
    }

    @Override
    public int getDataLength() {
        return GDR.get_u16(headerBytes, 1);
    }

    @Override
    public int getTransactionSequenceLength() {
        return GDR.cbits_to_u8(headerBytes, 3, 5, 3) + 1;
    }
    
 
    @Override
    public long getTransactionSequenceNumber() {
        final int startOffset = FIXED_PDU_HEADER_LENGTH + getEntityIdLength();
        if (startOffset + getTransactionSequenceLength() > headerBytes.length) {
            throw new IllegalStateException("Attempting to fetch PDU transaction number with inadequate number of bytes in the buffer");
        }
        return extractAnySizeLong(startOffset,  getTransactionSequenceLength());
    }

    @Override
    public int getEntityIdLength() {
        return GDR.cbits_to_u8(headerBytes, 3, 1, 3) + 1;
    }

    @Override
    public long getSourceEntityId() {
        final int startOffset = FIXED_PDU_HEADER_LENGTH;
        if (startOffset + getEntityIdLength() > headerBytes.length) {
            throw new IllegalStateException("Attempting to fetch PDU source entity ID with inadequate number of bytes in the buffer");
        }
        return extractAnySizeLong(startOffset, getEntityIdLength());
    }

    @Override
    public long getDestinationEntityId() {
        final int startOffset = FIXED_PDU_HEADER_LENGTH + getEntityIdLength() + getTransactionSequenceLength();
        if (startOffset + getEntityIdLength() > headerBytes.length) {
            throw new IllegalStateException("Attempting to fetch PDU destination entity ID with inadequate number of bytes in the buffer");
        }
        return extractAnySizeLong(startOffset, getEntityIdLength());
    }

    @Override
    public int getHeaderLength() {
        if (headerBytes == null) {
            throw new IllegalArgumentException("Attempting to get PDU header length before loading the header");
        }
        return FIXED_PDU_HEADER_LENGTH + getEntityIdLength() * 2 + getTransactionSequenceLength();

    }

    @Override
    public boolean isValid() {
        return headerBytes != null && headerBytes.length == getHeaderLength() && getVersion() == 0;
    }

    @Override
    public int loadFixedHeader(final byte[] buffer, final int offset) {
        if (buffer.length - offset < ICfdpPduHeader.FIXED_PDU_HEADER_LENGTH) {
            throw new IllegalArgumentException("Attempting to load fixed PDU header without enough bytes");
        }
        headerBytes = new byte[ICfdpPduHeader.FIXED_PDU_HEADER_LENGTH];
        System.arraycopy(buffer, offset, headerBytes, 0,ICfdpPduHeader.FIXED_PDU_HEADER_LENGTH);
 
        return offset + ICfdpPduHeader.FIXED_PDU_HEADER_LENGTH;
    }
    

    @Override
    public int load(final byte[] buffer, final int offset) {
        if (headerBytes == null || headerBytes.length == 0) {
            loadFixedHeader(buffer, offset);
        }
        final int headerLength = getHeaderLength();
        if (buffer.length - offset < headerLength) {
            throw new IllegalArgumentException("Attempting to load full PDU header without enough bytes");
        }
            
        headerBytes = new byte[headerLength];
        System.arraycopy(buffer, offset, headerBytes, 0, headerLength);
 
        return offset + getHeaderLength();
    }

    private long extractAnySizeLong(final int startOffset, final int size) {
        switch (size) {
        case 1: return GDR.get_u8(headerBytes, startOffset);
        case 2: return GDR.get_u16(headerBytes, startOffset);
        case 3: return GDR.get_u24(headerBytes, startOffset);
        case 4: return GDR.get_u32(headerBytes, startOffset);
        case 5: 
        case 6: 
        case 7: {
            final byte[] temp = new byte[8];
            System.arraycopy(headerBytes, startOffset, temp, 8 - size, size);
            return GDR.get_u64(temp, 0);
        }
        case 8: return GDR.get_u64(headerBytes, startOffset);
        default:
            throw new IllegalStateException("Illegal long field length in PDU header");
        }
    }
    
    @Override
    public String toString() {
        return "Version=" + getVersion() + 
                ", PDU type=" + getType() +
                ", data length=" + getDataLength() +
                ", direction=" + getDirection() +
                ", transmission mode=" + getTransmissionMode() +
                ", crc flag=" + hasCrc() + 
                ", entity ID length=" + getEntityIdLength() +
                ", transaction sequence length=" + getTransactionSequenceLength() +
                (headerBytes.length > FIXED_PDU_HEADER_LENGTH ?
                (", transaction sequence=" + Long.toUnsignedString(getTransactionSequenceNumber()) +
                ", source entity ID=" + getSourceEntityId() +
                ", destination entity ID=" + getDestinationEntityId()) : "");
    }
   
}
