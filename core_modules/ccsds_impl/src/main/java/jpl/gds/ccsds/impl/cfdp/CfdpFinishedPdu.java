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

import java.util.List;

/**
 * CFDP End-of-File PDUs are File Directive PDUs that are sent to signal all File PDUs for a transation have been
 * transmitted.
 *
 */
class CfdpFinishedPdu extends CfdpFileDirectivePdu implements
        ICfdpFinishedPdu {

    // PDU object needs to be corruptible after it's instantiated, so save the valid header data
    private FileDirectiveConditionCode conditionCode;
    private final CfdpEndSystemStatus endSystemStatus;
    private final CfdpDeliveryCode deliveryCode;
    private final CfdpFileStatus fileStatus;

    /**
     * Constructor that requires an ICfdpPduHeader object and the data
     *
     * @param header  a valid ICfdpPduHeader object
     * @param pduData the complete PDU data, including header
     */
    protected CfdpFinishedPdu(final ICfdpPduHeader header, final byte[] pduData) {
        super(header, pduData);
        if (pduData.length < header.getHeaderLength() + 1) {
            throw new IllegalArgumentException("Data field supplied for Finished PDU is not long enough");
        }

        // Cache the header data
        int val = GDR.get_u8(data, header.getHeaderLength() + 1) >>> 4;
        for (final FileDirectiveConditionCode code : FileDirectiveConditionCode.values()) {
            if (code.getBinaryValue() == val) {
                this.conditionCode = code;
            }
        }

        if (this.conditionCode == null) {
            throw new IllegalStateException("File directive condition code in PDU does not match any known directive code");
        }

        val = (GDR.get_u8(data, header.getHeaderLength() + 1) >>> 3) & 0x1;
        this.endSystemStatus = CfdpEndSystemStatus.values()[val];

        val = (GDR.get_u8(data, header.getHeaderLength() + 1) >>> 2) & 0x1;
        this.deliveryCode = CfdpDeliveryCode.values()[val];

        val = GDR.get_u8(data, header.getHeaderLength() + 1) & 0x3;
        this.fileStatus = CfdpFileStatus.values()[val];
    }

    @Override
    public FileDirectiveConditionCode getConditionCode() {
        return conditionCode;
    }

    /**
     * Gets the end system status from the Finished PDU.
     *
     * @return the end system status
     */
    @Override
    public CfdpEndSystemStatus getEndSystemStatus() {
        return endSystemStatus;
    }

    /**
     * Gets the delivery code from the Finished PDU.
     *
     * @return the delivery code
     */
    @Override
    public CfdpDeliveryCode getDeliveryCode() {
        return deliveryCode;
    }

    /**
     * Gets the file status from the Finished PDU.
     *
     * @return the delivery code
     */
    @Override
    public CfdpFileStatus getFileStatus() {
        return fileStatus;
    }

    /**
     * Gets the filestore responses from the Finished PDU.
     *
     * @return filestore responses
     */
    @Override
    public List<CfdpTlv> getFilestoreResponses() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CfdpTlv getFaultLocation() {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public String toString() {
        return "CFDP Finished PDU: " + super.toString() + ", condition code=" + getConditionCode()
                + ", end system status=" + getEndSystemStatus() + ", delivery code=" + getDeliveryCode()
                + ", file status=" + getFileStatus();
    }

}
