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

import jpl.gds.ccsds.api.cfdp.FileDirectiveCode;
import jpl.gds.ccsds.api.cfdp.ICfdpFileDirectivePdu;
import jpl.gds.ccsds.api.cfdp.ICfdpPduHeader;

/**
 * CFDP File Directive PDUs are a generic PDU class and are never actually created. Currently the only two
 * implementations are metadata and end of file PDUs.
 *
 *         TODO: extend with Prompt PDU and Keep Alive PDU
 *
 */
class CfdpFileDirectivePdu extends CfdpPdu implements ICfdpFileDirectivePdu {

    /* To allow PDUs to be corrupted after instantiation, save the directive code separately rather than
    parsing it all the time */
    private final FileDirectiveCode directiveCode;

    /**
     * Constructor
     *
     * @param header
     *            a valid ICfdpPduHeader object
     * @param pduData
     *            the complete PDU data, including header
     */
    protected CfdpFileDirectivePdu(final ICfdpPduHeader header, final byte[] pduData) {
        super(header, pduData);
        if (pduData.length < header.getHeaderLength() + 2) {
            throw new IllegalArgumentException("Data field supplied for file directive PDU is not long enough");
        }

        this.directiveCode = ICfdpFileDirectivePdu.getDirectiveCode(data, header.getHeaderLength());
    }

    @Override
    public FileDirectiveCode getDirectiveCode() {
        return directiveCode;
    }

    @Override
    public String toString() {
        return super.toString() + ", directive code=" + getDirectiveCode();
    }

}
