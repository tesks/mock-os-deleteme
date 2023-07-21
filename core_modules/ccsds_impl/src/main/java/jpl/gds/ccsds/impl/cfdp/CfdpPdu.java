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

import jpl.gds.ccsds.api.cfdp.ICfdpPdu;
import jpl.gds.ccsds.api.cfdp.ICfdpPduHeader;

/**
 * The standard representation of CFDP PDUs.
 * 
 *
 * @since R8
 */
class CfdpPdu implements ICfdpPdu {
    
    protected ICfdpPduHeader header;
    protected byte[] data;

    protected CfdpPdu(final ICfdpPduHeader header, final byte[] pduData) {
        if (header == null) {
            throw new IllegalArgumentException("PDU header may not be null");
        }
        if (pduData == null) {
            throw new IllegalArgumentException("PDU data may not be null");
        }
        if (pduData.length < header.getHeaderLength()) {
            throw new IllegalArgumentException("Data field supplied for general PDU is not long enough");
        }
        this.header = header;
        this.data = new byte[header.getHeaderLength() + header.getDataLength()];
        System.arraycopy(pduData, 0, data, 0, data.length);
    }

    @Override
    public ICfdpPduHeader getHeader() {
        return header;
    }

    @Override
    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return header.toString();
    }

    /**
     * Sets the PDU data. Only sets the data without altering the existing header data in the object.
     *
     * @param pduData PDU data to set
     */
    @Override
    public void setData(final byte[] pduData) {
        this.data = pduData;
    }

}
