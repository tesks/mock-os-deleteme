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

import jpl.gds.ccsds.api.cfdp.ICfdpFileDataPdu;
import jpl.gds.ccsds.api.cfdp.ICfdpPduHeader;
import jpl.gds.shared.gdr.GDR;

class CfdpFileDataPdu extends CfdpPdu implements ICfdpFileDataPdu {

    // PDU object needs to be corruptible after it's instantiated, so save the valid header data
    private final long offset;

    protected CfdpFileDataPdu(ICfdpPduHeader header, byte[] pduData) {
        super(header, pduData);
        if (pduData.length < header.getHeaderLength() + header.getDataLength()) {
            throw new IllegalArgumentException("Data field supplied for file data PDU is not long enough");
        }

        // Cache the header data
        this.offset = GDR.get_u32(data, header.getHeaderLength());
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public String toString() {
        return "CFDP FILE DATA PDU: " + super.toString() + ", offset=" + getOffset();
    }
 

}
