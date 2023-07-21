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
package jpl.gds.ccsds.api.cfdp;


import jpl.gds.shared.gdr.GDR;

/**
 * An interface to be implemented by CFDP ACK directive PDUs.
 *
 * @since R8
 */
public interface ICfdpAckPdu extends ICfdpFileDirectivePdu {

    /**
     * A static utility method that gets the 4-bit directive code of the acknowledged PDU.
     *
     * @param buffer the byte buffer containing the PDU header data.
     * @param offset the starting offset of the header in the buffer
     * @return FileDirectiveCode
     */
    public static FileDirectiveCode getAcknowledgedDirectiveCode(byte[] buffer, int offset) {
        // See Table 5-8 of CCSDS 727.0-B-4 (CFDP Blue Book)
        final int val = GDR.get_u8(buffer, offset + 1) >>> 4;

        if (FileDirectiveCode.EOF.getBinaryValue() == val) {
            return FileDirectiveCode.EOF;
        } else if (FileDirectiveCode.FINISHED.getBinaryValue() == val) {
            return FileDirectiveCode.FINISHED;
        } else {
            throw new IllegalStateException("ACK PDU had unexpected 4-bit directive code: " + val);
        }

    }

    /**
     * A static utility method to get the 2-bit transaction status of the ack PDU.
     *
     * @param buffer the byte buffer containing the PDU header data
     * @param offset the starting offset of the header in the buffer
     * @return AckTransactionStatus
     */
    public static AckTransactionStatus getAcknowledgedTransactionStatus(byte[] buffer, int offset) {
        // See Table 5-8 of CCSDS 727.0-B-5 (CFDP Blue Book)
        final int val = GDR.get_u8(buffer, offset + 2) & 0x03;
        return AckTransactionStatus.getAckTransactionStatus(val);
    }

}