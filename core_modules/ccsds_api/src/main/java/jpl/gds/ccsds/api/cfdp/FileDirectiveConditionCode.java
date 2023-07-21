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
 * An enumeration of CFDP file directive condition codes.
 * 
 * @since R8
 *
 */
public enum FileDirectiveConditionCode {
	/** No error condition */
    NO_ERROR(0),
    /** Positive acknowledgment limit reached condition */
    POSITIVE_ACK_LIMIT_REACHED(1),
    /** Keep alive limit reached condition */
    KEEP_ALIVE_LIMIT_REACHED(2),
    /** Invalid transmission mode condition */
    INVALID_TRANSMISSION_MODE(3),
    /** Filestore rejection condition */
    FILESTORE_REJECTION(4),
    /** File checksum failure condition */
    FILE_CHECKSUM_FAILURE(5),
    /** File size error condition */
    FILE_SIZE_ERROR(6),
    /** Negative acknowledgment limit reached condition */
    NAK_LIMIT_REACHED(7),
    /** Inactivity detected condition */
    INACTIVITY_DETECTED(8),
    /** Invalid file structure condition */
    INVALID_FILE_STRUCTURE(9),
    /** Check limit reached condition */
    CHECK_LIMIT_REACHED(10),
    /** Suspend request received condition */
    SUSPEND_REQUEST_RECEIVED(14),
    /** Cancel request received condition */
    CANCEL_REQUEST_RECEIVED(15);
    
    private int binaryValue;
    
    private FileDirectiveConditionCode(int pduValue) {
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
}
