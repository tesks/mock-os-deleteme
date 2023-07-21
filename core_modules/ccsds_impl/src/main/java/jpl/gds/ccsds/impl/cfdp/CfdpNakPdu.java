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

import jpl.gds.ccsds.api.cfdp.*;
import jpl.gds.shared.gdr.GDR;

import javax.swing.text.Segment;
import java.util.LinkedList;
import java.util.List;

/**
 * {@code CfdpNakPdu} represents a CFDP NAK PDU.
 *
 */
class CfdpNakPdu extends CfdpFileDirectivePdu implements
        ICfdpNakPdu {
    private final long startOfScope;
    private final long endOfScope;
    private final List<SegmentRequest> segmentRequests = new LinkedList<>();

    /**
     * Constructor that requires an ICfdpPduHeader object and the data
     *
     * @param header  a valid ICfdpPduHeader object
     * @param pduData the complete PDU data, including header
     */
    protected CfdpNakPdu(final ICfdpPduHeader header, final byte[] pduData) {
        super(header, pduData);

        // See CCSDS 727.0-B-4 Table 5-10
        if (pduData.length < header.getHeaderLength() + 9) {
            throw new IllegalArgumentException("Data field supplied for NAK PDU is not long enough");
        }

        startOfScope = GDR.get_u32(pduData, header.getHeaderLength() + 1);
        endOfScope = GDR.get_u32(pduData, header.getHeaderLength() + 5);

        int remainingSegmentRequestsBytes = pduData.length - (header.getHeaderLength() + 9);

        while (remainingSegmentRequestsBytes >= 8) {
            segmentRequests.add(new SegmentRequest(GDR.get_u32(pduData, pduData.length - remainingSegmentRequestsBytes),
                    GDR.get_u32(pduData, pduData.length - remainingSegmentRequestsBytes + 4)));
            remainingSegmentRequestsBytes -= 8;
        }

    }

    @Override
    public String toString() {
        return "CFDP NAK PDU: " + super.toString() + ", start of scope=" + getStartOfScope()
                + ", end of scope=" + getEndOfScope() + ", segment requests=" + getSegmentRequests();
    }

    /**
     * Retrieve the start of scope
     *
     * @return start of scope offset
     */
    @Override
    public long getStartOfScope() {
        return startOfScope;
    }

    /**
     * Retrieve the end of scope
     *
     * @return end of scope offset
     */
    @Override
    public long getEndOfScope() {
        return endOfScope;
    }

    @Override
    public List<SegmentRequest> getSegmentRequests() {
        return segmentRequests;
    }
}
