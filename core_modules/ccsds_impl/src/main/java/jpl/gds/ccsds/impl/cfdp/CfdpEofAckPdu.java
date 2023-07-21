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

import jpl.gds.ccsds.api.cfdp.ICfdpEofAckPdu;
import jpl.gds.ccsds.api.cfdp.ICfdpPduHeader;

/**
 * {@code CfdpEofAckPdu} is a CFDP Acknowledgement PDU for an EOF.
 *
 * @since 8.0.1
 */
public class CfdpEofAckPdu extends CfdpFileDirectivePdu implements
        ICfdpEofAckPdu {

    /**
     * Constructor that requires an ICfdpPduHeader object and the data
     *
     * @param header  a valid ICfdpPduHeader object
     * @param pduData the complete PDU data, including header
     */
    protected CfdpEofAckPdu(final ICfdpPduHeader header, final byte[] pduData) {
        super(header, pduData);
        if (pduData.length < header.getHeaderLength() + 3) {
            throw new IllegalArgumentException("Data field supplied for ACK (for EOF) PDU is not long enough");
        }
    }

    @Override
    public String toString() {
        return "CFDP ACK(EOF) PDU: " + super.toString();
    }

}
