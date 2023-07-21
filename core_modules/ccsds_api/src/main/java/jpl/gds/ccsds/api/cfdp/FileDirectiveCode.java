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
 * An enumeration of CFDP file directive codes.
 * 
 * @since R8
 *
 */
public enum FileDirectiveCode {
	/** Directive is an EOF */
    EOF(4),
	/** Directive indicates transfer is finished */
    FINISHED(5),
    /** Directive is an acknowledgment */
    ACK(6),
    /** Directive is metadata */
    METADATA(7),
    /** Directive is a non-acknowledgment */
    NAK(8),
    /** Directive is a user prompt */
    PROMPT(9),
    /** Directive is a network keep-alive */
    KEEP_ALIVE(10);
    
    private int binaryValue;
    
    private FileDirectiveCode(int pduValue) {
        this.binaryValue = pduValue;
    }
    
    /**
     * Gets the value found in the PDU header that indicates this type of directive.
     * @return PDU header value
     */
    public int getBinaryValue() {
        return this.binaryValue;
    }
    
}
