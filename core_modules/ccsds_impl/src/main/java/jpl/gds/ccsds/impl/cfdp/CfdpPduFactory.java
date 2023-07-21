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

import static jpl.gds.ccsds.api.cfdp.FileDirectiveCode.EOF;
import static jpl.gds.ccsds.api.cfdp.FileDirectiveCode.FINISHED;

/**
 * A factory for CFDP PDU objects.
 *
 * @since R8
 */
public final class CfdpPduFactory implements ICfdpPduFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public ICfdpPdu createPdu(final ICfdpPduHeader header, final byte[] pduData) {

        if (header.getType() == CfdpPduType.FILE) {
            return new CfdpFileDataPdu(header, pduData);
        } else {
            final FileDirectiveCode directiveType = ICfdpFileDirectivePdu.getDirectiveCode(pduData, header.getHeaderLength());
            switch (directiveType) {
                case EOF:
                    return new CfdpEofPdu(header, pduData);
                case METADATA:
                    return new CfdpMetadataPdu(header, pduData);
                case FINISHED:
                    return new CfdpFinishedPdu(header, pduData);
                case ACK:
                    FileDirectiveCode directiveAcked = ICfdpAckPdu.getAcknowledgedDirectiveCode(pduData, header.getHeaderLength());

                    if (directiveAcked == EOF) {
                        return new CfdpEofAckPdu(header, pduData);
                    } else if (directiveAcked == FINISHED) {
                        return new CfdpFinAckPdu(header, pduData);
                    }

                case NAK:
                    return new CfdpNakPdu(header, pduData);
                case KEEP_ALIVE:
                case PROMPT:
                    // Quickly add support to these remaining File Directive PDU types
                    return new CfdpFileDirectivePdu(header, pduData);
                default:
                    throw new UnsupportedOperationException("Unsupported PDU type: " + directiveType.name());

            }
        }

    }

    @Override
    public ICfdpPdu createPdu(final byte[] pduData) {
        final ICfdpPduHeader header = createPduHeader();
        header.load(pduData, 0);

        return createPdu(header, pduData);
    }

    @Override
    public ICfdpPduHeader createPduHeader() {
        return new CfdpPduHeader();
    }

}
