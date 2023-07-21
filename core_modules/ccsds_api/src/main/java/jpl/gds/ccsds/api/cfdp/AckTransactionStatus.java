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

/**
 * An enumeration for the value of the "transaction status" field in Ack PDU.
 *
 * @since 8.9
 */
public enum AckTransactionStatus {
    UNDEFINED(0),
    ACTIVE(1),
    TERMINATED(2),
    UNRECOGNIZED(3);

    private int binaryValue;

    private AckTransactionStatus(int pduValue) {
        this.binaryValue = pduValue;
    }

    /**
     * Gets the value the represents this condition in binary PDU headers.
     *
     * @return PDU header value
     */
    public int getBinaryValue() {
        return this.binaryValue;
    }

    /**
     * A static utility method to get the transaction status from ack PDU bit value
     * @param val pdu int value of ack transaction status
     * @return
     */
    public static AckTransactionStatus getAckTransactionStatus(int val) {
        if (AckTransactionStatus.UNDEFINED.getBinaryValue() == val) {
            return AckTransactionStatus.UNDEFINED;
        } else if (AckTransactionStatus.ACTIVE.getBinaryValue() == val) {
            return AckTransactionStatus.ACTIVE;
        } else if (AckTransactionStatus.TERMINATED.getBinaryValue() == val) {
            return AckTransactionStatus.TERMINATED;
        } else if (AckTransactionStatus.UNRECOGNIZED.getBinaryValue() == val) {
            return AckTransactionStatus.UNRECOGNIZED;
        } else {
            throw new IllegalStateException("ACK PDU had unexpected 2-bit transaction status: " + val);
        }
    }
}
