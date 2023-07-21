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

import jpl.gds.ccsds.api.cfdp.CfdpTlv;
import jpl.gds.ccsds.api.cfdp.FileDirectiveConditionCode;
import jpl.gds.ccsds.api.cfdp.ICfdpEofPdu;
import jpl.gds.ccsds.api.cfdp.ICfdpPduHeader;
import jpl.gds.shared.gdr.GDR;

/**
 * CFDP End-of-File PDUs are File Directive PDUs that are sent to signal all File PDUs for a transation have been
 * transmitted.
 * 
 */
class CfdpEofPdu extends CfdpFileDirectivePdu implements
        ICfdpEofPdu {

    // PDU object needs to be corruptible after it's instantiated, so save the valid header data
    private FileDirectiveConditionCode conditionCode;
    private final long fileSize;
    private final long fileChecksum;

    /**
     * Constructor that requires an ICfdpPduHeader object and the data
     * 
     * @param header
     *            a valid ICfdpPduHeader object
     * @param pduData
     *            the complete PDU data, including header
     */
    protected CfdpEofPdu(final ICfdpPduHeader header, final byte[] pduData) {
        super(header, pduData);
        if (pduData.length < header.getHeaderLength() + 9) {
            throw new IllegalArgumentException("Data field supplied for EOF PDU is not long enough");
        }

        // Cache the header data
        final int val = GDR.get_u8(data, header.getHeaderLength() + 1) >>> 4;
        for (final FileDirectiveConditionCode code: FileDirectiveConditionCode.values()) {
            if (code.getBinaryValue() == val) {
                this.conditionCode = code;
            }
        }

        if (this.conditionCode == null) {
            throw new IllegalStateException("File directive condition code in PDU does not match any known directive code");
        }

        this.fileSize = GDR.get_u32(data,  header.getHeaderLength() + 6);
        this.fileChecksum = GDR.get_u32(data,  header.getHeaderLength() + 2);
    }

    @Override
    public FileDirectiveConditionCode getConditionCode() {
        return conditionCode;
    }
    

    @Override
    public long getFileSize() {
        return fileSize;
    }


    @Override
    public long getFileChecksum() {
        return fileChecksum;
    }


    @Override
    public CfdpTlv getFaultLocation() {
        // TODO Auto-generated method stub
        return null;
    }

    
    @Override
    public String toString() {
        return "CFDP EOF PDU: " + super.toString() + ", condition code=" + getConditionCode() + ", file size=" + getFileSize() + ", file checksum=" + getFileChecksum();
    }

}
